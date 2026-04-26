package usaskoviy.hash;

import javax.imageio.ImageIO;
import java.awt.FontMetrics;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Построение графиков для лабораторной работы 9.
 */
public final class HashLabCharts {

    private HashLabCharts() {
    }

    public static void saveDiffChart(File file, Map<Integer, Integer> diffStats) throws IOException {
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : diffStats.entrySet()) {
            x.add((double) entry.getKey());
            y.add((double) entry.getValue());
        }
        drawLineChart(
                file,
                "Зависимость общей последовательности от количества отличий",
                "Количество отличий в исходных строках",
                "Макс. длина общей последовательности в хешах",
                x,
                y
        );
    }

    public static void saveSpeedChart(File file, Map<Integer, HashLabRunner.SpeedStat> speedStats) throws IOException {
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (Map.Entry<Integer, HashLabRunner.SpeedStat> entry : speedStats.entrySet()) {
            x.add((double) entry.getKey());
            y.add(entry.getValue().hashesPerSecond());
        }
        drawLineChart(
                file,
                "Скорость расчета MD5 от размера входных данных",
                "Размер входной строки (символы)",
                "Скорость (хеш/с)",
                x,
                y
        );
    }

    private static void drawLineChart(
            File file,
            String title,
            String xLabel,
            String yLabel,
            List<Double> xValues,
            List<Double> yValues) throws IOException {
        int width = 1200;
        int height = 700;
        int left = 200;       // Увеличен с 170 для длинных подписей Y
        int right = 60;
        int top = 80;
        int bottom = 180;     // Увеличен с 140, чтобы дать место подписям оси X с поворотом
        int plotWidth = width - left - right;
        int plotHeight = height - top - bottom;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Заголовок
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString(title, 30, 45);

        // Оси
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(left, top + plotHeight, left + plotWidth, top + plotHeight);  // Ось X
        g.drawLine(left, top, left, top + plotHeight);                            // Ось Y

        // Вычисление границ
        double minX = xValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxX = xValues.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double minY = yValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxY = yValues.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxX == minX) {
            maxX = minX + 1.0;
        }
        if (maxY == minY) {
            maxY = minY + 1.0;
        }
        double yPadding = (maxY - minY) * 0.1;
        minY = Math.max(0.0, minY - yPadding);
        maxY = maxY + yPadding;

        // Горизонтальная сетка и подписи оси Y
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (int i = 0; i <= 10; i++) {
            int yPix = top + plotHeight - (int) (plotHeight * (i / 10.0));
            g.setColor(new Color(235, 235, 235));
            g.drawLine(left, yPix, left + plotWidth, yPix);
            g.setColor(Color.BLACK);
            double yValue = minY + (maxY - minY) * i / 10.0;
            // Подписи Y теперь выравнены по правому краю с достаточным отступом
            String yLabelText = String.format("%.2f", yValue);
            FontMetrics fmY = g.getFontMetrics();
            int yLabelWidth = fmY.stringWidth(yLabelText);
            g.drawString(yLabelText, left - yLabelWidth - 10, yPix + 5);
        }

        // Линия графика и точки
        g.setColor(new Color(30, 80, 220));
        g.setStroke(new BasicStroke(3f));
        int prevX = -1;
        int prevY = -1;
        FontMetrics metrics = g.getFontMetrics(g.getFont());

        // Сначала рисуем линию и точки
        for (int i = 0; i < xValues.size(); i++) {
            int xPix = left + (int) ((xValues.get(i) - minX) / (maxX - minX) * plotWidth);
            int yPix = top + plotHeight - (int) ((yValues.get(i) - minY) / (maxY - minY) * plotHeight);
            g.fillOval(xPix - 4, yPix - 4, 8, 8);
            if (prevX >= 0) {
                g.drawLine(prevX, prevY, xPix, yPix);
            }
            prevX = xPix;
            prevY = yPix;
        }

        // Затем отдельно рисуем подписи оси X с поворотом на 45 градусов
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        FontMetrics metricsX = g.getFontMetrics();

        for (int i = 0; i < xValues.size(); i++) {
            int xPix = left + (int) ((xValues.get(i) - minX) / (maxX - minX) * plotWidth);
            String tick = String.format("%.0f", xValues.get(i));
            int tickWidth = metricsX.stringWidth(tick);

            // Сохраняем текущую трансформацию
            AffineTransform oldTransform = g.getTransform();
            // Поворачиваем текст на -45 градусов относительно точки (xPix, baseline)
            g.rotate(-Math.toRadians(45), xPix, top + plotHeight + 20);
            g.drawString(tick, xPix - tickWidth / 2, top + plotHeight + 20);
            // Восстанавливаем трансформацию
            g.setTransform(oldTransform);
        }

        // Подпись оси X
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics labelMetrics = g.getFontMetrics();
        int xLabelWidth = labelMetrics.stringWidth(xLabel);
        g.drawString(xLabel, left + (plotWidth - xLabelWidth) / 2, height - 40);

        // Подпись оси Y (поворот)
        AffineTransform old = g.getTransform();
        g.rotate(-Math.PI / 2);
        int yLabelWidth = labelMetrics.stringWidth(yLabel);
        int yLabelX = -(top + (plotHeight + yLabelWidth) / 2);
        int yLabelY = 35;  // Отступ от левого края
        g.drawString(yLabel, yLabelX, yLabelY);
        g.setTransform(old);

        g.dispose();

        ImageIO.write(image, "png", file);
    }
}