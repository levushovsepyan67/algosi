package usaskoviy;

import usaskoviy.algo.DijkstraAllPairs;
import usaskoviy.analysis.Lab5TimingChart;
import usaskoviy.graph.ConnectedWeightedGraphGenerator;
import usaskoviy.graph.WeightedGraph;

import java.io.File;
import java.util.Locale;
import java.util.Random;

/**
 * Лабораторная работа №5, задание 3 - вариант 1:
 * для всех пар вершин найти длины кратчайших путей алгоритмом Дейкстры (N запусков из каждой вершины).
 */

public final class Main {
    // Четыре размера графа строго по формулировке задания.
    private static final int[] N_VALUES = {10, 20, 50, 100};
    // На каждое N выполняется от 5 до 10 тестов; выбрано 7.
    private static final int TRIALS_PER_N = 7;

    public static void main(String[] args) throws Exception {
        // Единый числовой формат вывода на разных ОС.
        Locale.setDefault(Locale.ROOT);
        // Фиксированное зерно - воспроизводимые графы и замеры для отчёта.
        Random rng = new Random(20250405L);

        // Прогрев JVM перед серьёзными замерами (JIT и т.д.).
        warmUp(rng);

        // Сюда пишем среднее время в наносекундах для каждого из четырёх N.
        double[] avgNanos = new double[N_VALUES.length];

        // Внешний цикл: четыре разных размера графа.
        for (int ni = 0; ni < N_VALUES.length; ni++) {
            int n = N_VALUES[ni];
            // Минимальная степень для этого N из таблицы условия.
            int minDeg = ConnectedWeightedGraphGenerator.requiredMinDegree(n);
            System.out.println("=== N = " + n + ", минимальная степень вершины = " + minDeg + " ===\n");

            // Накопление суммы времён для последующего среднего.
            long sum = 0;

            // Внутренний цикл: несколько независимых случайных графов одного размера.
            for (int t = 0; t < TRIALS_PER_N; t++) {
                // Генерация связного взвешенного графа с весами 1..20 и ограничением степеней.
                WeightedGraph g = ConnectedWeightedGraphGenerator.generate(n, rng);

                // По условию граф выводят матрицей смежности; для краткости - первая проба каждого N.
                if (t == 0) {
                    System.out.println("Пример матрицы смежности (проба 1, веса 1…20, 0 / пусто - нет ребра):");
                    printWeightedAdjacency(g.adjacencyMatrix());
                    System.out.println();
                }

                // Замер только фазы "все пары Дейкстрой" (без генерации и печати).
                long t0 = System.nanoTime();
                int[][] dist = DijkstraAllPairs.shortestPathLengths(g);
                long elapsed = System.nanoTime() - t0;
                sum += elapsed;

                // Контроль: связный неориентированный граф - конечные симметричные расстояния.
                verifyAllPairsUndirected(g, dist);

                // Матрица кратчайших длин для отчёта: полностью для маленьких N, фрагмент для больших.
                if (t == 0) {
                    System.out.println("Матрица длин кратчайших путей между всеми парами:");
                    if (n <= 20) {
                        printDistanceMatrix(dist);
                    } else {
                        printDistanceMatrixFragment(dist, 8);
                        System.out.println("(показан фрагмент " + 8 + "×" + 8 + "; полная матрица " + n + "×" + n + " вычислена)\n");
                    }
                }

                System.out.printf("  Проба %d/%d: время всех пар (Дейкстра) = %d нс%n", t + 1, TRIALS_PER_N, elapsed);
            }

            // Среднее время по серии проб для точки на графике с абсциссой N.
            avgNanos[ni] = (double) sum / TRIALS_PER_N;
            System.out.printf("%nСреднее время для N=%d: %.0f нс (%.3f мс)%n%n", n, avgNanos[ni], avgNanos[ni] / 1_000_000.0);
        }

        // Файл PNG в рабочей директории процесса (в IDEA обычно корень модуля).
        File chartFile = new File("lab5_time_vs_N.png");
        Lab5TimingChart.savePng(N_VALUES, avgNanos, chartFile);
        System.out.println("График сохранён: " + chartFile.getAbsolutePath());

        printConclusion();
    }

    /** Несколько прогонов на маленьком графе до основных замеров. */
    private static void warmUp(Random rng) {
        for (int i = 0; i < 30; i++) {
            WeightedGraph g = ConnectedWeightedGraphGenerator.generate(10, rng);
            DijkstraAllPairs.shortestPathLengths(g);
        }
    }

    /**
     * Проверки корректности результата Дейкстры на неориентированном связном графе.
     */
    private static void verifyAllPairsUndirected(WeightedGraph g, int[][] dist) {
        int n = g.getVertexCount();
        for (int i = 0; i < n; i++) {
            // Расстояние от вершины до самой себя должно быть нулём.
            if (dist[i][i] != 0) {
                throw new IllegalStateException("dist[" + i + "][" + i + "] должно быть 0");
            }
            for (int j = i + 1; j < n; j++) {
                // В неориентированном графе dist[i][j] == dist[j][i].
                if (dist[i][j] != dist[j][i]) {
                    throw new IllegalStateException("Асимметрия dist для неориентированного графа");
                }
                // Связность: путь между любыми двумя вершинами существует.
                if (dist[i][j] == Integer.MAX_VALUE) {
                    throw new IllegalStateException("Граф должен быть связным: нет пути " + i + "—" + j);
                }
            }
        }
    }

    /**
     * Печать взвешенной матрицы смежности: заголовки 1..n, на диагонали "-", нет ребра - пусто.
     */
    private static void printWeightedAdjacency(int[][] w) {
        int n = w.length;
        System.out.print("     ");
        for (int j = 1; j <= n; j++) {
            System.out.printf("%4d", j);
        }
        System.out.println();
        for (int i = 0; i < n; i++) {
            System.out.printf("%4d ", i + 1);
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    System.out.print("   -");
                } else if (w[i][j] == 0) {
                    System.out.print("    ");
                } else {
                    System.out.printf("%4d", w[i][j]);
                }
            }
            System.out.println();
        }
    }

    /** Полная матрица кратчайших расстояний (удобно для n <= 20). */
    private static void printDistanceMatrix(int[][] d) {
        int n = d.length;
        System.out.print("     ");
        for (int j = 1; j <= n; j++) {
            System.out.printf("%6d", j);
        }
        System.out.println();
        for (int i = 0; i < n; i++) {
            System.out.printf("%4d ", i + 1);
            for (int j = 0; j < n; j++) {
                System.out.printf("%6d", d[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Верхний левый фрагмент k×k матрицы расстояний (чтобы не засорять консоль при N=50, 100).
     */
    private static void printDistanceMatrixFragment(int[][] d, int k) {
        int size = Math.min(k, d.length);
        System.out.print("     ");
        for (int j = 1; j <= size; j++) {
            System.out.printf("%6d", j);
        }
        System.out.println();
        for (int i = 0; i < size; i++) {
            System.out.printf("%4d ", i + 1);
            for (int j = 0; j < size; j++) {
                System.out.printf("%6d", d[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    /** Текст для отчёта: как читать график и почему время растёт с N. */
    private static void printConclusion() {
        System.out.println();
        System.out.println("Краткий вывод:");
        System.out.println("С ростом N время поиска кратчайших путей для всех пар растёт быстрее линейно:");
        System.out.println("выполняется N запусков Дейкстры, каждый на графе с всё большим числом вершин и рёбер.");
        System.out.println("На графике lab5_time_vs_N.png по оси X — N, по оси Y — среднее время серии из "
                + TRIALS_PER_N + " прогонов (наносекунды переведены в микросекунды для наглядности).");
    }
}

