"""Pruebas con dobles de AT-SPI; no certifican el POS ni una sesión Linux real."""
import importlib.util
import types
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("probe", Path(__file__).parents[1] / "src/main/resources/bridge/atspi_probe.py")
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)

class FakeNode:
    def __init__(self, name="POS", role="application", text=None, children=None, value=None):
        self.name, self.role, self.text = name, role, text
        self.children, self.value = children or [], value
    def get_name(self): return self.name
    def get_role_name(self): return self.role
    def get_accessible_id(self): return self.name
    def get_description(self): return ""
    def get_child_count(self): return len(self.children)
    def get_child_at_index(self, index): return self.children[index]
    def get_process_id(self): return 123
    def get_toolkit_name(self): return "mock-gtk"
    def get_state_set(self): return types.SimpleNamespace(contains=lambda state: False)
    def get_text_iface(self): return self if self.text is not None else None
    def get_value_iface(self): return self if self.value is not None else None
    def get_action_iface(self): return None

class FakeAtspi:
    StateType = types.SimpleNamespace()
    Text = types.SimpleNamespace(get_character_count=lambda n: len(n.text), get_text=lambda n, start, end: n.text[start:end])
    Value = types.SimpleNamespace(get_current_value=lambda n: n.value)
    def __init__(self, root): self.root = root
    def init(self): return 0
    def exit(self): pass
    def set_timeout(self, *_): pass
    def get_desktop(self, _): return FakeNode("desktop", children=[self.root])

def run(root, **overrides):
    settings = dict(app="pos", pid=0, list_only="", max_nodes=2000, max_depth=25, max_text=2000, timeout=45)
    settings.update(overrides)
    return probe.Probe(FakeAtspi(root), types.SimpleNamespace(**settings)).run()

class ProbeTests(unittest.TestCase):
    def test_reads_name_text_and_numeric_value(self):
        s = run(FakeNode(children=[FakeNode("Total", "text", "125.50", value=125.5)]))
        self.assertTrue(s["ok"])
        self.assertEqual(s["nodes"][1]["text"], "125.50")
        self.assertEqual(s["nodes"][1]["value"], 125.5)
    def test_list_does_not_traverse_app(self):
        s = run(FakeNode(children=[FakeNode("Total")]), list_only="true")
        self.assertEqual(s["nodes"], [])
        self.assertEqual(s["apps"][0]["pid"], 123)
    def test_missing_target_is_not_a_bridge_failure(self):
        s = run(FakeNode(), app="otra")
        self.assertTrue(s["ok"])
        self.assertEqual(s["matchedApps"], 0)
    def test_enforces_node_limit(self):
        s = run(FakeNode(children=[FakeNode("A"), FakeNode("B")]), max_nodes=2)
        self.assertEqual(len(s["nodes"]), 2)
        self.assertTrue(s["truncated"])
    def test_enforces_depth_limit(self):
        s = run(FakeNode(children=[FakeNode("A")]), max_depth=0)
        self.assertEqual(len(s["nodes"]), 1)
        self.assertTrue(s["truncated"])
    def test_bounds_text_and_marks_truncation(self):
        s = run(FakeNode(children=[FakeNode("Total", "text", "125.50")]), max_text=3)
        self.assertEqual(s["nodes"][1]["text"], "125")
        self.assertTrue(s["nodes"][1]["textTruncated"])
    def test_never_reads_password_content(self):
        s = run(FakeNode(children=[FakeNode("Clave", "password text", "secreto")]))
        self.assertTrue(s["nodes"][1]["protectedText"])
        self.assertIsNone(s["nodes"][1]["text"])
    def test_distinguishes_unsupported_from_empty_text(self):
        s = run(FakeNode(children=[FakeNode("A", "text", ""), FakeNode("B", "panel")]))
        self.assertEqual(s["nodes"][1]["text"], "")
        self.assertIsNone(s["nodes"][2]["text"])
    def test_provider_exception_is_reported_and_other_fields_survive(self):
        class Broken(FakeNode):
            def get_description(self): raise RuntimeError("DBus timeout")
        s = run(FakeNode(children=[Broken("Total", "text", "10.00")]))
        self.assertTrue(s["ok"])
        self.assertEqual(s["nodes"][1]["text"], "10.00")
        self.assertIn("DBus timeout", s["nodes"][1]["errors"][0])
    def test_nonfinite_numeric_value_not_written_to_json(self):
        s = run(FakeNode(children=[FakeNode("Total", value=float("nan"))]))
        self.assertIsNone(s["nodes"][1]["value"])
        self.assertTrue(s["nodes"][1]["errors"])
    def test_failed_registry_query_is_not_a_successful_empty_registry(self):
        class BadDesktop(FakeNode):
            def get_child_count(self): raise RuntimeError("sin registro")
        atspi = FakeAtspi(FakeNode())
        atspi.get_desktop = lambda _: BadDesktop()
        settings = types.SimpleNamespace(app="pos", pid=0, list_only="", max_nodes=10, max_depth=5, max_text=100, timeout=45)
        with self.assertRaisesRegex(RuntimeError, "registro"):
            probe.Probe(atspi, settings).run()

if __name__ == "__main__": unittest.main()
