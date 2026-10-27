package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.*;
import com.svbgua.util.Correo;
import com.svbgua.util.Fmt;
import com.svbgua.util.Qr;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.util.Duration;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/** Mapa de secciones (VIP de pie, Preferencial, Mesa), carrito y compra (boletos-tab.fxml). */
public class BoletosController {

    private static final Preferences PREFS = Preferences.userNodeForPackage(BoletosController.class);

    private final ApiClient api = ApiClient.INSTANCE;

    private Catalogo catalogo = Catalogo.vacio();
    private List<Inventario> inventarios = new ArrayList<>();
    private Inventario seleccionado;
    private List<Inventario> ordenActual = new ArrayList<>();
    private boolean modoAcomodar;
    private boolean vertical;
    private boolean ignorarCambio;
    private ToggleGroup grupo = new ToggleGroup();
    private final ObservableList<ItemCarrito> carrito = FXCollections.observableArrayList();
    /** Cada cuánto se consulta la disponibilidad al servidor (segundos). */
    private static final int SEGUNDOS_SONDEO = 15;

    @FXML private BorderPane raiz;
    @FXML private VBox columna;
    @FXML private ScrollPane scrollMapa;
    @FXML private VBox derecha;
    @FXML private VBox zonas;
    @FXML private ComboBox<Concierto> cbConcierto;
    @FXML private ToggleButton btnAcomodar;
    @FXML private Label lblActualizado;
    @FXML private Button btnRestablecer;
    @FXML private Label lblSeccion;
    @FXML private Label lblPrecio;
    @FXML private Label lblDisp;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private Button btnAgregar;
    @FXML private TableView<ItemCarrito> tabla;
    @FXML private TableColumn<ItemCarrito, String> cSeccion;
    @FXML private TableColumn<ItemCarrito, String> cCant;
    @FXML private TableColumn<ItemCarrito, String> cPrecio;
    @FXML private TableColumn<ItemCarrito, String> cSub;
    @FXML private Label lblTotal;
    @FXML private Button btnComprar;

    @FXML
    private void initialize() {
        btnRestablecer.visibleProperty().bind(btnAcomodar.selectedProperty());
        btnRestablecer.managedProperty().bind(btnAcomodar.selectedProperty());

        cbConcierto.valueProperty().addListener((o, anterior, nuevo) -> {
            if (ignorarCambio || nuevo == null) return;
            Platform.runLater(() -> alCambiarConcierto(anterior));
        });

        cSeccion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().localidad()));
        cCant.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().cantidad())));
        cPrecio.setCellValueFactory(c -> new SimpleStringProperty(Fmt.money(c.getValue().inventario().precio())));
        cSub.setCellValueFactory(c -> new SimpleStringProperty(Fmt.money(c.getValue().subtotal())));
        tabla.setItems(carrito);

        // Pantallas verticales (autoservicio): el mapa arriba y el carrito debajo.
        raiz.widthProperty().addListener((o, a, b) -> acomodarOrientacion());
        raiz.heightProperty().addListener((o, a, b) -> acomodarOrientacion());

        actualizarDetalle();
        actualizarTotales();

        // Disponibilidad "en vivo": consulta el servidor cada pocos segundos mientras la pestaña está a la vista.
        Timeline sondeo = new Timeline(new KeyFrame(Duration.seconds(SEGUNDOS_SONDEO), e -> refrescarSilencioso()));
        sondeo.setCycleCount(Animation.INDEFINITE);
        sondeo.play();
    }

    private boolean pestanaVisible() {
        if (raiz.getScene() == null || raiz.getScene().getWindow() == null || !raiz.getScene().getWindow().isShowing()) {
            return false;
        }
        for (Node n = raiz; n != null; n = n.getParent()) {
            if (!n.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /** Actualiza las existencias sin avisos ni parpadeos; solo redibuja si algo cambió. */
    private void refrescarSilencioso() {
        Concierto c = cbConcierto.getValue();
        if (c == null || api.usuario() == null || !pestanaVisible() || Progreso.trabajando(btnComprar)) {
            return;
        }
        Async.run(() -> api.listarInventario(c.id()), lista -> {
            if (c != cbConcierto.getValue() || Progreso.trabajando(btnComprar)) return;
            if (cambiaron(lista)) {
                inventarios = lista;
                dibujarZonas();
                actualizarTotales();
            }
            marcarActualizado();
        }, ex -> { /* silencioso: se reintenta en el próximo ciclo */ });
    }

    private boolean cambiaron(List<Inventario> nuevos) {
        if (nuevos.size() != inventarios.size()) return true;
        for (Inventario n : nuevos) {
            boolean igual = inventarios.stream().anyMatch(v -> v.id() == n.id() && v.disponible() == n.disponible()
                    && v.total() == n.total() && v.precio().compareTo(n.precio()) == 0);
            if (!igual) return true;
        }
        return false;
    }

    private void marcarActualizado() {
        lblActualizado.setText("● En vivo · actualizado " + java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @FXML
    private void alternarAcomodo() {
        modoAcomodar = btnAcomodar.isSelected();
        dibujarZonas();
    }

    private void acomodarOrientacion() {
        boolean v = raiz.getWidth() > 0 && raiz.getHeight() > raiz.getWidth();
        if (v == vertical) {
            return;
        }
        vertical = v;
        columna.getChildren().clear();
        raiz.setCenter(null);
        raiz.setRight(null);
        if (v) {
            derecha.setPrefWidth(Region.USE_COMPUTED_SIZE);
            derecha.setMinWidth(0);
            derecha.setPrefHeight(520);
            derecha.setMinHeight(380);
            derecha.setPadding(Insets.EMPTY);
            VBox.setVgrow(scrollMapa, Priority.ALWAYS);
            columna.getChildren().addAll(scrollMapa, derecha);
            raiz.setCenter(columna);
        } else {
            derecha.setPrefWidth(420);
            derecha.setMinWidth(420);
            derecha.setPrefHeight(Region.USE_COMPUTED_SIZE);
            derecha.setMinHeight(Region.USE_COMPUTED_SIZE);
            derecha.setPadding(new Insets(0, 0, 0, 16));
            raiz.setCenter(scrollMapa);
            raiz.setRight(derecha);
        }
    }

    // ------------------------------------------------------------ catalogo

    public void setCatalogo(Catalogo nuevo) {
        this.catalogo = nuevo;
        Concierto actual = cbConcierto.getValue();
        ignorarCambio = true;
        cbConcierto.setItems(FXCollections.observableArrayList(nuevo.activos()));
        if (actual != null && cbConcierto.getItems().contains(actual)) {
            cbConcierto.setValue(actual);
        } else if (!cbConcierto.getItems().isEmpty()) {
            cbConcierto.getSelectionModel().selectFirst();
        }
        ignorarCambio = false;
        cargarInventario();
    }

    private void alCambiarConcierto(Concierto anterior) {
        if (!carrito.isEmpty()) {
            if (!Dialogs.confirmar("Cambiar de concierto vaciará el carrito. ¿Continuar?")) {
                ignorarCambio = true;
                cbConcierto.setValue(anterior);
                ignorarCambio = false;
                return;
            }
            carrito.clear();
            refrescarTodo();
        }
        cargarInventario();
    }

    @FXML
    public void cargarInventario() {
        Concierto c = cbConcierto.getValue();
        if (c == null) {
            inventarios = new ArrayList<>();
            dibujarZonas();
            return;
        }
        zonas.getChildren().setAll(Progreso.caja("Cargando secciones..."));
        Async.run(() -> api.listarInventario(c.id()), lista -> {
            if (c != cbConcierto.getValue()) return;
            inventarios = lista;
            dibujarZonas();
            marcarActualizado();
        }, ex -> {
            zonas.getChildren().setAll(new Label("No se pudieron cargar las secciones."));
            Dialogs.error("No se pudo cargar el inventario", ex);
        });
    }

    // ---------------------------------------------------------------- mapa

    private void dibujarZonas() {
        Integer idPrevio = seleccionado == null ? null : seleccionado.id();
        zonas.getChildren().clear();
        grupo = new ToggleGroup();
        seleccionado = null;

        if (inventarios.isEmpty()) {
            ordenActual = new ArrayList<>();
            zonas.getChildren().add(new Label("Este concierto aún no tiene secciones con boletos."));
            actualizarDetalle();
            return;
        }

        // Orden: el que guardó el usuario (si lo hay); lo nuevo va al final segun su tipo.
        List<Integer> guardado = ordenGuardado();
        List<Inventario> orden = new ArrayList<>(inventarios);
        orden.sort(Comparator.comparingInt((Inventario i) -> {
            int idx = guardado.indexOf(i.idLocalidad());
            return idx >= 0 ? idx : 1000 + rango(catalogo.nombreLocalidad(i.idLocalidad()));
        }).thenComparingInt(Inventario::id));
        ordenActual = orden;

        for (int pos = 0; pos < orden.size(); pos++) {
            Inventario inv = orden.get(pos);
            String nombre = catalogo.nombreLocalidad(inv.idLocalidad());
            int disp = disponibleReal(inv);

            ToggleButton boton = new ToggleButton(nombre.toUpperCase(Locale.ROOT) + "\n" + Fmt.money(inv.precio()) + "\n"
                    + (disp > 0 ? disp + " disponibles" : "AGOTADO"));
            boton.setToggleGroup(grupo);
            boton.setUserData(inv);
            boton.getStyleClass().addAll("zona", claseZona(rango(nombre)));
            boton.setPrefSize(anchoPosicion(pos), 96);
            boton.setMinHeight(96);
            boton.setDisable(disp <= 0);

            if (modoAcomodar) {
                final int p = pos;
                Button arriba = new Button("▲");
                arriba.setDisable(pos == 0);
                arriba.setOnAction(e -> mover(p, -1));
                Button abajo = new Button("▼");
                abajo.setDisable(pos == orden.size() - 1);
                abajo.setOnAction(e -> mover(p, 1));
                VBox flechas = new VBox(8, arriba, abajo);
                flechas.setAlignment(Pos.CENTER);
                HBox fila = new HBox(10, boton, flechas);
                fila.setAlignment(Pos.CENTER);
                zonas.getChildren().add(fila);
            } else {
                zonas.getChildren().add(boton);
            }

            if (idPrevio != null && idPrevio == inv.id() && disp > 0) {
                boton.setSelected(true);
                seleccionado = inv;
            }
        }

        grupo.selectedToggleProperty().addListener((o, a, n) -> {
            seleccionado = n == null ? null : (Inventario) n.getUserData();
            actualizarDetalle();
        });
        actualizarDetalle();
    }

    // ---- orden de las secciones (se guarda por concierto en esta computadora)

    private List<Integer> ordenGuardado() {
        Concierto c = cbConcierto.getValue();
        List<Integer> ids = new ArrayList<>();
        if (c == null) return ids;
        for (String parte : PREFS.get("orden_" + c.id(), "").split(",")) {
            try {
                if (!parte.isBlank()) ids.add(Integer.parseInt(parte.trim()));
            } catch (NumberFormatException ignorar) {
                // valor viejo o dañado: se ignora
            }
        }
        return ids;
    }

    private void mover(int pos, int delta) {
        int nueva = pos + delta;
        Concierto c = cbConcierto.getValue();
        if (c == null || nueva < 0 || nueva >= ordenActual.size()) return;
        Collections.swap(ordenActual, pos, nueva);
        PREFS.put("orden_" + c.id(), ordenActual.stream().map(i -> String.valueOf(i.idLocalidad()))
                .collect(Collectors.joining(",")));
        dibujarZonas();
    }

    @FXML
    private void restablecerOrden() {
        Concierto c = cbConcierto.getValue();
        if (c == null) return;
        PREFS.remove("orden_" + c.id());
        dibujarZonas();
    }

    private static int rango(String nombre) {
        String n = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (n.contains("vip")) return 0;
        if (n.contains("pref")) return 1;
        if (n.contains("mesa")) return 2;
        return 3;
    }

    private static String claseZona(int rango) {
        return switch (rango) {
            case 0 -> "zona-vip";
            case 1 -> "zona-pref";
            case 2 -> "zona-mesa";
            default -> "zona-otra";
        };
    }

    /** Cuanto mas lejos del escenario, mas ancha la seccion. */
    private static double anchoPosicion(int pos) {
        return Math.min(620, 380 + pos * 60);
    }

    private int disponibleReal(Inventario inv) {
        return inv.disponible() - cantidadEnCarrito(inv.id());
    }

    private int cantidadEnCarrito(int idInventario) {
        for (ItemCarrito i : carrito) {
            if (i.inventario().id() == idInventario) return i.cantidad();
        }
        return 0;
    }

    private ItemCarrito buscarEnCarrito(int idInventario) {
        for (ItemCarrito i : carrito) {
            if (i.inventario().id() == idInventario) return i;
        }
        return null;
    }

    // -------------------------------------------------------------- detalle

    private void actualizarDetalle() {
        if (seleccionado == null) {
            lblSeccion.setText("Elige una sección del mapa");
            lblPrecio.setText("");
            lblDisp.setText("");
            spCantidad.setDisable(true);
            btnAgregar.setDisable(true);
            return;
        }
        int disp = disponibleReal(seleccionado);
        lblSeccion.setText(catalogo.nombreLocalidad(seleccionado.idLocalidad()).toUpperCase(Locale.ROOT));
        lblPrecio.setText("Precio por boleto: " + Fmt.money(seleccionado.precio()));
        lblDisp.setText("Disponibles: " + disp);
        spCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, disp), 1));
        spCantidad.setDisable(disp <= 0);
        btnAgregar.setDisable(disp <= 0);
    }

    // -------------------------------------------------------------- carrito

    @FXML
    private void agregarAlCarrito() {
        if (seleccionado == null) return;
        int cantidad = spCantidad.getValue();
        if (cantidad > disponibleReal(seleccionado)) {
            Dialogs.aviso("No hay suficientes boletos disponibles en esta sección.");
            return;
        }
        ItemCarrito existente = buscarEnCarrito(seleccionado.id());
        if (existente != null) {
            existente.setCantidad(existente.cantidad() + cantidad);
        } else {
            carrito.add(new ItemCarrito(seleccionado, catalogo.nombreLocalidad(seleccionado.idLocalidad()), cantidad));
        }
        refrescarTodo();
    }

    @FXML
    private void cambiarCantidad() {
        ItemCarrito item = tabla.getSelectionModel().getSelectedItem();
        if (item == null) {
            Dialogs.aviso("Selecciona una fila del carrito.");
            return;
        }
        TextInputDialog dialogo = new TextInputDialog(String.valueOf(item.cantidad()));
        dialogo.setTitle("Cambiar cantidad");
        dialogo.setHeaderText(item.localidad() + " (máximo " + item.inventario().disponible() + ")");
        dialogo.setContentText("Cantidad:");
        Campos.soloEnteros(dialogo.getEditor(), 4);
        dialogo.showAndWait().ifPresent(texto -> {
            try {
                int nueva = Integer.parseInt(texto.trim());
                if (nueva < 1 || nueva > item.inventario().disponible()) {
                    Dialogs.aviso("La cantidad debe estar entre 1 y " + item.inventario().disponible() + ".");
                    return;
                }
                item.setCantidad(nueva);
                refrescarTodo();
            } catch (NumberFormatException ex) {
                Dialogs.aviso("Escribe un número entero.");
            }
        });
    }

    @FXML
    private void quitarSeleccion() {
        ItemCarrito item = tabla.getSelectionModel().getSelectedItem();
        if (item == null) {
            Dialogs.aviso("Selecciona una fila del carrito.");
            return;
        }
        carrito.remove(item);
        refrescarTodo();
    }

    @FXML
    private void vaciarCarrito() {
        if (!carrito.isEmpty() && Dialogs.confirmar("¿Vaciar el carrito?")) {
            carrito.clear();
            refrescarTodo();
        }
    }

    private void refrescarTodo() {
        tabla.refresh();
        actualizarTotales();
        dibujarZonas();
    }

    private java.math.BigDecimal total() {
        java.math.BigDecimal t = java.math.BigDecimal.ZERO;
        for (ItemCarrito i : carrito) t = t.add(i.subtotal());
        return t;
    }

    private void actualizarTotales() {
        lblTotal.setText("Total: " + Fmt.money(total()));
        btnComprar.setDisable(carrito.isEmpty());
    }

    // --------------------------------------------------------------- compra

    @FXML
    private void comprar() {
        if (carrito.isEmpty()) {
            Dialogs.aviso("Agrega boletos al carrito primero.");
            return;
        }
        java.math.BigDecimal totalPago = total();
        new PagoDialog(totalPago).showAndWait().ifPresent(pago -> {
            List<ItemCarrito> items = new ArrayList<>(carrito);
            Concierto concierto = cbConcierto.getValue();
            Progreso.enBoton(btnComprar, "Procesando...");
            Async.run(() -> api.crearVenta(items), venta -> {
                Progreso.finBoton(btnComprar, "Comprar");
                carrito.clear();
                actualizarTotales();
                cargarInventario();
                mostrarComprobante(venta, pago, items, concierto);
            }, ex -> {
                Progreso.finBoton(btnComprar, "Comprar");
                actualizarTotales();
                Dialogs.error("No se pudo completar la compra", ex);
                cargarInventario();
            });
        });
    }

    private void mostrarComprobante(Venta venta, DatosPago pago, List<ItemCarrito> items, Concierto concierto) {
        StringBuilder sb = new StringBuilder();
        sb.append("TICKETS UMG · COMPROBANTE DE COMPRA\n");
        sb.append("================================\n");
        sb.append("Venta No.: ").append(venta.id()).append('\n');
        sb.append("Fecha:     ").append(Fmt.fecha(venta.fecha())).append('\n');
        if (concierto != null) sb.append("Concierto: ").append(concierto.titulo()).append('\n');
        sb.append("Cliente:   ").append(pago.clienteTexto()).append('\n');
        sb.append("NIT:       ").append(pago.nitTexto()).append('\n');
        sb.append("Tarjeta:   ").append(pago.tarjetaEnmascarada()).append('\n');
        sb.append("--------------------------------\n");
        for (ItemCarrito i : items) {
            sb.append(String.format(Locale.US, "%-14s %3d x %10s = %11s%n", i.localidad(), i.cantidad(),
                    Fmt.money(i.inventario().precio()), Fmt.money(i.subtotal())));
        }
        sb.append("--------------------------------\n");
        sb.append("TOTAL: ").append(Fmt.money(venta.total())).append('\n');
        String texto = sb.toString();

        List<String> lineasQr = new ArrayList<>();
        for (ItemCarrito i : items) {
            lineasQr.add(i.cantidad() + " x " + i.localidad() + " = " + Fmt.money(i.subtotal()));
        }
        String nombreConcierto = concierto != null ? concierto.titulo() : "TICKETS UMG";
        String fechaConcierto = concierto != null ? Fmt.fecha(concierto.fecha()) : "";
        String textoQr = Qr.contenido(venta.id(), nombreConcierto, fechaConcierto, venta.total(), lineasQr);

        Fxml.Vista<ComprobanteVentaController> vista = Fxml.cargar("comprobante-venta");
        ComprobanteVentaController cv = vista.controlador();
        cv.configurar(texto, Qr.imagen(textoQr, 220));
        String correo = pago.correo();
        if (Correo.valido(correo)) {
            cv.setEstadoCorreo("Enviando el comprobante con el QR a " + correo + "...", true);
            Async.run(() -> {
                Correo.enviarComprobante(correo, venta.id(), nombreConcierto, fechaConcierto, lineasQr,
                        venta.total(), Qr.png(textoQr, 400));
                return Boolean.TRUE;
            }, ok -> cv.setEstadoCorreo("✓ Comprobante enviado a " + correo, false),
                    ex -> cv.setEstadoCorreo("No se pudo enviar el correo: " + Dialogs.mensaje(ex), false));
        }

        ButtonType guardar = new ButtonType("Guardar comprobante...", ButtonBar.ButtonData.LEFT);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK, guardar);
        alerta.setTitle("Compra realizada");
        alerta.setHeaderText("¡Compra realizada con éxito!");
        alerta.getDialogPane().setContent(vista.raiz());
        alerta.showAndWait().ifPresent(bt -> {
            if (bt == guardar) guardarComprobante(venta.id(), texto);
        });
    }

    private void guardarComprobante(int idVenta, String texto) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar comprobante");
        fc.setInitialFileName("comprobante-venta-" + idVenta + ".txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));
        File archivo = fc.showSaveDialog(raiz.getScene().getWindow());
        if (archivo == null) return;
        try {
            Files.writeString(archivo.toPath(), texto, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Dialogs.error("No se pudo guardar el archivo", e);
        }
    }
}
