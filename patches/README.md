# Corrección experimental para Java ATK Wrapper 0.33.2

Estado: parche de fuentes preparado y comprobado contra el original de GNOME;
todavía no compilado ni validado en el Linux del cliente. No se aplica
automáticamente y no forma parte del JAR del laboratorio.

## Evidencia y alcance

El control Swing permite leer `TotalVenta = 125.50`, pero se observa que termina
con SIGSEGV tras una consulta. El registro del proceso de ejemplo muestra
`g_int_hash`, dirección inválida `0x1`, con Java 8u191 y el puente local cargado.
El fragmento desensamblado del paquete SUSE 0.33.2-2.39 muestra que se pasa el
identificador como puntero a `g_hash_table_lookup`, coherente con el defecto del
código original. Falta comprobar la estabilidad tras corregirlo; este hallazgo
no descarta otros problemas ni demuestra todavía que CASHIER sea accesible.

En la versión original, `listener_list` usa `g_int_hash`/`g_int_equal` e inserta
direcciones de enteros como claves. Al retirar un registro, en cambio, busca y
elimina con `GINT_TO_POINTER(tmp_idx)`. El parche usa `&tmp_idx` en ambas
operaciones, conservando el tipo de clave esperado y el resto de la lógica.

- [Fuente original 0.33.2, commit f527e3a](https://github.com/GNOME/java-atk-wrapper/blob/f527e3a2d02a93ae022d6af12c9428506506658f/jni/src/jawutil.c)
- [Contrato de g_int_hash](https://docs.gtk.org/glib/func.int_hash.html)
- [GNOME eliminó posteriormente esta implementación](https://github.com/GNOME/java-atk-wrapper/commit/e3353d742715)

Este parche mínimo es una propuesta local; no se presenta como parche oficial
de SUSE ni como sustituto de todas las correcciones posteriores de GNOME.

## Preparación pendiente

Antes de compilar, comprobar las herramientas de construcción y las cabeceras
de las dependencias mediante `pkg-config`. Tener las bibliotecas de ejecución
resueltas por `ldd` no garantiza que estén las cabeceras para compilar.

Construir sólo en un directorio nuevo dentro de `$HOME/pos-atk-prueba`, con la
ruta explícita al JDK 8 existente. Conservar intactos el puente extraído y el JRE.
No ejecutar `make install`, no usar `sudo` y no cambiar paquetes, alternativas
Java ni propiedades globales de accesibilidad. Si faltan herramientas, revisar
las opciones antes de instalar nada.

La fuente exacta del paquete de SUSE y sus parches deben revisarse antes de
sustituir su biblioteca por una reconstrucción. Aplicar el parche a una copia
de fuentes, nunca editar bytes del `.so` usando posiciones del desensamblado.

## Validación necesaria

Probar primero el control Swing con la copia corregida: esperar sin consultas,
validar el total inicial, comprobar que sigue vivo después de desconectarse el
lector, cambiar el código y validar el nuevo total. Repetir lecturas y cierres
del lector sin que termine la ventana. Sólo después evaluar la activación en
el POS real. Un `PASS` del laboratorio acredita una lectura, no la estabilidad
posterior del proceso consultado.
