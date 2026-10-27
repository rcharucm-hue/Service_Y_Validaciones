const express = require('express');
const { pool } = require('../db');
const { autenticar, soloAdmin } = require('../auth');
const { noEncontrado } = require('../errores');
const v = require('../validar');

const envolver = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

/**
 * Genera el CRUD estándar que usa el cliente:
 *   GET  /            (cualquier usuario autenticado)      GET /:id
 *   POST /create      (administrador)
 *   PUT  /update/:id  (administrador)
 *   DELETE /delete/:id (administrador)
 *
 * @param {object} c { tabla, pk, columnas[], validar(body)->objeto, orden, filtros?: {param: columna} }
 */
function crud(c) {
  const r = express.Router();
  const cols = c.columnas;

  r.get('/', autenticar, envolver(async (req, res) => {
    const where = [];
    const vals = [];
    for (const [param, col] of Object.entries(c.filtros || {})) {
      if (req.query[param] !== undefined) {
        vals.push(v.entero(req.query[param], param));
        where.push(`${col} = $${vals.length}`);
      }
    }
    const q = `SELECT * FROM ${c.tabla} ${where.length ? 'WHERE ' + where.join(' AND ') : ''} ORDER BY ${c.orden}`;
    res.json((await pool.query(q, vals)).rows);
  }));

  r.get('/:id', autenticar, envolver(async (req, res) => {
    const id = v.entero(req.params.id, 'ID');
    const f = (await pool.query(`SELECT * FROM ${c.tabla} WHERE ${c.pk} = $1`, [id])).rows[0];
    if (!f) throw noEncontrado();
    res.json(f);
  }));

  r.post('/create', autenticar, soloAdmin, envolver(async (req, res) => {
    const d = c.validar(req.body);
    const marcas = cols.map((_, i) => `$${i + 1}`).join(',');
    const f = (await pool.query(
      `INSERT INTO ${c.tabla} (${cols.join(',')}) VALUES (${marcas}) RETURNING *`, cols.map((k) => d[k]))).rows[0];
    res.status(201).json(f);
  }));

  r.put('/update/:id', autenticar, soloAdmin, envolver(async (req, res) => {
    const id = v.entero(req.params.id, 'ID');
    const d = c.validar(req.body);
    const sets = cols.map((k, i) => `${k} = $${i + 1}`).join(', ');
    const f = (await pool.query(
      `UPDATE ${c.tabla} SET ${sets} WHERE ${c.pk} = $${cols.length + 1} RETURNING *`,
      [...cols.map((k) => d[k]), id])).rows[0];
    if (!f) throw noEncontrado();
    res.json(f);
  }));

  r.delete('/delete/:id', autenticar, soloAdmin, envolver(async (req, res) => {
    const id = v.entero(req.params.id, 'ID');
    const n = (await pool.query(`DELETE FROM ${c.tabla} WHERE ${c.pk} = $1`, [id])).rowCount;
    if (!n) throw noEncontrado();
    res.json({ message: 'Eliminado correctamente.' });
  }));

  return r;
}

module.exports = { crud, envolver };
