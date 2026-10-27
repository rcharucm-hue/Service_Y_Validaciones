-- SVB-GUA · Esquema PostgreSQL
-- Uso:  psql "$DATABASE_URL" -f db/schema.sql      (es idempotente: no borra datos existentes)

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario       SERIAL PRIMARY KEY,
    nombre_usuario   VARCHAR(50)  NOT NULL UNIQUE CHECK (position(':' in nombre_usuario) = 0),
    nombre_completo  VARCHAR(100) NOT NULL,
    contrasena_hash  VARCHAR(100) NOT NULL,
    rol              VARCHAR(15)  NOT NULL CHECK (rol IN ('administrador', 'vendedor')),
    fecha_creacion   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS artistas (
    id_artista       SERIAL PRIMARY KEY,
    nombre_artistico VARCHAR(120) NOT NULL,
    genero_musical   VARCHAR(50)  NOT NULL,
    pais_origen      VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS localidades (
    id_localidad     SERIAL PRIMARY KEY,
    nombre           VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS conciertos (
    id_concierto     SERIAL PRIMARY KEY,
    id_artista       INTEGER      NOT NULL REFERENCES artistas(id_artista) ON DELETE RESTRICT,
    titulo_evento    VARCHAR(150) NOT NULL,
    fecha_concierto  TIMESTAMPTZ  NOT NULL,
    recinto          VARCHAR(120) NOT NULL,
    estado           VARCHAR(12)  NOT NULL DEFAULT 'programado' CHECK (estado IN ('programado', 'activo', 'finalizado'))
);

-- Aforo por concierto y localidad. La restricción CHECK garantiza a nivel de base de datos que
-- nunca se venda más de lo que hay (disponible entre 0 y total).
CREATE TABLE IF NOT EXISTS inventario_boletos (
    id_inventario       SERIAL PRIMARY KEY,
    id_concierto        INTEGER       NOT NULL REFERENCES conciertos(id_concierto) ON DELETE RESTRICT,
    id_localidad        INTEGER       NOT NULL REFERENCES localidades(id_localidad) ON DELETE RESTRICT,
    precio              NUMERIC(10,2) NOT NULL CHECK (precio > 0),
    cantidad_total      INTEGER       NOT NULL CHECK (cantidad_total > 0),
    cantidad_disponible INTEGER       NOT NULL,
    CONSTRAINT inventario_aforo_valido CHECK (cantidad_disponible >= 0 AND cantidad_disponible <= cantidad_total),
    CONSTRAINT inventario_unico UNIQUE (id_concierto, id_localidad)
);

-- Transacción compuesta: una venta (cabecera) con uno o más renglones (detalle).
CREATE TABLE IF NOT EXISTS ventas (
    id_venta     SERIAL PRIMARY KEY,
    id_vendedor  INTEGER       NOT NULL REFERENCES usuarios(id_usuario) ON DELETE RESTRICT,
    fecha_venta  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    total_venta  NUMERIC(10,2) NOT NULL CHECK (total_venta > 0)
);

CREATE TABLE IF NOT EXISTS detalle_ventas (
    id_detalle      SERIAL PRIMARY KEY,
    id_venta        INTEGER       NOT NULL REFERENCES ventas(id_venta) ON DELETE CASCADE,
    id_inventario   INTEGER       NOT NULL REFERENCES inventario_boletos(id_inventario) ON DELETE RESTRICT,
    cantidad        INTEGER       NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2) NOT NULL CHECK (precio_unitario > 0),
    subtotal        NUMERIC(10,2) NOT NULL,
    CONSTRAINT detalle_subtotal_exacto CHECK (subtotal = precio_unitario * cantidad),
    CONSTRAINT detalle_unico_por_venta UNIQUE (id_venta, id_inventario)
);

CREATE INDEX IF NOT EXISTS idx_conciertos_artista   ON conciertos (id_artista);
CREATE INDEX IF NOT EXISTS idx_conciertos_fecha     ON conciertos (fecha_concierto);
CREATE INDEX IF NOT EXISTS idx_inventario_concierto ON inventario_boletos (id_concierto);
CREATE INDEX IF NOT EXISTS idx_ventas_vendedor      ON ventas (id_vendedor);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha         ON ventas (fecha_venta);
CREATE INDEX IF NOT EXISTS idx_detalle_venta        ON detalle_ventas (id_venta);
CREATE INDEX IF NOT EXISTS idx_detalle_inventario   ON detalle_ventas (id_inventario);
