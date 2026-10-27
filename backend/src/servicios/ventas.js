/**
 * Venta compuesta (varios tipos de boleto en una sola transacción) con control estricto de aforo.
 *
 * Garantías:
 *  - TODO o NADA: si cualquier renglón falla se hace ROLLBACK y no se descuenta ningún boleto.
 *  - Sin sobreventa: el descuento es un UPDATE atómico condicionado (cantidad_disponible >= cantidad). PostgreSQL
 *    bloquea la fila hasta el COMMIT, así que dos taquillas vendiendo lo último a la vez nunca se pisan;
 *    la CHECK inventario_aforo_valido es la segunda línea de defensa.
 *  - Los renglones se procesan en orden de id_inventario para evitar interbloqueos (deadlocks).
 *  - El precio se toma de la base de datos al momento de la venta; el cliente no puede fijarlo.
 *  - No se vende un concierto "finalizado".
 */
const v = require('../validar');
const { invalido, noEncontrado, conflicto } = require('../errores');

const MAX_CENTAVOS = 9_999_999_999; // NUMERIC(10,2)

const aCentavos = (txt) => Number(txt.replace('.', '')); // NUMERIC(10,2) siempre llega con 2 decimales
const aTexto = (c) => `${Math.floor(c / 100)}.${String(c % 100).padStart(2, '0')}`;

const SQL_VENTA = `
  SELECT v.id_venta, v.fecha_venta, v.total_venta::text AS total_venta, v.id_vendedor,
         u.nombre_usuario, u.nombre_completo,
         COALESCE(json_agg(json_build_object(
             'id_inventario', d.id_inventario, 'cantidad', d.cantidad,
             'precio_unitario', d.precio_unitario::text, 'subtotal', d.subtotal::text,
             'inventario', json_build_object('id_inventario', inv.id_inventario,
                                             'id_concierto', inv.id_concierto, 'id_localidad', inv.id_localidad))
           ORDER BY d.id_detalle) FILTER (WHERE d.id_detalle IS NOT NULL), '[]'::json) AS detalles
    FROM ventas v
    JOIN usuarios u ON u.id_usuario = v.id_vendedor
    LEFT JOIN detalle_ventas d ON d.id_venta = v.id_venta
    LEFT JOIN inventario_boletos inv ON inv.id_inventario = d.id_inventario`;

/** Forma que consume el cliente Java (ApiClient.venta). */
function aRespuesta(f) {
  return {
    id_venta: f.id_venta,
    fecha_venta: f.fecha_venta,
    total_venta: f.total_venta,
    id_vendedor: f.id_vendedor,
    id_usuario: f.id_vendedor,
    usuario: { id_usuario: f.id_vendedor, nombre_usuario: f.nombre_usuario, nombre_completo: f.nombre_completo },
    detalles: f.detalles,
  };
}

/** Explica por qué no se pudo descontar un renglón. */
async function explicarFallo(client, { id_inventario, cantidad }) {
  const r = await client.query(
    `SELECT i.cantidad_disponible, c.estado, c.titulo_evento, l.nombre AS localidad
       FROM inventario_boletos i
       JOIN conciertos c ON c.id_concierto = i.id_concierto
       JOIN localidades l ON l.id_localidad = i.id_localidad
      WHERE i.id_inventario = $1`, [id_inventario]);
  const f = r.rows[0];
  if (!f) return noEncontrado(`El inventario ${id_inventario} no existe.`);
  if (f.estado === 'finalizado') return conflicto(`El concierto "${f.titulo_evento}" ya finalizó; no se pueden vender boletos.`);
  return conflicto(f.cantidad_disponible === 0
    ? `Boletos agotados en ${f.localidad} (${f.titulo_evento}).`
    : `Solo quedan ${f.cantidad_disponible} boletos en ${f.localidad} (${f.titulo_evento}); pediste ${cantidad}.`);
}

/**
 * @param {{connect: Function}} pool  pg.Pool (o un doble en las pruebas)
 * @param {number} idVendedor
 * @param {object} cuerpo  { detalles: [{ id_inventario, cantidad }] }
 */
async function crearVenta(pool, idVendedor, cuerpo) {
  const detalles = v.detallesVenta(cuerpo);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    let totalCent = 0;
    const renglones = [];

    for (const d of detalles) {
      const r = await client.query(
        `UPDATE inventario_boletos i
            SET cantidad_disponible = i.cantidad_disponible - $2
           FROM conciertos c
          WHERE i.id_inventario = $1
            AND c.id_concierto = i.id_concierto
            AND c.estado <> 'finalizado'
            AND i.cantidad_disponible >= $2
        RETURNING i.precio::text AS precio`, [d.id_inventario, d.cantidad]);
      if (r.rowCount === 0) throw await explicarFallo(client, d);

      const precioCent = aCentavos(r.rows[0].precio);
      const subCent = precioCent * d.cantidad;
      totalCent += subCent;
      if (totalCent > MAX_CENTAVOS) throw invalido('El total de la venta excede el máximo permitido (99999999.99).');
      renglones.push({ ...d, precio: aTexto(precioCent), subtotal: aTexto(subCent) });
    }

    const venta = await client.query(
      'INSERT INTO ventas (id_vendedor, total_venta) VALUES ($1, $2) RETURNING id_venta', [idVendedor, aTexto(totalCent)]);
    const idVenta = venta.rows[0].id_venta;
    for (const r of renglones) {
      await client.query(
        `INSERT INTO detalle_ventas (id_venta, id_inventario, cantidad, precio_unitario, subtotal)
         VALUES ($1,$2,$3,$4,$5)`, [idVenta, r.id_inventario, r.cantidad, r.precio, r.subtotal]);
    }

    const final = await client.query(`${SQL_VENTA} WHERE v.id_venta = $1 GROUP BY v.id_venta, u.id_usuario`, [idVenta]);
    await client.query('COMMIT');
    return aRespuesta(final.rows[0]);
  } catch (e) {
    try { await client.query('ROLLBACK'); } catch { /* la conexión ya se perdió */ }
    throw e;
  } finally {
    client.release();
  }
}

/** Administrador: todas. Vendedor: solo las suyas (idVendedor). Filtros opcionales por fecha (YYYY-MM-DD). */
async function listarVentas(db, { idVendedor = null, desde = null, hasta = null } = {}) {
  const r = await db.query(
    `${SQL_VENTA}
      WHERE ($1::int IS NULL OR v.id_vendedor = $1)
        AND ($2::date IS NULL OR v.fecha_venta >= $2::date)
        AND ($3::date IS NULL OR v.fecha_venta <  $3::date + 1)
      GROUP BY v.id_venta, u.id_usuario
      ORDER BY v.fecha_venta DESC, v.id_venta DESC`, [idVendedor, desde, hasta]);
  return r.rows.map(aRespuesta);
}

async function obtenerVenta(db, id, { idVendedor = null } = {}) {
  const r = await db.query(
    `${SQL_VENTA} WHERE v.id_venta = $1 AND ($2::int IS NULL OR v.id_vendedor = $2)
      GROUP BY v.id_venta, u.id_usuario`, [id, idVendedor]);
  if (!r.rows[0]) throw noEncontrado('Venta no encontrada.');
  return aRespuesta(r.rows[0]);
}

module.exports = { crearVenta, listarVentas, obtenerVenta, aCentavos, aTexto };
