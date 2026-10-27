const express = require('express');
const { pool } = require('../db');
const { autenticar } = require('../auth');
const { envolver } = require('./crud');
const { crearVenta, listarVentas, obtenerVenta } = require('../servicios/ventas');
const v = require('../validar');
const { invalido } = require('../errores');

const r = express.Router();
const esAdmin = (req) => req.usuario.rol === 'administrador';
const soloFecha = (x, campo) => {
  if (x === undefined) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(x)) throw invalido(`${campo} debe tener formato AAAA-MM-DD.`);
  v.fecha(`${x}T00:00:00`, campo);
  return x;
};

// Vender: admin o vendedor. El vendedor queda registrado desde la sesión, nunca desde el cuerpo de la petición.
r.post('/create', autenticar, envolver(async (req, res) => {
  res.status(201).json(await crearVenta(pool, req.usuario.id_usuario, req.body));
}));

// Administrador: todas. Vendedor: solo las suyas.
r.get('/', autenticar, envolver(async (req, res) => {
  res.json(await listarVentas(pool, {
    idVendedor: esAdmin(req) ? null : req.usuario.id_usuario,
    desde: soloFecha(req.query.desde, 'desde'),
    hasta: soloFecha(req.query.hasta, 'hasta'),
  }));
}));

r.get('/:id', autenticar, envolver(async (req, res) => {
  res.json(await obtenerVenta(pool, v.entero(req.params.id, 'ID'), { idVendedor: esAdmin(req) ? null : req.usuario.id_usuario }));
}));

module.exports = r;
