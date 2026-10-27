# Cobertura de objetivos y alcance

Proyecto: **Sistema de Venta de Boletos en Puntos Físicos para Conciertos y Eventos Musicales.**

Leyenda: ✅ cubierto · 🆕 cubierto en esta fusión (antes faltaba o estaba incompleto) · ⛔ fuera de alcance por diseño.

## Objetivo general
| Requisito | Estado | Dónde |
|---|---|---|
| Cliente de escritorio Java (JavaFX) | ✅ | `src/` |
| Backend Node.js con Express | 🆕 | `backend/` (ninguno de los tres archivos originales lo incluía) |
| Integridad de transacciones compuestas (varios boletos por compra) | 🆕 | `backend/src/servicios/ventas.js` (todo o nada) |
| Control estricto del aforo en tiempo real | 🆕 | descuento atómico + CHECK en BD; el cliente refresca cada 15–20 s |

## Objetivos específicos
| Objetivo | Estado | Dónde |
|---|---|---|
| Autenticación y autorización por roles (`administrador`, `vendedor`) | ✅ / 🆕 | Login y pestañas por rol en el cliente; `backend/src/auth.js` aplica el rol en el servidor |
| Interfaz Java ágil para el vendedor (artista/concierto, localidad, cantidad) | ✅ | `BoletosController`, autoservicio táctil |
| API RESTful (artistas, conciertos, localidades, inventario, transacciones con varios ítems) | 🆕 | `backend/src/rutas/` |
| Base de datos relacional PostgreSQL con catálogo y transacciones detalladas | 🆕 | `backend/db/schema.sql` (`ventas` + `detalle_ventas`) |
| Gestión de agenda y aforo (artistas, conciertos, aforo inicial por localidad) | ✅ / 🆕 | Artistas y localidades ahora tienen CRUD completo (antes solo se podían crear) |
| Procesamiento de ventas de 1+ boletos por transacción, descontando inventario | ✅ / 🆕 | Cliente + servicio transaccional del servidor |
| Reportes por concierto, por artista y por vendedor | ✅ / 🆕 | JasperReports; el filtro por vendedor ahora funciona porque el backend informa el vendedor de cada venta |

## Alcance — Incluye
| Elemento | Estado | Dónde |
|---|---|---|
| Login basado en la tabla Usuarios | ✅ / 🆕 | `usuarios` con bcrypt |
| Módulo de administración (Artistas, Conciertos, Localidades, Usuarios) | 🆕 | Pestaña nueva **Artistas y localidades** + las existentes |
| Módulo de ventas con filtros por artista o fecha | ✅ | `VentasController` (y `?desde=&hasta=` en la API) |
| Compra de múltiples boletos por transacción | ✅ | Carrito y autoservicio |
| Disponibilidad en tiempo real | ✅ | Mapa "En vivo" |
| Códigos QR o envío por correo | ✅ | `Qr.java`, `Correo.java` |
| Registro histórico de ventas y detalles | ✅ | `ventas` + `detalle_ventas` |
| Reportería con JasperReports | ✅ | `reportes/*.jrxml` |

## Fuera de alcance (no se implementaron a propósito)
En tu lista, los tres últimos puntos aparecían bajo "Incluye", pero por su naturaleza (el proyecto es de **puntos físicos**) los traté
como **no incluidos**. Si en realidad sí los quieres, dímelo: son proyectos grandes por sí solos.

| Elemento | Estado | Nota |
|---|---|---|
| Venta en línea para público general (e-commerce web) | ⛔ | Solo vendedores autorizados en taquilla |
| Pasarelas de pago integradas (cobro en efectivo/POS externo) | ⛔ | El diálogo de pago captura tarjeta/NIT solo para el comprobante; no se procesa ningún cobro y el servidor no guarda la tarjeta |
| Selección gráfica de asientos individualizados | ⛔ | El mapa elige por **localidad y cantidad** (las mesas se dibujan pero cada una es 1 unidad), coherente con el inventario por localidad |

## Qué se tomó de cada archivo
| Archivo | Aporte |
|---|---|
| `svb-gua-boletos-fxautoservicio_v6.zip` | **Base del proyecto**: ventas, autoservicio, QR, correo, JasperReports, FXML, usuarios |
| `Service_Y_Validaciones-main.zip` | Reglas de validación y sus pruebas (`com.svbgua.validacion`), chequeos del cliente HTTP (HTTPS, sin redirecciones, Basic Auth seguro) y el **contrato de la API** (`/usuarios/me`, `inventario` anidado en el detalle, actualización parcial de usuarios), que se usó para diseñar el backend |
| `svb-gua-boletos-fxautomejora2.rar` | Resultó ser una versión **anterior** de v6 (v6 contiene todo lo suyo y más: reportes, correo, QR, kiosco en FXML). No aportó funciones nuevas; se conservó v6 tal cual |
