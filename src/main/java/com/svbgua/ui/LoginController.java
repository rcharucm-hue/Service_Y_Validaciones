package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.kiosco.KioscoConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Pantalla de inicio de sesión (login.fxml). */
public class LoginController {

    /** Servidor del backend. Se puede cambiar con la variable de entorno SVB_URL. */
    public static final String URL_SERVIDOR = System.getenv("SVB_URL") != null && !System.getenv("SVB_URL").isBlank()
            ? System.getenv("SVB_URL").trim()
            : "https://svb-gua-backend.onrender.com";

    @FXML private TextField tfUsuario;
    @FXML private PasswordField pfContrasena;
    @FXML private Label lblMensaje;
    @FXML private Button btnEntrar;
    @FXML private Button btnAutoservicio;

    private Runnable alEntrar = () -> { };
    private Runnable alEntrarAutoservicio = () -> { };

    public void setAlEntrar(Runnable alEntrar) {
        this.alEntrar = alEntrar;
    }

    public void setAlEntrarAutoservicio(Runnable alEntrarAutoservicio) {
        this.alEntrarAutoservicio = alEntrarAutoservicio;
    }

    @FXML
    private void entrar() {
        if (tfUsuario.getText().isBlank() || pfContrasena.getText().isEmpty()) {
            lblMensaje.setText("Escribe tu usuario y contraseña.");
            return;
        }
        conectar(btnEntrar, "Entrando...", "Iniciar sesión",
                () -> ApiClient.INSTANCE.login(URL_SERVIDOR, tfUsuario.getText(), pfContrasena.getText()), alEntrar);
    }

    /**
     * El autoservicio no pide usuario: la terminal se conecta sola con su cuenta de servicio. La primera vez
     * (sin kiosco.properties) se pide esa cuenta y el PIN del personal, se comprueban contra el servidor y se guardan.
     */
    @FXML
    private void autoservicio() {
        KioscoConfig existente = null;
        try {
            existente = KioscoConfig.leer();
        } catch (IllegalStateException ignorar) {
            // falta configurarlo: se pide abajo
        }
        if (existente != null) {
            abrirAutoservicio(existente, false);
            return;
        }
        new KioscoConfigDialog(tfUsuario.getText(), pfContrasena.getText()).showAndWait()
                .ifPresent(cfg -> abrirAutoservicio(cfg, true));
    }

    private void abrirAutoservicio(KioscoConfig cfg, boolean guardar) {
        conectar(btnAutoservicio, "Abriendo autoservicio...", "Autoservicio",
                () -> ApiClient.INSTANCE.login(URL_SERVIDOR, cfg.usuario(), cfg.clave()),
                () -> {
                    if (guardar) {
                        try {
                            cfg.guardar();
                        } catch (IllegalStateException e) {
                            Dialogs.info("Configuración no guardada", e.getMessage()
                                    + " El autoservicio abre ahora, pero tendrás que configurarlo de nuevo la próxima vez.");
                        }
                    }
                    alEntrarAutoservicio.run();
                });
    }

    public void setMensaje(String texto) {
        lblMensaje.setText(texto);
    }

    private void conectar(Button boton, String textoTrabajando, String textoNormal,
                          java.util.function.Supplier<?> login, Runnable destino) {
        if (Progreso.trabajando(btnEntrar) || Progreso.trabajando(btnAutoservicio)) {
            return; // ya hay un intento en curso (p. ej. Enter repetido)
        }
        lblMensaje.setText("");
        Button otro = boton == btnEntrar ? btnAutoservicio : btnEntrar;
        otro.setDisable(true);
        Progreso.enBoton(boton, textoTrabajando);
        Async.run(login::get, u -> destino.run(), ex -> {
            lblMensaje.setText(Dialogs.mensaje(ex));
            Progreso.finBoton(boton, textoNormal);
            otro.setDisable(false);
        });
    }

    @FXML
    private void nuevoUsuario() {
        new UsuarioDialog(URL_SERVIDOR).showAndWait().ifPresent(u -> {
            tfUsuario.setText(u);
            pfContrasena.clear();
            lblMensaje.setText("");
            Dialogs.info("Usuario creado", "El usuario \"" + u + "\" fue creado. Ya puedes iniciar sesión.");
            pfContrasena.requestFocus();
        });
    }
}
