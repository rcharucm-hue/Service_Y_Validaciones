package com.svbgua.kiosco;

import com.svbgua.api.ApiClient;
import com.svbgua.kiosco.datos.FabricaLocalidades;
import com.svbgua.kiosco.modelo.InfoConcierto;
import com.svbgua.kiosco.modelo.LineaSeleccion;
import com.svbgua.kiosco.util.Formato;
import com.svbgua.model.Modelos.Catalogo;
import com.svbgua.model.Modelos.Concierto;
import com.svbgua.model.Modelos.Inventario;
import com.svbgua.ui.Async;
import com.svbgua.ui.Fxml;
import com.svbgua.util.Correo;
import com.svbgua.util.Qr;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Terminal de autoservicio (autoservicio.fxml). Coordina los pasos (paso1, paso2, paso3 y comprobante,
 * cada uno con su propio .fxml), los modales y la conexión con el backend. Se adapta sola a pantalla
 * vertical u horizontal.
 */
public class AutoservicioController implements Navegacion {

    private static final ZoneId GT = ZoneId.of("America/Guatemala");
    /** Si nadie toca la pantalla este tiempo, se borra todo para que el siguiente cliente no vea datos ajenos. */
    private static final int INACTIVIDAD_SEGUNDOS = 120;

    private final ApiClient api = ApiClient.INSTANCE;
    private final SesionKiosco sesion = new SesionKiosco();

    @FXML private StackPane raiz;
    @FXML private BorderPane marco;
    @FXML private StackPane capaModal;
    @FXML private StackPane tarjetaModal;
    @FXML private Label lblAviso;

    // Pasos incluidos con <fx:include fx:id="x"> (raíz = x, controlador = xController)
    @FXML private Node encabezado;
    @FXML private EncabezadoController encabezadoController;
    @FXML private Node paso1;
    @FXML private Paso1Controller paso1Controller;
    @FXML private Node paso2;
    @FXML private Paso2Controller paso2Controller;
    @FXML private Node paso3;
    @FXML private Paso3Controller paso3Controller;
    @FXML private Node comprobante;
    @FXML private ComprobanteController comprobanteController;

    private Runnable alSalirPersonal = () -> { };
    private String pinPersonal = "";
    /** Cada cuánto el autoservicio revisa si cambió la disponibilidad (segundos). */
    private static final int SEGUNDOS_SONDEO = 20;
    private final Timeline sondeo = new Timeline(new KeyFrame(Duration.seconds(SEGUNDOS_SONDEO), e -> sondearDisponibilidad()));
    private final PauseTransition inactividad = new PauseTransition(Duration.seconds(INACTIVIDAD_SEGUNDOS));
    private Catalogo catalogo;
    private Concierto concierto;
    private List<Inventario> inventarios = List.of();

    private boolean vertical;
    private int paso = 1; // 1 localidad, 2 datos, 3 pago, 4 comprobante
    private Timeline animAviso;

    @FXML
    private void initialize() {
        paso1Controller.iniciar(sesion, this);
        paso2Controller.iniciar(sesion, this);
        paso3Controller.iniciar(sesion, this);
        comprobanteController.iniciar(sesion, this);
        encabezadoController.setAlRegresar(this::alRegresar);
        encabezadoController.setAlCambiarConcierto(() -> mostrarSelectorConcierto(catalogo.activos()));
        encabezadoController.setAlGestoPersonal(this::pedirPin);

        inactividad.setOnFinished(e -> alInactividad());
        raiz.addEventFilter(InputEvent.ANY, e -> inactividad.playFromStart());

        raiz.widthProperty().addListener((o, a, b) -> alCambiarTamano());
        raiz.heightProperty().addListener((o, a, b) -> alCambiarTamano());
        sesion.seleccion.addListener(this::alCambiarSeleccion);
    }

    /**
     * Arranca el autoservicio. Los clientes no tienen forma de salir: solo el personal, con el gesto oculto
     * (5 toques al logo) y el PIN, que llama a {@code alSalirPersonal}.
     */
    public void iniciar(Runnable alSalirPersonal, String pin) {
        this.alSalirPersonal = alSalirPersonal;
        this.pinPersonal = pin == null ? "" : pin;
        inactividad.playFromStart();
        sondeo.setCycleCount(Animation.INDEFINITE);
        sondeo.play();
        cargarCatalogo();
    }

    // ================================================================== carga de datos

    private void cargarCatalogo() {
        mostrarCargando("Cargando conciertos...");
        Async.run(api::cargarCatalogo, c -> {
            catalogo = c;
            List<Concierto> activos = c.activos();
            if (activos.isEmpty()) {
                mostrarMensaje("No hay conciertos disponibles",
                        "Por ahora no hay conciertos programados para la venta.", "Reintentar", this::cargarCatalogo);
            } else if (activos.size() == 1) {
                elegirConcierto(activos.get(0));
            } else {
                mostrarSelectorConcierto(activos);
            }
        }, ex -> mostrarMensaje("No se pudo conectar", mensaje(ex), "Reintentar", this::cargarCatalogo));
    }

    private void elegirConcierto(Concierto c) {
        concierto = c;
        sesion.info = construirInfo(c);
        sesion.seleccion.limpiar();
        cargarInventario();
    }

    private void cargarInventario() {
        mostrarCargando("Cargando localidades...");
        Async.run(() -> api.listarInventario(concierto.id()), lista -> {
            inventarios = lista;
            actualizarOrientacion();
            reconstruirLocalidades();
            cerrarModal();
            mostrarPaso1();
        }, ex -> mostrarMensaje("No se pudieron cargar las localidades", mensaje(ex), "Reintentar", this::cargarInventario));
    }

    private void reconstruirLocalidades() {
        sesion.localidades = FabricaLocalidades.crear(inventarios, catalogo, vertical);
    }

    private InfoConcierto construirInfo(Concierto c) {
        String fecha;
        try {
            ZonedDateTime z = OffsetDateTime.parse(c.fecha()).atZoneSameInstant(GT);
            String t = z.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy · h:mm a", Locale.forLanguageTag("es-GT")));
            fecha = Character.toUpperCase(t.charAt(0)) + t.substring(1);
        } catch (RuntimeException e) {
            fecha = c.fecha();
        }
        return new InfoConcierto(catalogo.nombreArtista(c.idArtista()), c.titulo(), fecha, c.recinto());
    }

    private static String mensaje(Throwable t) {
        return t == null || t.getMessage() == null ? "Error desconocido." : t.getMessage();
    }

    // ================================================================== orientacion

    private void actualizarOrientacion() {
        if (raiz.getWidth() > 0 && raiz.getHeight() > 0) {
            vertical = raiz.getHeight() > raiz.getWidth();
        }
    }

    private void alCambiarTamano() {
        if (raiz.getWidth() <= 0 || raiz.getHeight() <= 0) {
            return;
        }
        boolean v = raiz.getHeight() > raiz.getWidth();
        if (v == vertical) {
            return;
        }
        vertical = v;
        if (catalogo == null || concierto == null || capaModal.isVisible()) {
            return;
        }
        switch (paso) {
            case 1 -> {
                reconstruirLocalidades();
                mostrarPaso1();
            }
            case 2 -> mostrarPaso2();
            case 3 -> mostrarPaso3();
            case 4 -> mostrarComprobante();
            default -> { }
        }
    }

    // ================================================================== pasos

    private void ponerEncabezado(String titulo, boolean conRegresar) {
        boolean conCambiar = paso == 1 && catalogo != null && catalogo.activos().size() > 1;
        encabezadoController.configurar(titulo, sesion.info, paso, conRegresar, conCambiar, vertical);
    }

    private void mostrarPaso1() {
        paso = 1;
        comprobanteController.detener();
        ponerEncabezado("Selecciona tu localidad", false);
        marco.setCenter(paso1);
        paso1Controller.mostrar(sesion.localidades, vertical);
    }

    private void mostrarPaso2() {
        paso = 2;
        comprobanteController.detener();
        ponerEncabezado("¿Cómo quieres tu factura?", true);
        marco.setCenter(paso2);
        paso2Controller.mostrar(vertical);
    }

    private void mostrarPaso3() {
        paso = 3;
        comprobanteController.detener();
        ponerEncabezado("Pago con tarjeta", true);
        marco.setCenter(paso3);
        paso3Controller.mostrar(vertical);
    }

    private void mostrarComprobante() {
        paso = 4;
        ponerEncabezado("¡Compra realizada!", false);
        marco.setCenter(comprobante);
        comprobanteController.mostrar();
    }

    private void alCambiarSeleccion() {
        if (paso == 1) {
            paso1Controller.refrescar();
        }
    }

    private void alRegresar() {
        switch (paso) {
            case 1 -> {
                sesion.seleccion.limpiar();
                aviso("Selección reiniciada");
            }
            case 2 -> mostrarPaso1();
            case 3 -> mostrarPaso2();
            default -> { }
        }
    }

    /** Gesto oculto del personal: pide el PIN y, si es correcto, sale del autoservicio. */
    private void pedirPin() {
        if (pinPersonal.isEmpty() || capaModal.isVisible()) {
            return;
        }
        Fxml.Vista<ModalPinController> v = Fxml.cargar("kiosco/modal-pin");
        v.controlador().configurar(pinPersonal, () -> {
            inactividad.stop();
            sondeo.stop();
            comprobanteController.detener();
            cerrarModal();
            alSalirPersonal.run();
        }, this::cerrarModal);
        mostrarModal(v.raiz());
    }

    /**
     * Disponibilidad en tiempo real: mientras el cliente mira el mapa sin haber elegido nada, se consulta el
     * servidor y, si alguien más compró, el mapa se actualiza solo. (La compra siempre se valida en el servidor.)
     */
    private void sondearDisponibilidad() {
        if (paso != 1 || concierto == null || capaModal.isVisible() || !sesion.seleccion.estaVacia()) {
            return;
        }
        Concierto c = concierto;
        Async.run(() -> api.listarInventario(c.id()), lista -> {
            if (paso != 1 || c != concierto || capaModal.isVisible() || !sesion.seleccion.estaVacia()) {
                return;
            }
            if (cambiaron(lista)) {
                inventarios = lista;
                reconstruirLocalidades();
                mostrarPaso1();
            }
        }, ex -> { /* sin conexión un momento: se reintenta en el próximo ciclo */ });
    }

    private boolean cambiaron(List<Inventario> nuevos) {
        if (nuevos.size() != inventarios.size()) {
            return true;
        }
        for (Inventario n : nuevos) {
            boolean igual = inventarios.stream().anyMatch(v -> v.id() == n.id() && v.disponible() == n.disponible()
                    && v.total() == n.total() && v.precio().compareTo(n.precio()) == 0);
            if (!igual) {
                return true;
            }
        }
        return false;
    }

    /** Sin actividad: se descarta la compra en curso y se vuelve al inicio. */
    private void alInactividad() {
        boolean enCompra = paso >= 2 && paso <= 3 || paso == 1 && !sesion.seleccion.estaVacia();
        if (enCompra && !capaModal.isVisible() && concierto != null) {
            limpiarDatosDePago();
            sesion.nit.set("");
            sesion.usaNit = false;
            nuevaCompra();
        }
        inactividad.playFromStart();
    }

    // ================================================================== Navegacion (la usan los pasos)

    @Override
    public void irPaso1() {
        mostrarPaso1();
    }

    @Override
    public void irPaso2() {
        mostrarPaso2();
    }

    @Override
    public void irPaso3() {
        mostrarPaso3();
    }

    @Override
    public void nuevaCompra() {
        comprobanteController.detener();
        sesion.seleccion.limpiar();
        cargarInventario();
    }

    @Override
    public void pagar() {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (LineaSeleccion ln : sesion.seleccion.lineas()) {
            cantidades.merge(ln.localidad().getIdInventario(), ln.cantidad(), Integer::sum);
        }
        List<LineaSeleccion> lineas = new ArrayList<>(sesion.seleccion.lineas());
        String numero = sesion.tarjeta.get();
        String nombreTitular = sesion.titular.get().trim();
        String factura = sesion.facturaTexto();
        String correo = sesion.correo.get().trim();

        mostrarCargando("Procesando tu pago...");
        Async.run(() -> api.crearVentaPorIds(cantidades), venta -> {
            sesion.ultimaVenta = venta;
            sesion.ultimasLineas = lineas;
            sesion.ultimaTarjeta = "**** **** **** " + numero.substring(Math.max(0, numero.length() - 4));
            sesion.ultimaFactura = factura;
            sesion.ultimoTitular = nombreTitular;

            List<String> lineasTexto = new ArrayList<>();
            for (LineaSeleccion ln : lineas) {
                String nombre = ln.localidad().getTipo().esMesa() ? ln.localidad().getNombre()
                        : ln.cantidad() + " x " + ln.localidad().getNombre();
                lineasTexto.add(nombre + " = " + Formato.moneda(ln.subtotal()));
            }
            sesion.ultimoQrTexto = Qr.contenido(venta.id(), concierto.titulo(), sesion.info.fecha(), venta.total(), lineasTexto);

            sesion.seleccion.limpiar();
            limpiarDatosDePago();
            sesion.nit.set("");
            sesion.usaNit = false;
            cerrarModal();
            mostrarComprobante();
            enviarCorreo(correo, venta.id(), lineasTexto, venta.total(), sesion.ultimoQrTexto);
        }, ex -> mostrarMensaje("No se pudo completar la compra", mensaje(ex), "Volver al mapa", () -> {
            sesion.seleccion.limpiar();
            limpiarDatosDePago();
            cargarInventario();
        }));
    }

    private void limpiarDatosDePago() {
        sesion.tarjeta.set("");
        sesion.cvv.set("");
        sesion.titular.set("");
        sesion.correo.set("");
    }

    /** Envía el comprobante con el QR si el cliente escribió un correo; muestra el avance en el comprobante. */
    private void enviarCorreo(String correo, int idVenta, List<String> lineas, java.math.BigDecimal total, String textoQr) {
        if (!Correo.valido(correo)) {
            return;
        }
        if (!Correo.configurado()) {
            comprobanteController.setEstadoCorreo(
                    "El envío por correo no está disponible en este equipo. Guarda una foto de este QR.", false);
            return;
        }
        comprobanteController.setEstadoCorreo("Enviando tu QR a " + correo + "...", true);
        String artista = sesion.info.artista() + " · " + sesion.info.gira();
        Async.run(() -> {
            Correo.enviarComprobante(correo, idVenta, artista, sesion.info.fecha(), lineas, total, Qr.png(textoQr, 400));
            return Boolean.TRUE;
        }, ok -> comprobanteController.setEstadoCorreo("✓ Te enviamos el QR a " + correo, false),
                ex -> {
                    Correo.registrarError(ex); // el motivo queda en ~/.svbgua/correo-error.log
                    comprobanteController.setEstadoCorreo("No pudimos enviar el correo. Guarda una foto de este QR.", false);
                });
    }

    // ================================================================== modales y avisos

    private void mostrarModal(Node contenido) {
        tarjetaModal.getChildren().setAll(contenido);
        capaModal.setOpacity(0.0);
        capaModal.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(160), capaModal);
        ft.setToValue(1.0);
        ft.play();
    }

    private void cerrarModal() {
        capaModal.setVisible(false);
    }

    private double anchoModal(double maximo) {
        return Math.min(maximo, Math.max(320, raiz.getWidth() - 120));
    }

    private void mostrarCargando(String texto) {
        Fxml.Vista<ModalController> v = Fxml.cargar("kiosco/modal-cargando");
        v.controlador().setTitulo(texto);
        v.controlador().setAncho(anchoModal(560));
        mostrarModal(v.raiz());
    }

    /** Mensaje con un solo botón. */
    private void mostrarMensaje(String titulo, String texto, String textoBoton, Runnable alAceptar) {
        Fxml.Vista<ModalController> v = Fxml.cargar("kiosco/modal-mensaje");
        ModalController m = v.controlador();
        m.setTitulo(titulo);
        m.setTexto(texto);
        m.setBotonOk(textoBoton, () -> {
            cerrarModal();
            alAceptar.run();
        });
        m.ocultarSecundario(); // los clientes no tienen botón de salida
        m.setAncho(anchoModal(620));
        mostrarModal(v.raiz());
    }

    private void mostrarSelectorConcierto(List<Concierto> activos) {
        Fxml.Vista<ModalConciertoController> v = Fxml.cargar("kiosco/modal-concierto");
        List<ModalConciertoController.Opcion> opciones = new ArrayList<>();
        for (Concierto c : activos) {
            opciones.add(new ModalConciertoController.Opcion(c,
                    catalogo.nombreArtista(c.idArtista()) + " · " + c.titulo(),
                    construirInfo(c).fecha() + "\n" + c.recinto()));
        }
        v.controlador().configurar(opciones, raiz.getWidth(), raiz.getHeight(), c -> {
            cerrarModal();
            elegirConcierto(c);
        });
        mostrarModal(v.raiz());
    }

    @Override
    public void aviso(String texto) {
        if (animAviso != null) {
            animAviso.stop();
        }
        lblAviso.setText(texto);
        lblAviso.setOpacity(0.0);
        lblAviso.setVisible(true);
        animAviso = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(lblAviso.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(180), new KeyValue(lblAviso.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(2300), new KeyValue(lblAviso.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(2700), new KeyValue(lblAviso.opacityProperty(), 0.0)));
        animAviso.setOnFinished(e -> lblAviso.setVisible(false));
        animAviso.play();
    }
}
