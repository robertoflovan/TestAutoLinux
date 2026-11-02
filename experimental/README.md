# Descargas experimentales

## JavaFX: siguiente prueba aislada

[`fx-lab-v1.1.zip`](fx-lab-v1.1.zip) incluye un inspector mediante agente Java 8,
un ejemplo JavaFX, fuentes y lanzadores para usar el Oracle 8u191 ya existente.
La [guía paso a paso](../fx-lab/README.md) permite probar lectura de controles y
validación del total sin cargar ATK y sin cerrar CASHIER.

La revisión 1.1 mantiene la carpeta `fx-lab-v1` y agrega tamaño explícito,
tamaño mínimo, centrado y diagnóstico de dimensiones a la ventana del ejemplo.
Se repitieron las [pruebas gráficas con éxito](https://github.com/robertoflovan/TestAutoLinux/actions/runs/41505196210).
Su efecto sobre la ventana diminuta observada en SLES aún debe comprobarse allí.

[Prueba de CI completada](https://github.com/robertoflovan/TestAutoLinux/actions/runs/41502527663)
con Ubuntu 22.04 y Liberica 8u513 + JavaFX: lectura repetida de 125.50 y 80.00,
cambio por teclado y aplicación viva después de las consultas. Se comprobaron
también rechazo de valores incorrectos, ausencia de ID y campos de contraseña;
el lector no reutiliza respuestas tras cerrar el ejemplo. El ZIP contiene
`BUILD-INFO.txt`, `BUILD-JAVA.txt`, `TEST-RESULTS.txt` y hashes de sus JAR.

**Pendiente:** ejecutar este ejemplo con SLES 15 SP7 / Oracle 8u191. El paquete
no incluye un JRE y todavía no inicia ni valida CASHIER con el agente.

## Puente ATK corregido: historial de la prueba Swing

**Resultado posterior en CASHIER Colombia:** el POS configurado con interfaz
JavaFX terminó durante el arranque con SIGSEGV en `libjvm.so`, después de cargar
este puente. No repetir el arranque del POS real con ATK hasta investigar esa
interacción en una aplicación JavaFX aislada. La corrección del defecto
`g_int_hash` sigue validada en el ejemplo Swing; no acredita compatibilidad
con JavaFX. Los originales del cliente permanecen intactos y permiten el
arranque habitual sin el puente.

`puente-corregido-v1.zip` contiene Java ATK Wrapper 0.33.2 reconstruido con la
corrección de claves de eventos. No contiene un JRE ni instala paquetes.

[Ejecución de CI verificada](https://github.com/robertoflovan/TestAutoLinux/actions/runs/40791886977):
el original devuelve PASS y luego termina con SIGSEGV en `g_int_hash`; el
corregido supera seis consultas, leyendo los totales 125.50 y 80.00 después de
un cambio por teclado, y sigue vivo tras las desconexiones del lector y una
espera final de 10 segundos.

La prueba de CI se ejecutó con Ubuntu 22.04 x86_64 y Temurin 8u504. El usuario
también confirmó ambos totales y la supervivencia del ejemplo Swing en
SLES 15 SP7 / Oracle 8u191. CASHIER con interfaz JavaFX falló al arrancar con
el puente, como se describe arriba; su compatibilidad no está acreditada. La
descarga incluye las fuentes modificadas, LGPL, autores, hashes, receta y
datos de compilación. Es una reconstrucción experimental, no un paquete
oficial de SUSE.

## Prueba en el cliente

Cerrar sólo las ventanas anteriores del ejemplo Swing. CASHIER puede seguir
abierto. Desde la sesión gráfica Linux del usuario, sin sudo:

```bash
curl --fail --location https://raw.githubusercontent.com/robertoflovan/TestAutoLinux/main/experimental/puente-corregido-v1.zip --output "$HOME/pos-atk-prueba/puente-corregido-v1.zip"
unzip "$HOME/pos-atk-prueba/puente-corregido-v1.zip" -d "$HOME/pos-atk-prueba"
bash "$HOME/pos-atk-prueba/puente-corregido-v1/probar.sh"
```

El lanzador usa explícitamente `/usr/java/jdk1.8.0_191`, verifica los hashes y
las dependencias y abre sólo el ejemplo. Si informa un error, conservarlo y no
instalar nada para resolverlo sin revisar primero. No reutilizar el PID de
una prueba anterior.

En otra terminal, desde `TestAutoLinux-main`, sustituir `12345` por el PID nuevo:

```bash
bash run.sh check --backend jna --pid 12345 --name TotalVenta --field text --equals 125.50 --out reports/corregido-inicial
```

Esperar un minuto y comprobar que sigue abierta. Cambiar el código a 1002 y
pulsar Enter. Después:

```bash
bash run.sh check --backend jna --pid 12345 --name TotalVenta --field text --equals 80.00 --out reports/corregido-cambio
```

Esperar otro minuto y repetir la consulta. Registrar tanto el resultado PASS
como la supervivencia del proceso. El puente original permanece en `extraido/`
y no se cambia la configuración de Java o del POS.
