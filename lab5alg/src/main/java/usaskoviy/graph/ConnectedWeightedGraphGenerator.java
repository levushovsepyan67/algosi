package usaskoviy.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Генератор связного неориентированного взвешенного графа по условиям ЛР5.
 * Веса рёбер - целые от 1 до 20 включительно.
 */
public final class ConnectedWeightedGraphGenerator {

    // Нижняя граница веса ребра по условию лабораторной.
    private static final int MIN_WEIGHT = 1;
    // Верхняя граница веса ребра по условию лабораторной.
    private static final int MAX_WEIGHT = 20;

    // Утилитный класс: только статические методы, экземпляр не создаём.
    private ConnectedWeightedGraphGenerator() {
    }

    /**
     * Возвращает минимальную степень каждой вершины для заданного N (таблица из условия ЛР5).
     */
    public static int requiredMinDegree(int n) {
        // Для каждого допустимого N своя константа; иначе ошибка - генератор только под эти размеры.
        return switch (n) {
            case 10 -> 3;   // при 10 вершинах не меньше 3 соседей у каждой
            case 20 -> 4;   // при 20 вершинах не меньше 4
            case 50 -> 10;  // при 50 - не меньше 10
            case 100 -> 20; // при 100 - не меньше 20
            default -> throw new IllegalArgumentException("Допустимы только N из {10, 20, 50, 100}: " + n);
        };
    }

    /**
     * Собирает случайный связный граф на n вершинах с ограничениями по минимальной степени.
     */
    public static WeightedGraph generate(int n, Random random) {
        // Минимальная степень по таблице из задания.
        int minDeg = requiredMinDegree(n);
        // В простом графе степень вершины не больше n-1; иначе требование невыполнимо.
        if (minDeg > n - 1) {
            throw new IllegalArgumentException("Невозможно иметь степень >= " + minDeg + " при N=" + n);
        }
        // Несколько попыток: tryBuild иногда не успевает за шаги (редко); тогда новая попытка.
        for (int attempt = 0; attempt < 500; attempt++) {
            int[][] m = tryBuild(n, minDeg, random);
            // Принимаем только связный граф и с выполненными нижними границами степеней.
            if (m != null && isConnected(m) && minDegreeOk(m, minDeg)) {
                return new WeightedGraph(m);
            }
        }
        throw new IllegalStateException("Не удалось сгенерировать граф за отведённые попытки: N=" + n);
    }

    /**
     * Один проход построения: дерево (связность) + случайные рёбра до выполнения minDeg.
     */
    private static int[][] tryBuild(int n, int minDeg, Random rnd) {
        // Матрица весов: 0 - нет ребра, иначе вес 1..20.
        int[][] m = new int[n][n];
        // Шаг 1: случайное остовное дерево - гарантирует связность всех вершин.
        spanningTree(m, n, rnd);
        // Текущие степени вершин (число инцидентных рёбер с положительным весом).
        int[] deg = degrees(m, n);
        // Ограничитель числа итераций добавления рёбер, чтобы не зациклиться.
        int maxSteps = n * n * 80;
        for (int step = 0; step < maxSteps; step++) {
            // Ищем вершину v, у которой степень ещё меньше требуемой.
            int v = -1;
            for (int i = 0; i < n; i++) {
                if (deg[i] < minDeg) {
                    v = i;
                    break; // достаточно одной "недобранной" вершины за шаг
                }
            }
            // Все вершины уже удовлетворяют minDeg - выходим из цикла доращивания.
            if (v < 0) {
                break;
            }
            // Кандидаты u: ещё нет ребра между v и u.
            List<Integer> cand = new ArrayList<>();
            for (int u = 0; u < n; u++) {
                if (u != v && m[v][u] == 0) {
                    cand.add(u);
                }
            }
            // Нет свободной вершины для ребра - конфигурация неисправима в этом проходе.
            if (cand.isEmpty()) {
                return null;
            }
            // Случайно выбираем второй конец ребра и случайный вес из [1, 20].
            int u = cand.get(rnd.nextInt(cand.size()));
            int w = MIN_WEIGHT + rnd.nextInt(MAX_WEIGHT - MIN_WEIGHT + 1);
            // Неориентированное ребро: симметрично записываем вес.
            m[v][u] = w;
            m[u][v] = w;
            deg[v]++;
            deg[u]++;
        }
        return m;
    }

    /**
     * Строит случайное дерево на вершинах 0..n-1: ровно n-1 ребро, все вершины достижимы.
     */
    private static void spanningTree(int[][] m, int n, Random rnd) {
        // Порядок подключения вершин 1..n-1 к дереву перемешиваем - больше разнообразия топологии.
        List<Integer> order = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            order.add(i);
        }
        Collections.shuffle(order, rnd);
        // Уже в дереве: стартуем с вершины 0.
        List<Integer> inTree = new ArrayList<>();
        inTree.add(0);
        // Каждую новую вершину цепляем к случайной уже включённой - получается дерево.
        for (int next : order) {
            int parent = inTree.get(rnd.nextInt(inTree.size()));
            int w = MIN_WEIGHT + rnd.nextInt(MAX_WEIGHT - MIN_WEIGHT + 1);
            m[parent][next] = w;
            m[next][parent] = w;
            inTree.add(next);
        }
    }

    /** Считает степени всех вершин по матрице весов. */
    private static int[] degrees(int[][] m, int n) {
        int[] d = new int[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Петель нет: i!=j и положительный вес означает ребро.
                if (i != j && m[i][j] > 0) {
                    d[i]++;
                }
            }
        }
        return d;
    }

    /** Проверка: у каждой вершины степень не меньше minDeg. */
    private static boolean minDegreeOk(int[][] m, int minDeg) {
        int n = m.length;
        for (int i = 0; i < n; i++) {
            int d = 0;
            for (int j = 0; j < n; j++) {
                if (i != j && m[i][j] > 0) {
                    d++;
                }
            }
            if (d < minDeg) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверка связности: обход в ширину из 0, все вершины должны быть достигнуты.
     */
    private static boolean isConnected(int[][] m) {
        int n = m.length;
        boolean[] seen = new boolean[n];
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        seen[0] = true;
        q.add(0);
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v = 0; v < n; v++) {
                if (u != v && m[u][v] > 0 && !seen[v]) {
                    seen[v] = true;
                    q.add(v);
                }
            }
        }
        for (boolean b : seen) {
            if (!b) {
                return false; // изолированная компонента
            }
        }
        return true;
    }
}