# SVB-GUA · Sistema de venta de boletos en puntos físicos

Aplicación cliente-servidor para vender boletos de conciertos en taquilla:

- **Cliente de escritorio** en Java/JavaFX (esta carpeta, `src/`).
- **Backend** en Node.js + Express con base de datos PostgreSQL (`backend/`, ver `backend/README.md`).

Estado de cada objetivo del proyecto: **`docs/COBERTURA_OBJETIVOS.md`**.

## Puesta en marcha rápida
1. Backend (ver `backend/README.md`): crear la base de datos, `npm install`, `npm run crear-admin -- admin "<clave>" "Administrador"`, `npm start`.
2. Cliente: JDK 17+ y Maven. Apunta al backend con la variable de entorno `SVB_URL` (por defecto
   `https://svb-gua-backend.onrender.com`; para uno local: `SVB_URL=http://localhost:3000`).
3. Ejecutar: `mvn clean javafx:run`
4. Jar ejecutable: `mvn clean package` y luego `java -jar target/svb-gua-boletos-fx-1.0.0.jar`

Inicia sesión con un usuario del backend (vendedor o administrador).
Las localidades se reconocen por nombre: "Preferencial", "VIP de pie" y "Mesa".
El número de tarjeta y el NIT/CF se piden en el pago y se muestran en el comprobante; el backend actual no los almacena.

## Funciones
- Comprar boletos por sección (VIP de pie, Preferencial, Mesa...). El botón "Acomodar secciones" permite subir/bajar cada sección respecto al escenario (se guarda por concierto en esta computadora).
- Administración (solo admin): Conciertos (con artistas), Inventario y Usuarios.
- "Añadir usuario" en el login pide las credenciales de un administrador (así lo exige el backend).

## Autoservicio (pantalla táctil vertical)
- Botón **Autoservicio** (arriba a la derecha) abre la interfaz de terminal táctil en pantalla completa.
- Se adapta sola a pantalla **vertical** u horizontal. F11 alterna pantalla completa.
- Flujo: 1 Localidad (mapa) → 2 Datos (CF o NIT con teclado en pantalla) → 3 Pago (número de tarjeta) → comprobante.
- Usa los datos reales del backend. El mapa reconoce las localidades por nombre ("VIP", "Preferencial", "Mesa" y cualquier otra como General).
  El backend maneja cantidades, no mesas individuales: la localidad "Mesa" se dibuja como hasta 30 mesas y cada mesa elegida es 1 unidad.
- Para abrir directo en autoservicio: `mvn javafx:run -Djavafx.args="--kiosco"` (pide iniciar sesión primero).
- El diseño original del mapa está en `com.svbgua.kiosco`.

## Interfaces en FXML
Todas las pantallas están en `src/main/resources/com/svbgua/fxml/` (login, main, pestañas, diálogos y la carpeta
`kiosco/` con el autoservicio). Cada `.fxml` tiene su controlador (`*Controller`) en `com.svbgua.ui` y `com.svbgua.kiosco`.
Los elementos que se generan con datos (mapa del recinto, filas de listas) se dibujan desde el controlador.

## Código QR y correo
Al terminar una compra se genera un QR con los datos de la venta (se ve en pantalla). Si el cliente escribe su correo,
se le envía el comprobante con el QR. Para que el envío funcione hay que configurar la cuenta que envía:
copia `correo.properties.ejemplo` a `~/.svbgua/correo.properties` (o define SVB_SMTP_USER y SVB_SMTP_PASS).
Sin esa configuración la compra funciona igual y solo se avisa que no se pudo enviar el correo.

## Servidor
El login no pide servidor; usa `https://svb-gua-backend.onrender.com` o el valor de la variable de entorno `SVB_URL`.

## Autoservicio sin login
El autoservicio no pide usuario: la terminal se conecta sola con la cuenta de `~/.svbgua/kiosco.properties`.
La primera vez, al pulsar **Autoservicio** en el login, la app pide esa cuenta y el PIN del personal, los comprueba contra
el servidor y crea el archivo sola (también se puede crear a mano: ver `kiosco.properties.ejemplo`). Usar un usuario con rol *vendedor*. Se abre desde el botón **Autoservicio** del login o
directo al arrancar con `--kiosco`. Los clientes no tienen forma de salir (sin botón, sin Esc, sin Alt+F4):
el personal toca 5 veces el logo SVB-GUA e ingresa su PIN, lo que cierra la sesión y vuelve al login.
Si nadie toca la pantalla durante 2 minutos se descarta la compra en curso (no quedan tarjeta ni correo del cliente anterior).

## Reportes con Jasper
Pestaña Administración > Reportes: detalle de ventas, popularidad de artistas e inventario/recaudación, con filtros por
concierto, artista, vendedor y fechas. Ver `docs/GUIA_JASPER.md`.

**Filtro por vendedor:** el backend incluido ya devuelve en `GET /api/ventas` quién hizo cada venta (`id_usuario` y el objeto
`usuario`). Si se usa un backend distinto que no lo informe, la app avisa y los demás reportes funcionan igual.

## Disponibilidad en tiempo real
El mapa de boletos del sistema de ventas se actualiza solo cada 15 s (indicador "En vivo"); el autoservicio revisa cada 20 s
mientras el cliente aún no eligió nada. La compra siempre se valida en el servidor.

## Validaciones
`com.svbgua.validacion.Validaciones` (adaptada de *Service_Y_Validaciones*) aplica en el cliente las mismas reglas que la base
de datos: longitudes, DECIMAL(10,2), estados y roles permitidos, contraseña de 8 caracteres y máximo 72 bytes, entre 1 y 50 tipos
de boleto por venta, URL del servidor solo HTTPS (HTTP únicamente en localhost) y fecha ISO 8601 real. El servidor repite todas
las reglas (`backend/src/validar.js`): nunca confía en el cliente. El cliente HTTP no sigue redirecciones (no reenvía las
credenciales a otro destino) y no muestra HTML crudo cuando Render responde 502/503 mientras despierta.

Pruebas del cliente (no necesitan Maven ni JavaFX):
```
javac -d out src/main/java/com/svbgua/validacion/*.java src/test/java/com/svbgua/validacion/*.java
java -cp out com.svbgua.validacion.ValidacionesPrueba
```

## Administración de catálogo
Administración > **Artistas y localidades**: crear, editar y eliminar (con confirmación; el servidor impide borrar lo que ya
tiene conciertos o inventario). Conciertos, inventario/precios y usuarios tienen su propia pestaña.

## Si no llegan los correos
Revisa `~/.svbgua/correo-error.log`: ahí queda el motivo (sin `correo.properties`, contraseña de aplicación incorrecta,
puerto 587 bloqueado por el firewall, etc.).


## Panel de reportes con graficas Jasper

Al entrar a Administracion > Reportes se cargan las graficas de ventas, popularidad de artistas,
inventario y recaudacion por concierto. Los PDF exportados incluyen tabla, totales y graficas.
Consultar docs/REPORTES_GRAFICAS.md para uso, filtros, criterios de recaudacion y pruebas.
