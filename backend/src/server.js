require('dotenv').config();
const app = require('./app');
const { pool } = require('./db');

const puerto = Number(process.env.PORT) || 3000;
const servidor = app.listen(puerto, () => console.log(`SVB-GUA API escuchando en el puerto ${puerto}`));

function cerrar() {
  servidor.close(() => pool.end().then(() => process.exit(0)));
}
process.on('SIGTERM', cerrar);
process.on('SIGINT', cerrar);
