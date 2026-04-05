package usaskoviy.analysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Визуализация результатов эксперимента: столбчатая диаграмма времени BFS и DFS (IDDFS).
 *
 * Время хранится в наносекундах в {@link BenchmarkRow}; на график выводится в микросекундах
 * (деление на 1000), чтобы столбцы были удобнее читать при малых величинах.
 *
 * Зависимость {@code jfreechart} подключается через Maven, файл {@code pom.xml}.
 */
public final class TimingChart {

    private TimingChart() {
    }

    /**
     * Строит диаграмму по строкам бенчмарка и сохраняет в PNG.
     *
     * @param rows       10 (или другие) замеров с подписями {@link BenchmarkRow#label()}
     * @param outputFile куда записать изображение (часто {@code benchmark_chart.png} в рабочей директории)
     */
    public static void savePng(List<BenchmarkRow> rows, File outputFile) throws IOException {
        // Категории по оси X — подписи графов; в каждой категории два столбца (серии) — BFS и DFS
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (BenchmarkRow r : rows) {
            dataset.addValue(r.bfsNanos() / 1000.0, "BFS (мкс)", r.label());
            dataset.addValue(r.dfsNanos() / 1000.0, "DFS (IDDFS, мкс)", r.label());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Кратчайший путь: BFS vs DFS (итеративное углубление, IDDFS)",
                "Граф (|V|, |E|)",
                "Время, мкс (нс / 10³)",
                dataset,
                PlotOrientation.VERTICAL,
                true,  // легенда
                true,  // tooltips
                false // URLs
        );

        ChartUtils.saveChartAsPNG(outputFile, chart, 900, 520);
    }
}
