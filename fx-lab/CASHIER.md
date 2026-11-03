# Guía de validación y automatización de CASHIER con JavaFX

En CASHIER Colombia ya se puede consultar una pantalla desde una prueba Java y comparar sus textos. En el menú principal se leyó la versión `2.0.0.2.101` y se obtuvo `PASS` para la opción `[2]` con el texto `Devolucion...`. El entorno comprobado fue SLES 15 SP7 con Oracle Java `1.8.0_191`. La navegación la hizo el operador. La automatización del teclado y los flujos completos de venta son etapas posteriores.

**Si CASHIER ya está abierto con el agente y el lector 1.2 está descargado, ve directamente a [Validar una pantalla](#5-validar-una-pantalla).** En un equipo nuevo, empieza en el paso 1. El reinicio del paso 4 corresponde exclusivamente al equipo de certificación de Colombia.

## Cómo funciona

| Pieza | Qué hace | Cuándo se usa |
|---|---|---|
| Kit `fx-lab-v1.1.zip` | Contiene la aplicación de ejemplo y el agente `fx-inspector.jar`. Se extrae en `fx-lab-v1`. | Para probar el entorno y para cargar el agente al arrancar el POS. |
| Lanzador `restart-cashier-fx.sh` | Crea copias privadas de los scripts de arranque de Colombia. Conserva los parámetros del POS, AspectJ y las rutas de los periféricos. Añade el agente a la JVM del POS. | Al preparar el plan y al iniciar una sesión instrumentada de CASHIER. |
| Lector `fx-reader-v1.2.zip` | `leer-cashier.sh` ejecuta `fx-reader.jar` en un proceso Java separado. | Para inspeccionar la pantalla o comprobar un campo tantas veces como requiera la prueba. |

Java 8 admite cargar un JAR al arrancar una aplicación mediante la opción `-javaagent`. Nuestro agente inicia un hilo que espera peticiones en una carpeta privada. El lector crea una petición con un identificador único. Al recibirla, el agente usa `Platform.runLater` para recorrer las ventanas y el árbol de controles en el hilo gráfico de JavaFX. Recoge el tipo, el ID, la ruta y el texto que expone cada control. El lector espera la respuesta de esa petición, compara el valor y escribe un reporte local. Así cada validación consulta el estado de la aplicación en ese momento. [Agentes de Java 8](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html) · [Hilo gráfico de JavaFX](https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html) · [Implementación del recorrido](src/mx/demo/posfx/FxSnapshot.java)

El agente se carga una vez por arranque instrumentado. Las consultas siguientes usan la misma sesión. La demostración y CASHIER guardan sus sesiones por separado. Actualizar únicamente el lector compatible permite seguir consultando un POS abierto.

## 1. Identificar el ambiente

Desde una terminal del escritorio del usuario que opera la caja, ejecuta cada línea por separado.

```bash
hostname
cat /etc/os-release
uname -m
java -version
```

Anota también el Java **del proceso de caja**. Puede diferir del que selecciona la terminal. Con CASHIER abierto, ejecuta la siguiente orden y haz clic en su ventana.

```bash
xprop _NET_WM_PID WM_CLASS WM_NAME
```

Usa el número de `_NET_WM_PID` en la siguiente consulta. Si falta `xprop`, el equipo de soporte puede identificar el PID a partir del lanzador y la clase principal. En Colombia esa clase termina en `CVPOSInstance`.

```bash
read -r -p "PID actual de CASHIER: " pos_pid
readlink -f "/proc/$pos_pid/exe"
readlink -f "/proc/$pos_pid/cwd"
```

En Colombia el ejecutable fue `/usr/java/jdk1.8.0_191/bin/java` y el log de inicio indicó modo `jfx`. El icono de Java por sí solo no identifica la tecnología de los controles. Si la versión, ruta de Java o tecnología difiere, registra la diferencia y sigue el apartado [Otro equipo o Perú](#otro-equipo-o-perú) antes de preparar el POS. El requisito del cliente es conservar el Java instalado y su selección predeterminada.

## 2. Obtener el kit

Trabaja como usuario normal en su sesión gráfica. En el equipo probado se utilizaron Bash, `unzip`, `sha256sum` y, para la descarga, `curl`. Los JAR vienen compilados. Estos comandos descargan los artefactos de una revisión concreta del repositorio para poder repetir la prueba. Ejecuta las líneas en la misma terminal.

```bash
mkdir -p "$HOME/pos-fx-prueba"
cd "$HOME/pos-fx-prueba"
pos_base="https://raw.githubusercontent.com/robertoflovan/TestAutoLinux/77eeb56da1888c4dcf86471004b69de2520ee959"
curl -fL "$pos_base/experimental/fx-lab-v1.1.zip" -o fx-lab-v1.1.zip
curl -fL "$pos_base/experimental/fx-reader-v1.2.zip" -o fx-reader-v1.2.zip
curl -fL "$pos_base/experimental/SHA256SUMS" -o SHA256SUMS
grep -E '  (fx-lab-v1[.]1|fx-reader-v1[.]2)[.]zip$' SHA256SUMS | sha256sum -c -
```

Continúa cuando **ambos ZIP** muestren `OK`. En una caja sin acceso a GitHub, transfiere esos dos ZIP y el manifiesto desde la misma revisión, comprueba sus hashes en la carpeta de destino y extrae allí. Para Colombia se necesita además el script del paso 4.

```bash
unzip -o fx-lab-v1.1.zip
unzip -o fx-reader-v1.2.zip
```

## 3. Probar el escritorio con el ejemplo

Con la ruta y versión de Java de Colombia, abre el ejemplo. Deja esa terminal abierta. El POS habitual puede seguir funcionando durante esta prueba.

```bash
bash "$HOME/pos-fx-prueba/fx-lab-v1/probar.sh"
```

En **otra terminal**, consulta el total inicial.

```bash
bash "$HOME/pos-fx-prueba/fx-lab-v1/leer.sh" check TotalVenta 125.50
```

En la ventana **POS - Prueba JavaFX Inspector**, reemplaza el código `1001` por `1002` y pulsa Enter. Cuando el total muestre `80.00`, ejecuta lo siguiente.

```bash
bash "$HOME/pos-fx-prueba/fx-lab-v1/leer.sh" check TotalVenta 80.00
```

El criterio de éxito son **dos PASS y la ventana todavía abierta**. Esto distingue un problema de la instalación gráfica de uno específico del POS. Puedes cerrar la ventana del ejemplo al terminar. El ID `TotalVenta` pertenece al ejemplo. Los campos de las pantallas reales se descubren con `inspect`.

## 4. Preparar e iniciar CASHIER en Colombia

El lanzador publicado comprueba el host `QACOL-CERT-VIRREY`, la instalación `geopos2oxxo-colombia-2.0.0.2.101`, el ejecutable Java y la estructura del arranque revisado. Desde la terminal del usuario, descarga el lanzador y prepara el plan.

```bash
cd "$HOME/pos-fx-prueba"
curl -fL https://raw.githubusercontent.com/robertoflovan/TestAutoLinux/77eeb56da1888c4dcf86471004b69de2520ee959/scripts/restart-cashier-fx.sh -o restart-cashier-fx.sh
bash "$HOME/pos-fx-prueba/restart-cashier-fx.sh" --preparar
```

La preparación muestra la ubicación de `cambios.txt` y deja las aplicaciones abiertas. Revisa el archivo. La copia de `iniciaGeopos.sh` debe llamar a la copia de `runPos.sh`. La línea de Java debe conservar el Java y los argumentos habituales, incluido AspectJ, y agregar el inspector JavaFX con su carpeta de sesión. Los scripts originales siguen siendo el camino de recuperación.

**El siguiente comando ejecuta el cierre habitual de CASHIER, callvlc.sh y VLC.** Úsalo en la caja de pruebas cuando se pueda reiniciar y se haya terminado la operación en curso. Cerrar la ventana con la X puede dejar vivo el proceso Java. Este lanzador ya ejecuta la secuencia de reinicio del proveedor.

```bash
bash "$HOME/pos-fx-prueba/restart-cashier-fx.sh" --iniciar
```

El comando vuelve a preparar las copias desde la configuración vigente. Espera hasta ver la pantalla operable de CASHIER. La consulta se ejecuta desde otra terminal del mismo usuario.

## 5. Validar una pantalla

Ejecuta esta primera línea en cada terminal nueva para abreviar la ruta del lector. Luego pide una captura.

```bash
pos_lector="$HOME/pos-fx-prueba/fx-reader-v1.2/leer-cashier.sh"
bash "$pos_lector" inspect
```

La salida indica el número de ventanas y nodos, si la captura fue parcial y la ruta del reporte `.txt`. En el menú de Colombia se obtuvieron `Ventanas=2 Nodos=163 Parcial=false`. Es una observación de esa pantalla, no una cantidad que deba coincidir en cada versión. Cada fila del reporte presenta ruta, tipo, ID y texto.

| Necesidad | Cómo localizar el dato | Ejemplo comprobado |
|---|---|---|
| Reconocer una pantalla | `check` exige un ID único y compara el texto exacto. | `title` muestra `Menú Principal`. |
| Identificar la versión del POS | Leer el nodo de versión por su ID. | `version` muestra `2.0.0.2.101`. |
| Validar una opción sin ID propio | `check-option` busca el contenedor por su ID, el número entre corchetes y la etiqueta de esa tarjeta. | En `hBoxFavorite`, la opción `[2]` muestra `Devolucion...`. |

```bash
bash "$pos_lector" check title "Menú Principal"
bash "$pos_lector" check version "2.0.0.2.101"
bash "$pos_lector" check-option hBoxFavorite 2 "Devolucion..."
```

La última comprobación dio `PASS` con `Devolucion...` y `FAIL` al exigir `Devoluciones...`. El valor esperado de una prueba de negocio debe salir del requisito acordado. Conserva el fallo si ese requisito exige el plural. Acentos, espacios, mayúsculas y puntos forman parte de la comparación. El número `2` identifica la tarjeta rotulada `[2]`, incluso si cambia su posición.

**Para añadir un caso**, entra manualmente a la pantalla con datos de prueba, ejecuta `inspect`, localiza el campo y anota el selector. Obtén el valor esperado de los datos del caso. Ejecuta el `check`, cambia el dato o la pantalla y repítelo. Guarda el reporte y el código de salida de cada lectura. Para consultar el código en Bash, ejecuta `echo $?` inmediatamente después del lector.

| Código del lector Java | Interpretación para una prueba |
|---|---|
| `0` | La comprobación pasó, o la inspección terminó con nodos y captura completa. |
| `3` | Falló la aserción por texto diferente, selector ambiguo o ausente, o campo ilegible. Lee el detalle. |
| `2` | Error de consulta, sesión o argumentos. También se usa al intentar validar una captura parcial. |
| `4` | `inspect` produjo una captura vacía o parcial. Revisa el reporte antes de crear una aserción. |

Para un futuro flujo por teclado, registra **pantalla inicial → tecla → pantalla o dato esperado → comprobación**. El ejecutor de teclado podrá, por ejemplo, usar Java Robot en la sesión gráfica, dar el foco al POS y esperar la condición de la pantalla siguiente con un plazo máximo. Esa capa aún requiere implementación y pruebas en la caja. Hoy se pueden ejecutar las validaciones desde un proceso de pruebas y evaluar su código de salida. [Java Robot en Java 8](https://docs.oracle.com/javase/8/docs/api/java/awt/Robot.html)

## Diagnóstico y regreso al uso habitual

| Síntoma | Qué revisar |
|---|---|
| Se abrió una ventana diminuta o sólo ves la terminal | Verifica que abriste el ejemplo 1.1 en la sesión gráfica. Esa revisión centra la ventana y escribe sus dimensiones. |
| El lector indica que falta una sesión o agotó la espera | Revisa que CASHIER abrió con el lanzador de pruebas y terminó de cargar. `cat "$HOME/pos-fx-prueba/ultima-sesion-cashier.txt"` muestra la carpeta activa. El ejemplo utiliza otro archivo de sesión. |
| Hay dos textos iguales o falta el ID | Busca el contenedor en el reporte. JavaFX puede mostrar una etiqueta y otra copia en su representación interna. Usa `check-option` para tarjetas numeradas con la estructura descrita. |
| Aparece `Parcial=true` o falta contenido visible | Repite al estabilizarse la pantalla. La captura limita tiempo, profundidad y cantidad de nodos. Canvas, contenido web y filas virtualizadas pueden requerir otra estrategia. |
| Falla la preparación | Lee el motivo sobre host, Java, archivos, opciones de entorno o formato de scripts. Revisa el ambiente antes de intentar el inicio. |
| CASHIER se cierra al iniciar | Conserva el reporte de JVM `hs_err*.log` si existe y copia `logs/output.log` antes de otro arranque, que puede reemplazarlo. Registra la última pantalla y si se había ejecutado una consulta. |

En Colombia, estas órdenes guardan el log del intento y muestran sus últimas líneas.

```bash
cp "$HOME/geopos/current/logs/output.log" "$HOME/pos-fx-prueba/output-$(date +%Y%m%d-%H%M%S).log"
tail -n 80 "$HOME/geopos/current/logs/output.log"
```

Para detener **sólo las lecturas** de la sesión, usa `bash "$pos_lector" stop`. El POS sigue abierto. Una sesión nueva con el agente requiere otro arranque instrumentado. Para volver al **uso habitual de Colombia**, usa el icono GEOPOS o `bash "$HOME/iniciaGeopos.sh"` cuando corresponda el reinicio normal. En otro equipo se usa su lanzador original confirmado.

Los reportes se guardan en la carpeta de sesión local. El inspector omite el texto y los hijos de `PasswordField`. Otros campos, los argumentos de inicio y los logs pueden incluir información del cliente. Usa datos de prueba y comparte sólo las líneas necesarias.

## Otro equipo o Perú

**Perú está pendiente de validación.** Se recibieron scripts de referencia de ese ambiente. La similitud de los nombres permite orientar la revisión, pero la información decisiva es la de la caja que se probará.

1. **Completa la ficha inferior** con el POS arrancado de forma habitual. Confirma la ruta del Java del proceso y el modo de interfaz. Si aparece `jfx`, prueba primero la ventana independiente. Los scripts distribuidos para el ejemplo y ambos lectores fijan la ruta de Oracle 8u191. Si el ejecutable local está en otra ruta, adapta copias para apuntar al Java existente del ambiente y comprueba su versión y disponibilidad de JavaFX.
2. **Reconstruye el arranque local.** Lee el campo `Exec=` del acceso directo que usa el operador. Sigue sus llamadas hasta la línea Java. En Colombia el icono usa `iniciaGeopos.sh` y el inicio de sesión tiene una entrada diferente. Confirma cuál aplica allí. Revisa qué cierra el procedimiento y cómo se recupera el uso habitual.
3. **Prepara una variante específica del lanzador.** Verifica el host y la instalación local, la clase principal, archivo de configuración de la instancia, zona horaria, memoria, AspectJ, classpath, rutas de periféricos, variables, logs y selección de Java. Trabaja sobre copias. Mantén las comprobaciones de coincidencia, integridad y preparación previa. Cambiar sólo el país o el hostname deja sin revisar esos supuestos.
   En la orden de lanzamiento, la opción tiene la forma `-javaagent:/ruta/fx-inspector.jar=/ruta/sesion` y se coloca entre las opciones de JVM, antes de la clase principal. La variante debe crear una carpeta privada nueva por arranque y escribir su ruta en `~/pos-fx-prueba/ultima-sesion-cashier.txt` para que la encuentre el lector. Revisa en la copia el punto de inserción respecto al agente AspectJ existente. [Referencia del lanzador de Colombia](../scripts/restart-cashier-fx.sh)

4. **Repite las pruebas en orden.** Dos totales en el ejemplo → revisión de diferencias en preparación → arranque del POS de certificación → inspección del menú → una comparación correcta y otra con diferencia conocida → nueva consulta con el proceso vivo. Descubre los IDs, opciones e idioma en el reporte local. Registra el resultado y la vuelta al arranque normal antes de declarar validado ese equipo.

### Ficha para cada ambiente

| Registrar | Resultado local |
|---|---|
| País, equipo de certificación, versión de SLES y arquitectura | |
| Versión del POS, modo de interfaz y pantalla de prueba | |
| Ruta de Java del proceso, salida de versión y carpeta de trabajo | |
| Acceso directo, scripts originales, carpeta `current`, cierres y recuperación | |
| Configuración de la instancia, zona horaria, otros agentes y periféricos revisados | |
| Versiones y hashes de los kits, revisión del lanzador, fecha de prueba | |
| Demo inicial y cambiada, estabilidad después de consultar | |
| ID o contenedor/número, texto esperado y obtenido, código de salida, ruta de evidencia | |
| Resultado en POS real, pendientes y resultado del retorno al arranque habitual | |

## Qué pruebas históricas se conservan

Al investigar una aplicación desconocida se comprobó AT-SPI con Java/JNA. El escritorio expuso otras aplicaciones, pero CASHIER no apareció en esa lista. Un ejemplo Swing permitió probar Java ATK Wrapper con el Java instalado. El puente original llegó a leer un total y posteriormente falló en código nativo. Una corrección permitió repetir las lecturas del **ejemplo Swing**. Al cargar ese puente en CASHIER, configurado en modo JavaFX, el POS terminó durante el arranque con `SIGSEGV`. El log sitúa el fallo en código nativo y la JVM, sin establecer una causa definitiva.

Esas pruebas explican la elección tecnológica. La ambientación reproducible de JavaFX es la de los pasos 1 a 5. La instalación de RPM, la compilación del puente y los lanzadores ATK pertenecen al diagnóstico histórico. [Historial y evidencia de los experimentos](../experimental/README.md)

Las pruebas de laboratorio automatizadas cubren el lector con Java 8 y una ventana JavaFX real, y la preparación del lanzador con scripts simulados. La evidencia del menú de Colombia procede de las consultas en la caja. [Pruebas del lector](https://github.com/robertoflovan/TestAutoLinux/actions/runs/41511784872) · [Pruebas del lanzador](https://github.com/robertoflovan/TestAutoLinux/actions/runs/41508579829)
