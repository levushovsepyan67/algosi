package usaskoviy.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Случайная генерация простого графа с ограничениями из лабораторной работы.
 *
 * Алгоритм: случайно выбираются |V| и целевое число рёбер, затем в цикле предлагаются
 * случайные пары вершин; ребро добавляется, если его ещё нет и не нарушены ограничения степеней.
 * Если за отведённые попытки набрать нужное число рёбер не удалось, граф возвращается с меньшим |E|.
 *
 * Результат передаётся в конструктор {@link Graph}; класс графа остаётся только моделью данных.
 */
public final class RandomGraphGenerator {
    private final GeneratorConfig config;
    private final Random random;

    public RandomGraphGenerator(GeneratorConfig config, Random random) {
        this.config = config;
        this.random = random;
    }

    /** Генератор с собственным {@link Random#Random()} для недетерминированных прогонов. */
    public RandomGraphGenerator(GeneratorConfig config) {
        this(config, new Random());
    }

    /**
     * Строит один случайный граф по полям {@link GeneratorConfig}.
     * Шаг 1: случайный выбор числа вершин v в отрезке [minV, maxV].
     * Шаг 2: случайный выбор целевого числа рёбер eTarget в отрезке [minE, maxE].
     * Шаг 3: пока не набрано eTarget рёбер или не исчерпан лимит попыток — случайная пара (a,b), проверки, добавление.
     */
    public Graph generate() {
        // Равные границы — без вызова nextInt(0), иначе было бы nextInt(1) только для диапазона из одного числа
        int v = config.minVertices() == config.maxVertices()
                ? config.minVertices()
                : config.minVertices() + random.nextInt(config.maxVertices() - config.minVertices() + 1);
        int eTarget = config.minEdges() == config.maxEdges()
                ? config.minEdges()
                : config.minEdges() + random.nextInt(config.maxEdges() - config.minEdges() + 1);

        List<Edge> collected = new ArrayList<>();
        Set<String> used = new HashSet<>();
        int maxAttempts = Math.max(10_000, v * v * 4);
        int attempts = 0;

        // Счётчики степеней обновляются синхронно с добавлением рёбер
        int[] outDeg = new int[v];
        int[] inDeg = new int[v];
        int[] undDeg = new int[v];

        while (collected.size() < eTarget && attempts < maxAttempts) {
            attempts++;
            int a = random.nextInt(v);
            int b = random.nextInt(v);
            if (a == b) {
                continue;
            }

            String key;
            if (config.directed()) {
                key = a + "->" + b;
                if (used.contains(key)) {
                    continue;
                }
                // Лимиты исходящей/входящей степени для концов дуги
                if (outDeg[a] >= config.maxOutDegree() || inDeg[b] >= config.maxInDegree()) {
                    continue;
                }
                // Дополнительно: суммарная степень вершины (вход+исход) не больше maxVertexDegree
                if (outDeg[a] + inDeg[a] >= config.maxVertexDegree()
                        || outDeg[b] + inDeg[b] >= config.maxVertexDegree()) {
                    continue;
                }
                used.add(key);
                outDeg[a]++;
                inDeg[b]++;
                collected.add(new Edge(a, b));
            } else {
                // Неориентированное ребро — каноническая пара для множества и для Graph
                int u = Math.min(a, b);
                int w = Math.max(a, b);
                key = u + "-" + w;
                if (used.contains(key)) {
                    continue;
                }
                if (undDeg[u] >= config.maxVertexDegree() || undDeg[w] >= config.maxVertexDegree()) {
                    continue;
                }
                used.add(key);
                undDeg[u]++;
                undDeg[w]++;
                collected.add(new Edge(u, w));
            }
        }

        return new Graph(v, config.directed(), collected);
    }

    /**
     * Все настраиваемые параметры лабораторной в одном record.
     * Для неориентированного графа поля maxInDegree/maxOutDegree в логике не используются
     * (в {@link #undirected} подставляются «бесконечные» запасные значения).
     */
    public record GeneratorConfig(
            int minVertices,
            int maxVertices,
            int minEdges,
            int maxEdges,
            int maxVertexDegree,
            boolean directed,
            int maxInDegree,
            int maxOutDegree
    ) {
        public GeneratorConfig {
            if (minVertices < 0 || maxVertices < minVertices) {
                throw new IllegalArgumentException("Некорректный диапазон числа вершин");
            }
            if (minEdges < 0 || maxEdges < minEdges) {
                throw new IllegalArgumentException("Некорректный диапазон числа рёбер");
            }
            if (maxVertexDegree < 0) {
                throw new IllegalArgumentException("maxVertexDegree < 0");
            }
            if (directed) {
                if (maxInDegree < 0 || maxOutDegree < 0) {
                    throw new IllegalArgumentException("Степени захода/исхода не могут быть отрицательными");
                }
            }
        }

        /** Удобный конструктор конфигурации для неориентированного графа. */
        public static GeneratorConfig undirected(
                int minV, int maxV, int minE, int maxE, int maxDegree) {
            return new GeneratorConfig(minV, maxV, minE, maxE, maxDegree, false, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        /** Удобный конструктор для ориентированного графа с лимитами in/out и общей степенью. */
        public static GeneratorConfig directed(
                int minV, int maxV, int minE, int maxE, int maxVertexDegree, int maxIn, int maxOut) {
            return new GeneratorConfig(minV, maxV, minE, maxE, maxVertexDegree, true, maxIn, maxOut);
        }
    }
}
