package usaskoviy.analysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;

/**
 * Построение графика зависимости времени от N по условию ЛР5 (ось X - N, ось Y - время).
 */
public final class Lab5TimingChart {

    private Lab5TimingChart() {
    }

    /**
     * Сохраняет линейный график: точки (N_i, среднее время в микросекундах).
     *
     * @param vertexCounts значения N по оси X (10, 20, 50, 100)
     * @param avgNanos     среднее время одного полного вычисления всех пар в наносекундах для каждого N
     * @param outputFile   путь к PNG
     */
    public static void savePng(int[] vertexCounts, double[] avgNanos, File outputFile) throws IOException {
        if (vertexCounts.length != avgNanos.length) {
            throw new IllegalArgumentException("Размеры массивов не совпадают");
        }
        // Серия точек для линейного графика JFreeChart.
        XYSeries series = new XYSeries("Среднее время (все пары, Дейкстра)");
        for (int i = 0; i < vertexCounts.length; i++) {
            // Перевод нс в мкс: числа на оси Y читаются проще.
            double micros = avgNanos[i] / 1000.0;
            series.add(vertexCounts[i], micros);
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Время поиска кратчайших путей для всех пар (Дейкстра)",
                "N — число вершин",
                "Среднее время, мкс (нс / 10³)",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true,  // показать легенду
                true,  // всплывающие подсказки
                false  // без URL
        );
        // Размер изображения в пикселях.
        ChartUtils.saveChartAsPNG(outputFile, chart, 900, 520);
    }
}
