package usaskoviy.graph;

import java.util.Arrays;

/**
 * Неориентированный взвешенный простой граф.
 * Симметричная матрица весов: положительное значение в (i, j) - ребро с этим весом, 0 - ребра нет.
 * Диагональ всегда 0 (петель нет).
 */
public final class WeightedGraph {

    // Внутреннее хранение: weights[i][j] = weights[j][i], на диагонали нули.
    private final int[][] weights;

    /**
     * Создаёт граф из готовой матрицы; проверяется квадратность, симметрия и нулевая диагональ.
     */
    public WeightedGraph(int[][] weights) {
        int n = weights.length;
        // Каждая строка должна иметь длину n - иначе это не матрица смежности n x n.
        for (int[] row : weights) {
            if (row.length != n) {
                throw new IllegalArgumentException("Матрица должна быть квадратной");
            }
        }
        // Копируем вход, чтобы снаружи нельзя было испортить внутреннее состояние.
        this.weights = deepCopy(weights);
        // Проверка инвариантов неориентированного графа без петель.
        for (int i = 0; i < n; i++) {
            if (this.weights[i][i] != 0) {
                throw new IllegalArgumentException("Диагональ должна быть нулевой");
            }
            // Достаточно проверить пару (i,j) при j>i - симметрия.
            for (int j = i + 1; j < n; j++) {
                if (this.weights[i][j] != this.weights[j][i]) {
                    throw new IllegalArgumentException("Матрица должна быть симметричной");
                }
            }
        }
    }

    /** Глубокая копия матрицы, чтобы изменения внешнего массива не затронули граф. */
    private static int[][] deepCopy(int[][] a) {
        int n = a.length;
        int[][] c = new int[n][n];
        for (int i = 0; i < n; i++) {
            c[i] = Arrays.copyOf(a[i], n);
        }
        return c;
    }

    /** Число вершин n (индексы 0..n-1). */
    public int getVertexCount() {
        return weights.length;
    }

    /** Копия матрицы смежности с весами: 0 означает отсутствие ребра. */
    public int[][] adjacencyMatrix() {
        return deepCopy(weights);
    }

    /** Степень вершины v: сколько рёбер (положительных весов в строке v, кроме диагонали). */
    public int degree(int v) {
        int count = 0;
        for (int j = 0; j < weights.length; j++) {
            if (v != j && weights[v][j] > 0) {
                count++;
            }
        }
        return count;
    }
}
