package com.svbgua;

import com.svbgua.api.ApiClient;
import com.svbgua.kiosco.AutoservicioController;
import com.svbgua.kiosco.KioscoConfig;
import com.svbgua.ui.Async;
import com.svbgua.ui.Dialogs;
import com.svbgua.ui.Fxml;
import com.svbgua.ui.LoginController;
import com.svbgua.ui.MainController;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Arranque. Funciona en pantallas horizontales y verticales (autoservicio).
 * Con el argumento --kiosco la terminal entra directo al autoservicio (sin pantalla de login), conectándose con
 * la cuenta de servicio de kiosco.properties. En modo autoservicio no hay forma de salir sin el PIN del personal.
 */
public class Main extends Application {

    private static final String CSS_VENTAS = "/com/svbgua/style.css";
    private static final String CSS_KIOSCO = "/com/svbgua/kiosco.css";

    private Stage stage;
    private boolean enKiosco;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        boolean arrancarEnKiosco = getParameters().getRaw().contains("--kiosco");

                stage.setTitle("TICKETS UMG · Venta de boletos");
        stage.setMinWidth(640);
        stage.setMinHeight(600);
        stage.setFullScreenExitHint("F11 para salir de pantalla completa");
        // En autoservicio la ventana no se puede cerrar con Alt+F4: se sale con el gesto y el PIN del personal.
        stage.setOnCloseRequest(e -> {
            if (enKiosco) {
                e.consume();
            }
        });

        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        boolean pantallaVertical = pantalla.getHeight() > pantalla.getWidth();
        double ancho = pantallaVertical ? Math.min(900, pantalla.getWidth() * 0.95) : 1100;
        double alto = pantallaVertical ? pantalla.getHeight() * 0.92 : 720;

        if (arrancarEnKiosco) {
            stage.setScene(crearEscena(Fxml.cargar("arranque").raiz(), CSS_KIOSCO, ancho, alto));
            stage.setMaximized(pantallaVertical);
            stage.show();
            conectarAutoservicio(null);
        } else {
            stage.setScene(crearEscena(crearLogin(null), CSS_VENTAS, ancho, alto));
            stage.setMaximized(pantallaVertical);
            stage.show();
        }
    }

    /** Conecta la terminal con su cuenta de servicio y abre el autoservicio; si falla, vuelve al login con el motivo. */
    private void conectarAutoservicio(String ignorado) {
        KioscoConfig cfg;
        try {
            cfg = KioscoConfig.leer();
        } catch (IllegalStateException e) {
            mostrarLogin(e.getMessage() + " Pulsa «Autoservicio» para configurarlo (solo una vez).");
            return;
        }
        Async.run(() -> ApiClient.INSTANCE.login(LoginController.URL_SERVIDOR, cfg.usuario(), cfg.clave()),
                u -> mostrarAutoservicio(cfg.pin()),
                ex -> mostrarLogin(Dialogs.mensaje(ex)));
    }

    private void mostrarLogin() {
        mostrarLogin(null);
    }

    private void mostrarLogin(String mensaje) {
        salirDeKiosco();
        stage.setScene(crearEscena(crearLogin(mensaje), CSS_VENTAS, 0, 0));
    }

    private Parent crearLogin(String mensaje) {
        Fxml.Vista<LoginController> vista = Fxml.cargar("login");
        vista.controlador().setAlEntrar(this::mostrarPrincipal);
        vista.controlador().setAlEntrarAutoservicio(() -> mostrarAutoservicio(KioscoConfig.leer().pin()));
        if (mensaje != null) {
            vista.controlador().setMensaje(mensaje);
        }
        return vista.raiz();
    }

    private void mostrarPrincipal() {
        salirDeKiosco();
        Fxml.Vista<MainController> vista = Fxml.cargar("main");
        stage.setScene(crearEscena(vista.raiz(), CSS_VENTAS, 0, 0));
        vista.controlador().iniciar(this::mostrarLogin);
    }

    private void mostrarAutoservicio(String pin) {
        enKiosco = true;
        Fxml.Vista<AutoservicioController> vista = Fxml.cargar("kiosco/autoservicio");
        stage.setScene(crearEscena(vista.raiz(), CSS_KIOSCO, 0, 0));
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // Esc no sale de pantalla completa
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        vista.controlador().iniciar(this::salidaDelPersonal, pin);
    }

    /** El personal salió con el PIN: se cierra la sesión de la terminal y se vuelve al login. */
    private void salidaDelPersonal() {
        ApiClient.INSTANCE.cerrarSesion();
        mostrarLogin();
    }

    private void salirDeKiosco() {
        if (enKiosco) {
            enKiosco = false;
            stage.setFullScreen(false);
            stage.setFullScreenExitKeyCombination(null); // vuelve a la tecla por defecto (Esc)
            stage.setFullScreenExitHint("F11 para salir de pantalla completa");
        }
    }

    /** Conserva el tamaño de la ventana al cambiar de pantalla; ancho/alto solo se usan la primera vez. */
    private Scene crearEscena(Parent raiz, String css, double ancho, double alto) {
        double w = stage.getScene() != null ? stage.getScene().getWidth() : ancho;
        double h = stage.getScene() != null ? stage.getScene().getHeight() : alto;
        Scene escena = new Scene(raiz, w, h);
        escena.getStylesheets().add(getClass().getResource(css).toExternalForm());
        escena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11 && !enKiosco) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
        return escena;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
