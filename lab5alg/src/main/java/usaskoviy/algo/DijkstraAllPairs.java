package usaskoviy.algo;

import usaskoviy.graph.WeightedGraph;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Кратчайшие пути между всеми парами вершин во взвешенном графе с неотрицательными весами.
 * Вариант 1 (ЛР5): N раз запускается алгоритм Дейкстры (из каждой вершины как из старта).
 */
public final class DijkstraAllPairs {

    private DijkstraAllPairs() {
    }

    /**
     * Строит матрицу кратчайши х расстояний: all[s][t] - сумма весов на кратчайшем пути s -> t.
     */
    public static int[][] shortestPathLengths(WeightedGraph graph) {
        // Рабочая копия матрицы весов (алгоритм её не меняет, но копия защищает от побочных эффектов).
        int[][] w = graph.adjacencyMatrix();
        int n = w.length;
        // Строка s матрицы all - результат Дейкстры со стартом в s.
        int[][] all = new int[n][n];
        for (int s = 0; s < n; s++) {
            all[s] = dijkstraFromSource(w, s);
        }
        return all;
    }

    /**
     * Классическая Дейкстра: неотрицательные веса, приоритетная очередь по текущей дистанции.
     *
     * @param w      матрица весов, 0 - нет ребра
     * @param start  индекс стартовой вершины
     * @return dist[v] - кратчайшее расстояние от start до v
     */
    public static int[] dijkstraFromSource(int[][] w, int start) {
        int n = w.length;
        // Изначально все расстояния "бесконечность"; start - 0.
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;
        // Очередь: пара (вершина, текущая лучшая дистанция), порядок - по второму полю (минимум сверху).
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.add(new int[]{start, 0});
        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int u = cur[0];
            int d = cur[1];
            // Устаревшая запись в куче (уже нашли путь короче) - пропускаем.
            if (d > dist[u]) {
                continue;
            }
            // Релаксация всех рёбер (u, v) с положительным весом.
            for (int v = 0; v < n; v++) {
                if (u == v) {
                    continue;
                }
                int ww = w[u][v];
                if (ww <= 0) {
                    continue; // ребра нет
                }
                long nd = (long) d + ww; // long, чтобы не переполнить int на сумме
                if (nd < dist[v]) {
                    dist[v] = (int) nd;
                    pq.add(new int[]{v, dist[v]});
                }
            }
        }
        return dist;
    }
}
