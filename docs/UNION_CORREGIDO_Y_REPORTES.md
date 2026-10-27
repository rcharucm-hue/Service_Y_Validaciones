# Union de version corregida y reportes Jasper

Se integraron en el proyecto original los cambios de svb-gua-boletos-corregido:

- Pestañas independientes de Artistas y Localidades y sus controladores/formularios.
- Nuevo orden de pestañas de administracion.
- Paleta y estilos actualizados del kiosco y de la aplicacion.

Se conservaron las graficas Jasper, los filtros, la carga automatica al entrar en Reportes,
la exportacion PDF y la recaudacion calculada con subtotales de ventas confirmadas.
Los 15 archivos de reportes se comprobaron por SHA-256 antes y despues: no fueron sustituidos.
MainController y main.fxml se combinaron para enlazar las nuevas pestañas y el panel Jasper.

Verificacion: compilacion para Java 17, 47 pruebas de validaciones, 18 comprobaciones Jasper
con datos ficticios y carga de la ventana principal completa con sus controladores FXML.
La prueba grafica aislo las preferencias en memoria; no modifico el registro de Windows.
No se realizaron solicitudes de escritura a la API ni cambios en el backend.
La carpeta svb-gua-boletos-corregido se utilizo solo como origen de lectura.

Para ejecutar, cerrar la instancia anterior e iniciar el proyecto original en NetBeans
o ejecutar target/svb-gua-boletos-fx-1.0.0.jar. Como administrador, Reportes esta dentro de Administracion.
