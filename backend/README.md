# SVB-GUA · Backend (Node.js + Express + PostgreSQL)

API REST que consume el cliente JavaFX. Autenticación **HTTP Basic** (contraseñas con bcrypt) y dos roles:
`administrador` y `vendedor`.

## Instalación
```bash
cd backend
npm install
cp .env.example .env          # editar DATABASE_URL (y PGSSL=true en Render)
npm run db:schema             # crea las tablas (idempotente)
npm run db:seed               # opcional: localidades y artistas de ejemplo
npm run crear-admin -- admin "UnaClaveSegura1" "Administrador General"
npm start                     # http://localhost:3000
```
En Render: *Build* `npm install`, *Start* `npm start`, variables `DATABASE_URL` y `PGSSL=true`.

## Endpoints
Todos devuelven JSON; los errores tienen la forma `{ "message": "..." }`.
**Lectura**: cualquier usuario autenticado. **Escritura de catálogo y usuarios**: solo administrador.

| Recurso | Rutas |
|---|---|
| Usuarios | `POST /api/usuarios/login` (público) · `GET /api/usuarios/me` · `GET /` y `GET /:id` (admin) · `POST /create` · `PUT /update/:id` (parcial) · `DELETE /delete/:id` |
| Artistas | `GET /api/artistas` · `GET /:id` · `POST /create` · `PUT /update/:id` · `DELETE /delete/:id` |
| Localidades | `/api/localidades` (igual que artistas) |
| Conciertos | `/api/conciertos` (filtro `?id_artista=`) |
| Inventario/aforo | `/api/inventario-boletos` (filtros `?id_concierto=` `?id_localidad=`) — `cantidad_disponible` en tiempo real |
| Ventas | `POST /api/ventas/create` (admin o vendedor) · `GET /api/ventas` (`?desde=&hasta=` AAAA-MM-DD) · `GET /api/ventas/:id` |

El **vendedor** solo ve sus propias ventas; el **administrador** ve todas. El vendedor de una venta sale de la sesión, nunca del
cuerpo de la petición, y el precio se toma de la base de datos (el cliente no puede fijarlo).

### Venta con varios boletos
```json
POST /api/ventas/create
{ "detalles": [ { "id_inventario": 3, "cantidad": 2 }, { "id_inventario": 5, "cantidad": 4 } ] }
```
Una sola transacción: **todo o nada**. Cada renglón descuenta con `UPDATE ... WHERE cantidad_disponible >= cantidad`, por lo que dos
taquillas vendiendo lo último a la vez no pueden sobrevender (PostgreSQL serializa la fila hasta el COMMIT); la restricción
`inventario_aforo_valido` del esquema es la segunda defensa. Si un renglón no alcanza → `409` con el mensaje (ej. "Solo quedan 2
boletos en VIP") y no se descuenta nada. No se venden conciertos `finalizado`.

## Pruebas
```bash
npm test      # Node >= 18, sin dependencias: validaciones y lógica transaccional de la venta
```
Las pruebas de ventas usan un doble en memoria de `pg.Pool`: verifican el flujo (orden de bloqueo, totales exactos, ROLLBACK,
liberación de conexión) pero **no** ejecutan el SQL. Antes de producción conviene una prueba de integración contra un PostgreSQL
real, incluyendo dos ventas concurrentes sobre el último boleto.

## Seguridad
- Basic Auth solo es seguro sobre **HTTPS** (Render lo da por defecto); el cliente rechaza HTTP salvo en localhost.
- bcrypt (costo 10); las claves se limitan a 72 bytes (límite de bcrypt); verificación recordada 60 s en memoria (hash SHA-256).
- Freno de fuerza bruta en el login (10 intentos/minuto por IP) y no se puede eliminar ni degradar al único administrador.
