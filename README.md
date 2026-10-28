# Laboratorio de accesibilidad para un punto de venta en Linux

Proyecto compatible con **Java 8 (1.8) o superior**, desde la versión **1.1.0**, para averiguar si un POS expone **elementos, etiquetas, texto y valores**, antes de automatizar su teclado. No necesitas conocer todavía la tecnología de la aplicación. El ZIP portable incluye el JAR compilado y el código; **Linux no necesita Maven ni Internet para ejecutar el JAR** una vez instaladas las dependencias del sistema.

Si ya descargaste la versión anterior (que requería Java 17), actualiza con `git pull --ff-only` desde la carpeta del repositorio o descarga el ZIP otra vez. `bash run.sh help` debe mostrar **1.1.1 · Java 8 (1.8)+**. No hace falta cambiar el Java del equipo ni el que utiliza el POS.

La finalidad es obtener evidencia: ¿aparece el proceso?, ¿hay controles dentro de la ventana?, ¿se puede leer el contenido del producto/total/mensajes?, ¿cambia al operar manualmente?, ¿puede localizarse sin coordenadas?

## Descargar desde GitHub en el equipo Linux

El repositorio incluye **`pos-accessibility-lab.jar` ya compilado**. No necesitas Maven para ejecutarlo. Desde una terminal de tu escritorio, puedes descargarlo a tu carpeta personal:

```bash
cd "$HOME"
git clone https://github.com/robertoflovan/TestAutoLinux.git
cd TestAutoLinux
java -version
rpm -q at-spi2-core libatspi0
```

Los dos últimos comandos comprueban Java (1.8 o superior) y las bibliotecas de SUSE/SLES. Si están disponibles, deja el POS abierto y ejecuta desde esa carpeta:

```bash
bash run.sh doctor --backend jna --out reports/entorno
bash run.sh list --backend jna --out reports/aplicaciones
# Cambia 1234 por el PID real del POS que aparezca en la lista.
bash run.sh suite --backend jna --pid 1234 --out reports/pos
```

Abre `reports/pos/report.html` en el navegador. Consulta la sección de SLES más abajo si falta alguna dependencia. Ejecuta el laboratorio con el mismo usuario y en la misma sesión gráfica del POS.

Si no tienes Git, puedes abrir [el repositorio](https://github.com/robertoflovan/TestAutoLinux) en el navegador del Linux y elegir **Code → Download ZIP**. Extrae el ZIP, abre una terminal en la carpeta `TestAutoLinux-main` y sigue los comandos desde `java -version` en adelante.

Para obtener cambios posteriores con Git, entra a `TestAutoLinux` y ejecuta `git pull --ff-only`. Los resultados locales de `reports/` no se suben al repositorio.

## Qué se compara

| Vía | Implementación | Dependencias en Linux |
|---|---|---|
| `jna` | Java → JNA → biblioteca nativa `libatspi` | Java, libatspi, GLib, GObject y sesión D-Bus de accesibilidad |
| `python` | Java → proceso Python → GObject Introspection (`Atspi`) | Las anteriores + Python3, PyGObject e introspección de Atspi |

Son **dos integraciones con la misma infraestructura AT-SPI**, no dos motores de accesibilidad independientes. Si una falla y la otra funciona, puede haber un problema del enlace o sus dependencias. Dogtail y Accerciser también se apoyan en AT-SPI; cambiar de herramienta no crea controles que el POS no exponga.

No se usa OCR. No se envían teclas, no se cambia el foco, no se invocan acciones ni se modifica configuración. Se enumeran los nombres de las acciones que anuncian los controles, pero no se ejecutan. Las consultas pueden activar el servicio D-Bus de accesibilidad si está disponible. Los textos de controles con rol de contraseña no se leen. Otros campos pueden contener datos del cliente: revisa los reportes antes de compartirlos.

## 1. Preparar el equipo Linux

Extrae el ZIP y abre **una terminal dentro de la misma sesión de escritorio y con el mismo usuario que ejecuta el POS**. No ejecutes la herramienta con `sudo`. Un SSH aislado, una consola diferente, un contenedor o un VNC con otro escritorio pueden observar otra sesión.

Comprueba primero:

```bash
java -version
```

Se requiere Java **8 (1.8) o superior**. Si ya hay Java 1.8, no necesitas sustituirlo. La vía Java no requiere instalar Python. En Debian/Ubuntu, si faltan dependencias, estos son los paquetes habituales; los instala el administrador del equipo:

```bash
sudo apt-get update
sudo apt-get install at-spi2-core libatspi2.0-0
# Sólo si no existe ningún Java compatible, ejemplo de JRE disponible en Ubuntu:
sudo apt-get install openjdk-17-jre

# Para comparar también con Python y abrir el control de ejemplo:
sudo apt-get install python3 python3-gi gir1.2-atspi-2.0 gir1.2-gtk-3.0
```

Para otras distribuciones, busca los equivalentes de Java 8+, libatspi, PyGObject y los typelibs de Atspi/GTK3. La disponibilidad depende de la distribución y sus repositorios. No utilices `pip install gi`: se trata de paquetes del sistema. Si usas conda/venv y no ve `gi`, especifica `--python /usr/bin/python3`.

No necesitas dar permiso de ejecución al script: usa `bash run.sh`. También puedes ejecutar cualquier comando con `java -jar pos-accessibility-lab.jar ...`.

### SUSE Linux Enterprise Server 15 SP7 (SLES 15.7)

El proyecto no está limitado a Ubuntu: la vía JNA detecta Linux y carga las bibliotecas AT-SPI del sistema. **SLES 15 SP7 es un destino posible, pero no se ha probado este laboratorio en una instalación SLES real.** La lectura del POS depende de su accesibilidad y de la sesión, no sólo de la distribución.

En SUSE se utiliza `zypper`, no los comandos `apt-get` del ejemplo anterior. **Java 1.8 existente es suficiente para la versión 1.1.0 del laboratorio; no es necesario instalar Java 17/21.** Los paquetes de accesibilidad se llaman `at-spi2-core` y `libatspi0`; no `libatspi2.0-0` como en Debian/Ubuntu. Su disponibilidad en el equipo depende de los módulos y repositorios habilitados.

Primero consulta lo que ya existe, sin instalar ni modificar nada:

```bash
cat /etc/os-release
uname -m
java -version
rpm -q at-spi2-core libatspi0
```

Si faltan paquetes, se pueden buscar en los repositorios configurados con `zypper search -s NOMBRE_DEL_PAQUETE`. Para revisar paquetes y módulos del producto, SUSE también documenta `zypper search-packages NOMBRE_DEL_PAQUETE`. Una búsqueda sin resultados no implica incompatibilidad del sistema.

**Empieza por Java/JNA**, que no necesita Python ni sus módulos. Desde una terminal de la misma sesión que muestra el POS:

```bash
bash run.sh doctor --backend jna --out reports/sles-entorno
bash run.sh list --backend jna --out reports/sles-aplicaciones
# Sustituye 1234 por el PID identificado en la lista.
bash run.sh suite --backend jna --pid 1234 --out reports/sles-pos
```

Para comparar después con la vía Python, comprueba primero que el intérprete elegido tiene PyGObject y Atspi:

```bash
/usr/bin/python3 -c 'import gi; gi.require_version("Atspi", "2.0"); from gi.repository import Atspi; print("Binding Atspi disponible")'
```

Esto sólo verifica la importación, no el acceso al POS. Los nombres de paquetes SUSE que conviene buscar son `python3-gobject` y `typelib-1_0-Atspi-2_0`; para el control GTK, también `python3-gobject-Gdk` y `typelib-1_0-Gtk-3_0`. Confirma su disponibilidad para los módulos y el intérprete de esa instalación. No reemplaces el Python del sistema ni cambies el Java predeterminado del POS para ejecutar el laboratorio.

Que el producto se llame **Server** no demuestra que sea una sesión sin escritorio: SLES ofrece entornos gráficos. Lo que hay que confirmar es dónde se ejecuta realmente la interfaz del POS. Si es una terminal de texto o un visor remoto, la interpretación del árbol será diferente.

Referencias SUSE: [versiones de Java en SLES 15 SP7](https://www.suse.com/releasenotes/x86_64/SUSE-SLES/15-SP7/index.html), [inventario de una imagen oficial SP7 que incluye at-spi2-core y libatspi0](https://publiccloudimagechangeinfo.suse.com/google/sles-15-sp7-v20250619-x86-64/package_list.html), [módulos y búsqueda de paquetes](https://documentation.suse.com/sles/15-SP7/pdf/article-modules_en.pdf), [escritorio GNOME en SLES SP7](https://documentation.suse.com/sles/15-SP7/single-html/SLES-gnome-user/index.html).

## 2. Revisar el entorno y encontrar el POS

Mantén abierto el punto de venta y ejecuta:

```bash
bash run.sh doctor --python /usr/bin/python3 --out reports/entorno
bash run.sh list --backend both --python /usr/bin/python3 --out reports/aplicaciones
```

`doctor` recoge tipo de sesión, escritorio, versión de Java y presencia de variables de sesión; además consulta el registro por las dos vías. No demuestra por sí solo que los campos del POS se puedan leer. Un `AWT headless=true` afecta a herramientas como Robot, pero no determina que AT-SPI sea inaccesible.

`list` imprime **PID, nombre accesible y toolkit**. El nombre accesible de la aplicación puede diferir del título de su ventana. Usa preferentemente el PID que aparece en esta lista. Si no aparece, mira la sección de diagnóstico antes de concluir que el POS es incompatible.

Desde 1.1.1, `doctor` y `list` informan `INVENTARIO_LEIDO` y el número de aplicaciones: estos comandos no recorren controles. En 1.1.0 podían mostrar `OBJETIVO_AMBIGUO · 0 elementos` al enumerar varias aplicaciones; ese mensaje no significaba que el acceso al escritorio hubiera fallado. Usa `inspect --pid PID` o `suite --pid PID` para explorar una aplicación concreta.

## 3. Ejecutar las pruebas comparativas

Sustituye `1234` por el PID real:

```bash
bash run.sh suite --pid 1234 --python /usr/bin/python3 --out reports/pos
```

También puedes seleccionar por una parte literal del nombre, sin distinguir mayúsculas:

```bash
bash run.sh suite --app "nombre del POS" --out reports/pos
```

Durante `suite`, deja la pantalla quieta. Ejecuta dos lecturas independientes de cada vía y compara:

1. Enumeración y selección del proceso objetivo.
2. Exploración de la jerarquía de controles.
3. Lectura de `name`, `description`, `role` y `accessibleId`, cuando el POS los ofrece.
4. Lectura de `Text` y `Value` en los elementos que implementan esas interfaces.
5. Estados, como `focused`, `editable`, `enabled`, `visible` y `showing`.
6. Diferencias entre dos capturas sin operar la aplicación. Es una observación inicial de repetibilidad, no una garantía de estabilidad.

Abre **`reports/pos/report.html`** con el navegador. Funciona sin conexión. Archivos:

| Archivo | Contenido |
|---|---|
| `report.html` | Resumen legible y tabla de elementos de todas las lecturas |
| `summary.txt`, `summary.json` | Veredicto, conteos y limitaciones |
| `jna-1.json`, `jna-2.json` | Capturas del enlace Java |
| `python-1.json`, `python-2.json` | Capturas del enlace Python |
| `*-changes.json` | Diferencias entre lecturas por ruta posicional |
| `*.stderr.log`, `*.stdout.log` | Errores del proceso lector y bibliotecas |
| `atspi_probe.py` | Puente extraído del JAR para ejecutar la vía Python |

Si una vía falla, se conserva su diagnóstico y se prueba la otra. Cada lectura tiene su propio proceso y un timeout para que un control bloqueado no congele indefinidamente el laboratorio. Los reportes no son una captura atómica: el POS puede cambiar mientras se recorre el árbol.

## 4. Buscar y validar un campo como con Winium

Explora una sola vía:

```bash
bash run.sh inspect --pid 1234 --backend jna --out reports/arbol
bash run.sh find --pid 1234 --backend jna --name "Total" --out reports/busqueda
```

`find` escribe `matches.json`. Se pueden combinar `--name`, `--role`, `--text` (subcadenas literales sin distinción de mayúsculas), `--id` y `--path` (igualdad exacta). Cada comando toma una **nueva lectura** del proceso vivo.

No confundas propiedades:

| Propiedad | Ejemplo | Interpretación |
|---|---|---|
| `name` | `TotalVenta` | Etiqueta/nombre accesible; algunas etiquetas muestran aquí el contenido |
| `text` | `125.50` | Contenido obtenido por la interfaz Text; puede ser `""` si está vacío |
| `value` | `125.5` | Valor numérico de la interfaz Value, si existe; no todos los importes la implementan |
| `accessibleId` | `total` | Identificador que el toolkit expone; puede estar vacío o no ser único |
| `path` | `app[2]/0/1/3` | Posición en esa captura; cambia si se reordena el árbol o el registro |

Una vez identificado el elemento, comprueba un resultado conocido. Los siguientes nombres son ilustrativos; reemplázalos por los que obtengas del POS:

```bash
bash run.sh check --pid 1234 --backend jna \
  --name "TotalVenta" --role "text" --field text --equals "125.50" \
  --out reports/validacion-total

# Si el POS expone un ID y el contenido en name:
bash run.sh check --pid 1234 --id "mensaje-estado" \
  --field name --contains "Venta completada" --out reports/validacion-mensaje
```

`check` requiere **exactamente una aplicación y un elemento**. Una selección ambigua, un texto truncado, una captura limitada o errores en el elemento impiden el PASS. La comparación es literal y distingue mayúsculas, espacios y saltos de línea. No elimina separadores, no interpreta monedas y no trata `null` como texto vacío. `value` utiliza la representación decimal de Java, por ejemplo `125.5`.

Para validar la actualización, deja el proceso observando y cambia **manualmente** un producto o navega a otro estado:

```bash
bash run.sh watch --pid 1234 --backend jna --samples 4 --interval 5000 \
  --out reports/cambio-manual
```

`interval` es la pausa en milisegundos **entre lecturas**, además del tiempo que tarda cada exploración. Mira `changes.json`, las capturas y el HTML. Si no hay diferencias, puede que el cambio sucediera fuera de las muestras: no prueba por sí solo que los datos no se actualicen. Una diferencia por ruta puede deberse a reordenación; verifica rol, ID, PID y contenido.

También puedes guardar dos estados por separado y compararlos:

```bash
bash run.sh inspect --pid 1234 --out reports/antes
# Cambia manualmente el estado del POS.
bash run.sh inspect --pid 1234 --out reports/despues
bash run.sh diff --before reports/antes/jna-snapshot.json \
  --after reports/despues/jna-snapshot.json --out reports/comparacion
```

## 5. Control positivo para distinguir entorno y aplicación

Abre este ejemplo **en la misma sesión Linux**, desde otra terminal:

```bash
/usr/bin/python3 examples/gtk_control.py
```

Es una ventana GTK3 con datos ficticios: `CodigoProducto`, `DescripcionProducto`, `Cantidad` y `TotalVenta`. No usa el sistema del cliente. Vuelve a ejecutar `list` y usa el PID que corresponde a `pos-accessibility-control`/la ventana de control (el nombre exacto puede depender del toolkit):

```bash
bash run.sh list --backend both --out reports/lista-control
bash run.sh suite --pid PID_DEL_CONTROL --out reports/control
bash run.sh check --pid PID_DEL_CONTROL --name "TotalVenta" \
  --field text --equals "125.50" --out reports/control-total
```

En la ventana, escribe `1002` en el código y pulsa Enter; el total pasa a `80.00`. Ejecuta de nuevo `check` con `--equals "80.00"` o utiliza `watch` durante el cambio. Esta prueba valida la cadena de lectura y actualización con un valor conocido. No certifica el POS.

## Cómo decidir con los resultados

| Resultado | Siguiente paso |
|---|---|
| `ELEMENTOS_LEIBLES` y `check` confirma producto, total y mensajes | Candidato para automatización por elementos. Si funciona JNA, se puede mantener el acceso en Java; si sólo funciona Python, usar un puente. Integrar después las acciones de teclado y esperas por estado. |
| Sólo aparecen títulos o contenedores | Comprueba configuración del toolkit y repite en pantallas distintas. Ver el título no demuestra acceso a los campos. |
| `LECTURA_PARCIAL` | Revisa errores, límites y campos relevantes. No concluyas incompatibilidad ni estabilidad a partir del conteo. |
| `APLICACION_NO_ENCONTRADA` | Revisa PID, usuario, sesión, que el POS siga abierto y el control positivo. |
| `OBJETIVO_AMBIGUO` | Usa PID o una selección más precisa. |
| `ERROR_DE_ENTORNO`, registro vacío o también falla el control GTK | Resuelve dependencias/sesión antes de evaluar el POS. |

Los conteos no eligen automáticamente la mejor herramienta. Lo decisivo es si puedes leer **el valor correcto de los campos que necesitas**, volver a localizarlos y observar sus cambios. No toda etiqueta implementa Text; su texto puede estar en Name. No todas las tablas exponen todas las filas como descendientes (virtualización); este prototipo explora los hijos publicados y no implementa consultas especializadas de tablas/Collection.

## Si no se ven los elementos

- **Sesión:** ejecuta desde el escritorio del usuario del POS, sin `sudo`, con D-Bus de esa sesión. No arranques un `dbus-run-session` nuevo para inspeccionar una app ya abierta: crearía otra sesión.
- **GNOME:** consulta `gsettings get org.gnome.desktop.interface toolkit-accessibility`. Si está desactivado y el cliente permite cambiarlo, activa `gsettings set org.gnome.desktop.interface toolkit-accessibility true` y vuelve a abrir la app. El proyecto no cambia este ajuste. Puedes revertirlo con `false` si ése era el valor original.
- **GTK:** `NO_AT_BRIDGE=1` puede desactivar el puente. Revisa cómo se lanzó el proceso. Cambiar variables en la terminal del laboratorio no modifica la aplicación que ya está abierta.
- **Qt:** si se confirma que usa Qt, prueba a lanzar una instancia de prueba con `QT_LINUX_ACCESSIBILITY_ALWAYS_ON=1 /ruta/al/pos`. Algunas versiones requieren `QT_ACCESSIBILITY=1`. Confirma con el proveedor la configuración de su versión; no es una solución universal.
- **Java Swing:** puede requerir el puente Java ATK de su distribución/JRE (por ejemplo `libatk-wrapper-java` y su componente JNI). Primero confirma que realmente es Swing y cómo carga su JVM. El laboratorio no inyecta agentes ni modifica el JRE del cliente. AssertJ Swing sería otra opción si puedes integrar las pruebas en la JVM de la aplicación.
- **Wayland:** AT-SPI puede leer elementos en Wayland; las restricciones de captura/inyección de teclado son otro asunto. No cambies a X11 únicamente por ver `wayland` en el diagnóstico.
- **Terminal o escritorio remoto:** puedes estar inspeccionando un emulador/visor, no el proceso POS. Un terminal puede publicar una superficie de texto sin campos separados. En RDP/VNC, el lector debería ejecutarse dentro de la sesión donde realmente está el POS.
- **Campos dibujados a medida:** podrían no exponer semántica accesible. Antes de descartar la vía, compara con Accerciser y consulta al proveedor. Puede haber un modo de accesibilidad o integración que necesite habilitarse.

## Control Swing con un Java 8 existente y un puente extraído

Si no puedes cambiar Java, `examples/run-swing-atk.sh` abre una ventana independiente con datos ficticios. Requiere un **JDK 8 ya instalado** (incluido `javac`) y el JAR y `.so` de Java ATK Wrapper previamente extraídos de un RPM confiable. No instala paquetes ni cambia alternativas Java, archivos del JRE o propiedades de accesibilidad del usuario. La carga del puente se configura sólo para el proceso de prueba mediante la [propiedad de accesibilidad de Java](https://docs.oracle.com/javase/8/docs/technotes/guides/access/new-features.html).

Antes de ejecutarlo, comprueba la compatibilidad del bytecode del puente con Java 8 y sus dependencias nativas con `ldd`. Que esas comprobaciones pasen no demuestra todavía que el puente funcione.

Desde una terminal de la misma sesión gráfica, sin `sudo`:

```bash
bash examples/run-swing-atk.sh /usr/java/jdk1.8.0_191 "$HOME/pos-atk-prueba/extraido/usr/lib64/java-atk-wrapper"
```

El script compila el ejemplo en `$HOME/pos-atk-prueba/clases`, muestra la versión y PID de la prueba y mantiene la ventana abierta. Su variable `LD_LIBRARY_PATH` sólo afecta al script y al proceso que lanza. Deja esa terminal abierta. En otra terminal, desde la carpeta del laboratorio, sustituye `12345` por el **PID de la prueba** mostrado:

```bash
bash run.sh inspect --backend jna --pid 12345 --out reports/swing-inicial
bash run.sh check --backend jna --pid 12345 --name TotalVenta --field text --equals 125.50 --out reports/swing-total-inicial
```

Se espera encontrar `CodigoProducto` con `1001`, `DescripcionProducto` con `Cafe de prueba` y `TotalVenta` con `125.50`. En la ventana de ejemplo cambia el código a `1002` y pulsa Enter. Después:

```bash
bash run.sh check --backend jna --pid 12345 --name TotalVenta --field text --equals 80.00 --out reports/swing-total-cambiado
```

Un `PASS` en ambas lecturas confirma la lectura de ese campo y su cambio en el ejemplo. No demuestra aún la accesibilidad del POS real: depende de sus componentes y de cómo se active el puente en su JVM. Si la ventana no aparece, conserva el error de la primera terminal; si aparece pero no se puede inspeccionar, conserva los reportes. Cerrar la ventana termina sólo el ejemplo.

## Límites y códigos de salida

Valores por defecto por captura: 2,000 elementos, profundidad 25, 2,000 caracteres por campo, timeout total 45 segundos, timeout de consulta AT-SPI 800 ms. Ejemplo para ampliar:

```bash
bash run.sh inspect --pid 1234 --max-nodes 5000 --max-depth 40 \
  --max-text 5000 --timeout 90 --out reports/ampliado
```

El indicador `truncated` significa que no se exploró todo; `textTruncated` identifica límites de texto. Los errores se conservan por campo. No se escribe un número no finito ni se convierte un fallo de lectura en un valor válido.

| Código | Significado |
|---|---|
| `0` | Operación/lectura satisfactoria; en doctor/list, sólo confirma la consulta al registro, no la accesibilidad del POS |
| `2` | Error de uso o todas las vías consultadas fallaron por infraestructura |
| `3` | `find` sin coincidencias o `check` fallido |
| `4` | Diagnóstico parcial/no concluyente o sin campos útiles |

En `suite`, un código `0` indica que al menos una lectura produjo elementos útiles. Revisa también las demás capturas: un éxito aislado no prueba estabilidad. Usa una carpeta `--out` distinta por corrida para conservar el historial.

## Compilar, probar y crear el ZIP

Para compilar se recomienda JDK 17+ y Maven; `maven.compiler.release=8` comprueba las APIs de Java 8 y genera bytecode compatible con ese runtime. El equipo Linux de destino sólo necesita su JRE 1.8+, no el JDK de compilación:

```bash
mvn clean package
java -jar target/pos-accessibility-lab.jar help
python3 -m unittest discover -s tests -v
```

El script `run.sh` utiliza primero el JAR de `target/` cuando existe, y en otro caso el JAR portable de la raíz. Si vas a publicar una nueva compilación en GitHub, actualiza también el portable con `cp target/pos-accessibility-lab.jar pos-accessibility-lab.jar` (PowerShell: `Copy-Item target/pos-accessibility-lab.jar pos-accessibility-lab.jar`).

En Windows, para las pruebas Python puedes usar `py -3.14 -m unittest discover -s tests -v` (o tu versión instalada). Para empaquetar sin compilar otra vez:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package.ps1
```

El ZIP `dist/pos-accessibility-lab.zip` contiene el JAR y fuentes, sin reportes ni cachés. El puente Python va dentro del JAR. Las dependencias Java se descargan **al compilar** y quedan incluidas; las bibliotecas AT-SPI y PyGObject siguen siendo dependencias del Linux destino.

Las pruebas locales usan datos sintéticos para verificar selectores, validaciones, límites, errores, serialización y reportes. **No sustituyen ejecutar el laboratorio contra el escritorio Linux y el POS real.**

## Referencias de la implementación

- [Infraestructura y protocolos AT-SPI](https://github.com/GNOME/at-spi2-core)
- [API de Accessible](https://gnome.pages.gitlab.gnome.org/at-spi2-core/libatspi/class.Accessible.html)
- [Interfaz Text](https://gnome.pages.gitlab.gnome.org/at-spi2-core/libatspi/iface.Text.html)
- [Interfaz Value](https://gnome.pages.gitlab.gnome.org/at-spi2-core/libatspi/iface.Value.html)
- [Bus de accesibilidad y sesión D-Bus](https://github.com/GNOME/at-spi2-core/blob/main/bus/README.md)
- [GNOME recomienda GObject Introspection para código Python nuevo](https://github.com/GNOME/pyatspi2)
- [JNA](https://github.com/java-native-access/jna)
- [Accerciser](https://wiki.gnome.org/Apps/Accerciser)

Se utiliza GObject Introspection directamente en lugar de añadir una dependencia nueva de `pyatspi`, conforme a la recomendación de su proyecto.
