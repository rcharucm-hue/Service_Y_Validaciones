const { crud } = require('./crud');
const v = require('../validar');

const artistas = crud({
  tabla: 'artistas', pk: 'id_artista', orden: 'nombre_artistico',
  columnas: ['nombre_artistico', 'genero_musical', 'pais_origen'], validar: v.artista,
});

const localidades = crud({
  tabla: 'localidades', pk: 'id_localidad', orden: 'nombre',
  columnas: ['nombre'], validar: v.localidad,
});

const conciertos = crud({
  tabla: 'conciertos', pk: 'id_concierto', orden: 'fecha_concierto',
  columnas: ['id_artista', 'titulo_evento', 'fecha_concierto', 'recinto', 'estado'], validar: v.concierto,
  filtros: { id_artista: 'id_artista' },
});

// Consulta de disponibilidad en tiempo real: cualquier usuario autenticado lee cantidad_disponible.
const inventario = crud({
  tabla: 'inventario_boletos', pk: 'id_inventario', orden: 'id_concierto, id_localidad',
  columnas: ['id_concierto', 'id_localidad', 'precio', 'cantidad_total', 'cantidad_disponible'], validar: v.inventario,
  filtros: { id_concierto: 'id_concierto', id_localidad: 'id_localidad' },
});

module.exports = { artistas, localidades, conciertos, inventario };
