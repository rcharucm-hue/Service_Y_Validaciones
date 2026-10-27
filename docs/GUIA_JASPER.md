# Reportes con JasperReports (ya integrados)

Pestaña **Administración > Reportes** (solo administrador). Genera PDF con tres reportes:

| Reporte | Plantilla | Filtros |
|---|---|---|
| Detalle de ventas | `ventas-detalle.jrxml` | concierto, artista, vendedor, fechas |
| Popularidad de artistas (ranking) | `artistas-popularidad.jrxml` | concierto, artista, vendedor, fechas |
| Inventario y recaudación por concierto | `inventario-concierto.jrxml` | concierto, artista |

## Cómo funciona
`ApiClient` (REST) → `ReporteDatos` (filtra y agrupa, sin Jasper) → filas JavaBean (`FilaVenta`, `FilaArtista`,
`FilaInventario`) → `ReporteService` (compila el `.jrxml`, lo llena con `JRBeanCollectionDataSource` y exporta el PDF).

Archivos: `src/main/java/com/svbgua/reportes/`, plantillas en `src/main/resources/com/svbgua/reportes/`,
pantalla en `fxml/reportes-tab.fxml` + `ui/ReportesController`.

## Editar el diseño
Abre cualquier `.jrxml` con **Jaspersoft Studio** (versión 6.x). Los *fields* deben conservar su nombre (coinciden con los
getters de la clase fila: `getSubtotal` ↔ `subtotal`). Parámetros disponibles: `TITULO`, `SUBTITULO` (filtros aplicados),
`GENERADO`, `USUARIO`. Las plantillas usan *Language = java* y la fuente DejaVu Sans (dependencia `jasperreports-fonts`).

## Agregar un reporte nuevo
1. Crea la clase fila (JavaBean con getters; **no** `record`) en `com.svbgua.reportes`.
2. Agrega el método que arma las filas en `ReporteDatos` (con sus filtros).
3. Copia una plantilla `.jrxml`, cambia *fields* y columnas.
4. Añade un valor al enum `ReporteService.Tipo` y el `case` en `ReportesController.generar`.

## Errores típicos
| Error | Causa |
|---|---|
| `Font 'DejaVu Sans' is not available` | Falta `jasperreports-fonts`. |
| Valores vacíos | El nombre del field no coincide con el getter. |
| Funciona en el IDE y no en el .jar | Falta el `AppendingTransformer` de `jasperreports_extension.properties` (ya está en el pom). |
| Error al compilar la plantilla | Falta `ecj` (ya está en el pom) o la plantilla quedó en *Language = groovy*. |
