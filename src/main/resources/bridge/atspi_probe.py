#!/usr/bin/env python3
"""Puente AT-SPI de sólo lectura. No envía teclas, clics ni invoca acciones."""
import argparse
import datetime
import json
import math
import platform
import time
from collections import deque
from pathlib import Path


def empty_snapshot():
    return dict(schemaVersion=1, backend="python", capturedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                ok=False, error=None, truncated=False, matchedApps=0, apps=[], nodes=[], warnings=[])


def safe(errors, label, fn, default=None):
    try:
        return fn()
    except Exception as exc:
        errors.append(f"{label}: {type(exc).__name__}: {exc}")
        return default


class Probe:
    def __init__(self, atspi, args):
        self.atspi, self.args = atspi, args
        self.result = empty_snapshot()
        self.deadline = time.monotonic() + max(1, args.timeout - 3)

    def expired(self):
        if time.monotonic() < self.deadline:
            return False
        self.result["truncated"] = True
        if "Tiempo de exploración agotado" not in self.result["warnings"]:
            self.result["warnings"].append("Tiempo de exploración agotado")
        return True

    def read_node(self, obj, app, path, depth):
        errors = []
        n = dict(path=path, app=app["name"], pid=app["pid"], depth=depth, errors=errors,
                 name=safe(errors, "name", obj.get_name, "") or "",
                 role=safe(errors, "role", obj.get_role_name, "") or "",
                 accessibleId=safe(errors, "accessibleId", lambda: obj.get_accessible_id(), "") or "",
                 description=safe(errors, "description", obj.get_description, "") or "",
                 childCount=safe(errors, "children", obj.get_child_count, -1),
                 text=None, characterCount=None, value=None, textSupported=False, valueSupported=False,
                 protectedText=False, textTruncated=False, states=[], actions=[])
        n["protectedText"] = "password" in n["role"].lower()
        states = safe(errors, "states", obj.get_state_set)
        if states is not None:
            for name in ("ACTIVE", "BUSY", "CHECKED", "DEFUNCT", "EDITABLE", "ENABLED", "FOCUSABLE",
                         "FOCUSED", "SELECTED", "SENSITIVE", "SHOWING", "VISIBLE", "MANAGES_DESCENDANTS", "READ_ONLY"):
                state = getattr(self.atspi.StateType, name, None)
                if state is not None and states.contains(state):
                    n["states"].append(name.lower())
        text = safe(errors, "text.interface", obj.get_text_iface)
        n["textSupported"] = text is not None
        if text is not None and not n["protectedText"]:
            count = safe(errors, "characterCount", lambda: self.atspi.Text.get_character_count(text))
            if count is not None and count >= 0:
                n["characterCount"] = count
                n["textTruncated"] = count > self.args.max_text
                n["text"] = safe(errors, "text", lambda: self.atspi.Text.get_text(text, 0, min(count, self.args.max_text)))
        value = safe(errors, "value.interface", obj.get_value_iface)
        n["valueSupported"] = value is not None
        if value is not None and not n["protectedText"]:
            number = safe(errors, "value", lambda: self.atspi.Value.get_current_value(value))
            if number is not None and math.isfinite(number):
                n["value"] = number
            elif number is not None:
                errors.append("value: valor no finito")
        action = safe(errors, "action.interface", obj.get_action_iface)
        if action is not None:
            count = safe(errors, "actions", lambda: self.atspi.Action.get_n_actions(action), 0)
            for index in range(min(max(count, 0), 16)):
                n["actions"].append(safe(errors, f"action[{index}]", lambda: self.atspi.Action.get_action_name(action, index), ""))
        return n

    def traverse(self, root, app):
        queue = deque([(root, f'app[{app["desktopIndex"]}]', 0)])
        while queue:
            if self.expired() or len(self.result["nodes"]) >= self.args.max_nodes:
                self.result["truncated"] = True
                break
            obj, path, depth = queue.popleft()
            n = self.read_node(obj, app, path, depth)
            self.result["nodes"].append(n)
            count = n["childCount"]
            if depth >= self.args.max_depth:
                if count > 0:
                    self.result["truncated"] = True
                continue
            available = max(0, self.args.max_nodes - len(self.result["nodes"]) - len(queue))
            take = min(max(count, 0), available)
            if take < count:
                self.result["truncated"] = True
            for index in range(take):
                if self.expired():
                    break
                child = safe(n["errors"], f"child[{index}]", lambda: obj.get_child_at_index(index))
                if child is None:
                    self.result["truncated"] = True
                else:
                    queue.append((child, f"{path}/{index}", depth + 1))

    def run(self):
        status = self.atspi.init()
        if status not in (0, 1):
            raise RuntimeError(f"Atspi.init devolvió {status}")
        try:
            self.atspi.set_timeout(800, 1500)
            desktop = self.atspi.get_desktop(0)
            if desktop is None:
                raise RuntimeError("AT-SPI no devolvió escritorio. Comprueba sesión gráfica y D-Bus.")
            count = safe(self.result["warnings"], "desktop.children", desktop.get_child_count, -1)
            if count < 0:
                raise RuntimeError("No se pudo consultar el registro AT-SPI: " + str(self.result["warnings"]))
            for index in range(min(count, 512)):
                if self.expired():
                    break
                root = safe(self.result["warnings"], f"desktop.child[{index}]", lambda: desktop.get_child_at_index(index))
                if root is None:
                    continue
                errors = []
                app = dict(desktopIndex=index, errors=errors,
                           name=safe(errors, "name", root.get_name, "") or "",
                           pid=safe(errors, "pid", root.get_process_id, 0),
                           toolkit=safe(errors, "toolkit", root.get_toolkit_name, "") or "")
                app["selected"] = ((not self.args.app or self.args.app.lower() in app["name"].lower())
                                   and (not self.args.pid or self.args.pid == app["pid"]))
                self.result["apps"].append(app)
                if app["selected"]:
                    self.result["matchedApps"] += 1
                    if not self.args.list_only:
                        self.traverse(root, app)
            if count > 512:
                self.result["truncated"] = True
                self.result["warnings"].append("Más de 512 aplicaciones: inventario limitado")
            if not self.result["apps"]:
                self.result["warnings"].append("Registro vacío: verifica la sesión y la aplicación de control GTK.")
            self.result["ok"] = True
            return self.result
        finally:
            self.atspi.exit()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--result", required=True)
    parser.add_argument("--app", default="")
    parser.add_argument("--pid", type=int, default=0)
    parser.add_argument("--max-nodes", type=int, default=2000)
    parser.add_argument("--max-depth", type=int, default=25)
    parser.add_argument("--max-text", type=int, default=2000)
    parser.add_argument("--timeout", type=int, default=45)
    parser.add_argument("--list-only", default="")
    args = parser.parse_args()
    result = empty_snapshot()
    try:
        if platform.system() != "Linux":
            raise RuntimeError("El puente requiere Linux")
        import gi
        gi.require_version("Atspi", "2.0")
        from gi.repository import Atspi
        result = Probe(Atspi, args).run()
    except Exception as exc:
        result["error"] = f"{type(exc).__name__}: {exc}. En Debian/Ubuntu: python3-gi y gir1.2-atspi-2.0; usa /usr/bin/python3."
    destination = Path(args.result)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    return 0 if result["ok"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
