package usaskoviy.path;

import java.util.List;
import java.util.Optional;

/**
 * Унифицированный ответ алгоритма поиска пути между двумя вершинами.
 *
 * Поле {@link #vertices()}: если путь найден — список вершин по порядку от старта до цели;
 * если нет — {@link Optional#empty()}.
 * Поле {@link #edgeLength()}: число рёбер на пути; если пути нет, в коде принято значение {@code -1}.
 *
 * Для пути из k вершин длина в рёбрах равна {@code k - 1}; одна вершина без рёбер даёт длину 0.
 */
public record PathResult(Optional<List<Integer>> vertices, int edgeLength) {

    /** Нет пути: вершин нет, длина -1 (договорённость для таблицы и проверок). */
    public static PathResult none() {
        return new PathResult(Optional.empty(), -1);
    }

    /**
     * Упаковка найденного пути: вычисляем длину в рёбрах как {@code размер - 1}.
     * Пустой или {@code null} список трактуется как отсутствие пути.
     * Один элемент — путь длины 0 (старт совпал с целью): длина 0 рёбер.
     */
    public static PathResult of(List<Integer> pathVertices) {
        if (pathVertices == null || pathVertices.size() < 2) {
            return pathVertices == null || pathVertices.isEmpty() ? none() : new PathResult(Optional.of(pathVertices), 0);
        }
        return new PathResult(Optional.of(pathVertices), pathVertices.size() - 1);
    }
}
