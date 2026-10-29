# Puente corregido: descarga experimental

`puente-corregido-v1.zip` contiene Java ATK Wrapper 0.33.2 reconstruido con la
corrección de claves de eventos. No contiene un JRE ni instala paquetes.

[Ejecución de CI verificada](https://github.com/robertoflovan/TestAutoLinux/actions/runs/40791886977):
el original devuelve PASS y luego termina con SIGSEGV en `g_int_hash`; el
corregido supera seis consultas, leyendo los totales 125.50 y 80.00 después de
un cambio por teclado, y sigue vivo tras las desconexiones del lector y una
espera final de 10 segundos.

La prueba se ejecutó con Ubuntu 22.04 x86_64 y Temurin 8u504. **Pendiente**:
validación en SLES 15 SP7 / Oracle 8u191 y, posteriormente, en CASHIER. La
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
