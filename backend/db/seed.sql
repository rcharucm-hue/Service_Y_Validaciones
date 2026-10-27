-- Datos de ejemplo (opcional). Los usuarios se crean con:  npm run crear-admin -- <usuario> <clave> "<Nombre completo>"
INSERT INTO localidades (nombre) VALUES ('VIP de pie'), ('Preferencial'), ('Mesa')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO artistas (nombre_artistico, genero_musical, pais_origen)
SELECT * FROM (VALUES
    ('Artista de ejemplo 1', 'Rock', 'Guatemala'),
    ('Artista de ejemplo 2', 'Pop',  'México')
) AS v(nombre_artistico, genero_musical, pais_origen)
WHERE NOT EXISTS (SELECT 1 FROM artistas);
