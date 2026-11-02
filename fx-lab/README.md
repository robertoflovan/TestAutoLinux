# Inspector JavaFX: prueba independiente con Java 8

Esta vía usa un **agente Java (`java.lang.instrument`, `-javaagent`)** y el
árbol de escenas de JavaFX. Lee tipos de controles, IDs y textos en el hilo
gráfico de JavaFX. No carga Java ATK Wrapper ni usa AT-SPI, OCR o Winium.
No transforma clases ni envía teclas. El agente vive dentro de la aplicación;
por eso primero se prueba en el ejemplo incluido, separado de CASHIER.

## Ejecutar en SLES con el Java existente

CASHIER puede seguir abierto normalmente. Usa la terminal de la misma sesión
gráfica, sin sudo. No ejecutes los lanzadores anteriores del puente ATK.

```bash
mkdir -p "$HOME/pos-fx-prueba"
curl --fail --location https://raw.githubusercontent.com/robertoflovan/TestAutoLinux/main/experimental/fx-lab-v1.zip --output "$HOME/pos-fx-prueba/fx-lab-v1.zip"
unzip "$HOME/pos-fx-prueba/fx-lab-v1.zip" -d "$HOME/pos-fx-prueba"
bash "$HOME/pos-fx-prueba/fx-lab-v1/probar.sh"
```

`probar.sh` usa exclusivamente `/usr/java/jdk1.8.0_191/bin/java`, comprueba la
versión y los hashes, y abre **POS - Prueba JavaFX Inspector**. No instala
paquetes, cambia Java, cierra CASHIER ni modifica sus scripts. Deja esa terminal
abierta. Si el ejemplo no abre, conserva el error; no instales otro Java.

En **otra terminal**, lee el total inicial:

```bash
bash "$HOME/pos-fx-prueba/fx-lab-v1/leer.sh" check TotalVenta 125.50
```

Debe mostrar `PASS`. En la ventana de ejemplo, cambia el código de `1001` a
`1002` y pulsa Enter. Después:

```bash
bash "$HOME/pos-fx-prueba/fx-lab-v1/leer.sh" check TotalVenta 80.00
bash "$HOME/pos-fx-prueba/fx-lab-v1/leer.sh" inspect
```

La última orden guarda un reporte de controles y muestra su ruta. Comprueba
también que la ventana sigue abierta después de varias consultas. Para parar
sólo el inspector, conservando la aplicación:

```bash
bash "$HOME/pos-fx-prueba/fx-lab-v1/leer.sh" stop
```

Para terminar la prueba, cierra la ventana de ejemplo. Cada arranque crea una
carpeta privada `~/pos-fx-prueba/sesion-...`. `leer.sh` consulta la última sesión;
no necesita que copies un PID. Una consulta usa una respuesta nueva y no
reutiliza resultados anteriores si la aplicación ya terminó.

## Alcance y siguiente paso

Este paquete **sólo lanza el ejemplo**. Leerlo correctamente no demuestra aún
que funcionen los controles propios de CASHIER ni su combinación con AspectJ.
Después de validar aquí, se puede preparar un arranque del POS que conserve
su Java y sus argumentos y añada este agente, sin el puente ATK.

La captura recorre escenas de ventanas abiertas y nodos marcados visibles;
no determina si otro nodo los tapa ni si están dentro del área recortada.
Canvas, contenido de WebView y filas virtualizadas pueden necesitar otra vía.
Los controles reales pueden carecer de ID; este primer `check` exige un ID
único, texto completo y una captura completa. Un `FAIL` también puede señalar
un ID ausente o duplicado, no sólo un valor distinto.

Se omiten el texto y los descendientes de controles JavaFX `PasswordField`.
Otros textos pueden contener información del cliente: revisar antes de
compartir. La comunicación usa archivos de la sesión, sin servidor de red.
La captura limita nodos, profundidad, longitud y tiempo de recorrido. Es un
límite cooperativo: una llamada de un control que se bloquee no puede ser
interrumpida de forma segura. Una captura pendiente evita encolar más lecturas.

En JavaFX 8 la enumeración de ventanas usa `Window.impl_getWindows`, una API
interna y obsoleta. Por eso es necesaria la prueba en Oracle 8u191. El resto de
la lectura usa las APIs de controles y escenas. El agente es experimental y
su presencia y consultas añaden trabajo al proceso.

## Compilar y verificar en un entorno de desarrollo

Requiere un JDK 8 que incluya JavaFX; no ejecutar la compilación ni instalar
herramientas en el equipo del cliente. Desde la raíz del repositorio:

```bash
bash fx-lab/build.sh
```

El workflow `test-fx-inspector.yml` compila y ejecuta el ejemplo con Java 8 y
JavaFX en un Linux desechable. Prueba lectura repetida, cambio real por teclado,
rechazo de valores incorrectos y contraseñas, desconexiones del lector y
ausencia de respuestas antiguas después del cierre. Esa prueba no sustituye
la validación pendiente en SLES 15 SP7 / Oracle 8u191. El ZIP no incluye un JRE.

Documentación: [agentes Java 8](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html),
[Platform.runLater](https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html),
[árbol de nodos](https://docs.oracle.com/javase/8/javafx/api/javafx/scene/Parent.html).
