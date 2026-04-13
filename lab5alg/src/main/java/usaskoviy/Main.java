package usaskoviy; // корневой пакет приложения с точкой входа main

import usaskoviy.analysis.Lab6Charts; // сохранение PNG-графиков по результатам замеров
import usaskoviy.tree.AvlTree; // самобалансирующееся дерево
import usaskoviy.tree.BinarySearchTree; // обычное BST

import java.io.File; // путь к файлам lab6_*.png в рабочем каталоге
import java.util.Locale; // ROOT — единый формат чисел с точкой на любой ОС
import java.util.Random; // генерация перестановок и случайных ключей поиска

/**
 * Лабораторная работа №6: сравнение BST и AVL и линейного поиска в массиве.
 * 10 серий (i = 1…10), N = 2^(10+i); в каждой серии 20 циклов: 10 случайная перестановка 0…N−1, 10 по возрастанию.
 */
public final class Main {

    private static final int SERIES_COUNT = 10; // число серий по методичке
    private static final int CYCLES_PER_SERIES = 20; // циклов в серии: 10 случайных + 10 отсортированных
    private static final int RANDOM_CYCLES = 10; // первые 10 циклов — случайный порядок данных
    private static final int SEARCH_OPS = 1000; // сколько поисков замеряем за цикл
    private static final int DELETE_OPS = 1000; // сколько удалений замеряем за цикл
    private static final long RNG_SEED = 20260411L; // фиксированное зерно — воспроизводимость эксперимента

    private static final int MAX_N = 1 << 20; // максимальный N = 2^20 — размер буфера для выбора уникальных ключей удаления

    /** Точка входа: полный прогон серий, вывод таблицы, сохранение трёх графиков. */
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT); // числа в printf с точкой, не зависят от русской локали
        Random rng = new Random(RNG_SEED); // один генератор на всю программу

        int[] nValues = new int[SERIES_COUNT]; // N для каждой серии (индекс 0 = первая серия)
        for (int si = 0; si < SERIES_COUNT; si++) { // si = 0…9 — индекс серии в массиве
            int seriesIndex = si + 1; // номер серии i в формуле N = 2^(10+i), от 1 до 10
            nValues[si] = 1 << (10 + seriesIndex); // сдвиг: 2^(10+i), например i=1 → 2^11
        }

        // Накопители суммарного времени (нс) по 10 циклам «случайного» режима (R) и «отсортированного» (S)
        double[] sumInsBstR = new double[SERIES_COUNT]; // вставка BST, случайный порядок
        double[] sumInsAvlR = new double[SERIES_COUNT]; // вставка AVL, случайный порядок
        double[] sumInsBstS = new double[SERIES_COUNT]; // вставка BST, отсортированный массив
        double[] sumInsAvlS = new double[SERIES_COUNT]; // вставка AVL, отсортированный массив

        double[] sumSearchBstR = new double[SERIES_COUNT]; // поиск BST, случайный порядок данных
        double[] sumSearchAvlR = new double[SERIES_COUNT]; // поиск AVL, случайный
        double[] sumSearchArrR = new double[SERIES_COUNT]; // линейный поиск в массиве, случайный порядок
        double[] sumSearchBstS = new double[SERIES_COUNT]; // поиск BST, отсортированный порядок
        double[] sumSearchAvlS = new double[SERIES_COUNT]; // поиск AVL, отсортированный
        double[] sumSearchArrS = new double[SERIES_COUNT]; // линейный поиск, отсортированный массив

        double[] sumDelBstR = new double[SERIES_COUNT]; // удаление BST, случайный порядок вставки
        double[] sumDelAvlR = new double[SERIES_COUNT]; // удаление AVL, случайный
        double[] sumDelBstS = new double[SERIES_COUNT]; // удаление BST после отсортированных вставок
        double[] sumDelAvlS = new double[SERIES_COUNT]; // удаление AVL после отсортированных вставок

        int[] searchKeys = new int[SEARCH_OPS]; // буфер из 1000 ключей для поиска (общий для BST, AVL, массива)
        int[] deleteKeys = new int[DELETE_OPS]; // 1000 различных ключей для удаления из деревьев
        int[] keyPool = new int[MAX_N]; // вспомогательный массив 0…N−1 для частичной перестановки (выбор удаляемых)

        BinarySearchTree bst = new BinarySearchTree(); // одно BST на все циклы (очищается каждый цикл)
        AvlTree avl = new AvlTree(); // одно AVL на все циклы

        warmUp(rng, bst, avl, searchKeys, deleteKeys, keyPool); // прогрев JIT до основных замеров

        System.out.println("Лабораторная работа №6: BST, AVL, линейный поиск в массиве.");
        System.out.println("10 серий, в каждой — 20 циклов (циклы 1–10: случайная перестановка 0..N−1, 11–20: по возрастанию).");
        System.out.println("В каждом цикле: вставка N элементов; 1000 поисков; 1000 удалений. Ключи поиска — случайные в [0, N−1].");
        System.out.println(); // пустая строка для читаемости

        for (int si = 0; si < SERIES_COUNT; si++) { // перебор 10 серий
            int n = nValues[si]; // размер массива и число вставляемых ключей в этом цикле
            int[] data = new int[n]; // рабочий массив: перестановка или 0,1,…,N−1

            System.out.println("=== Серия " + (si + 1) + ", N = 2^" + (11 + si) + " = " + n + " ==="); // 11+si = 10+(si+1)

            for (int cycle = 0; cycle < CYCLES_PER_SERIES; cycle++) { // 20 циклов на серию
                boolean randomOrder = cycle < RANDOM_CYCLES; // циклы 0…9 — случайные данные, 10…19 — отсортированные
                if (n >= (1 << 17)) { // для больших N долго — печатаем прогресс (опционально для отладки)
                    System.out.printf("  цикл %d/20 (%s)...%n",
                            cycle + 1,
                            randomOrder ? "случайная перестановка" : "по возрастанию");
                    System.out.flush(); // сразу выводим в консоль без буфера
                }
                if (randomOrder) {
                    fillRandomPermutation(data, rng); // заполняем случайной перестановкой 0…N−1
                } else {
                    fillAscending(data); // заполняем 0,1,…,N−1 по возрастанию
                }

                bst.clear(); // пустое BST перед вставкой
                long t0 = System.nanoTime(); // начало замера вставки в BST
                for (int v : data) {
                    bst.insert(v); // вставляем все элементы в порядке следования в массиве
                }
                long insBst = System.nanoTime() - t0; // длительность полной вставки в BST в нс

                avl.clear(); // пустое AVL
                t0 = System.nanoTime();
                for (int v : data) {
                    avl.insert(v); // тот же порядок ключей — честное сравнение с BST
                }
                long insAvl = System.nanoTime() - t0; // полная вставка в AVL

                for (int k = 0; k < SEARCH_OPS; k++) {
                    searchKeys[k] = rng.nextInt(n); // случайный ключ из [0, N−1] — все существуют в деревьях
                }

                t0 = System.nanoTime();
                for (int k : searchKeys) {
                    bst.contains(k); // 1000 поисков в BST одним и тем же набором ключей
                }
                long seaBst = System.nanoTime() - t0;

                t0 = System.nanoTime();
                for (int k : searchKeys) {
                    avl.contains(k); // те же 1000 ключей в AVL
                }
                long seaAvl = System.nanoTime() - t0;

                t0 = System.nanoTime();
                for (int k : searchKeys) {
                    linearSearch(data, k); // линейный проход по исходному массиву для тех же ключей
                }
                long seaArr = System.nanoTime() - t0;

                pickUniqueFromRange(keyPool, rng, n, deleteKeys, DELETE_OPS); // 1000 разных ключей из 0…N−1 для удаления

                t0 = System.nanoTime();
                for (int k : deleteKeys) {
                    bst.delete(k); // удаляем из BST (после поисков дерево ещё полное по размеру N)
                }
                long delBst = System.nanoTime() - t0;

                t0 = System.nanoTime();
                for (int k : deleteKeys) {
                    avl.delete(k); // те же ключи из AVL
                }
                long delAvl = System.nanoTime() - t0;

                if (randomOrder) { // накопление статистики по «случайным» 10 циклам
                    sumInsBstR[si] += insBst;
                    sumInsAvlR[si] += insAvl;
                    sumSearchBstR[si] += seaBst;
                    sumSearchAvlR[si] += seaAvl;
                    sumSearchArrR[si] += seaArr;
                    sumDelBstR[si] += delBst;
                    sumDelAvlR[si] += delAvl;
                } else { // накопление по «отсортированным» 10 циклам
                    sumInsBstS[si] += insBst;
                    sumInsAvlS[si] += insAvl;
                    sumSearchBstS[si] += seaBst;
                    sumSearchAvlS[si] += seaAvl;
                    sumSearchArrS[si] += seaArr;
                    sumDelBstS[si] += delBst;
                    sumDelAvlS[si] += delAvl;
                }
            }

            printSeriesMeans( // печать усреднённых по 10 циклам метрик для текущей серии
                    n,
                    sumInsBstR[si], sumInsAvlR[si], sumInsBstS[si], sumInsAvlS[si],
                    sumSearchBstR[si], sumSearchAvlR[si], sumSearchArrR[si],
                    sumSearchBstS[si], sumSearchAvlS[si], sumSearchArrS[si],
                    sumDelBstR[si], sumDelAvlR[si], sumDelBstS[si], sumDelAvlS[si]);
            System.out.println();
        }

        int div = RANDOM_CYCLES; // делитель для усреднения: 10 циклов каждого типа
        double[] avgInsBstR = avg(sumInsBstR, div); // среднее суммарное время вставки BST (случайный порядок), нс
        double[] avgInsAvlR = avg(sumInsAvlR, div);
        double[] avgInsBstS = avg(sumInsBstS, div);
        double[] avgInsAvlS = avg(sumInsAvlS, div);

        double[] avgSeaBstR = avgPerOp(sumSearchBstR, div, SEARCH_OPS); // среднее время одного поиска BST, нс
        double[] avgSeaAvlR = avgPerOp(sumSearchAvlR, div, SEARCH_OPS);
        double[] avgSeaArrR = avgPerOp(sumSearchArrR, div, SEARCH_OPS);
        double[] avgSeaBstS = avgPerOp(sumSearchBstS, div, SEARCH_OPS);
        double[] avgSeaAvlS = avgPerOp(sumSearchAvlS, div, SEARCH_OPS);
        double[] avgSeaArrS = avgPerOp(sumSearchArrS, div, SEARCH_OPS);

        double[] avgDelBstR = avgPerOp(sumDelBstR, div, DELETE_OPS); // среднее время одного удаления
        double[] avgDelAvlR = avgPerOp(sumDelAvlR, div, DELETE_OPS);
        double[] avgDelBstS = avgPerOp(sumDelBstS, div, DELETE_OPS);
        double[] avgDelAvlS = avgPerOp(sumDelAvlS, div, DELETE_OPS);

        File dir = new File(".").getAbsoluteFile(); // каталог запуска JVM (в IDEA — обычно корень модуля)
        File f1 = new File(dir, "lab6_insertion.png"); // график вставки
        File f2 = new File(dir, "lab6_search.png"); // график поиска
        File f3 = new File(dir, "lab6_deletion.png"); // график удаления

        Lab6Charts.saveInsertionChart(nValues, avgInsBstR, avgInsAvlR, avgInsBstS, avgInsAvlS, f1);
        Lab6Charts.saveSearchChart(
                nValues,
                avgSeaBstR, avgSeaAvlR, avgSeaArrR,
                avgSeaBstS, avgSeaAvlS, avgSeaArrS,
                f2);
        Lab6Charts.saveDeletionChart(nValues, avgDelBstR, avgDelAvlR, avgDelBstS, avgDelAvlS, f3);

        System.out.println("Графики сохранены в каталоге запуска:");
        System.out.println("  " + f1.getAbsolutePath());
        System.out.println("  " + f2.getAbsolutePath());
        System.out.println("  " + f3.getAbsolutePath());
        printConclusion(); // краткий текстовый вывод для консоли
    }

    /** Печать усреднённых по 10 циклам суммарных и средних времён для одной серии. */
    private static void printSeriesMeans(
            int n, // размер N (для справки в заголовке серии уже выведен)
            double insBr, double insAr, double insBs, double insAs, // суммы вставок по 10 циклам
            double seaBr, double seaAr, double seaArr, // суммы времён 1000 поисков
            double seaBs, double seaAs, double seaAsArr,
            double delBr, double delAr, double delBs, double delAs) { // суммы времён 1000 удалений
        int d = RANDOM_CYCLES; // 10 — число циклов каждого типа
        System.out.printf(
                "  Вставка всех N элементов — суммарное время (среднее по %d циклам), нс: BST случайный порядок %.0f, AVL %.0f; BST отсорт. %.0f, AVL %.0f%n",
                d, insBr / d, insAr / d, insBs / d, insAs / d);
        System.out.printf(
                "  Поиск 1000 раз — суммарное время (среднее по %d циклам), нс: BST случ. %.0f, AVL %.0f, массив %.0f; BST сорт. %.0f, AVL %.0f, массив %.0f%n",
                d, seaBr / d, seaAr / d, seaArr / d, seaBs / d, seaAs / d, seaAsArr / d);
        System.out.printf(
                "  Поиск — среднее за одну операцию, нс: BST случ. %.2f, AVL %.2f, массив %.2f; BST сорт. %.2f, AVL %.2f, массив %.2f%n",
                seaBr / d / SEARCH_OPS,
                seaAr / d / SEARCH_OPS,
                seaArr / d / SEARCH_OPS,
                seaBs / d / SEARCH_OPS,
                seaAs / d / SEARCH_OPS,
                seaAsArr / d / SEARCH_OPS);
        System.out.printf(
                "  Удаление 1000 раз — суммарное время (среднее по %d циклам), нс: BST случ. %.0f, AVL %.0f; BST сорт. %.0f, AVL %.0f%n",
                d, delBr / d, delAr / d, delBs / d, delAs / d);
        System.out.printf(
                "  Удаление — среднее за одну операцию, нс: BST случ. %.2f, AVL %.2f; BST сорт. %.2f, AVL %.2f%n",
                delBr / d / DELETE_OPS,
                delAr / d / DELETE_OPS,
                delBs / d / DELETE_OPS,
                delAs / d / DELETE_OPS);
    }

    /** Делит каждый элемент массива сумм на число циклов — среднее суммарное время за цикл. */
    private static double[] avg(double[] sums, int cycles) {
        double[] out = new double[sums.length];
        for (int i = 0; i < sums.length; i++) {
            out[i] = sums[i] / cycles; // например средняя полная вставка за один цикл
        }
        return out;
    }

    /** Среднее время одной операции поиска или удаления в наносекундах (после усреднения по циклам). */
    private static double[] avgPerOp(double[] sums, int cycles, int ops) {
        double[] out = new double[sums.length];
        for (int i = 0; i < sums.length; i++) {
            out[i] = sums[i] / cycles / ops; // сначала среднее за цикл, потом /1000 операций
        }
        return out;
    }

    /** Несколько полных проходов на малом N без записи в отчёт — стабилизация JIT. */
    private static void warmUp(
            Random rng,
            BinarySearchTree bst,
            AvlTree avl,
            int[] searchKeys,
            int[] deleteKeys,
            int[] keyPool) {
        int n = 1 << 11; // 2048 — достаточно для прогрева, не слишком долго
        int[] data = new int[n];
        fillRandomPermutation(data, rng);
        for (int r = 0; r < 5; r++) { // пять повторов полного сценария
            bst.clear();
            for (int v : data) {
                bst.insert(v);
            }
            avl.clear();
            for (int v : data) {
                avl.insert(v);
            }
            for (int k = 0; k < SEARCH_OPS; k++) {
                searchKeys[k] = rng.nextInt(n);
            }
            for (int k : searchKeys) {
                bst.contains(k);
            }
            for (int k : searchKeys) {
                avl.contains(k);
            }
            for (int k : searchKeys) {
                linearSearch(data, k);
            }
            pickUniqueFromRange(keyPool, rng, n, deleteKeys, DELETE_OPS);
            for (int k : deleteKeys) {
                bst.delete(k);
            }
            for (int k : deleteKeys) {
                avl.delete(k);
            }
        }
    }

    /** Заполняет массив значениями 0, 1, …, length−1 по возрастанию. */
    private static void fillAscending(int[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = i; // отсортированный порядок вставки по условию
        }
    }

    /** Равномерная случайная перестановка: алгоритм Фишера–Йетса. */
    private static void fillRandomPermutation(int[] data, Random rng) {
        for (int i = 0; i < data.length; i++) {
            data[i] = i; // сначала тождественная перестановка
        }
        for (int i = data.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1); // случайный индекс от 0 до i включительно
            int t = data[i];
            data[i] = data[j]; // обмен элементов
            data[j] = t;
        }
    }

    /** Линейный поиск: сравнивает каждый элемент массива с key до первого совпадения. */
    private static boolean linearSearch(int[] arr, int key) {
        for (int v : arr) {
            if (v == key) {
                return true; // найдено
            }
        }
        return false; // прошли весь массив — нет
    }

    /**
     * Выбирает k различных чисел из диапазона 0…n−1: частичная перестановка начала массива pool.
     */
    private static void pickUniqueFromRange(int[] pool, Random rng, int n, int[] out, int k) {
        for (int i = 0; i < n; i++) {
            pool[i] = i; // заполняем pool значениями 0…n−1
        }
        for (int i = 0; i < k; i++) {
            int j = i + rng.nextInt(n - i); // случайный индекс от i до n−1
            int t = pool[i];
            pool[i] = pool[j]; // меняем i-й с j-м — стандартный выбор без повторений
            pool[j] = t;
        }
        System.arraycopy(pool, 0, out, 0, k); // первые k позиций — выбранные ключи удаления
    }

    /** Краткие выводы по смыслу эксперимента (дублируют идеи отчёта). */
    private static void printConclusion() {
        System.out.println();
        System.out.println("Краткий вывод:");
        System.out.println("При случайном порядке вставки BST и AVL дают сопоставимое время вставки и поиска (оба O(log N) в среднем).");
        System.out.println("При вставке уже отсортированных ключей BST вырождается в список — вставка и операции заметно медленнее, чем у сбалансированного AVL.");
        System.out.println("Линейный поиск в массиве по условию замеров медленнее поиска в BST/AVL при больших N.");
    }
}
