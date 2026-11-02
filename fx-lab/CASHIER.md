# Primera inspección JavaFX de CASHIER

El usuario ya confirmó que el ejemplo, con Oracle 8u191 en SLES, permite
leer 125.50 y después 80.00, y enumerar 24 nodos sin captura parcial.
Esto aún no prueba el arranque ni los controles propios de CASHIER.

El lanzador se limita al equipo y la instalación Colombia revisados. Crea
copias privadas de `iniciaGeopos.sh` y `runPos.sh`: conserva el cierre habitual
del POS y VLC, los argumentos, AspectJ, classpath, bibliotecas de dispositivos
y `logs/output.log`. Añade el inspector JavaFX y desactiva la propiedad del
puente ATK para ese proceso. Usa exactamente el Java 8u191 ya instalado.
Los originales se leen y no se modifican.

[Verificación con scripts y Java simulados](https://github.com/robertoflovan/TestAutoLinux/actions/runs/41508579829):
preparación sin ejecutar cierres, secuencia de reinicio conservada, argumentos
y rutas originales conservados, agente añadido, sesión del ejemplo separada y
rechazo de un lanzador ambiguo antes del cierre. Esta prueba no ejecuta CASHIER
ni comprueba aún la convivencia real del inspector con AspectJ dentro del POS.

## Descargar y preparar

El kit `fx-lab-v1` de la prueba anterior debe seguir en `~/pos-fx-prueba`.
Desde la sesión gráfica habitual, sin sudo:

```bash
curl --fail --location https://raw.githubusercontent.com/robertoflovan/TestAutoLinux/main/scripts/restart-cashier-fx.sh --output "$HOME/pos-fx-prueba/restart-cashier-fx.sh"
curl --fail --location https://raw.githubusercontent.com/robertoflovan/TestAutoLinux/main/scripts/read-cashier-fx.sh --output "$HOME/pos-fx-prueba/read-cashier-fx.sh"
bash "$HOME/pos-fx-prueba/restart-cashier-fx.sh" --preparar
```

Este paso verifica archivos y crea las copias y `cambios.txt`; no cierra ni
inicia aplicaciones. Revisa ese archivo en la ruta mostrada. Si falla, conserva
la salida y no continúes con el arranque.

## Reiniciar e inspeccionar

Cuando corresponda reiniciar el POS de pruebas, `--iniciar` vuelve a preparar
las copias y ejecuta **el cierre habitual de CASHIER y VLC**, seguido del
arranque con el inspector. No ejecuta el ejemplo JavaFX.

```bash
bash "$HOME/pos-fx-prueba/restart-cashier-fx.sh" --iniciar
```

Espera a que CASHIER termine de cargar. Comprueba que la pantalla funciona
antes de consultar el árbol. Luego, en otra terminal:

```bash
bash "$HOME/pos-fx-prueba/read-cashier-fx.sh" inspect
```

Este lector usa una sesión separada de la del ejemplo. No asumas que existe
el ID `TotalVenta`: el reporte del POS mostrará sus tipos, IDs y textos reales.
Si indica captura parcial, aún no se puede validar un valor con `check`.
Revisa los datos del cliente antes de compartir el reporte.

## Validar una opción numerada sin ID propio

El [lector separado 1.2](reader/README.md) añade `check-option CONTENEDOR NUMERO TEXTO`.
Puede usarse con el agente ya cargado: se descarga en otra carpeta y no requiere
reiniciar CASHIER. Para la sección `hBoxFavorite`, la opción identificada por
`[2]` se busca dentro de su tarjeta, aunque cambie el orden de las tarjetas.
Una búsqueda ambigua falla, en lugar de elegir la primera etiqueta encontrada.
El texto esperado se compara exactamente, incluyendo abreviaturas y puntos.

El arranque y la lectura aún requieren validación en CASHIER real. Si la
ventana no abre o se cierra, conserva la salida y las últimas líneas de
`~/geopos/current/logs/output.log` antes de otro arranque, que puede reemplazarlas.
Para volver al arranque normal, usa el icono habitual o `bash "$HOME/iniciaGeopos.sh"`.
Ese procedimiento conserva también sus cierres habituales; no se cambia Java.
