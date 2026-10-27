# Correccion de mesas en autoservicio

Se corrigio el autoservicio para el uso confirmado de una sola computadora.

## Cambios

- Cada respuesta de inventario se filtra por id_concierto antes de construir el mapa.
  Las mesas y sus precios solo salen del inventario de ese concierto. Si no hay mesas, no se dibujan ni se venden.
- Al cambiar de concierto se descartan inventario, localidades y seleccion anteriores.
  Las respuestas y sondeos iniciados antes de una nueva carga o compra no pueden sustituir el mapa actual.
- Antes de registrar una venta se vuelve a consultar y validar el inventario del concierto.
- Al recibir una venta confirmada se guardan los identificadores de los asientos seleccionados.
  Si se venden los cuatro asientos de una mesa, los cuatro y la mesa aparecen ocupados al reconstruir el mapa.
- La ocupacion se conserva en ~/.svbgua/asientos-vendidos.properties, separada por servidor,
  concierto e inventario. No contiene nombres, contrasenas, tarjetas ni correos.
- Las selecciones abandonadas y las solicitudes fallidas no se registran como ventas.
- Los bloqueos locales se combinan con las cantidades disponibles del servidor; no reemplazan
  las comprobaciones de stock de la API. Las escrituras del archivo se hacen mediante reemplazo atomico.
- Si una venta se confirma y falla la escritura en disco, se conserva la ocupacion en memoria
  y se avisa; la venta no se presenta como fallida para evitar comprar dos veces.

## Alcance

Este registro corresponde a una terminal. La API sigue almacenando cantidades, no identidades de asiento.
Los numeros exactos de compras anteriores a esta correccion no pueden recuperarse del backend actual;
se respetan sus cantidades vendidas. Las nuevas compras confirmadas desde esta version si guardan
los asientos exactos. El archivo de ocupacion debe conservarse junto con la configuracion de esa terminal.
Para varias computadoras seria necesario incorporar la asignacion de asientos en la API/base de datos.

## Verificacion

22 comprobaciones nuevas: concierto sin mesas, mesa ajena de Q20, precio real, bloqueo de una mesa completa,
recarga, reinicio, orientaciones horizontal/vertical, seleccion rechazada de asientos vendidos, separacion por
servidor/concierto, respuesta antigua y cantidad insuficiente. Las pruebas usan archivos temporales del workspace.
Las 47 validaciones existentes siguen pasando. Se compilo para Java 17 y se verifico visualmente el mapa JavaFX
con datos de prueba, sin realizar ventas en Render. No se cambiaron los reportes Jasper ni el backend.

Cerrar la aplicacion anterior y abrir el ejecutable actualizado o ejecutar el proyecto original en NetBeans.
