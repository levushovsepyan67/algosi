package usaskoviy.path;

import usaskoviy.graph.Graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * Поиск кратчайшего пути в невзвешенном графе методом обхода в ширину (BFS).
 *
 * Идея: из старта сначала посещаем все вершины на расстоянии одно ребро, затем два, и так далее.
 * Первая достигнутая цель {@code end} лежит на минимальном числе рёбер от {@code start}.
 *
 * Сложность: O(|V| + |E|) при представлении списком смежности.
 */
public final class BfsShortestPath {

    private BfsShortestPath() {
    }

    /**
     * @param graph граф
     * @param start индекс вершины A
     * @param end   индекс вершины B
     * @return путь как список вершин или отсутствие пути ({@link PathResult#none()})
     */
    public static PathResult find(Graph graph, int start, int end) {
        int n = graph.getVertexCount();
        if (start < 0 || end < 0 || start >= n || end >= n) {
            throw new IllegalArgumentException("Некорректные вершины");
        }
        // Тривиальный случай: путь нулевой длины
        if (start == end) {
            return PathResult.of(List.of(start));
        }

        List<List<Integer>> adj = graph.adjacencyView();
        // parent[v] = вершина, из которой мы впервые попали в v; -1 = «ещё не известно»
        int[] parent = new int[n];
        java.util.Arrays.fill(parent, -1);
        boolean[] seen = new boolean[n];
        // Очередь FIFO: кто раньше встал в очередь, тот раньше обрабатывается — это и есть «ширина»
        Queue<Integer> q = new ArrayDeque<>();
        seen[start] = true;
        q.add(start);

        while (!q.isEmpty()) {
            int u = q.poll();
            // Достигли цели — дальше раскручивать соседей необязательно (все ближайшие слои уже обработаны порядком BFS)
            if (u == end) {
                break;
            }
            for (int v : adj.get(u)) {
                if (!seen[v]) {
                    seen[v] = true;
                    parent[v] = u;
                    q.add(v);
                }
            }
        }

        // Цель ни разу не помечена — в той же компоненте достижимости нет
        if (!seen[end]) {
            return PathResult.none();
        }

        // Восстановление пути от end к start по ссылкам parent, затем разворот
        List<Integer> path = new ArrayList<>();
        for (int at = end; at != -1; at = parent[at]) {
            path.add(at);
        }
        // Защита: если start изолирован и не совпал с end, цепочка не дойдёт до start (на практике уже отсечено seen[end])
        if (path.get(path.size() - 1) != start) {
            return PathResult.none();
        }
        Collections.reverse(path);
        return PathResult.of(path);
    }
}
