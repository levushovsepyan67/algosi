package usaskoviy.heaps;

import org.jfree.chart.ChartFactory; 
import org.jfree.chart.ChartUtils; 
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File; 
import java.io.IOException;

/**
 * Построение шести графиков по результатам бенчмарка: среднее и максимум для findMin, deleteMin, insert.
 */
public final class HeapCharts {

    private HeapCharts() {
    }

    /**
     * Сохраняет все графики в каталог outDir: по два на каждую операцию (среднее время и максимум).
     */
    public static void saveAllCharts(
            File outDir,
            int[] nValues,
            double[] binPeekAvg, double[] bioPeekAvg, double[] binPeekMax, double[] bioPeekMax,
            double[] binDelAvg, double[] bioDelAvg, double[] binDelMax, double[] bioDelMax,
            double[] binInsAvg, double[] bioInsAvg, double[] binInsMax, double[] bioInsMax) throws IOException {

        saveLineChart(
                outDir,
                "heap_avg_findMin.png", // имя файла для отчёта
                "findMin: среднее время одной операции (из 1000)",
                "Среднее время, нс",
                nValues,
                binPeekAvg,
                bioPeekAvg);

        saveLineChart(
                outDir,
                "heap_max_findMin.png",
                "findMin: максимальное время одной операции (из 1000)",
                "Максимум, нс",
                nValues,
                binPeekMax,
                bioPeekMax);

        saveLineChart(
                outDir,
                "heap_avg_deleteMin.png",
                "deleteMin: среднее время одной операции (из 1000)",
                "Среднее время, нс",
                nValues,
                binDelAvg,
                bioDelAvg);

        saveLineChart(
                outDir,
                "heap_max_deleteMin.png",
                "deleteMin: максимальное время одной операции (из 1000)",
                "Максимум, нс",
                nValues,
                binDelMax,
                bioDelMax);

        saveLineChart(
                outDir,
                "heap_avg_insert.png",
                "insert: среднее время одной операции (из 1000)",
                "Среднее время, нс",
                nValues,
                binInsAvg,
                bioInsAvg);

        saveLineChart(
                outDir,
                "heap_max_insert.png",
                "insert: максимальное время одной операции (из 1000)",
                "Максимум, нс",
                nValues,
                binInsMax,
                bioInsMax);
    }

    /**
     * Одна XY-диаграмма: по X — N (логарифмическая шкала), по Y — время; две кривые — бинарная и биномиальная куча.
     */
    private static void saveLineChart(
            File outDir,
            String fileName,
            String title,
            String yLabel,
            int[] nValues,
            double[] seriesBinary,
            double[] seriesBinomial) throws IOException {

        XYSeriesCollection dataset = new XYSeriesCollection(); // контейнер для линий
        XYSeries s1 = new XYSeries("Бинарная куча"); // первая серия — бинарная min-куча
        XYSeries s2 = new XYSeries("Биномиальная куча"); // вторая — биномиальная
        for (int i = 0; i < nValues.length; i++) {
            s1.add(nValues[i], seriesBinary[i]); // точка (N, время) для бинарной
            s2.add(nValues[i], seriesBinomial[i]); // точка для биномиальной
        }
        dataset.addSeries(s1);
        dataset.addSeries(s2);

        JFreeChart chart = ChartFactory.createXYLineChart(
                title, // заголовок окна графика
                "N (число элементов в куче)", // подпись оси X (до замены на лог-ось)
                yLabel, // подпись оси Y: наносекунды
                dataset, // данные
                PlotOrientation.VERTICAL, // ось времени вверх
                true, // показать легенду
                true, // всплывающие подсказки
                false); // не генерировать URL

        XYPlot plot = chart.getXYPlot(); // получаем панель графика
        LogarithmicAxis logX = new LogarithmicAxis("N (логарифмическая шкала)"); // log10 по абсциссе
        logX.setAllowNegativesFlag(false); // отрицательные N запрещены
        logX.setLog10TickLabelsFlag(true); // удобные подписи делений
        plot.setDomainAxis(logX); // подменяем ось X на логарифмическую
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis(); // ось Y остаётся линейной
        yAxis.setAutoRangeIncludesZero(false); // не тянуть ось к нулю, если все значения > 0

        File out = new File(outDir, fileName); // полный путь к PNG
        ChartUtils.saveChartAsPNG(out, chart, 1000, 600); // ширина × высота в пикселях
    }
}
