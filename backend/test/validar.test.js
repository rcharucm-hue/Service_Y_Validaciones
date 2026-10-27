const test = require('node:test');
const assert = require('node:assert/strict');
const v = require('../src/validar');

const rechaza = (fn, texto) => assert.throws(fn, (e) => e.status === 400 && (!texto || e.message.includes(texto)));

test('artista: obligatorios y longitudes', () => {
  rechaza(() => v.artista({ nombre_artistico: ' ', genero_musical: 'Rock' }));
  rechaza(() => v.artista({ nombre_artistico: 'x'.repeat(121), genero_musical: 'Rock' }), '120');
  rechaza(() => v.artista(null));
  assert.deepEqual(v.artista({ nombre_artistico: ' A ', genero_musical: 'Rock' }),
    { nombre_artistico: 'A', genero_musical: 'Rock', pais_origen: null });
});

test('dinero: >0, 2 decimales, tope DECIMAL(10,2)', () => {
  rechaza(() => v.dinero('0', 'Precio'));
  rechaza(() => v.dinero('0.001', 'Precio'));
  rechaza(() => v.dinero('-5', 'Precio'));
  rechaza(() => v.dinero('100000000.00', 'Precio'));
  rechaza(() => v.dinero('abc', 'Precio'));
  assert.equal(v.dinero('99999999.99', 'Precio'), '99999999.99');
  assert.equal(v.dinero(150, 'Precio'), '150.00');
  assert.equal(v.dinero('0.10', 'Precio'), '0.10');
});

test('fecha: calendario estricto (el 30 de febrero no existe)', () => {
  rechaza(() => v.fecha('2026-02-30T20:00:00', 'Fecha'));
  rechaza(() => v.fecha('2026-11-20T25:00:00Z', 'Fecha'));
  rechaza(() => v.fecha('mañana', 'Fecha'));
  rechaza(() => v.fecha(undefined, 'Fecha'));
  assert.equal(v.fecha('2028-02-29T20:00:00-06:00', 'Fecha'), '2028-02-29T20:00:00-06:00'); // bisiesto
  rechaza(() => v.fecha('2027-02-29T20:00:00', 'Fecha'));
  assert.equal(v.fecha('2026-11-20T20:00:00Z', 'Fecha'), '2026-11-20T20:00:00Z');
  assert.equal(v.fecha(null, 'Fecha', false), null);
});

test('concierto: estado por defecto y valores permitidos', () => {
  const base = { id_artista: 1, titulo_evento: 'Gira', fecha_concierto: '2026-11-20T20:00:00-06:00', recinto: 'Estadio' };
  assert.equal(v.concierto(base).estado, 'programado');
  rechaza(() => v.concierto({ ...base, estado: 'CANCELADO' }));
  rechaza(() => v.concierto({ ...base, id_artista: 0 }));
});

test('inventario: disponible no puede superar el total', () => {
  const b = { id_concierto: 1, id_localidad: 1, precio: '25.00', cantidad_total: 10, cantidad_disponible: 11 };
  rechaza(() => v.inventario(b), 'no puede superar');
  rechaza(() => v.inventario({ ...b, cantidad_disponible: -1 }));
  assert.equal(v.inventario({ ...b, cantidad_disponible: 0 }).cantidad_disponible, 0);
  assert.equal(v.inventario({ ...b, cantidad_disponible: undefined }).cantidad_disponible, 10); // por defecto = total
});

test('usuario: rol, ":" y límites de contraseña (bcrypt = 72 bytes)', () => {
  const b = { nombre_usuario: 'ana', nombre_completo: 'Ana', rol: 'vendedor', contrasena: '12345678' };
  rechaza(() => v.usuario({ ...b, nombre_usuario: 'a:b' }), 'dos puntos');
  rechaza(() => v.usuario({ ...b, rol: 'root' }));
  rechaza(() => v.usuario({ ...b, contrasena: 'corta' }));
  rechaza(() => v.usuario({ ...b, contrasena: 'ñ'.repeat(37) })); // 74 bytes
  assert.equal(v.usuario({ ...b, contrasena: '' }, false).contrasena, null); // editar sin cambiar clave
  rechaza(() => v.usuario({ ...b, contrasena: '' }, true));
});

test('detallesVenta: 1..50 renglones, sin repetidos, ordenados', () => {
  rechaza(() => v.detallesVenta({ detalles: [] }));
  rechaza(() => v.detallesVenta({}));
  rechaza(() => v.detallesVenta({ detalles: [{ id_inventario: 1, cantidad: 0 }] }));
  rechaza(() => v.detallesVenta({ detalles: [{ id_inventario: 1, cantidad: 1 }, { id_inventario: 1, cantidad: 2 }] }), 'agrupa');
  rechaza(() => v.detallesVenta({ detalles: Array.from({ length: 51 }, (_, i) => ({ id_inventario: i + 1, cantidad: 1 })) }));
  const r = v.detallesVenta({ detalles: [{ id_inventario: 9, cantidad: 1 }, { id_inventario: 2, cantidad: 3 }] });
  assert.deepEqual(r.map((x) => x.id_inventario), [2, 9]);
});

test('usuarioParcial: solo cambia lo enviado y conserva el resto', () => {
  const actual = { nombre_usuario: 'ana', nombre_completo: 'Ana Pérez', rol: 'vendedor' };
  const r = v.usuarioParcial({ nombre_completo: 'Ana María Pérez' }, actual);
  assert.deepEqual(r, { nombre_usuario: 'ana', nombre_completo: 'Ana María Pérez', rol: 'vendedor', contrasena: null });
  assert.equal(v.usuarioParcial({ contrasena: 'nuevaClave1' }, actual).contrasena, 'nuevaClave1');
  rechaza(() => v.usuarioParcial({}, actual), 'al menos un campo');
  rechaza(() => v.usuarioParcial({ rol: 'dios' }, actual));
  rechaza(() => v.usuarioParcial({ contrasena: 'corta' }, actual));
});
