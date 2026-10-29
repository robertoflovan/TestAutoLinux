PUENTE JAVA ATK CORREGIDO - PRUEBA EXPERIMENTAL V1

Este paquete NO contiene ni instala Java. Usa el JDK existente:
/usr/java/jdk1.8.0_191

Abrir desde la misma sesion grafica Linux del usuario, sin sudo:
bash "$HOME/pos-atk-prueba/puente-corregido-v1/probar.sh"

El lanzador verifica hashes, dependencias nativas y la ruta de Java. Abre
UNICAMENTE el ejemplo PosSwingControl con el puente local. No cambia paquetes,
alternativas Java, propiedades globales de accesibilidad ni CASHIER. Conserva
el puente anterior en extraido/. Compila el ejemplo en pos-atk-prueba/clases.

Deja la ventana abierta. En otra terminal, dentro de TestAutoLinux-main,
sustituye 12345 por el PID NUEVO mostrado por el ejemplo:

bash run.sh check --backend jna --pid 12345 --name TotalVenta --field text --equals 125.50 --out reports/corregido-inicial

Espera un minuto y comprueba que la ventana sigue abierta. Cambia en el ejemplo
el codigo 1001 por 1002, pulsa Enter y ejecuta:

bash run.sh check --backend jna --pid 12345 --name TotalVenta --field text --equals 80.00 --out reports/corregido-cambio

Espera un minuto mas y vuelve a consultar. Un PASS solo valida el valor leido;
tambien hay que comprobar que la aplicacion sigue viva tras las consultas.
Si aparece un error de dependencias o un cierre, conservar los mensajes/logs.

ALCANCE DE LAS PRUEBAS
BUILD-INFO.txt identifica el entorno de compilacion. TEST-RESULTS.txt registra
la comparacion automatica realizada en Ubuntu 22.04 x86_64 / Temurin Java 8.
No sustituye la prueba en SLES 15 SP7 / Oracle Java 8u191. Tampoco demuestra
todavia la accesibilidad de los controles de CASHIER.

FUENTES Y LICENCIA
Java ATK Wrapper: LGPL 2.1 o posterior, ver COPYING.LESSER y AUTHORS.
modified-sources.tar.xz contiene las fuentes modificadas de esta compilacion.
Se incluye el parche aplicado y la receta de construccion.
Receta, fuentes de distribucion y pruebas:
https://github.com/robertoflovan/TestAutoLinux/tree/main/patches
https://github.com/robertoflovan/TestAutoLinux/tree/main/scripts

Esta es una reconstruccion experimental basada en las fuentes y parches
publicos de openSUSE Leap 15.6. No es un paquete oficial de SUSE.
