/**
 * Prueba la lógica transaccional de crearVenta con un doble en memoria de pg.Pool que imita BEGIN/COMMIT/ROLLBACK
 * y las sentencias que usa el servicio. NO sustituye una prueba contra PostgreSQL real (ver README del backend).
 */
const test = require('node:test');
const assert = require('node:assert/strict');
const { crearVenta, aCentavos, aTexto } = require('../src/servicios/ventas');

function crearPool(inventario, conciertos = { 1: 'programado' }) {
  const estado = { inventario: structuredClone(inventario), ventas: [], detalles: [], log: [] };
  let snapshot = null;
  const client = {
    async query(sql, p = []) {
      const s = sql.replace(/\s+/g, ' ').trim();
      estado.log.push(s.split(' ')[0]);
      if (s === 'BEGIN') { snapshot = structuredClone({ i: estado.inventario, v: estado.ventas, d: estado.detalles }); return { rows: [], rowCount: 0 }; }
      if (s === 'COMMIT') { snapshot = null; return { rows: [], rowCount: 0 }; }
      if (s === 'ROLLBACK') { Object.assign(estado, { inventario: snapshot.i, ventas: snapshot.v, detalles: snapshot.d }); return { rows: [], rowCount: 0 }; }
      if (s.startsWith('UPDATE inventario_boletos')) {
        const [id, cant] = p;
        const f = estado.inventario[id];
        if (!f || conciertos[f.id_concierto] === 'finalizado' || f.cantidad_disponible < cant) return { rows: [], rowCount: 0 };
        f.cantidad_disponible -= cant;
        return { rows: [{ precio: f.precio }], rowCount: 1 };
      }
      if (s.startsWith('SELECT i.cantidad_disponible, c.estado')) {
        const f = estado.inventario[p[0]];
        return f ? { rows: [{ cantidad_disponible: f.cantidad_disponible, estado: conciertos[f.id_concierto], titulo_evento: 'Gira', localidad: f.localidad }], rowCount: 1 } : { rows: [], rowCount: 0 };
      }
      if (s.startsWith('INSERT INTO ventas')) { estado.ventas.push({ id_venta: 1, id_vendedor: p[0], total_venta: p[1] }); return { rows: [{ id_venta: 1 }], rowCount: 1 }; }
      if (s.startsWith('INSERT INTO detalle_ventas')) { estado.detalles.push({ id_inventario: p[1], cantidad: p[2], precio_unitario: p[3], subtotal: p[4] }); return { rows: [], rowCount: 1 }; }
      if (s.includes('FROM ventas v')) {
        const v = estado.ventas[0];
        return { rows: [{ ...v, fecha_venta: 'ahora', nombre_usuario: 'ana', nombre_completo: 'Ana', detalles: estado.detalles }], rowCount: 1 };
      }
      throw new Error('SQL no previsto en el doble: ' + s);
    },
    release() { estado.log.push('RELEASE'); },
  };
  return { estado, connect: async () => client };
}

const INV = () => ({
  1: { id_concierto: 1, localidad: 'VIP', precio: '100.00', cantidad_disponible: 5 },
  2: { id_concierto: 1, localidad: 'Mesa', precio: '0.10', cantidad_disponible: 10 },
  3: { id_concierto: 1, localidad: 'General', precio: '45.50', cantidad_disponible: 2 },
});

test('conversión exacta de dinero (sin errores de punto flotante)', () => {
  assert.equal(aCentavos('0.10') * 3, 30);
  assert.equal(aTexto(30), '0.30');
  assert.equal(aTexto(4550 * 3), '136.50');
  assert.equal(aTexto(5), '0.05');
});

test('venta con varios tipos de boleto: descuenta, calcula el total y registra el detalle', async () => {
  const pool = crearPool(INV());
  const r = await crearVenta(pool, 7, { detalles: [{ id_inventario: 3, cantidad: 2 }, { id_inventario: 1, cantidad: 3 }, { id_inventario: 2, cantidad: 3 }] });
  assert.equal(pool.estado.inventario[1].cantidad_disponible, 2);
  assert.equal(pool.estado.inventario[2].cantidad_disponible, 7);
  assert.equal(pool.estado.inventario[3].cantidad_disponible, 0);
  assert.equal(pool.estado.ventas[0].total_venta, '391.30'); // 300.00 + 0.30 + 91.00
  assert.equal(pool.estado.ventas[0].id_vendedor, 7);
  assert.equal(pool.estado.detalles.length, 3);
  assert.deepEqual(pool.estado.detalles.map((d) => d.id_inventario), [1, 2, 3]); // orden fijo de bloqueo
  assert.equal(r.usuario.nombre_usuario, 'ana');
  assert.equal(pool.estado.log.at(-1), 'RELEASE');
  assert.ok(pool.estado.log.includes('COMMIT'));
});

test('sobreventa: si un renglón no alcanza se revierte TODA la venta', async () => {
  const pool = crearPool(INV());
  await assert.rejects(
    crearVenta(pool, 7, { detalles: [{ id_inventario: 1, cantidad: 3 }, { id_inventario: 3, cantidad: 5 }] }),
    (e) => e.status === 409 && /Solo quedan 2/.test(e.message));
  assert.equal(pool.estado.inventario[1].cantidad_disponible, 5, 'el renglón 1 se restauró');
  assert.equal(pool.estado.ventas.length, 0);
  assert.equal(pool.estado.detalles.length, 0);
  assert.ok(pool.estado.log.includes('ROLLBACK'));
  assert.ok(!pool.estado.log.includes('COMMIT'));
  assert.equal(pool.estado.log.at(-1), 'RELEASE', 'la conexión siempre se libera');
});

test('agotado, inexistente y concierto finalizado dan mensajes claros', async () => {
  const inv = INV();
  inv[3].cantidad_disponible = 0;
  inv[2].id_concierto = 2;
  const pool = crearPool(inv, { 1: 'programado', 2: 'finalizado' });
  await assert.rejects(crearVenta(pool, 1, { detalles: [{ id_inventario: 3, cantidad: 1 }] }), /agotados/);
  await assert.rejects(crearVenta(pool, 1, { detalles: [{ id_inventario: 99, cantidad: 1 }] }), (e) => e.status === 404);
  await assert.rejects(crearVenta(pool, 1, { detalles: [{ id_inventario: 2, cantidad: 1 }] }), /finaliz/);
});

test('datos inválidos se rechazan antes de abrir transacción', async () => {
  const pool = crearPool(INV());
  await assert.rejects(crearVenta(pool, 1, { detalles: [] }), (e) => e.status === 400);
  await assert.rejects(crearVenta(pool, 1, { detalles: [{ id_inventario: 1, cantidad: -2 }] }), (e) => e.status === 400);
  assert.equal(pool.estado.log.length, 0);
});

test('el total no puede exceder NUMERIC(10,2)', async () => {
  const inv = { 1: { id_concierto: 1, localidad: 'Gold', precio: '99999999.99', cantidad_disponible: 5 } };
  const pool = crearPool(inv);
  await assert.rejects(crearVenta(pool, 1, { detalles: [{ id_inventario: 1, cantidad: 2 }] }), (e) => e.status === 400 && /excede/.test(e.message));
  assert.equal(pool.estado.inventario[1].cantidad_disponible, 5);
});
