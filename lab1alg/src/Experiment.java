import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
/**
 * Класс эксперимента: запускает сортировку для разных размеров массива много раз,
 * собирает время, проходы и обмены, считает минимум/максимум/среднее и сохраняет данные.
 */
public class Experiment {

    /** Размеры массивов, для которых проводим замеры (8 серий по заданию). */
    private static final int[] SIZES = {1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000};
    /** Сколько раз запускать сортировку для каждого размера (20 попыток на серию). */
    private static final int RUNS_PER_SIZE = 20;
    /** Генератор случайных чисел; seed 42 — чтобы результаты были воспроизводимы при повторном запуске. */
    private static final Random RND = new Random(42);

    /**
     * Точка входа эксперимента: создаём папку для результатов, запускаем все серии,
     * экспортируем CSV и строим графики.
     */
    public static void main(String[] args) throws IOException {
        // Папка, куда сохраняем data.csv и PNG-графики (создаётся, если её нет).
        Path outDir = Path.of("results");
        Files.createDirectories(outDir);

        System.out.println("Запуск эксперимента: сортировка перемешиванием");
        System.out.println("Размеры: " + Arrays.toString(SIZES));
        System.out.println("Запусков на размер: " + RUNS_PER_SIZE);

        // Запускаем все серии (для каждого размера из SIZES — по RUNS_PER_SIZE запусков).
        List<SeriesResult> results = runAllSeries();
        // Сохраняем сводную таблицу в results/data.csv (разделитель — точка с запятой).
        exportCsv(outDir, results);
        // Печатаем аккуратную таблицу результатов в консоль.
        printTable(results);
        // Строим 4 графика в папке results (PNG).
        Charts.buildAll(outDir, results);

        System.out.println("Результаты сохранены в " + outDir.toAbsolutePath());
    }

    /**
     * Запускает все серии: для каждого размера массива делаем RUNS_PER_SIZE запусков,
     * собираем времена/проходы/обмены и считаем best/worst/avg по времени и средние проходы/обмены.
     *
     * @return список результатов по каждой серии (одна серия = один размер массива)
     */
    private static List<SeriesResult> runAllSeries() {
        List<SeriesResult> results = new ArrayList<>();

        // Перебираем все размеры из SIZES (1000, 2000, ... 128000).
        for (int size : SIZES) {
            System.out.println("Размер массива: " + size);

            // Массивы для хранения 20 замеров в этой серии.
            double[] times = new double[RUNS_PER_SIZE];
            long[] passes = new long[RUNS_PER_SIZE];
            long[] swaps = new long[RUNS_PER_SIZE];

            for (int run = 0; run < RUNS_PER_SIZE; run++) {
                // Генерируем новый случайный массив длины size (числа от -1 до 1).
                double[] arr = generateArray(size);
                long start = System.nanoTime();
                // Запускаем сортировку; массив arr отсортируется, r содержит passes и swaps.
                ShakerSort.Result r = ShakerSort.sort(arr);
                long end = System.nanoTime();

                times[run] = (end - start) / 1_000_000.0;
                passes[run] = r.passes;
                swaps[run] = r.swaps;
            }

            // По 20 замерам считаем: лучшее и наихудшее время, среднее время, средние проходы и обмены.
            double best = Arrays.stream(times).min().orElse(0);
            double worst = Arrays.stream(times).max().orElse(0);
            double avgTime = Arrays.stream(times).average().orElse(0);
            double avgPasses = Arrays.stream(passes).asDoubleStream().average().orElse(0);
            double avgSwaps = Arrays.stream(swaps).asDoubleStream().average().orElse(0);

            // Сохраняем результат серии в список (пригодится для графиков и CSV).
            results.add(new SeriesResult(size, best, worst, avgTime, avgPasses, avgSwaps, times));
        }

        return results;
    }

    /**
     * Создаёт массив длины size, заполненный случайными double в интервале [-1, 1].
     * Формула: -1 + 2 * random() даёт равномерное распределение от -1 до 1.
     *
     * @param size длина массива
     * @return новый массив со случайными значениями
     */
    private static double[] generateArray(int size) {
        double[] a = new double[size];
        for (int i = 0; i < size; i++) {
            // RND.nextDouble() даёт число от 0.0 до 1.0; умножаем на 2 и сдвигаем на -1.
            a[i] = -1 + 2 * RND.nextDouble();
        }
        return a;
    }

    /**
     * Записывает сводную таблицу по всем сериям в CSV-файл (разделитель — точка с запятой).
     * Первая строка — заголовки: size; best_ms; worst_ms; avg_ms; avg_passes; avg_swaps.
     *
     * @param dir    папка, в которую сохраняем (обычно results)
     * @param results список SeriesResult по всем размерам
     */
    private static void exportCsv(Path dir, List<SeriesResult> results) throws IOException {
        Path csv = dir.resolve("data.csv");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(csv, StandardCharsets.UTF_8))) {
            w.println("size;best_ms;worst_ms;avg_ms;avg_passes;avg_swaps");
            for (SeriesResult r : results) {
                w.printf("%d;%.4f;%.4f;%.4f;%.1f;%.1f%n",
                        r.size, r.bestTime, r.worstTime, r.avgTime, r.avgPasses, r.avgSwaps);
            }
        }
        System.out.println("CSV: " + csv);
    }

    /**
     * Печатает в консоль аккуратную таблицу с теми же данными, что и в CSV.
     *
     * @param results список результатов серий
     */
    private static void printTable(List<SeriesResult> results) {
        String line = "+----------+------------+------------+------------+--------------+--------------+";
        String format = "| %8s | %10s | %10s | %10s | %12s | %12s |%n";

        System.out.println();
        System.out.println("Сводная таблица результатов (по данным CSV):");
        System.out.println(line);
        System.out.printf(format, "size", "best_ms", "worst_ms", "avg_ms", "avg_passes", "avg_swaps");
        System.out.println(line);
        for (SeriesResult r : results) {
            System.out.printf(
                    format,
                    r.size,
                    String.format("%.4f", r.bestTime),
                    String.format("%.4f", r.worstTime),
                    String.format("%.4f", r.avgTime),
                    String.format("%.1f", r.avgPasses),
                    String.format("%.1f", r.avgSwaps)
            );
        }
        System.out.println(line);
        System.out.println();
    }

    /**
     * Результат одной серии: один размер массива, по 20 запускам посчитаны best/worst/avg время
     * и средние проходы и обмены. allTimes — все 20 замеров времени (для детального анализа при необходимости).
     */
    public static class SeriesResult {
        /** Размер массива в этой серии (например, 4000). */
        public final int size;
        /** Наименьшее время среди 20 запусков (мс). */
        public final double bestTime;
        /** Наибольшее время среди 20 запусков (мс). */
        public final double worstTime;
        /** Среднее время по 20 запускам (мс). */
        public final double avgTime;
        /** Среднее количество проходов по 20 запускам. */
        public final double avgPasses;
        /** Среднее количество обменов по 20 запускам. */
        public final double avgSwaps;
        /** Все 20 замеров времени (мс) — массив длины RUNS_PER_SIZE. */
        public final double[] allTimes;

        public SeriesResult(int size, double bestTime, double worstTime, double avgTime, double avgPasses, double avgSwaps, double[] allTimes) {
            this.size = size;
            this.bestTime = bestTime;
            this.worstTime = worstTime;
            this.avgTime = avgTime;
            this.avgPasses = avgPasses;
            this.avgSwaps = avgSwaps;
            this.allTimes = allTimes;
        }
    }
}