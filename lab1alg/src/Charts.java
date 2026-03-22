import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;


public class Charts {

    private static final int W = 800;
    private static final int H = 500;
    private static final int MARGIN = 60;

    public static void buildAll(Path dir, List<Experiment.SeriesResult> results) throws IOException {
        buildWorstAndBigO(dir, results);
        buildTimeComparison(dir, results);
        buildAvgSwaps(dir, results);
        buildPasses(dir, results);
    }

    /* График 1: на одной картинке — наихудшее время выполнения (по точкам для каждого размера)
    и теоретическая кривая O(c·n²). Константа c подбирается так, чтобы при n > 1000
    кривая O(c·n²) была выше графика наихудшего времени (по заданию). */
    private static void buildWorstAndBigO(Path dir, List<Experiment.SeriesResult> results) throws IOException {
        double maxWorst = results.stream().mapToDouble(r -> r.worstTime).max().orElse(1);
        Experiment.SeriesResult last = results.get(results.size() - 1);
        long nMax = last.size;
        // c такова, что c·n² >= worstTime(n) при n>1000; для наглядности масштаб: c·nMax² = maxWorst
        double c = 0;
        for (Experiment.SeriesResult r : results) {
            if (r.size <= 1000) continue;
            double val = r.worstTime / ((double) r.size * r.size);
            if (val > c) c = val;
        }
        if (c <= 0) c = maxWorst / ((double) nMax * nMax);
        final double C = c;
        double maxY = Math.max(maxWorst, C * nMax * nMax) * 1.05;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxY, "Размер N", "Время (мс) / O(n²)");

        // Наихудшее время
        g.setColor(new Color(200, 50, 50));
        int[] xWorst = new int[results.size()];
        int[] yWorst = new int[results.size()];
        for (int i = 0; i < results.size(); i++) {
            Experiment.SeriesResult r = results.get(i);
            xWorst[i] = MARGIN + (int) ((Math.log(r.size) - Math.log(minSize)) / (Math.log(maxSize) - Math.log(minSize)) * graphW);
            yWorst[i] = H - MARGIN - (int) (r.worstTime / maxY * graphH);
        }
        g.setStroke(new BasicStroke(2));
        g.drawPolyline(xWorst, yWorst, results.size());
        g.drawString("Наихудшее время", W - MARGIN - 140, MARGIN + 15);

        // O(c·n²)
        g.setColor(new Color(50, 100, 200));
        int points = 100;
        int[] xO = new int[points];
        int[] yO = new int[points];
        for (int i = 0; i < points; i++) {
            double n = minSize * Math.pow(maxSize / minSize, (double) i / (points - 1));
            double on2 = C * n * n;
            xO[i] = MARGIN + (int) ((Math.log(n) - Math.log(minSize)) / (Math.log(maxSize) - Math.log(minSize)) * graphW);
            yO[i] = H - MARGIN - (int) (Math.min(on2, maxY) / maxY * graphH);
        }
        g.drawPolyline(xO, yO, points);
        g.drawString("O(c·n²)", W - MARGIN - 60, MARGIN + 30);

        g.dispose();
        ImageIO.write(img, "png", dir.resolve("graph1_worst_and_bigO.png").toFile());
        System.out.println("График 1: graph1_worst_and_bigO.png");
    }

    /** График 2: среднее, наилучшее и наихудшее время */
    private static void buildTimeComparison(Path dir, List<Experiment.SeriesResult> results) throws IOException {
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
        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxT * 1.05, "Размер N", "Время (мс)");

        for (int series = 0; series < 3; series++) {
            int[] xs = new int[results.size()];
            int[] ys = new int[results.size()];
            for (int i = 0; i < results.size(); i++) {
                Experiment.SeriesResult r = results.get(i);
                xs[i] = MARGIN + (int) ((Math.log(r.size) - Math.log(minSize)) / (Math.log(maxSize) - Math.log(minSize)) * graphW);
                double val = series == 0 ? r.bestTime : (series == 1 ? r.avgTime : r.worstTime);
                ys[i] = H - MARGIN - (int) (val / maxT * graphH);
            }
            g.setColor(series == 0 ? new Color(50, 150, 50) : (series == 1 ? new Color(200, 120, 0) : new Color(200, 50, 50)));
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
        ImageIO.write(img, "png", dir.resolve("graph2_times.png").toFile());
        System.out.println("График 2: graph2_times.png");
    }

    /** График 3: среднее количество обменов от размера */
    private static void buildAvgSwaps(Path dir, List<Experiment.SeriesResult> results) throws IOException {
        double maxSwaps = results.stream().mapToDouble(r -> r.avgSwaps).max().orElse(1);
        double minSwaps = results.stream().mapToDouble(r -> r.avgSwaps).min().orElse(0);
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;

        // Позволяем шкале Y учитывать возможные отрицательные значения, но линия остаётся в рамках.
        double yMin = Math.min(0, minSwaps);
        double yMax = maxSwaps * 1.05;
        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, yMin, yMax, "Размер N", "Среднее кол-во обменов");

        int[] xs = new int[results.size()];
        int[] ys = new int[results.size()];
        for (int i = 0; i < results.size(); i++) {
            Experiment.SeriesResult r = results.get(i);
            xs[i] = MARGIN + (int) ((Math.log(r.size) - Math.log(minSize)) / (Math.log(maxSize) - Math.log(minSize)) * graphW);

            // Нормализуем значение в диапазон [yMin, yMax] и дополнительно жёстко ограничиваем пиксели,
            // чтобы линия не вылезала за рамку прямоугольника графика.
            double val = Math.max(yMin, Math.min(yMax, r.avgSwaps));
            double norm = (val - yMin) / (yMax - yMin);
            int yPix = H - MARGIN - (int) (norm * graphH);
            yPix = Math.max(MARGIN, Math.min(H - MARGIN, yPix));
            ys[i] = yPix;
        }
        g.setColor(new Color(100, 50, 180));
        g.setStroke(new BasicStroke(2));
        g.drawPolyline(xs, ys, results.size());
        g.dispose();
        ImageIO.write(img, "png", dir.resolve("graph3_avg_swaps.png").toFile());
        System.out.println("График 3: graph3_avg_swaps.png");
    }

    /** График 4: количество проходов от размера */
    private static void buildPasses(Path dir, List<Experiment.SeriesResult> results) throws IOException {
        double maxPasses = results.stream().mapToDouble(r -> r.avgPasses).max().orElse(1);
        double minSize = results.get(0).size;
        double maxSize = results.get(results.size() - 1).size;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        int graphW = W - 2 * MARGIN;
        int graphH = H - 2 * MARGIN;
        drawGridAndAxes(g, graphW, graphH, minSize, maxSize, 0, maxPasses * 1.05, "Размер N", "Кол-во проходов");

        int[] xs = new int[results.size()];
        int[] ys = new int[results.size()];
        for (int i = 0; i < results.size(); i++) {
            Experiment.SeriesResult r = results.get(i);
            xs[i] = MARGIN + (int) ((Math.log(r.size) - Math.log(minSize)) / (Math.log(maxSize) - Math.log(minSize)) * graphW);
            ys[i] = H - MARGIN - (int) (r.avgPasses / maxPasses * graphH);
        }
        g.setColor(new Color(0, 120, 150));
        g.setStroke(new BasicStroke(2));
        g.drawPolyline(xs, ys, results.size());
        g.dispose();
        ImageIO.write(img, "png", dir.resolve("graph4_passes.png").toFile());
        System.out.println("График 4: graph4_passes.png");
    }

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

            // Подписи по оси X слегка поворачиваем, чтобы они не наезжали друг на друга.
            java.awt.geom.AffineTransform old = g.getTransform();
            g.rotate(-Math.PI / 6, xPos, xLabelY);
            g.drawString(String.format("%.0f", xVal), xPos - 15, xLabelY);
            g.setTransform(old);

            // Подписи по оси Y сдвигаем правее, чтобы они не пересекались с названием оси.
            g.drawString(String.format("%.2g", yVal), MARGIN - 45, MARGIN + i * graphH / 5 + 4);
        }

        // Название оси X по центру снизу.
        g.drawString(labelX, W / 2 - 30, H - 5);

        // Название оси Y рисуем вертикально вдоль левого поля, чтобы не налезало на числа.
        java.awt.geom.AffineTransform old = g.getTransform();
        g.rotate(-Math.PI / 2, MARGIN - 70, H / 2.0);
        g.drawString(labelY, MARGIN - 70, H / 2.0f);
        g.setTransform(old);
    }
}
