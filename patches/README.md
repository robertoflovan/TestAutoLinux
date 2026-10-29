# Corrección experimental para Java ATK Wrapper 0.33.2

Estado: biblioteca compilada en un entorno Linux separado mediante GitHub
Actions; validación en el Linux del cliente pendiente. El parche no se aplica
automáticamente y no forma parte del JAR del laboratorio. La reconstrucción
experimental conserva los parches de distribución descritos en
[suse-0.33.2/README.md](suse-0.33.2/README.md).

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

## Compilación fuera del POS

El equipo del cliente carece de compilador y no encuentra las cabeceras de
desarrollo con `pkg-config`. No necesita instalarlas para ejecutar la copia
precompilada. Tener las bibliotecas de ejecución resueltas por `ldd` no implica
tener las cabeceras para compilar.

El workflow `build-atk-bridge.yml` instala herramientas únicamente en el runner
efímero de GitHub. Construye con Ubuntu 22.04 x86_64 y Temurin Java 8, sin
`make install`. Los scripts de compilación/prueba de CI no deben ejecutarse en
el equipo del POS. La JVM de compilación no se incluye en la descarga.

En el cliente, usar sólo el lanzador `probar.sh` del ZIP experimental, con la
ruta explícita a su JDK 8u191. Conservar intactos el puente extraído y el JRE.
No usar `sudo` ni cambiar paquetes, alternativas Java o propiedades globales
de accesibilidad. El lanzador comprueba hashes y dependencias antes de abrir
la ventana de ejemplo.

Se revisaron la receta pública de openSUSE Leap 15.6 y sus parches. No es una
reproducción exacta ni un paquete oficial de SLES 15 SP7; requiere validación en
destino. El parche se aplica a una copia de fuentes, nunca se editan bytes del
`.so` usando posiciones del desensamblado.

## Validación necesaria

En [CI](https://github.com/robertoflovan/TestAutoLinux/actions/runs/40791886977),
el original reprodujo el SIGSEGV tras una lectura correcta; el corregido
superó seis consultas (125.50 y 80.00), sobrevivió a las desconexiones y a una
espera final de 10 segundos. El entorno fue Ubuntu 22.04 / Temurin 8u504.
La [descarga experimental](../experimental/README.md) queda pendiente de
validación en SLES 15 SP7 / Oracle 8u191.

Probar primero el control Swing con la copia corregida: esperar sin consultas,
validar el total inicial, comprobar que sigue vivo después de desconectarse el
lector, cambiar el código y validar el nuevo total. Repetir lecturas y cierres
del lector sin que termine la ventana. Sólo después evaluar la activación en
el POS real. Un `PASS` del laboratorio acredita una lectura, no la estabilidad
posterior del proceso consultado.
