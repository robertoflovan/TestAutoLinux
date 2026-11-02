# Lector JavaFX 1.2: opciones numeradas

Este paquete es sólo un lector. Utiliza el agente ya cargado en CASHIER y su
sesión actual; no reemplaza el JAR del agente, reinicia el POS ni cambia Java.
Ejecuta explícitamente el Java 8u191 existente. No contiene un agente ni JavaFX.

```bash
curl --fail --location https://raw.githubusercontent.com/robertoflovan/TestAutoLinux/main/experimental/fx-reader-v1.2.zip --output "$HOME/pos-fx-prueba/fx-reader-v1.2.zip"
unzip "$HOME/pos-fx-prueba/fx-reader-v1.2.zip" -d "$HOME/pos-fx-prueba"
bash "$HOME/pos-fx-prueba/fx-reader-v1.2/leer-cashier.sh" check-option hBoxFavorite 2 "Devoluciones..."
```

El comando busca el contenedor por ID, la etiqueta `[2]` en una tarjeta hija
y la única etiqueta de descripción de esa tarjeta. Compara el texto completo
con lo esperado. No depende de que la tarjeta sea la segunda en el árbol.
No trata la copia interna `LabeledText` como otra etiqueta ni confunde iconos
con descripciones. Rechaza contenedores, números o descripciones ambiguos,
ausentes y capturas parciales. Está pensado para esta estructura de tarjetas;
otros menús pueden requerir otro selector.

La comparación es exacta: `Devolucion...` y `Devoluciones...` son diferentes,
igual que tres puntos y el carácter `…`. El texto esperado debe proceder del
requisito de la prueba. Un FAIL muestra el texto encontrado y el esperado.
Los textos abreviados por la propia aplicación se comparan tal como los
expone el control; `Parcial=false` no significa que se haya recuperado un
texto completo oculto detrás de los puntos suspensivos.

Cada consulta solicita una captura nueva al agente. También admite las
operaciones existentes `inspect`, `check ID TEXTO` y `stop`.
