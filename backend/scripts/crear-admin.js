// Crea (o actualiza la clave de) un usuario. Uso:
//   npm run crear-admin -- admin "MiClaveSegura1" "Administrador General" [administrador|vendedor]
require('dotenv').config();
const bcrypt = require('bcryptjs');
const { pool } = require('../src/db');
const v = require('../src/validar');

(async () => {
  const [usuario, clave, nombre = 'Administrador', rol = 'administrador'] = process.argv.slice(2);
  try {
    if (!usuario || !clave) throw new Error('Uso: npm run crear-admin -- <usuario> <clave> "<Nombre completo>" [rol]');
    v.usuario({ nombre_usuario: usuario, nombre_completo: nombre, rol, contrasena: clave }, true);
    const hash = await bcrypt.hash(clave, 10);
    const r = await pool.query(
      `INSERT INTO usuarios (nombre_usuario, nombre_completo, contrasena_hash, rol)
       VALUES ($1,$2,$3,$4)
       ON CONFLICT (nombre_usuario) DO UPDATE SET contrasena_hash = EXCLUDED.contrasena_hash,
         nombre_completo = EXCLUDED.nombre_completo, rol = EXCLUDED.rol
       RETURNING id_usuario`, [usuario.trim(), nombre.trim(), hash, rol]);
    console.log(`Usuario "${usuario}" (${rol}) listo con id ${r.rows[0].id_usuario}.`);
  } catch (e) {
    console.error(e.message);
    process.exitCode = 1;
  } finally {
    await pool.end();
  }
})();
