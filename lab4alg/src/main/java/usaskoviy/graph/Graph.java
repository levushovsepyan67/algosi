package usaskoviy.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Модель графа: хранение структуры и преобразование в стандартные представления.
 *
 * Важно для задания: этот класс не генерирует случайные графы — только принимает
 * число вершин, флаг ориентации и список рёбер. Генерация вынесена в {@link RandomGraphGenerator}.
 *
 * Внутри для обходов поддерживается список смежности {@link #adjacency}; он строится из
 * нормализованного списка рёбер {@link #edges}.
 */
public final class Graph {
    /** Число вершин n; допустимые индексы 0 … n−1. */
    private final int vertexCount;
    /** true — ориентированный граф, false — рёбра симметричны в смысле достижимости. */
    private final boolean directed;
    /** Канонический упорядоченный список рёбер (без петель и дубликатов). */
    private final List<Edge> edges;
    /** Для каждой вершины — отсортированный список соседей (куда можно пройти за одно ребро). */
    private final List<List<Integer>> adjacency;

    /**
     * Собирает граф: нормализует рёбра и строит списки смежности.
     *
     * @param vertexCount число вершин n
     * @param directed    ориентированность
     * @param edges       сырой список рёбер (могут быть отфильтрованы петли и повторы)
     */
    public Graph(int vertexCount, boolean directed, List<Edge> edges) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("Число вершин не может быть отрицательным");
        }
        this.vertexCount = vertexCount;
        this.directed = directed;
        // List.copyOf — внешний список неизменяем; содержимое уже «наша» нормализация
        this.edges = List.copyOf(normalizeEdges(vertexCount, directed, edges));
        this.adjacency = buildAdjacency(vertexCount, directed, this.edges);
    }

    /**
     * Приводит рёбра к виду без петель и без повторов.
     * Неориентированное ребро всегда (min(u,v), max(u,v)) и один ключ в множестве.
     * Ориентированное — пара (from, to) и ключ "from->to".
     */
    private static List<Edge> normalizeEdges(int n, boolean directed, List<Edge> raw) {
        List<Edge> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Edge e : raw) {
            if (e.from() >= n || e.to() >= n) {
                throw new IllegalArgumentException("Ребро выходит за пределы вершин: " + e);
            }
            // Петля в простой граф не включаем
            if (e.from() == e.to()) {
                continue;
            }
            Edge canon = directed ? e : new Edge(Math.min(e.from(), e.to()), Math.max(e.from(), e.to()));
            String key = directed ? canon.from() + "->" + canon.to() : canon.from() + "-" + canon.to();
            if (seen.add(key)) {
                out.add(canon);
            }
        }
        return out;
    }

    /**
     * Заполняет списки смежности: для каждого ребра from→to добавляем to в список from;
     * для неориентированного — также from в список to. Сортировка соседей для предсказуемого порядка обхода.
     */
    private static List<List<Integer>> buildAdjacency(int n, boolean directed, List<Edge> edges) {
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        for (Edge e : edges) {
            adj.get(e.from()).add(e.to());
            if (!directed) {
                adj.get(e.to()).add(e.from());
            }
        }
        for (List<Integer> list : adj) {
            Collections.sort(list);
        }
        return adj;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public boolean isDirected() {
        return directed;
    }

    /** Список рёбер в фиксированном порядке — совпадает с порядком столбцов в {@link #incidenceMatrix()}. */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * Список смежности (копия): безопасно изменять возвращённые внутренние списки снаружи.
     * Для каждой вершины i — список вершин j, достижимых одним ребром из i.
     */
    public List<List<Integer>> adjacencyList() {
        List<List<Integer>> copy = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            copy.add(new ArrayList<>(adjacency.get(i)));
        }
        return copy;
    }

    /**
     * Тот же список смежности, но только для чтения и без копирования массивов соседей —
     * удобно для BFS/DFS на больших графах (меньше аллокаций).
     */
    public List<List<Integer>> adjacencyView() {
        List<List<Integer>> view = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            view.add(Collections.unmodifiableList(adjacency.get(i)));
        }
        return Collections.unmodifiableList(view);
    }

    /**
     * Матрица смежности размера n×n: m[i][j]=1, если есть ребро i→j, иначе 0.
     * Неориентированный граф даёт симметричную матрицу (если есть ребро между i и j).
     */
    public int[][] adjacencyMatrix() {
        int[][] m = new int[vertexCount][vertexCount];
        for (Edge e : edges) {
            if (e.from() == e.to()) {
                continue;
            }
            m[e.from()][e.to()] = 1;
            if (!directed) {
                m[e.to()][e.from()] = 1;
            }
        }
        return m;
    }

    /**
     * Матрица инцидентности: строки — вершины, столбцы — рёбра в порядке {@link #getEdges()}.
     * Неориентированный граф: в столбце ребра между u и v стоят единицы в строках u и v.
     * Ориентированный граф: в столбце дуги +1 в строке начала, −1 в строке конца.
     */
    public int[][] incidenceMatrix() {
        int m = edges.size();
        int[][] mat = new int[vertexCount][m];
        for (int j = 0; j < m; j++) {
            Edge e = edges.get(j);
            if (e.from() == e.to()) {
                continue;
            }
            if (directed) {
                mat[e.from()][j] = 1;
                mat[e.to()][j] = -1;
            } else {
                mat[e.from()][j] = 1;
                mat[e.to()][j] = 1;
            }
        }
        return mat;
    }

    /**
     * Список рёбер: каждое ребро как int[2] = {from, to} в том же порядке, что и {@link #getEdges()}.
     */
    public List<int[]> edgeList() {
        List<int[]> list = new ArrayList<>(edges.size());
        for (Edge e : edges) {
            if (e.from() == e.to()) {
                continue;
            }
            list.add(new int[]{e.from(), e.to()});
        }
        return list;
    }
}
