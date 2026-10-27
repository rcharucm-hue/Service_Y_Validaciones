const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { pool } = require('./db');
const { noAutenticado, prohibido } = require('./errores');

// bcrypt es lento a propósito; el cliente envía Basic Auth en cada petición, así que se recuerda por 60 s
// una verificación exitosa (clave: hash SHA-256 de usuario:clave, nunca la clave en claro).
const CACHE_MS = 60_000;
const cache = new Map();
const HASH_FALSO = bcrypt.hashSync('relleno-para-igualar-tiempos', 10);

const sha = (t) => crypto.createHash('sha256').update(t).digest('hex');

/** Verifica usuario y clave. Devuelve { id_usuario, nombre_usuario, nombre_completo, rol } o null. */
async function verificar(nombre, clave, db = pool) {
  if (typeof nombre !== 'string' || typeof clave !== 'string' || !nombre || !clave) return null;
  const llave = sha(`${nombre}:${clave}`);
  const hit = cache.get(llave);
  if (hit && hit.hasta > Date.now()) return hit.usuario;

  const r = await db.query(
    'SELECT id_usuario, nombre_usuario, nombre_completo, rol, contrasena_hash FROM usuarios WHERE nombre_usuario = $1',
    [nombre.trim()]);
  const fila = r.rows[0];
  // Se compara siempre contra algún hash para que el tiempo no revele si el usuario existe.
  const ok = await bcrypt.compare(clave, fila ? fila.contrasena_hash : HASH_FALSO);
  if (!fila || !ok) return null;
  const { contrasena_hash, ...usuario } = fila;
  cache.set(llave, { usuario, hasta: Date.now() + CACHE_MS });
  return usuario;
}

function olvidarCache() {
  cache.clear();
}

function leerBasic(req) {
  const h = req.headers.authorization || '';
  if (!h.startsWith('Basic ')) return null;
  const dec = Buffer.from(h.slice(6), 'base64').toString('utf8');
  const i = dec.indexOf(':');
  return i < 0 ? null : { nombre: dec.slice(0, i), clave: dec.slice(i + 1) };
}

/** Exige Basic Auth válido; deja el usuario en req.usuario. */
async function autenticar(req, res, next) {
  try {
    const c = leerBasic(req);
    const u = c && (await verificar(c.nombre, c.clave));
    if (!u) {
      res.set('WWW-Authenticate', 'Basic realm="svb-gua"');
      throw noAutenticado('Usuario o contraseña incorrectos.');
    }
    req.usuario = u;
    next();
  } catch (e) {
    next(e);
  }
}

const soloAdmin = (req, res, next) =>
  req.usuario && req.usuario.rol === 'administrador' ? next() : next(prohibido('Solo un administrador puede hacer esto.'));

module.exports = { verificar, autenticar, soloAdmin, olvidarCache, leerBasic };
