package usaskoviy.analysis;

/**
 * Одна строка эксперимента: один сгенерированный граф и замеры на паре вершин (A, B).
 *
 * {@code label} — короткая подпись для графика и консоли (например {@code |V|=8,|E|=15}).
 * {@code vertices}, {@code edges} — фактические размеры графа после генерации.
 * {@code from}, {@code to} — случайно выбранные концы пути (A и B).
 * {@code bfsNanos}, {@code dfsNanos} — время работы в наносекундах ({@link System#nanoTime()}).
 * {@code pathLengthBfs}, {@code pathLengthDfs} — длина кратчайшего пути в рёбрах (−1 если пути нет);
 * при корректной реализации BFS и IDDFS эти значения совпадают.
 */
public record BenchmarkRow(
        String label,
        int vertices,
        int edges,
        int from,
        int to,
        long bfsNanos,
        long dfsNanos,
        int pathLengthBfs,
        int pathLengthDfs
) {
}
