const express = require('express');
const { ErrorHttp, desdePg } = require('./errores');
const { artistas, localidades, conciertos, inventario } = require('./rutas/catalogo');

const app = express();
app.disable('x-powered-by');
app.set('trust proxy', 1); // Render / proxies: req.ip real para el freno de login
app.use(express.json({ limit: '100kb' }));

app.get('/', (req, res) => res.json({ servicio: 'SVB-GUA API', estado: 'ok' }));
app.get('/health', (req, res) => res.json({ estado: 'ok' }));

app.use('/api/usuarios', require('./rutas/usuarios'));
app.use('/api/artistas', artistas);
app.use('/api/localidades', localidades);
app.use('/api/conciertos', conciertos);
app.use('/api/inventario-boletos', inventario);
app.use('/api/ventas', require('./rutas/ventas'));

app.use((req, res) => res.status(404).json({ message: 'Ruta no encontrada.' }));

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  if (err.type === 'entity.parse.failed') return res.status(400).json({ message: 'El cuerpo de la petición no es JSON válido.' });
  const e = err instanceof ErrorHttp ? err : desdePg(err);
  if (e) return res.status(e.status).json({ message: e.message });
  console.error(err);
  res.status(500).json({ message: 'Error interno del servidor.' });
});

module.exports = app;
