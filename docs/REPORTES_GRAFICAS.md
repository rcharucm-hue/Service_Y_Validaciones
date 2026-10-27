# Reportes con graficas Jasper

## Uso

1. Abrir el proyecto en NetBeans y ejecutar com.svbgua.Main, o usar el JAR de target.
2. Iniciar sesion como administrador.
3. Entrar a Administracion > Reportes: se consultan catalogo, inventario, ventas y usuarios.
4. Se muestran automaticamente las graficas Jasper de detalle de ventas, popularidad de artistas,
   inventario y recaudacion confirmada por concierto.
5. Seleccionar filtros y pulsar Aplicar filtros. Actualizar datos vuelve a consultar la API.
6. Elegir el tipo de reporte y pulsar Guardar PDF con grafica.

Cada PDF contiene tabla completa, totales y grafica. Inventario incluye dos graficas separadas
para no mezclar unidades monetarias con cantidades. Las graficas muestran las diez categorias principales;
las tablas y los totales del PDF incluyen todas las filas filtradas.
Si no existen datos, la pantalla y el PDF muestran un mensaje: no se inventan ventas ni categorias.

## Criterios

- Detalle de ventas: grafica de ingresos confirmados por concierto y tabla de cada detalle.
- Popularidad: clasificacion por boletos vendidos por artista, con su recaudacion en la tabla.
- Inventario: cantidades vendidas confirmadas y disponibilidad actual agrupadas por concierto.
- Recaudacion: suma de subtotales historicos de los detalles, independiente del precio actual.
- Ocupacion: proporcion del inventario total que actualmente no esta disponible.
- Concierto y artista aplican a las tres secciones. Vendedor y fechas aplican a ventas/popularidad;
  inventario y su recaudacion historica solo usan concierto/artista. La pantalla lo indica.
- El PDF usa los datos de la ultima vista aplicada. Pulsar Aplicar filtros tras cambiar una seleccion.

## Implementacion

Las graficas son barChart de JasperReports; no se sustituyen por graficas JavaFX.
ReporteService renderiza un JasperPrint como PNG para mostrarlo en los ImageView del panel.
Los PDF incluyen los barChart dentro de las plantillas JRXML originales.
ReporteGraficas agrega las categorias antes de enviarlas al subDataset de Jasper.
La consulta a la API y la compilacion/renderizacion de Jasper se ejecutan fuera del hilo JavaFX.
La version de cada solicitud impide que una vista anterior sustituya los filtros mas recientes.

No se modifico la API ni la base de datos y no se guardan datos de acceso en estos cambios.
La primera carga puede tardar por el arranque de Render y la compilacion inicial de Jasper.
El boton Actualizar datos permite reintentar cuando el servicio no esta disponible.

## Verificacion local

- Compilacion de las fuentes para Java 17 con las dependencias del JAR incluido: correcta.
- 47 comprobaciones de validacion existentes: correctas.
- 18 comprobaciones nuevas de agregacion y Jasper: correctas.
- Carga real del FXML y ejecucion asincrona del controlador con datos ficticios: cuatro graficas
  generadas, mensajes de vacio ocultos y exportacion habilitada.
- Generacion de los tres PDF con datos y sus tres variantes sin datos; revision visual de las seis paginas con datos.
- La prueba no utiliza credenciales ni escribe ventas en Render.
- El ejecutable de target se actualizo con las clases y recursos verificados; no se hizo una resolucion Maven limpia.

La prueba src/test/java/com/svbgua/reportes/ReportesPrueba.java puede ejecutarse con las clases de prueba
compiladas y las dependencias del proyecto en el classpath. Recibe opcionalmente la carpeta de salida.
