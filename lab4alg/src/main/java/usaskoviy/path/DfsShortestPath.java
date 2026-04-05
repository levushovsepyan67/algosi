package usaskoviy.path;

import usaskoviy.graph.Graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Кратчайший путь через итеративное углубление (IDDFS): серия обходов в глубину (DFS)
 * с ограничением «не длиннее L рёбер»; L перебирается от 1 до |V|−1.
 *
 * Один наивный DFS до первого попадания в цель не гарантирует минимальное число рёбер.
 * IDDFS сохраняет правильность для невзвешенного графа и остаётся в семействе методов на основе DFS.
 *
 * В худшем случае время хуже, чем у BFS из-за повторных обходов; это видно на графике в лабораторной.
 */
public final class DfsShortestPath {

    private DfsShortestPath() {
    }

    public static PathResult find(Graph graph, int start, int end) {
        int n = graph.getVertexCount();
        if (start < 0 || end < 0 || start >= n || end >= n) {
            throw new IllegalArgumentException("Некорректные вершины");
        }
        if (start == end) {
            return PathResult.of(List.of(start));
        }

        List<List<Integer>> adj = graph.adjacencyView();
        // В простом пути не более n−1 ребра (n различных вершин максимум)
        int maxEdgeLimit = Math.max(1, n - 1);

        // Пробуем сначала очень короткие пути; первый успех даёт глобально кратчайший в невзвешенном графе
        for (int limit = 1; limit <= maxEdgeLimit; limit++) {
            boolean[] onPath = new boolean[n];
            List<Integer> path = new ArrayList<>();
            if (limitedDfs(adj, start, end, limit, onPath, path)) {
                return PathResult.of(path);
            }
        }
        return PathResult.none();
    }

    /**
     * DFS с лимитом: оставшаяся «глубина» измеряется в числе рёбер, которые ещё можно пройти.
     *
     * @param depthLeft сколько рёбер максимум осталось пройти от текущей вершины к цели (включая шаги ниже по рекурсии)
     * @param onPath    вершины на текущем пути стека вызовов — запрет циклов в текущем пути
     * @param path      накапливаемый путь; при успехе содержит цепочку от start до goal
     */
    private static boolean limitedDfs(
            List<List<Integer>> adj,
            int u,
            int goal,
            int depthLeft,
            boolean[] onPath,
            List<Integer> path) {
        path.add(u);
        onPath[u] = true;
        if (u == goal) {
            return true;
        }
        // Лимит исчерпан, но цель не достигнута на этом шаге — откат
        if (depthLeft <= 0) {
            path.remove(path.size() - 1);
            onPath[u] = false;
            return false;
        }
        for (int v : adj.get(u)) {
            if (!onPath[v] && limitedDfs(adj, v, goal, depthLeft - 1, onPath, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        onPath[u] = false;
        return false;
    }
}
