/**
 * Validaciones del servidor. Son las mismas reglas que aplica el cliente Java (com.svbgua.validacion.Validaciones):
 * el servidor NUNCA confía en el cliente, así que se repiten aquí.
 */
const { invalido } = require('./errores');

const ROLES = ['administrador', 'vendedor'];
const ESTADOS = ['programado', 'activo', 'finalizado'];
const MAX_TIPOS_POR_VENTA = 50;

const exigir = (cumple, mensaje) => { if (!cumple) throw invalido(mensaje); };

function objeto(b) {
  exigir(b && typeof b === 'object' && !Array.isArray(b), 'Debe proporcionar los datos.');
  return b;
}

function entero(v, campo, { min = 1, max = 2147483647 } = {}) {
  const n = typeof v === 'string' && /^-?\d+$/.test(v.trim()) ? Number(v) : v;
  exigir(Number.isInteger(n) && n >= min && n <= max, `${campo} debe ser un entero ${min > 0 ? 'positivo' : 'no negativo'}.`);
  return n;
}

function texto(v, campo, max, obligatorio = true) {
  if (v === undefined || v === null || (typeof v === 'string' && v.trim() === '')) {
    exigir(!obligatorio, `${campo} es obligatorio.`);
    return null;
  }
  exigir(typeof v === 'string', `${campo} debe ser texto.`);
  exigir(v.trim().length <= max, `${campo} supera ${max} caracteres.`);
  return v.trim();
}

/** Mayor que cero, máximo 2 decimales, cabe en NUMERIC(10,2). Devuelve texto "123.40". */
function dinero(v, campo) {
  const t = typeof v === 'number' ? String(v) : v;
  exigir(typeof t === 'string' && /^\d{1,8}(\.\d{1,2})?$/.test(t.trim()),
    `${campo} debe ser mayor que cero, con máximo dos decimales y hasta 99999999.99.`);
  exigir(Number(t) > 0, `${campo} debe ser mayor que cero.`);
  return Number(t).toFixed(2);
}

/** ISO 8601 con calendario estricto (rechaza 30 de febrero, hora 25, etc.). Devuelve el mismo texto. */
function fecha(v, campo, obligatorio = true) {
  if (v === undefined || v === null || v === '') {
    exigir(!obligatorio, `${campo} es obligatorio.`);
    return null;
  }
  const m = typeof v === 'string' && v.trim().match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?(Z|[+-]\d{2}:?\d{2})?$/);
  exigir(m, `${campo} debe usar fecha y hora ISO 8601.`);
  const [, a, mes, d, h, mi, s = '0', zona] = m;
  const A = +a, M = +mes, D = +d;
  const diasMes = new Date(Date.UTC(A, M, 0)).getUTCDate();
  exigir(M >= 1 && M <= 12 && D >= 1 && D <= diasMes && +h <= 23 && +mi <= 59 && +s <= 59,
    `${campo} no es una fecha válida.`);
  if (zona && zona !== 'Z') {
    const hz = +zona.slice(1, 3), mz = +zona.slice(-2);
    exigir(hz <= 18 && mz <= 59, `${campo} tiene una zona horaria inválida.`);
  }
  return v.trim();
}

function opcion(v, campo, lista) {
  exigir(lista.includes(v), `${campo} debe ser uno de: ${lista.join(', ')}.`);
  return v;
}

function contrasena(v) {
  exigir(typeof v === 'string' && v.length >= 8 && Buffer.byteLength(v, 'utf8') <= 72,
    'La contraseña debe tener al menos 8 caracteres y como máximo 72 bytes UTF-8.');
  return v;
}

// ------------------------------------------------------------- entidades

function artista(b) {
  objeto(b);
  return {
    nombre_artistico: texto(b.nombre_artistico, 'Nombre artístico', 120),
    genero_musical: texto(b.genero_musical, 'Género musical', 50),
    pais_origen: texto(b.pais_origen, 'País de origen', 50, false),
  };
}

function localidad(b) {
  objeto(b);
  return { nombre: texto(b.nombre, 'Localidad', 50) };
}

function concierto(b) {
  objeto(b);
  return {
    id_artista: entero(b.id_artista, 'Artista'),
    titulo_evento: texto(b.titulo_evento, 'Título', 150),
    fecha_concierto: fecha(b.fecha_concierto, 'Fecha del concierto'),
    recinto: texto(b.recinto, 'Recinto', 120),
    estado: b.estado == null ? 'programado' : opcion(b.estado, 'Estado', ESTADOS),
  };
}

function inventario(b) {
  objeto(b);
  const total = entero(b.cantidad_total, 'Cantidad total');
  const disp = entero(b.cantidad_disponible ?? b.cantidad_total, 'Cantidad disponible', { min: 0 });
  exigir(disp <= total, 'Cantidad disponible no puede superar la cantidad total.');
  return {
    id_concierto: entero(b.id_concierto, 'Concierto'),
    id_localidad: entero(b.id_localidad, 'Localidad'),
    precio: dinero(b.precio, 'Precio'),
    cantidad_total: total,
    cantidad_disponible: disp,
  };
}

/** @param {boolean} completo true al crear (todo obligatorio); false al editar (contraseña opcional) */
function usuario(b, completo = true) {
  objeto(b);
  const nombre_usuario = texto(b.nombre_usuario, 'Nombre de usuario', 50);
  exigir(!nombre_usuario.includes(':'), 'El nombre de usuario no puede contener dos puntos.');
  const salida = {
    nombre_usuario,
    nombre_completo: texto(b.nombre_completo, 'Nombre completo', 100),
    rol: opcion(b.rol, 'Rol', ROLES),
    contrasena: null,
  };
  if (completo || (b.contrasena !== undefined && b.contrasena !== null && b.contrasena !== '')) {
    salida.contrasena = contrasena(b.contrasena);
  }
  return salida;
}

/**
 * Actualización parcial: solo se validan y cambian los campos enviados; el resto se conserva de `actual`.
 * Devuelve { nombre_usuario, nombre_completo, rol, contrasena|null } ya fusionado.
 */
function usuarioParcial(b, actual) {
  objeto(b);
  const campos = ['nombre_usuario', 'nombre_completo', 'rol', 'contrasena'];
  exigir(campos.some((c) => b[c] !== undefined && b[c] !== null && b[c] !== ''), 'Envía al menos un campo para actualizar.');
  const quita = (c) => (b[c] === undefined || b[c] === null || b[c] === '' ? actual[c] : b[c]);
  return usuario({
    nombre_usuario: quita('nombre_usuario'),
    nombre_completo: quita('nombre_completo'),
    rol: quita('rol'),
    contrasena: b.contrasena,
  }, false);
}

/** Devuelve [{id_inventario, cantidad}] ordenado por id_inventario (orden fijo de bloqueo → sin deadlocks). */
function detallesVenta(b) {
  objeto(b);
  const d = b.detalles;
  exigir(Array.isArray(d) && d.length >= 1 && d.length <= MAX_TIPOS_POR_VENTA,
    `La venta debe incluir entre 1 y ${MAX_TIPOS_POR_VENTA} tipos de boleto.`);
  const vistos = new Set();
  const lista = d.map((x) => {
    objeto(x);
    const id_inventario = entero(x.id_inventario, 'Inventario');
    const cantidad = entero(x.cantidad, 'Cantidad');
    exigir(!vistos.has(id_inventario), 'No repitas inventario; agrupa las cantidades.');
    vistos.add(id_inventario);
    return { id_inventario, cantidad };
  });
  return lista.sort((p, q) => p.id_inventario - q.id_inventario);
}

module.exports = {
  ROLES, ESTADOS, MAX_TIPOS_POR_VENTA,
  entero, texto, dinero, fecha, opcion, contrasena,
  artista, localidad, concierto, inventario, usuario, usuarioParcial, detallesVenta,
};
