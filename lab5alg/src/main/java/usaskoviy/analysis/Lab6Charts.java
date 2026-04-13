package usaskoviy.analysis; // классы построения графиков для отчёта по ЛР6

import org.jfree.chart.ChartFactory; // фабрика готовых типов диаграмм
import org.jfree.chart.ChartUtils; // сохранение графика в PNG
import org.jfree.chart.JFreeChart; // объект графика
import org.jfree.chart.axis.LogarithmicAxis; // логарифмическая ось X для N = 2^k
import org.jfree.chart.axis.NumberAxis; // числовая ось Y
import org.jfree.chart.plot.PlotOrientation; // вертикальная ориентация
import org.jfree.chart.plot.XYPlot; // доступ к осям графика
import org.jfree.data.xy.XYSeries; // одна линия (серия точек)
import org.jfree.data.xy.XYSeriesCollection; // набор серий для XY-графика

import java.io.File; // путь к файлу PNG
import java.io.IOException; // ошибки записи файла

/**
 * Построение трёх графиков ЛР6: вставка, поиск (с массивом), удаление;
 * отдельно режимы случайного и отсортированного порядка данных.
 */
public final class Lab6Charts {

    /** Закрытый конструктор — только статические методы сохранения графиков. */
    private Lab6Charts() {
    }

    /**
     * График суммарного времени вставки N элементов (усреднение по 10 циклам уже в данных y).
     */
    public static void saveInsertionChart(
            int[] nValues, // абсциссы: размеры N по сериям
            double[] bstRandomNs, // среднее суммарное время вставки BST, случайный порядок, нс
            double[] avlRandomNs, // то же для AVL, случайный порядок
            double[] bstSortedNs, // BST, отсортированный порядок вставки
            double[] avlSortedNs, // AVL, отсортированный порядок
            File outputFile) throws IOException { // куда записать PNG
        XYSeriesCollection dataset = new XYSeriesCollection(); // контейнер для всех линий
        addSeriesMillis(dataset, "BST, случайный порядок данных", nValues, bstRandomNs); // первая кривая
        addSeriesMillis(dataset, "AVL, случайный порядок данных", nValues, avlRandomNs); // вторая
        addSeriesMillis(dataset, "BST, отсортированный порядок данных", nValues, bstSortedNs); // третья
        addSeriesMillis(dataset, "AVL, отсортированный порядок данных", nValues, avlSortedNs); // четвёртая
        saveLogXChart(
                "Вставка N элементов (суммарное время за полную вставку)", // заголовок окна графика
                "N — число элементов", // подпись оси X (до добавления про логарифм)
                "Среднее суммарное время по 10 циклам, мс (нс / 10⁶)", // ось Y: нс переведены в мс
                dataset, // все серии
                outputFile); // файл результата
    }

    /**
     * График среднего времени одного поиска из 1000; шесть линий: BST/AVL/массив × два режима.
     */
    public static void saveSearchChart(
            int[] nValues, // размеры N
            double[] bstRandomNs, // среднее время одного поиска BST, случайные данные, нс
            double[] avlRandomNs, // AVL, случайные
            double[] arrayRandomNs, // массив, случайные
            double[] bstSortedNs, // BST, отсортированный порядок в массиве
            double[] avlSortedNs, // AVL
            double[] arraySortedNs, // линейный поиск по отсортированному массиву
            File outputFile) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        addSeriesMicros(dataset, "BST, случайные данные", nValues, bstRandomNs);
        addSeriesMicros(dataset, "AVL, случайные данные", nValues, avlRandomNs);
        addSeriesMicros(dataset, "Массив (линейный поиск), случайные", nValues, arrayRandomNs);
        addSeriesMicros(dataset, "BST, отсортированные данные", nValues, bstSortedNs);
        addSeriesMicros(dataset, "AVL, отсортированные данные", nValues, avlSortedNs);
        addSeriesMicros(dataset, "Массив (линейный поиск), отсортированные", nValues, arraySortedNs);
        saveLogXChart(
                "Поиск: среднее время одной операции из 1000 (случайный ключ в диапазоне значений)",
                "N — число элементов",
                "Среднее время одного поиска, мкс (нс / 10³)",
                dataset,
                outputFile);
    }

    /**
     * График среднего времени одного удаления из 1000 для BST и AVL в обоих режимах.
     */
    public static void saveDeletionChart(
            int[] nValues,
            double[] bstRandomNs,
            double[] avlRandomNs,
            double[] bstSortedNs,
            double[] avlSortedNs,
            File outputFile) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        addSeriesMicros(dataset, "BST, случайные данные", nValues, bstRandomNs);
        addSeriesMicros(dataset, "AVL, случайные данные", nValues, avlRandomNs);
        addSeriesMicros(dataset, "BST, отсортированные данные", nValues, bstSortedNs);
        addSeriesMicros(dataset, "AVL, отсортированные данные", nValues, avlSortedNs);
        saveLogXChart(
                "Удаление: среднее время одной операции из 1000",
                "N — число элементов",
                "Среднее время одного удаления, мкс (нс / 10³)",
                dataset,
                outputFile);
    }

    /** Добавляет серию точек: значения времени в нс делятся на 10⁶ → миллисекунды на оси Y. */
    private static void addSeriesMillis(XYSeriesCollection dataset, String label, int[] x, double[] yNs) {
        XYSeries s = new XYSeries(label); // легенда и имя серии
        for (int i = 0; i < x.length; i++) {
            s.add(x[i], yNs[i] / 1_000_000.0); // точка (N, время в мс)
        }
        dataset.addSeries(s); // подключаем линию к набору данных
    }

    /** Добавляет серию: время одной операции в нс делится на 10³ → микросекунды на оси Y. */
    private static void addSeriesMicros(XYSeriesCollection dataset, String label, int[] x, double[] yNs) {
        XYSeries s = new XYSeries(label);
        for (int i = 0; i < x.length; i++) {
            s.add(x[i], yNs[i] / 1000.0); // точка (N, время в мкс)
        }
        dataset.addSeries(s);
    }

    /** Строит линейный XY-график и сохраняет в PNG; ось X — логарифмическая по N. */
    private static void saveLogXChart(
            String title, // заголовок диаграммы
            String xLabel, // базовая подпись оси X
            String yLabel, // подпись оси Y (единицы времени)
            XYSeriesCollection dataset, // все кривые
            File outputFile) throws IOException { // путь к PNG
        JFreeChart chart = ChartFactory.createXYLineChart(
                title, // заголовок
                xLabel, // будет заменён на логарифмическую ось с расширенной подписью
                yLabel, // ось Y — линейная, время
                dataset, // данные
                PlotOrientation.VERTICAL, // ось Y вверх
                true, // показать легенду
                true, // всплывающие подсказки
                false); // без генерации URL
        XYPlot plot = chart.getXYPlot(); // получаем область построения
        LogarithmicAxis logX = new LogarithmicAxis(xLabel + " (логарифмическая шкала)"); // log10 по N
        logX.setAllowNegativesFlag(false); // отрицательные N запрещены
        logX.setLog10TickLabelsFlag(true); // подписи делений в удобном виде
        plot.setDomainAxis(logX); // подменяем ось абсцисс
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis(); // ось ординат
        yAxis.setAutoRangeIncludesZero(false); // не тянуть ось к нулю, если все времена положительные
        ChartUtils.saveChartAsPNG(outputFile, chart, 1000, 600); // записываем файл 1000×600 px
    }
}
