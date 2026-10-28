#!/usr/bin/env python3
"""Control positivo GTK3: cambiar código y pulsar Enter modifica descripción y total."""
import gi
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, GLib

GLib.set_prgname("pos-accessibility-control")
window = Gtk.Window(title="POS - Control de accesibilidad")
window.set_default_size(500, 220)
window.connect("destroy", Gtk.main_quit)
box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=12)
box.set_border_width(20)
window.add(box)
help_label = Gtk.Label(label="Código 1001 o 1002, después Enter (sólo datos ficticios)")
entry = Gtk.Entry()
entry.set_text("1001")
entry.get_accessible().set_name("CodigoProducto")
description = Gtk.Label(label="Café de prueba")
description.get_accessible().set_name("DescripcionProducto")
total = Gtk.Entry()
total.set_editable(False)
total.set_text("125.50")
total.get_accessible().set_name("TotalVenta")
quantity = Gtk.SpinButton.new_with_range(1, 10, 1)
quantity.get_accessible().set_name("Cantidad")

def update(*_):
    second = entry.get_text() == "1002"
    description.set_text("Té de prueba" if second else "Café de prueba")
    total.set_text(f"{(80 if second else 125.5) * quantity.get_value():.2f}")

entry.connect("activate", update)
quantity.connect("value-changed", update)
for widget in (help_label, entry, description, quantity, total):
    box.pack_start(widget, False, False, 0)
window.show_all()
entry.grab_focus()
Gtk.main()
