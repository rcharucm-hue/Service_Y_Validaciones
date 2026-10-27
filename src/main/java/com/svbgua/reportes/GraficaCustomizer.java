package com.svbgua.reportes;
import net.sf.jasperreports.engine.JRChart;
import net.sf.jasperreports.engine.JRChartCustomizer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
/** La cantidad de boletos usa marcas enteras, nunca fracciones de boleto. */
public final class GraficaCustomizer implements JRChartCustomizer {
    @Override public void customize(JFreeChart chart, JRChart modelo) {
        var plot=chart.getCategoryPlot();var datos=plot.getDataset();
        if(datos!=null && datos.getRowCount()>0 && !"Recaudacion Q".equals(datos.getRowKey(0))
                && plot.getRangeAxis() instanceof NumberAxis eje) {
            eje.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        }
    }
}
