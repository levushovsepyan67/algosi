import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class MergeSortExperiment {

    /** Размеры массивов для восьми серий. */
    private static final int[] SIZES = {1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000};
    /** Число запусков сортировки для каждого размера. */
    private static final int RUNS_PER_SIZE = 20;
    /** Генератор случайных чисел. */
    private static final Random RND = new Random(4242);

    public static void main(String[] args) throws IOException {
        // Все результаты (CSV и графики) складываем в общую папку results
        Path outDir = Path.of("results");
        Files.createDirectories(outDir);

        System.out.println("Запуск эксперимента: сортировка слиянием");
        System.out.println("Размеры: " + Arrays.toString(SIZES));
        System.out.println("Запусков на размер: " + RUNS_PER_SIZE);

        List<SeriesResult> results = runAllSeries();
        exportCsv(outDir, results);
        printTable(results);
        MergeCharts.buildAll(outDir, results);

        System.out.println("Результаты второй лабораторной сохранены в " + outDir.toAbsolutePath());
    }

    /**
     * Запускает полный набор экспериментов:
     * для каждого размера из {@link #SIZES} выполняет {@link #RUNS_PER_SIZE} запусков,
     * собирает сырые измерения и сводит их в один объект {@link SeriesResult}.
     */
    private static List<SeriesResult> runAllSeries() {
        List<SeriesResult> results = new ArrayList<>();

        for (int size : SIZES) {
            System.out.println("Размер массива: " + size);

            double[] times = new double[RUNS_PER_SIZE];
            long[] depths = new long[RUNS_PER_SIZE];
            long[] calls = new long[RUNS_PER_SIZE];
            long[] memBytes = new long[RUNS_PER_SIZE];

            for (int run = 0; run < RUNS_PER_SIZE; run++) {
                double[] arr = generateArray(size);

                long start = System.nanoTime();
                MergeSort.Result r = MergeSort.sort(arr);
                long end = System.nanoTime();

                times[run] = (end - start) / 1_000_000.0;
                depths[run] = r.maxDepth;
                calls[run] = r.recursiveCalls;
                memBytes[run] = r.maxExtraBytes;
            }

            Stats timeStats = calcStats(times);
            Stats depthStats = calcStats(depths);
            Stats callStats = calcStats(calls);
            Stats memStats = calcStats(memBytes);

            results.add(new SeriesResult(
                    size,
                    timeStats.best, timeStats.worst, timeStats.avg,
                    depthStats.best, depthStats.worst, depthStats.avg,
                    callStats.best, callStats.worst, callStats.avg,
                    memStats.best, memStats.worst, memStats.avg
            ));
        }

        return results;
    }

    /**
     * Генерирует массив длины {@code size}, заполненный равномерно
     * распределёнными случайными числами из интервала [-1; 1].
     * Используется в каждом запуске эксперимента.
     */
    private static double[] generateArray(int size) {
        double[] a = new double[size];
        for (int i = 0; i < size; i++) {
            a[i] = -1 + 2 * RND.nextDouble();
        }
        return a;
    }

    /**
     * Универсальный контейнер для тройки статистик:
     * лучшее (минимум), худшее (максимум) и среднее значение выборки.
     * Используется как для времени, так и для глубины, вызовов и памяти.
     */
    private static class Stats {
        final double best;
        final double worst;
        final double avg;

        Stats(double best, double worst, double avg) {
            this.best = best;
            this.worst = worst;
            this.avg = avg;
        }
    }

    /**
     * Считает минимум, максимум и среднее по массиву вещественных значений.
     * Используется для статистики по времени выполнения.
     */
    private static Stats calcStats(double[] values) {
        double best = Double.POSITIVE_INFINITY;
        double worst = Double.NEGATIVE_INFINITY;
        double sum = 0.0;
        for (double v : values) {
            if (v < best) best = v;
            if (v > worst) worst = v;
            sum += v;
        }
        double avg = values.length == 0 ? 0.0 : sum / values.length;
        if (!Double.isFinite(best)) best = 0.0;
        if (!Double.isFinite(worst)) worst = 0.0;
        return new Stats(best, worst, avg);
    }

    /**
     * Считает минимум, максимум и среднее по массиву целых значений,
     * возвращая их в виде {@link Stats} (double используется только для удобства вывода).
     */
    private static Stats calcStats(long[] values) {
        if (values.length == 0) {
            return new Stats(0.0, 0.0, 0.0);
        }
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long sum = 0;
        for (long v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        double avg = sum / (double) values.length;
        return new Stats(min, max, avg);
    }

    /**
     * Экспортирует сводные результаты по всем сериям в CSV-файл.
     * Формат строк: один размер массива = одна строка с 13 полями, разделённых ';'.
     */
    private static void exportCsv(Path dir, List<SeriesResult> results) throws IOException {
        Path csv = dir.resolve("merge_data.csv");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(csv, StandardCharsets.UTF_8))) {
            w.println("size;" +
                    "best_ms;worst_ms;avg_ms;" +
                    "best_depth;worst_depth;avg_depth;" +
                    "best_calls;worst_calls;avg_calls;" +
                    "best_mem_bytes;worst_mem_bytes;avg_mem_bytes");
            for (SeriesResult r : results) {
                w.printf("%d;%.4f;%.4f;%.4f;%.1f;%.1f;%.1f;%.1f;%.1f;%.1f;%.1f;%.1f;%.1f%n",
                        r.size,
                        r.bestTime, r.worstTime, r.avgTime,
                        r.bestDepth, r.worstDepth, r.avgDepth,
                        r.bestCalls, r.worstCalls, r.avgCalls,
                        r.bestMemBytes, r.worstMemBytes, r.avgMemBytes
                );
            }
        }
        System.out.println("CSV (merge sort): " + csv);
    }

    /**
     * Печатает в консоль компактную таблицу с усреднёнными значениями,
     * удобную для быстрого просмотра без открытия CSV-файла.
     */
    private static void printTable(List<SeriesResult> results) {
        String line = "+----------+------------+------------+------------+------------+------------+------------+";
        String format = "| %8s | %10s | %10s | %10s | %10s | %10s | %10s |%n";

        System.out.println();
        System.out.println("Сводная таблица (время и глубина рекурсии):");
        System.out.println(line);
        System.out.printf(format,
                "size",
                "best_ms",
                "avg_ms",
                "worst_ms",
                "avg_depth",
                "avg_calls",
                "avg_memKB");
        System.out.println(line);
        for (SeriesResult r : results) {
            System.out.printf(format,
                    r.size,
                    String.format("%.3f", r.bestTime),
                    String.format("%.3f", r.avgTime),
                    String.format("%.3f", r.worstTime),
                    String.format("%.1f", r.avgDepth),
                    String.format("%.1f", r.avgCalls),
                    String.format("%.1f", r.avgMemBytes / 1024.0)
            );
        }
        System.out.println(line);
        System.out.println();
    }

    /**
     * Результат одной серии для одного значения N.
     * Содержит агрегированные характеристики по 20 запускам:
     * время, глубина рекурсии, количество вызовов и объём дополнительной памяти.
     */
    public static class SeriesResult {
        public final int size;

        public final double bestTime;
        public final double worstTime;
        public final double avgTime;

        public final double bestDepth;
        public final double worstDepth;
        public final double avgDepth;

        public final double bestCalls;
        public final double worstCalls;
        public final double avgCalls;

        public final double bestMemBytes;
        public final double worstMemBytes;
        public final double avgMemBytes;

        public SeriesResult(int size,
                            double bestTime, double worstTime, double avgTime,
                            double bestDepth, double worstDepth, double avgDepth,
                            double bestCalls, double worstCalls, double avgCalls,
                            double bestMemBytes, double worstMemBytes, double avgMemBytes) {
            this.size = size;
            this.bestTime = bestTime;
            this.worstTime = worstTime;
            this.avgTime = avgTime;

            this.bestDepth = bestDepth;
            this.worstDepth = worstDepth;
            this.avgDepth = avgDepth;

            this.bestCalls = bestCalls;
            this.worstCalls = worstCalls;
            this.avgCalls = avgCalls;

            this.bestMemBytes = bestMemBytes;
            this.worstMemBytes = worstMemBytes;
            this.avgMemBytes = avgMemBytes;
        }
    }
}

