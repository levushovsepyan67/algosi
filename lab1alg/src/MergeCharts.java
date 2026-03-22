import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Построение графиков для второй лабораторной (сортировка слиянием).
 *
 * 1. Наихудшее время и теоретическая кривая O(c·n·log₂n).
 * 2. Совмещённый график лучшего, среднего и наихудшего времени.
 * 3. Совмещённый график лучшей, средней и наихудшей глубины рекурсии.
 * 4. Совмещённый график лучшей, средней и наихудшей дополнительной памяти.
 * 5. Совмещённый график лучшего, среднего и наихудшего количества рекурсивных вызовов.
 */
public class MergeCharts {

    private static final int W = 800;
    private static final int H = 500;
    private static final int MARGIN = 60;

    /**
     * Строит полный набор графиков для одной серии экспериментов.
     * На вход подаётся список результатов по всем размерам N.
     */
    public static void buildAll(Path dir, List<MergeSortExperiment.SeriesResult> results) throws IOException {
        buildWorstAndBigO(dir, results);
        buildTimeComparison(dir, results);
        buildDepthComparison(dir, results);
        buildMemoryComparison(dir, results);
        buildCallsComparison(dir, results);
    }

    /**
     * График 1: наихудшее время и теоретическая кривая O(c·n·log₂n).
     * Константа c подбирается по экспериментальным точкам так,
     * чтобы кривая располагалась не ниже графика наихудшего времени при больших N.
     */
    private static void buildWorstAndBigO(Path dir, List<MergeSortExperiment.SeriesResult> results) throws IOException {
        double maxWorst = results.stream().mapToDouble(r -> r.worstTime).max().orElse(1);
        MergeSortExperiment.SeriesResult last = results.get(results.size() - 1);
        long nMax = last.size;

        // Оцениваем константу c как максимум отношения "время / (n log n)" по всем сериям (кроме самых маленьких)
        double c = 0.0;
        for (MergeSortExperiment.SeriesResult r : results) {
            if (r.size <= 1000) continue;
            double n = r.size;
            double val = r.worstTime / (n * log2(n));
            if (val > c) c = val;
        }
        if (c <= 0.0) {
            // Защитный случай: если данных мало, берём c по самой большой точке
            double n = nMax;
            c = maxWorst / (n * log2(n));
        }
        final double C = c;
        // Верхняя граница графика по оси Y с небольшим запасом (5%)
        double maxY = Math.max(maxWorst, C * nMax * log2(nMax)) * 1.05;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;
        // Диапазон значений N: от минимального до максимального размера массива
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxY,
                "Размер N", "Время (мс) / O(n·log n)");

        g.setColor(new Color(200, 50, 50));
        int[] xWorst = new int[results.size()];
        int[] yWorst = new int[results.size()];
        for (int i = 0; i < results.size(); i++) {
            MergeSortExperiment.SeriesResult r = results.get(i);
            xWorst[i] = xCoord(r.size, minSize, maxSize, graphW);
            yWorst[i] = yCoord(r.worstTime, 0, maxY, graphH);
        }
        g.setStroke(new BasicStroke(2));
        g.drawPolyline(xWorst, yWorst, results.size());
        g.drawString("Наихудшее время", W - MARGIN - 140, MARGIN + 15);

        g.setColor(new Color(50, 100, 200));
        int points = 100;
        int[] xO = new int[points];
        int[] yO = new int[points];
        for (int i = 0; i < points; i++) {
            double n = minSize * Math.pow(maxSize / minSize, (double) i / (points - 1));
            double onlogn = C * n * log2(n);
            xO[i] = xCoord(n, minSize, maxSize, graphW);
            yO[i] = yCoord(Math.min(onlogn, maxY), 0, maxY, graphH);
        }
        g.drawPolyline(xO, yO, points);
        g.drawString("O(c·n·log₂n)", W - MARGIN - 90, MARGIN + 30);

        g.dispose();
        ImageIO.write(img, "png", dir.resolve("merge_graph1_worst_and_bigO.png").toFile());
        System.out.println("График 1 (merge): merge_graph1_worst_and_bigO.png");
    }

    /** График 2: лучшее, среднее и наихудшее время в одной системе координат. */
    private static void buildTimeComparison(Path dir, List<MergeSortExperiment.SeriesResult> results) throws IOException {
        double maxT = results.stream().mapToDouble(r -> r.worstTime).max().orElse(1);
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;
        double maxY = maxT * 1.05;
        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxY,
                "Размер N", "Время (мс)");

        for (int series = 0; series < 3; series++) {
            int[] xs = new int[results.size()];
            int[] ys = new int[results.size()];
            for (int i = 0; i < results.size(); i++) {
                MergeSortExperiment.SeriesResult r = results.get(i);
                xs[i] = xCoord(r.size, minSize, maxSize, graphW);
                double val = series == 0 ? r.bestTime : (series == 1 ? r.avgTime : r.worstTime);
                ys[i] = yCoord(val, 0, maxY, graphH);
            }
            g.setColor(series == 0
                    ? new Color(50, 150, 50)
                    : (series == 1 ? new Color(200, 120, 0) : new Color(200, 50, 50)));
            g.setStroke(new BasicStroke(2));
            g.drawPolyline(xs, ys, results.size());
        }

        g.setColor(new Color(50, 150, 50));
        g.drawString("Наилучшее", W - MARGIN - 90, MARGIN + 15);
        g.setColor(new Color(200, 120, 0));
        g.drawString("Среднее", W - MARGIN - 60, MARGIN + 30);
        g.setColor(new Color(200, 50, 50));
        g.drawString("Наихудшее", W - MARGIN - 75, MARGIN + 45);

        g.dispose();
        ImageIO.write(img, "png", dir.resolve("merge_graph2_times.png").toFile());
        System.out.println("График 2 (merge): merge_graph2_times.png");
    }

    /** График 3: глубина рекурсии (best/avg/worst) от размера массива. */
    private static void buildDepthComparison(Path dir, List<MergeSortExperiment.SeriesResult> results) throws IOException {
        double maxDepth = results.stream().mapToDouble(r -> r.worstDepth).max().orElse(1);
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;
        double maxY = maxDepth * 1.05;
        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxY,
                "Размер N", "Глубина рекурсии");

        for (int series = 0; series < 3; series++) {
            int[] xs = new int[results.size()];
            int[] ys = new int[results.size()];
            for (int i = 0; i < results.size(); i++) {
                MergeSortExperiment.SeriesResult r = results.get(i);
                xs[i] = xCoord(r.size, minSize, maxSize, graphW);
                double val = series == 0 ? r.bestDepth : (series == 1 ? r.avgDepth : r.worstDepth);
                ys[i] = yCoord(val, 0, maxY, graphH);
            }
            g.setColor(series == 0
                    ? new Color(70, 130, 180)
                    : (series == 1 ? new Color(180, 70, 130) : new Color(120, 50, 120)));
            g.setStroke(new BasicStroke(2));
            g.drawPolyline(xs, ys, results.size());
        }

        g.dispose();
        ImageIO.write(img, "png", dir.resolve("merge_graph3_depth.png").toFile());
        System.out.println("График 3 (merge): merge_graph3_depth.png");
    }

    /** График 4: дополнительная память (байты) как функция размера массива. */
    private static void buildMemoryComparison(Path dir, List<MergeSortExperiment.SeriesResult> results) throws IOException {
        double maxMem = results.stream().mapToDouble(r -> r.worstMemBytes).max().orElse(1);
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;
        double maxY = maxMem * 1.05;
        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxY,
                "Размер N", "Доп. память (байт)");

        for (int series = 0; series < 3; series++) {
            int[] xs = new int[results.size()];
            int[] ys = new int[results.size()];
            for (int i = 0; i < results.size(); i++) {
                MergeSortExperiment.SeriesResult r = results.get(i);
                xs[i] = xCoord(r.size, minSize, maxSize, graphW);
                double val = series == 0 ? r.bestMemBytes : (series == 1 ? r.avgMemBytes : r.worstMemBytes);
                ys[i] = yCoord(val, 0, maxY, graphH);
            }
            g.setColor(series == 0
                    ? new Color(100, 50, 180)
                    : (series == 1 ? new Color(200, 160, 0) : new Color(200, 80, 80)));
            g.setStroke(new BasicStroke(2));
            g.drawPolyline(xs, ys, results.size());
        }

        g.dispose();
        ImageIO.write(img, "png", dir.resolve("merge_graph4_memory.png").toFile());
        System.out.println("График 4 (merge): merge_graph4_memory.png");
    }

    /** График 5: количество рекурсивных вызовов (best/avg/worst) от размера массива. */
    private static void buildCallsComparison(Path dir, List<MergeSortExperiment.SeriesResult> results) throws IOException {
        double maxCalls = results.stream().mapToDouble(r -> r.worstCalls).max().orElse(1);
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;
        double maxY = maxCalls * 1.05;
        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxY,
                "Размер N", "Рекурсивные вызовы");

        for (int series = 0; series < 3; series++) {
            int[] xs = new int[results.size()];
            int[] ys = new int[results.size()];
            for (int i = 0; i < results.size(); i++) {
                MergeSortExperiment.SeriesResult r = results.get(i);
                xs[i] = xCoord(r.size, minSize, maxSize, graphW);
                double val = series == 0 ? r.bestCalls : (series == 1 ? r.avgCalls : r.worstCalls);
                ys[i] = yCoord(val, 0, maxY, graphH);
            }
            g.setColor(series == 0
                    ? new Color(0, 120, 150)
                    : (series == 1 ? new Color(180, 90, 30) : new Color(150, 0, 80)));
            g.setStroke(new BasicStroke(2));
            g.drawPolyline(xs, ys, results.size());
        }

        g.dispose();
        ImageIO.write(img, "png", dir.resolve("merge_graph5_calls.png").toFile());
        System.out.println("График 5 (merge): merge_graph5_calls.png");
    }

    /**
     * Переводит значение N в координату X на графике.
     * Используется логарифмическое масштабирование по оси X,
     * чтобы более компактно отобразить размеры от 1000 до 128000.
     */
    private static int xCoord(double n, double minSize, double maxSize, int graphW) {
        return MARGIN + (int) ((Math.log(n) - Math.log(minSize)) / (Math.log(maxSize) - Math.log(minSize)) * graphW);
    }

    /**
     * Переводит измеренное значение (время, глубина, память и т.д.)
     * в координату Y на графике, с учётом минимального и максимального значений
     * и инвертированного вертикального направления в системе координат Java2D.
     */
    private static int yCoord(double value, double minY, double maxY, int graphH) {
        double clipped = Math.max(minY, Math.min(maxY, value));
        return H - MARGIN - (int) ((clipped - minY) / (maxY - minY) * graphH);
    }

    /** Логарифм по основанию 2, вынесен в отдельный метод для удобства чтения формул. */
    private static double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    /**
     * Рисует рамку, сетку и подписи осей для графика.
     * По оси X значения N распределены логарифмически, по оси Y — линейно.
     */
    private static void drawGridAndAxes(Graphics2D g, int graphW, int graphH,
                                        double minX, double maxX, double minY, double maxY,
                                        String labelX, String labelY) {
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i <= 5; i++) {
            int x = MARGIN + i * graphW / 5;
            int y = MARGIN + i * graphH / 5;
            g.drawLine(x, MARGIN, x, H - MARGIN);
            g.drawLine(MARGIN, y, W - MARGIN, y);
        }

        g.setColor(Color.BLACK);
        g.drawRect(MARGIN, MARGIN, graphW, graphH);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));

        for (int i = 0; i <= 5; i++) {
            double xVal = minX * Math.pow(maxX / minX, (double) i / 5);
            double yVal = minY + (maxY - minY) * (5 - i) / 5;

            int xPos = MARGIN + i * graphW / 5;
            int xLabelY = H - MARGIN + 25;

            java.awt.geom.AffineTransform old = g.getTransform();
            g.rotate(-Math.PI / 6, xPos, xLabelY);
            g.drawString(String.format("%.0f", xVal), xPos - 15, xLabelY);
            g.setTransform(old);

            g.drawString(String.format("%.2g", yVal), MARGIN - 50, MARGIN + i * graphH / 5 + 4);
        }

        g.drawString(labelX, W / 2 - 30, H - 5);

        java.awt.geom.AffineTransform old = g.getTransform();
        g.rotate(-Math.PI / 2, MARGIN - 70, H / 2.0);
        g.drawString(labelY, MARGIN - 70, H / 2.0f);
        g.setTransform(old);
    }
}

