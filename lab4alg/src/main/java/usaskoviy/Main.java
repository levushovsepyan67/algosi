package usaskoviy;

// Классы отчёта и графика (одна строка эксперимента, сохранение PNG).
import usaskoviy.analysis.BenchmarkRow;
import usaskoviy.analysis.TimingChart;
// Модель графа и случайный генератор по параметрам из лабораторной.
import usaskoviy.graph.Graph;
import usaskoviy.graph.RandomGraphGenerator;
import usaskoviy.graph.RandomGraphGenerator.GeneratorConfig;
// Поиск кратчайшего пути: обход в ширину и итеративное углубление (семейство DFS).
import usaskoviy.path.BfsShortestPath;
import usaskoviy.path.DfsShortestPath;
import usaskoviy.path.PathResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Точка входа лабораторной работы №4.
 * Содержимое программы по шагам:
 * 1) демонстрация четырёх представлений графа на малых примерах (неориентированный и ориентированный);
 * 2) десять прогонов со случайными графами, растущими параметрами, случайными вершинами A и B, замер BFS и DFS (IDDFS);
 * 3) таблица в консоли, файл графика benchmark_chart.png, текстовый анализ.
 * Развёрнутая теория: файл LAB4_Теория_и_защита.md в корне проекта.
 */
public final class Main {

    /**
     * Запуск всей лабораторной подряд: демо, бенчмарк, график, анализ.
     * args — аргументы командной строки (здесь не используются).
     */
    public static void main(String[] args) throws Exception {
        // Локаль ROOT: десятичная точка в printf и единый вид вывода на любой ОС.
        Locale.setDefault(Locale.ROOT);

        // Показать матрицы и списки на маленьких графах (требование ЛР — уметь выводить представления).
        demoRepresentations();

        // Построить 10 графов, замерить алгоритмы, собрать строки таблицы.
        List<BenchmarkRow> rows = runTenGraphBenchmark();
        // Напечатать таблицу времён и длин путей в консоль.
        printTable(rows);

        // Имя файла; путь относительный — файл появится в рабочей директории процесса (в IDEA часто корень модуля).
        File chartFile = new File("benchmark_chart.png");
        // JFreeChart строит столбчатую диаграмму по rows и пишет PNG на диск.
        TimingChart.savePng(rows, chartFile);
        // Пустая строка для читаемости после таблицы.
        System.out.println();
        // Абсолютный путь удобно скопировать в отчёт.
        System.out.println("График сохранён: " + chartFile.getAbsolutePath());
        // Итоговый текст сравнения BFS и DFS (IDDFS) и суммарные наносекунды.
        printAnalysis(rows);
    }

    /**
     * Выводит в консоль все обязательные представления графа на наглядных примерах.
     * Сначала треугольник K3 без ориентации, затем маленький орграф из трёх дуг.
     */
    private static void demoRepresentations() {
        // Заголовок блока демонстрации.
        System.out.println("=== Демонстрация представлений графа (малый неориентированный) ===\n");
        // Три ребра: 0—1, 1—2, 2—0 (цикл из трёх вершин).
        List<usaskoviy.graph.Edge> edges = List.of(
                new usaskoviy.graph.Edge(0, 1),
                new usaskoviy.graph.Edge(1, 2),
                new usaskoviy.graph.Edge(2, 0)
        );
        // 3 вершины с индексами 0,1,2; false — рёбра двусторонние.
        Graph g = new Graph(3, false, edges);

        // Для каждой вершины печатаем список соседей (список смежности).
        System.out.println("Список смежности:");
        // i пробегает все вершины 0..n-1.
        for (int i = 0; i < g.getVertexCount(); i++) {
            // adjacencyList() возвращает копию; get(i) — соседи вершины i.
            System.out.println("  " + i + " -> " + g.adjacencyList().get(i));
        }

        // Пустая строка перед следующей матрицей.
        System.out.println("\nМатрица смежности:");
        // n×n, единица означает ребро из строки в столбец (для неориентированного симметрично).
        printMatrix(g.adjacencyMatrix());

        // Столбцы матрицы инцидентности соответствуют рёбрам в порядке getEdges().
        System.out.println("Матрица инцидентности (столбцы — рёбра в порядке списка рёбер):");
        printMatrix(g.incidenceMatrix());

        // Явный перечень пар (from, to) как в задании «список рёбер».
        System.out.println("Список рёбер:");
        // Каждое ребро — массив из двух int.
        for (int[] e : g.edgeList()) {
            System.out.println("  (" + e[0] + ", " + e[1] + ")");
        }
        // Напоминание, как задать ориентированный генератор (сам генератор здесь не вызывается).
        System.out.println("Параметры ориентированного генератора задаются через "
                + "GeneratorConfig.directed(minV,maxV,minE,maxE,maxDegTotal,maxIn,maxOut).");

        // Второй пример — дуги 0→1, 1→2, 0→2.
        System.out.println("\n--- Малый ориентированный граф (те же представления) ---");
        Graph dg = new Graph(3, true, List.of(
                new usaskoviy.graph.Edge(0, 1),
                new usaskoviy.graph.Edge(1, 2),
                new usaskoviy.graph.Edge(0, 2)
        ));
        System.out.println("Матрица смежности (ориентированная):");
        printMatrix(dg.adjacencyMatrix());
        // В инцидентности: +1 у хвоста дуги, −1 у головы.
        System.out.println("Матрица инцидентности (+1 / −1):");
        printMatrix(dg.incidenceMatrix());
        System.out.println();
    }

    /**
     * Печатает матрицу целых чисел: одна строка массива — одна строка консоли.
     */
    private static void printMatrix(int[][] m) {
        // Для каждой строки матрицы...
        for (int[] row : m) {
            // Arrays.toString даёт вид [a, b, c] без ручного цикла.
            System.out.println("  " + Arrays.toString(row));
        }
    }

    /**
     * Десять итераций эксперимента из условия ЛР: растущий размер, случайные A и B, два замера времени.
     */
    private static List<BenchmarkRow> runTenGraphBenchmark() {
        // Одно и то же зерно: повторный запуск даёт те же графы и те же пары (A,B) — удобно для отчёта.
        Random rng = new Random(20250322L);
        // Сюда накапливаем строки для таблицы и графика.
        List<BenchmarkRow> rows = new ArrayList<>();

        // Прогрев виртуальной машины до замеров (JIT и т.п.).
        warmUp();

        // Ровно 10 графов, номер итерации i = 0..9.
        for (int i = 0; i < 10; i++) {
            // Нижняя граница числа вершин для этого прогона (растёт с i).
            int vMin = 6 + i * 2;
            // Верхняя граница чуть больше — генератор выберет случайное v между vMin и vMax.
            int vMax = vMin + 2;
            // Максимум рёбер в простом неориентированном графе на vMax вершинах: vMax*(vMax-1)/2.
            int maxEdgesForV = vMax * (vMax - 1) / 2;
            // Нижняя граница желаемого числа рёбер (не больше, чем возможно в полном графе).
            int eLo = Math.min(maxEdgesForV, 3 + i * 4 + vMin);
            // Верхняя граница — шире коридор, генератор выберет случайное число в [eLo, eHi].
            int eHi = Math.min(maxEdgesForV, eLo + 4 + i * 2);
            // На всякий случай: если формула дала eHi < eLo, выравниваем.
            if (eHi < eLo) {
                eHi = eLo;
            }
            // Степень вершины не больше min(vMax-1, 3+i) — ограничение из сценария лабораторной.
            int maxDeg = Math.min(vMax - 1, 3 + i);

            // Конфигурация: неориентированный граф, диапазоны |V| и |E|, макс. степень.
            GeneratorConfig cfg = GeneratorConfig.undirected(
                    vMin, vMax,
                    eLo, eHi,
                    maxDeg
            );
            // Генератор использует наш rng, чтобы последовательность была воспроизводимой.
            RandomGraphGenerator gen = new RandomGraphGenerator(cfg, rng);
            // Фактический граф: случайное v, случайное число рёбер с учётом ограничений.
            Graph graph = gen.generate();

            // Число вершин у уже построенного графа (может быть vMin..vMax).
            int n = graph.getVertexCount();
            // Случайная вершина A.
            int a = rng.nextInt(n);
            // Случайная вершина B; цикл гарантирует B ≠ A.
            int b = rng.nextInt(n);
            while (b == a) {
                b = rng.nextInt(n);
            }

            // Замер BFS: время до и после вызова.
            long t0 = System.nanoTime();
            PathResult rb = BfsShortestPath.find(graph, a, b);
            long bfsNs = System.nanoTime() - t0;

            // Замер второго алгоритма (IDDFS) тем же способом.
            t0 = System.nanoTime();
            PathResult rd = DfsShortestPath.find(graph, a, b);
            long dfsNs = System.nanoTime() - t0;

            // Длина пути в рёбрах: число вершин минус 1; если пути нет — Optional пустой, orElse(-1).
            int lb = rb.vertices().map(p -> p.size() - 1).orElse(-1);
            int ld = rd.vertices().map(p -> p.size() - 1).orElse(-1);
            // Обе реализации должны дать одинаковую длину кратчайшего пути; иначе ошибка в коде.
            if (lb != ld) {
                throw new IllegalStateException("Несовпадение длины пути BFS/DFS(IDDFS): " + lb + " vs " + ld);
            }

            // Подпись столбца на диаграмме и в таблице.
            String label = "|V|=" + graph.getVertexCount() + ",|E|=" + graph.getEdges().size();
            // Одна запись: размеры, концы пути, наносекунды, длины (дублируются для контроля).
            rows.add(new BenchmarkRow(
                    label,
                    graph.getVertexCount(),
                    graph.getEdges().size(),
                    a,
                    b,
                    bfsNs,
                    dfsNs,
                    lb,
                    ld
            ));
        }
        // Все 10 строк готовы для печати и графика.
        return rows;
    }

    /**
     * Много раз вызывает оба поиска пути на цепочке из пяти вершин — «разогрев» перед честными замерами.
     */
    private static void warmUp() {
        // Линейный путь 0—1—2—3—4 из четырёх рёбер.
        Graph tiny = new Graph(5, false, List.of(
                new usaskoviy.graph.Edge(0, 1),
                new usaskoviy.graph.Edge(1, 2),
                new usaskoviy.graph.Edge(2, 3),
                new usaskoviy.graph.Edge(3, 4)
        ));
        // 500 проходов — достаточно, чтобы JVM скомпилировала горячие методы.
        for (int k = 0; k < 500; k++) {
            BfsShortestPath.find(tiny, 0, 4);
            DfsShortestPath.find(tiny, 0, 4);
        }
    }

    /**
     * Форматированный вывод таблицы: по одной строке на каждый из 10 графов.
     */
    private static void printTable(List<BenchmarkRow> rows) {
        System.out.println("=== Замеры на 10 графах (растущие |V|, |E|, диапазоны min–max); A и B — случайные вершины ===\n");
        // Заголовок: выравнивание по ширине полей для ровных столбцов.
        System.out.printf("%-28s %6s %6s %8s %12s %12s %s%n",
                "Граф", "A", "B", "Длина", "BFS нс", "DFS нс", "Путь есть?");
        // Разделитель под заголовком.
        System.out.println("-".repeat(92));
        // По всем накопленным замерам.
        for (BenchmarkRow r : rows) {
            // Путь есть, если длина не -1 (т.е. граф достижимости соединяет A и B).
            boolean ok = r.pathLengthBfs() >= 0;
            System.out.printf("%-28s %6d %6d %8d %12d %12d %s%n",
                    r.label(),
                    r.from(),
                    r.to(),
                    r.pathLengthBfs(),
                    r.bfsNanos(),
                    r.dfsNanos(),
                    ok ? "да" : "нет");
        }
    }

    /**
     * Печатает готовый текст анализа для отчёта и суммарное отношение времён DFS к BFS.
     */
    private static void printAnalysis(List<BenchmarkRow> rows) {
        System.out.println();
        System.out.println("=== Краткий анализ ===");
        // Многострочная строка: объяснение BFS, IDDFS и вывод для практики.
        System.out.println("""
                Оба алгоритма ищут кратчайший путь в невзвешенном графе (число рёбер).
                BFS — классический обход в ширину; за один проход даёт минимальное число рёбер.
                Второй замер — IDDFS (итеративное углубление): это серия обходов в глубину (DFS)
                с ограничением глубины 1, 2, … до |V|−1. Такой DFS-вариант даёт тот же кратчайший путь,
                что и BFS; наивный одиночный DFS «до первого достижения цели» кратчайший путь не гарантирует.
                IDDFS многократно переобходит граф, поэтому обычно медленнее BFS; при росте |V| и |E|
                разрыв типично растёт. Для кратчайшего пути в приложениях чаще берут BFS (или Дейкстру при весах).
                """);
        // Сумма наносекунд по всем 10 запускам BFS.
        long sumBfs = rows.stream().mapToLong(BenchmarkRow::bfsNanos).sum();
        // Сумма по DFS (IDDFS).
        long sumDfs = rows.stream().mapToLong(BenchmarkRow::dfsNanos).sum();
        // Отношение сумм показывает, во сколько раз в сумме второй метод был медленнее/быстрее.
        System.out.printf("Суммарно по 10 запускам: BFS %d нс, DFS(IDDFS) %d нс (отношение DFS/BFS ≈ %.2f).%n",
                sumBfs, sumDfs, sumBfs == 0 ? 0 : (double) sumDfs / sumBfs);
    }
}
