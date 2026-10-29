# Fuentes de distribución usadas para la compilación experimental

Origen: API pública del Open Build Service de openSUSE, proyecto
`openSUSE:Leap:15.6`, paquete `java-atk-wrapper`, revisión de fuentes
`d5ff2350023b5086f6d3e47644482452`.

URL base: <https://api.opensuse.org/public/source/openSUSE:Leap:15.6/java-atk-wrapper/>

Se conservan la fuente original 0.33.2, la receta y los cinco parches de
distribución. `jaw-gdk.patch` elimina el requisito de GTK 3; se usa GTK 2,
coherente con las bibliotecas cargadas que muestra el registro del cliente.
La fuente sigue la licencia LGPL incluida en el archivo original. El ZIP de
prueba incluye la licencia, autores, fuentes modificadas y datos de compilación.

SHA256 del archivo original:

`b8f685caed7c424babac8b158f51355c772d1e2a0b8a1ce8ced4980edae223e2`

Esta reconstrucción usa la receta pública de Leap 15.6. No se presenta como
compilación oficial de SUSE ni como reproducción exacta del RPM de SLES 15 SP7.
La comprobación de dependencias y la validación en el equipo destino siguen
siendo necesarias. No se descarga ni se cambia Java en el equipo del cliente.
