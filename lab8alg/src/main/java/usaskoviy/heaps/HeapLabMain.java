package usaskoviy.heaps; // пакет лабораторной по кучам

import java.io.File; // каталог для CSV и PNG
import java.io.PrintWriter; // запись таблицы результатов
import java.nio.charset.StandardCharsets; // UTF-8 для CSV
import java.util.Locale; // Locale.ROOT — точка в числах независимо от системы
import java.util.Random; // генерация случайных ключей для заполнения кучи

/**
 * Точка входа: сравнение бинарной и биномиальной min-кучи по условию лабораторной (размеры N, 1000 операций, графики).
 */
public final class HeapLabMain {

    private static final int OPS = 1000; // сколько раз повторяем каждую операцию в одной серии замеров
    private static final int BATCH = 25; // размер пакета для оценки «пиков» 
    private static final int[] POWERS = {3, 4, 5, 6, 7}; // степени десятки: N = 10^3 … 10^7
    private static final long SEED = 20260208L; // фиксированное зерно — воспроизводимость эксперимента

    private HeapLabMain() {
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT); // printf/CSV с точкой как разделителем дроби
        Random rng = new Random(SEED); // один генератор на всю программу

        File outDir = new File("heap_lab_out"); // папка результатов рядом с рабочей директорией JVM
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("Cannot create " + outDir.getAbsolutePath()); // не смогли создать каталог
        }

        int series = POWERS.length; // число серий (разных N)
        int[] nValues = new int[series]; // массив размеров N для оси графиков
        for (int i = 0; i < series; i++) {
            nValues[i] = pow10(POWERS[i]); // N = 10^i
        }

        // Накопители метрик для бинарной (bin) и биномиальной (bio) куч: среднее, макс за одну операцию, макс среднего по пакетам
        double[] binPeekAvg = new double[series];
        double[] binPeekMax = new double[series];
        double[] binPeekBatchMax = new double[series];
        double[] bioPeekAvg = new double[series];
        double[] bioPeekMax = new double[series];
        double[] bioPeekBatchMax = new double[series];

        double[] binDelAvg = new double[series];
        double[] binDelMax = new double[series];
        double[] binDelBatchMax = new double[series];
        double[] bioDelAvg = new double[series];
        double[] bioDelMax = new double[series];
        double[] bioDelBatchMax = new double[series];

        double[] binInsAvg = new double[series];
        double[] binInsMax = new double[series];
        double[] binInsBatchMax = new double[series];
        double[] bioInsAvg = new double[series];
        double[] bioInsMax = new double[series];
        double[] bioInsBatchMax = new double[series];

        System.out.println("Кучи: бинарная min и биномиальная min");
        System.out.println("N = 10^3..10^7, по " + OPS + " операций findMin / deleteMin / insert");
        System.out.println();

        for (int si = 0; si < series; si++) { // перебор всех размеров N
            int n = nValues[si]; // текущий размер кучи
            int[] keys = new int[n]; // массив ключей для заполнения обеих куч в одном порядке
            for (int i = 0; i < n; i++) {
                keys[i] = rng.nextInt(); // случайные int — одинаковая последовательность для честного сравнения
            }

            warmup(keys, Math.min(n, 50_000)); // прогрев JVM на подмножестве (не раздуваем память на малых тестах)

            BinaryMinHeap binary = new BinaryMinHeap(n + OPS + 16); // запас под N + 1000 вставок при замере insert
            BinomialMinHeap binomial = new BinomialMinHeap(); // биномиальная куча без предзадания ёмкости
            fillBinary(binary, keys); // вставляем все ключи в бинарную кучу
            fillBinomial(binomial, keys); // те же ключи в том же порядке — в биномиальную

            Timings peekB = measurePeek(binary); // 1000× findMin на бинарной куче размера N
            Timings peekO = measurePeek(binomial); // то же для биномиальной
            binPeekAvg[si] = peekB.avgNs; // среднее время одного findMin (нс)
            binPeekMax[si] = peekB.maxSingleNs; // максимум за одну операцию findMin
            binPeekBatchMax[si] = peekB.maxBatchAvgNs; // максимум средних по пакетам по 25 операций
            bioPeekAvg[si] = peekO.avgNs;
            bioPeekMax[si] = peekO.maxSingleNs;
            bioPeekBatchMax[si] = peekO.maxBatchAvgNs;

            binary.clear(); // сбрасываем бинарную кучу
            binomial.clear(); // сбрасываем биномиальную
            fillBinary(binary, keys); // снова заполняем до N — куча как в начале серии delete
            fillBinomial(binomial, keys);

            Timings delB = measureDelete(binary, OPS); // 1000 удалений минимума (размер уменьшится до N-1000)
            Timings delO = measureDelete(binomial, OPS);
            binDelAvg[si] = delB.avgNs;
            binDelMax[si] = delB.maxSingleNs;
            binDelBatchMax[si] = delB.maxBatchAvgNs;
            bioDelAvg[si] = delO.avgNs;
            bioDelMax[si] = delO.maxSingleNs;
            bioDelBatchMax[si] = delO.maxBatchAvgNs;

            binary.clear(); // снова чистые кучи для замера вставок
            binomial.clear();
            fillBinary(binary, keys); // снова ровно N элементов
            fillBinomial(binomial, keys);

            Timings insB = measureInsert(binary, OPS); // 1000 вставок новых ключей (не пересекающихся с исходным наполнением)
            Timings insO = measureInsert(binomial, OPS);
            binInsAvg[si] = insB.avgNs;
            binInsMax[si] = insB.maxSingleNs;
            binInsBatchMax[si] = insB.maxBatchAvgNs;
            bioInsAvg[si] = insO.avgNs;
            bioInsMax[si] = insO.maxSingleNs;
            bioInsBatchMax[si] = insO.maxBatchAvgNs;

            System.out.printf("N = %,d — peek avg: bin %.2f ns, bio %.2f | del avg: bin %.2f, bio %.2f | ins avg: bin %.2f, bio %.2f%n",
                    n, binPeekAvg[si], bioPeekAvg[si], binDelAvg[si], bioDelAvg[si], binInsAvg[si], bioInsAvg[si]); // краткая сводка в консоль
        }

        writeCsv(outDir, nValues, // сохраняем полную таблицу в CSV
                binPeekAvg, binPeekMax, binPeekBatchMax, bioPeekAvg, bioPeekMax, bioPeekBatchMax,
                binDelAvg, binDelMax, binDelBatchMax, bioDelAvg, bioDelMax, bioDelBatchMax,
                binInsAvg, binInsMax, binInsBatchMax, bioInsAvg, bioInsMax, bioInsBatchMax);

        HeapCharts.saveAllCharts( // строим 6 графиков (среднее и максимум для трёх операций)
                outDir,
                nValues,
                binPeekAvg, bioPeekAvg, binPeekMax, bioPeekMax,
                binDelAvg, bioDelAvg, binDelMax, bioDelMax,
                binInsAvg, bioInsAvg, binInsMax, bioInsMax);

        System.out.println();
        System.out.println("Готово: " + outDir.getAbsolutePath()); // путь к папке с результатами
    }

    /** Возвращает 10^p целым (p от 3 до 7). */
    private static int pow10(int p) {
        int x = 1;
        for (int i = 0; i < p; i++) {
            x *= 10; // умножаем на 10 ровно p раз
        }
        return x;
    }

    /** Несколько прогонов без записи в отчёт — стабилизация JIT-компилятора. */
    private static void warmup(int[] keysSample, int take) {
        int n = Math.min(keysSample.length, take); // не берём больше, чем есть в выборке
        int[] k = new int[n];
        System.arraycopy(keysSample, 0, k, 0, n); // копируем префикс ключей
        for (int r = 0; r < 3; r++) { // три раунда прогрева
            BinaryMinHeap b = new BinaryMinHeap(n + OPS);
            BinomialMinHeap o = new BinomialMinHeap();
            fillBinary(b, k);
            fillBinomial(o, k);
            measurePeek(b); // прогреваем поиск
            measurePeek(o);
            b.clear();
            o.clear();
            fillBinary(b, k);
            fillBinomial(o, k);
            measureDelete(b, Math.min(OPS, n)); // прогреваем удаление (не больше размера кучи)
            measureDelete(o, Math.min(OPS, n));
        }
    }

    /** Заполнение бинарной кучи массивом ключей подряд. */
    private static void fillBinary(BinaryMinHeap h, int[] keys) {
        for (int key : keys) {
            h.insert(key); // порядок вставок задаёт один и тот же набор ключей
        }
    }

    /** Заполнение биномиальной кучи тем же массивом в том же порядке. */
    private static void fillBinomial(BinomialMinHeap h, int[] keys) {
        for (int key : keys) {
            h.insert(key);
        }
    }

    /** Замер 1000 операций findMin: сумма, среднее, макс одной операции, макс среднего по пакетам BATCH. */
    private static Timings measurePeek(BinaryMinHeap h) {
        long total = 0; // суммарное время всех OPS операций (нс)
        long maxSingle = 0; // максимальная длительность одной findMin
        long maxBatchAvg = 0; // максимум средних длительностей по пакетам
        int batches = OPS / BATCH; // число пакетов (1000/25 = 40)
        for (int b = 0; b < batches; b++) {
            long batch = 0; // суммарное время текущего пакета
            for (int i = 0; i < BATCH; i++) {
                long t0 = System.nanoTime(); // начало интервала замера
                h.findMin(); // просмотр минимума без изменения кучи
                long dt = System.nanoTime() - t0; // длительность одной операции
                batch += dt;
                total += dt;
                if (dt > maxSingle) {
                    maxSingle = dt; // обновляем максимум «одного вызова»
                }
            }
            long avgBatch = batch / BATCH; // среднее время внутри пакета (аналог из методички)
            if (avgBatch > maxBatchAvg) {
                maxBatchAvg = avgBatch; // ищем «худший» пакет по среднему
            }
        }
        return new Timings((double) total / OPS, maxSingle, maxBatchAvg); // среднее = total/OPS
    }

    /** То же для биномиальной кучи. */
    private static Timings measurePeek(BinomialMinHeap h) {
        long total = 0;
        long maxSingle = 0;
        long maxBatchAvg = 0;
        int batches = OPS / BATCH;
        for (int b = 0; b < batches; b++) {
            long batch = 0;
            for (int i = 0; i < BATCH; i++) {
                long t0 = System.nanoTime();
                h.findMin();
                long dt = System.nanoTime() - t0;
                batch += dt;
                total += dt;
                if (dt > maxSingle) {
                    maxSingle = dt;
                }
            }
            long avgBatch = batch / BATCH;
            if (avgBatch > maxBatchAvg) {
                maxBatchAvg = avgBatch;
            }
        }
        return new Timings((double) total / OPS, maxSingle, maxBatchAvg);
    }

    /** Замер count операций deleteMin (count обычно 1000). */
    private static Timings measureDelete(BinaryMinHeap h, int count) {
        long total = 0;
        long maxSingle = 0;
        long maxBatchAvg = 0;
        int batches = count / BATCH;
        for (int b = 0; b < batches; b++) {
            long batch = 0;
            for (int i = 0; i < BATCH; i++) {
                long t0 = System.nanoTime();
                h.deleteMin(); // извлечение и удаление минимума
                long dt = System.nanoTime() - t0;
                batch += dt;
                total += dt;
                if (dt > maxSingle) {
                    maxSingle = dt;
                }
            }
            long avgBatch = batch / BATCH;
            if (avgBatch > maxBatchAvg) {
                maxBatchAvg = avgBatch;
            }
        }
        return new Timings((double) total / count, maxSingle, maxBatchAvg);
    }

    private static Timings measureDelete(BinomialMinHeap h, int count) {
        long total = 0;
        long maxSingle = 0;
        long maxBatchAvg = 0;
        int batches = count / BATCH;
        for (int b = 0; b < batches; b++) {
            long batch = 0;
            for (int i = 0; i < BATCH; i++) {
                long t0 = System.nanoTime();
                h.deleteMin();
                long dt = System.nanoTime() - t0;
                batch += dt;
                total += dt;
                if (dt > maxSingle) {
                    maxSingle = dt;
                }
            }
            long avgBatch = batch / BATCH;
            if (avgBatch > maxBatchAvg) {
                maxBatchAvg = avgBatch;
            }
        }
        return new Timings((double) total / count, maxSingle, maxBatchAvg);
    }

    /** Замер count вставок; ключи с большого константного смещения, чтобы не совпасть с случайным наполнением. */
    private static Timings measureInsert(BinaryMinHeap h, int count) {
        long total = 0;
        long maxSingle = 0;
        long maxBatchAvg = 0;
        int batches = count / BATCH;
        int idx = 0; // счётчик для уникальных ключей вставки
        for (int b = 0; b < batches; b++) {
            long batch = 0;
            for (int i = 0; i < BATCH; i++) {
                int key = 0x7000_0000 + (idx++); // область ключей вне типичного random.nextInt() в fill
                long t0 = System.nanoTime();
                h.insert(key); // добавляем новый элемент
                long dt = System.nanoTime() - t0;
                batch += dt;
                total += dt;
                if (dt > maxSingle) {
                    maxSingle = dt;
                }
            }
            long avgBatch = batch / BATCH;
            if (avgBatch > maxBatchAvg) {
                maxBatchAvg = avgBatch;
            }
        }
        return new Timings((double) total / count, maxSingle, maxBatchAvg);
    }

    private static Timings measureInsert(BinomialMinHeap h, int count) {
        long total = 0;
        long maxSingle = 0;
        long maxBatchAvg = 0;
        int batches = count / BATCH;
        int idx = 0;
        for (int b = 0; b < batches; b++) {
            long batch = 0;
            for (int i = 0; i < BATCH; i++) {
                int key = 0x7000_0000 + (idx++);
                long t0 = System.nanoTime();
                h.insert(key);
                long dt = System.nanoTime() - t0;
                batch += dt;
                total += dt;
                if (dt > maxSingle) {
                    maxSingle = dt;
                }
            }
            long avgBatch = batch / BATCH;
            if (avgBatch > maxBatchAvg) {
                maxBatchAvg = avgBatch;
            }
        }
        return new Timings((double) total / count, maxSingle, maxBatchAvg);
    }

    /** Запись одной строки заголовка и строк по каждому N во все колонки метрик. */
    private static void writeCsv(
            File dir,
            int[] nValues,
            double[] binPeekAvg, double[] binPeekMax, double[] binPeekBatchMax,
            double[] bioPeekAvg, double[] bioPeekMax, double[] bioPeekBatchMax,
            double[] binDelAvg, double[] binDelMax, double[] binDelBatchMax,
            double[] bioDelAvg, double[] bioDelMax, double[] bioDelBatchMax,
            double[] binInsAvg, double[] binInsMax, double[] binInsBatchMax,
            double[] bioInsAvg, double[] bioInsMax, double[] bioInsBatchMax) throws Exception {
        File f = new File(dir, "heap_lab_metrics.csv"); // имя файла таблицы
        try (PrintWriter pw = new PrintWriter(f, StandardCharsets.UTF_8)) {
            pw.println("N," // первая колонка — размер кучи
                    + "peek_avg_ns_binary,peek_max_ns_binary,peek_batchmaxavg_ns_binary,"
                    + "peek_avg_ns_binomial,peek_max_ns_binomial,peek_batchmaxavg_ns_binomial,"
                    + "delete_avg_ns_binary,delete_max_ns_binary,delete_batchmaxavg_ns_binary,"
                    + "delete_avg_ns_binomial,delete_max_ns_binomial,delete_batchmaxavg_ns_binomial,"
                    + "insert_avg_ns_binary,insert_max_ns_binary,insert_batchmaxavg_ns_binary,"
                    + "insert_avg_ns_binomial,insert_max_ns_binomial,insert_batchmaxavg_ns_binomial");
            for (int i = 0; i < nValues.length; i++) {
                pw.printf(Locale.ROOT,
                        "%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                        nValues[i],
                        binPeekAvg[i], binPeekMax[i], binPeekBatchMax[i],
                        bioPeekAvg[i], bioPeekMax[i], bioPeekBatchMax[i],
                        binDelAvg[i], binDelMax[i], binDelBatchMax[i],
                        bioDelAvg[i], bioDelMax[i], bioDelBatchMax[i],
                        binInsAvg[i], binInsMax[i], binInsBatchMax[i],
                        bioInsAvg[i], bioInsMax[i], bioInsBatchMax[i]);
            }
        }
    }

    /** Результат одного блока замеров (одна операция, много повторов). */
    private static final class Timings {
        final double avgNs; // среднее время одной операции в наносекундах
        final long maxSingleNs; // максимум длительности одной операции
        final double maxBatchAvgNs; // максимум из средних по пакетам BATCH операций

        Timings(double avgNs, long maxSingleNs, double maxBatchAvgNs) {
            this.avgNs = avgNs;
            this.maxSingleNs = maxSingleNs;
            this.maxBatchAvgNs = maxBatchAvgNs;
        }
    }
}
