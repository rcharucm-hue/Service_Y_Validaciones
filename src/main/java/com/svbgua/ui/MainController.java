package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.Catalogo;
import com.svbgua.model.Modelos.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Ventana principal (main.fxml): encabezado, compra de boletos, historial y administración. */
public class MainController {

    @FXML private TabPane tabs;
    @FXML private Tab tabBoletos;
    @FXML private Tab tabVentas;
    @FXML private Tab tabAdmin;
    @FXML private Tab tabReportes;
    @FXML private Label lblUsuario;

    // Controladores de las pestañas incluidas con <fx:include fx:id="x"> (campo = id + "Controller").
    @FXML private BoletosController boletosController;
    @FXML private VentasController ventasController;
    @FXML private ConciertosAdminController conciertosController;
    @FXML private ArtistasAdminController artistasController;
    @FXML private LocalidadesAdminController localidadesController;
    @FXML private InventarioAdminController inventarioController;
    @FXML private UsuariosAdminController usuariosController;
    @FXML private ReportesController reportesController;

    private Runnable alSalir = () -> { };
    private boolean esAdmin;

    public void iniciar(Runnable alSalir) {
        this.alSalir = alSalir;

        Usuario u = ApiClient.INSTANCE.usuario();
        esAdmin = u.esAdmin();
        lblUsuario.setText(u.nombreCompleto() + "  (" + u.rol() + ")");

        if (esAdmin) {
            conciertosController.setRecargarCatalogo(this::cargarCatalogo);
            artistasController.setRecargarCatalogo(this::cargarCatalogo);
            localidadesController.setRecargarCatalogo(this::cargarCatalogo);
            inventarioController.setRecargarCatalogo(this::cargarCatalogo);
            usuariosController.cargar();
            tabReportes.selectedProperty().addListener((o, anterior, seleccionada) -> {
                if (seleccionada && tabAdmin.isSelected()) reportesController.actualizar();
            });
        } else {
            tabs.getTabs().remove(tabAdmin);
        }

        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n == tabVentas) ventasController.cargar();
            if (esAdmin && n == tabAdmin && tabReportes.isSelected()) reportesController.actualizar();
            if (n == tabBoletos) boletosController.cargarInventario();
        });

        cargarCatalogo();
    }

    @FXML
    private void cerrarSesion() {
        ApiClient.INSTANCE.cerrarSesion();
        alSalir.run();
    }

    private void cargarCatalogo() {
        Async.run(() -> ApiClient.INSTANCE.cargarCatalogo(), this::aplicarCatalogo,
                ex -> Dialogs.error("No se pudo cargar el catálogo", ex));
    }

    private void aplicarCatalogo(Catalogo c) {
        boletosController.setCatalogo(c);
        ventasController.setCatalogo(c);
        if (esAdmin) {
            artistasController.setCatalogo(c);
            localidadesController.setCatalogo(c);
            inventarioController.setCatalogo(c);
            conciertosController.setCatalogo(c);
            reportesController.setCatalogo(c);
        }
    }
}