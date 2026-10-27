const express = require('express');
const bcrypt = require('bcryptjs');
const { pool } = require('../db');
const { autenticar, soloAdmin, verificar, olvidarCache } = require('../auth');
const { envolver } = require('./crud');
const { ErrorHttp, noAutenticado, noEncontrado, conflicto } = require('../errores');
const v = require('../validar');

const r = express.Router();
const PUBLICO = 'id_usuario, nombre_usuario, nombre_completo, rol, fecha_creacion';

// Freno básico a la fuerza bruta: 10 intentos fallidos por IP y minuto.
const intentos = new Map();
function frenoLogin(req, res, next) {
  const ip = req.ip;
  const ahora = Date.now();
  const e = intentos.get(ip);
  if (e && e.hasta > ahora && e.n >= 10) return next(new ErrorHttp(429, 'Demasiados intentos. Espera un minuto.'));
  if (!e || e.hasta <= ahora) intentos.set(ip, { n: 0, hasta: ahora + 60_000 });
  next();
}

r.post('/login', frenoLogin, envolver(async (req, res) => {
  const { nombre_usuario, contrasena } = req.body || {};
  const u = await verificar(nombre_usuario, contrasena);
  if (!u) {
    intentos.get(req.ip).n += 1;
    throw noAutenticado('Usuario o contraseña incorrectos.');
  }
  intentos.delete(req.ip);
  res.json({ message: 'Inicio de sesión correcto.', usuario: u });
}));

r.get('/', autenticar, soloAdmin, envolver(async (req, res) => {
  res.json((await pool.query(`SELECT ${PUBLICO} FROM usuarios ORDER BY nombre_usuario`)).rows);
}));

// Identidad de la sesión actual (cualquier rol). Debe ir ANTES de '/:id'.
r.get('/me', autenticar, (req, res) => res.json(req.usuario));

r.get('/:id', autenticar, soloAdmin, envolver(async (req, res) => {
  const f = (await pool.query(`SELECT ${PUBLICO} FROM usuarios WHERE id_usuario = $1`, [v.entero(req.params.id, 'ID')])).rows[0];
  if (!f) throw noEncontrado('Usuario no encontrado.');
  res.json(f);
}));

r.post('/create', autenticar, soloAdmin, envolver(async (req, res) => {
  const d = v.usuario(req.body, true);
  const hash = await bcrypt.hash(d.contrasena, 10);
  const f = (await pool.query(
    `INSERT INTO usuarios (nombre_usuario, nombre_completo, contrasena_hash, rol) VALUES ($1,$2,$3,$4)
     RETURNING ${PUBLICO}`, [d.nombre_usuario, d.nombre_completo, hash, d.rol])).rows[0];
  res.status(201).json(f);
}));

/** Impide dejar el sistema sin administradores. */
async function otroAdminExiste(id) {
  const n = (await pool.query(
    "SELECT count(*)::int AS n FROM usuarios WHERE rol = 'administrador' AND id_usuario <> $1", [id])).rows[0].n;
  return n > 0;
}

r.put('/update/:id', autenticar, soloAdmin, envolver(async (req, res) => {
  const id = v.entero(req.params.id, 'ID');
  const actual = (await pool.query(
    'SELECT nombre_usuario, nombre_completo, rol FROM usuarios WHERE id_usuario = $1', [id])).rows[0];
  if (!actual) throw noEncontrado('Usuario no encontrado.');
  const d = v.usuarioParcial(req.body, actual); // acepta cambios parciales (solo los campos enviados)
  if (actual.rol === 'administrador' && d.rol !== 'administrador' && !(await otroAdminExiste(id))) {
    throw conflicto('No puedes quitar el rol al único administrador.');
  }
  const hash = d.contrasena ? await bcrypt.hash(d.contrasena, 10) : null;
  const f = (await pool.query(
    `UPDATE usuarios SET nombre_usuario=$1, nombre_completo=$2, rol=$3,
            contrasena_hash = COALESCE($4, contrasena_hash)
      WHERE id_usuario=$5 RETURNING ${PUBLICO}`, [d.nombre_usuario, d.nombre_completo, d.rol, hash, id])).rows[0];
  olvidarCache(); // la clave o el rol pudieron cambiar
  res.json(f);
}));

r.delete('/delete/:id', autenticar, soloAdmin, envolver(async (req, res) => {
  const id = v.entero(req.params.id, 'ID');
  if (id === req.usuario.id_usuario) throw conflicto('No puedes eliminar tu propia cuenta.');
  const actual = (await pool.query('SELECT rol FROM usuarios WHERE id_usuario = $1', [id])).rows[0];
  if (!actual) throw noEncontrado('Usuario no encontrado.');
  if (actual.rol === 'administrador' && !(await otroAdminExiste(id))) {
    throw conflicto('No puedes eliminar al único administrador.');
  }
  await pool.query('DELETE FROM usuarios WHERE id_usuario = $1', [id]); // 23503 si ya tiene ventas
  olvidarCache();
  res.json({ message: 'Usuario eliminado.' });
}));

module.exports = r;
