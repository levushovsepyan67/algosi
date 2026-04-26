package usaskoviy.hash;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Полный прогон лабораторной работы для MD5.
 */
public final class HashLabRunner {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int[] DIFFS = {1, 2, 4, 8, 16};
    private static final int[] SIZE_SERIES = {64, 128, 256, 512, 1024, 2048, 4096, 8192};
    private static final long SEED = 20260426L;

    private final Random random = new Random(SEED);

    public LabResult runAll() throws IOException {
        File outDir = new File("lab9_results");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create output directory: " + outDir.getAbsolutePath());
        }

        Map<Integer, Integer> maxCommonByDiff = runDiffExperiment();
        Map<Integer, CollisionStat> collisionStats = runCollisionExperiment();
        Map<Integer, SpeedStat> speedStats = runSpeedExperiment();

        writeDiffCsv(new File(outDir, "diff_experiment.csv"), maxCommonByDiff);
        writeCollisionCsv(new File(outDir, "collision_experiment.csv"), collisionStats);
        writeSpeedCsv(new File(outDir, "speed_experiment.csv"), speedStats);
        writeSummaryMd(new File(outDir, "summary.md"), maxCommonByDiff, collisionStats, speedStats);

        HashLabCharts.saveDiffChart(new File(outDir, "chart_diff_vs_common.png"), maxCommonByDiff);
        HashLabCharts.saveSpeedChart(new File(outDir, "chart_size_vs_speed.png"), speedStats);

        return new LabResult(outDir, maxCommonByDiff, collisionStats, speedStats);
    }

    private Map<Integer, Integer> runDiffExperiment() {
        Map<Integer, Integer> maxCommonByDiff = new LinkedHashMap<>();
        for (int diff : DIFFS) {
            int globalMax = 0;
            for (int i = 0; i < 1000; i++) {
                String first = randomString(128);
                String second = mutateString(first, diff);
                String hashA = Md5Hasher.hashHex(first);
                String hashB = Md5Hasher.hashHex(second);
                int longest = longestCommonSubstring(hashA, hashB);
                if (longest > globalMax) {
                    globalMax = longest;
                }
            }
            maxCommonByDiff.put(diff, globalMax);
        }
        return maxCommonByDiff;
    }

    private Map<Integer, CollisionStat> runCollisionExperiment() {
        Map<Integer, CollisionStat> result = new LinkedHashMap<>();
        for (int pow = 2; pow <= 6; pow++) {
            int n = (int) Math.pow(10, pow);
            Map<String, Integer> frequency = new LinkedHashMap<>(n * 2);
            int duplicateCount = 0;
            for (int i = 0; i < n; i++) {
                String data = randomString(256);
                String hash = Md5Hasher.hashHex(data);
                int next = frequency.getOrDefault(hash, 0) + 1;
                frequency.put(hash, next);
                if (next > 1) {
                    duplicateCount++;
                }
            }
            result.put(n, new CollisionStat(duplicateCount > 0, duplicateCount));
        }
        return result;
    }

    private Map<Integer, SpeedStat> runSpeedExperiment() {
        Map<Integer, SpeedStat> result = new LinkedHashMap<>();
        for (int size : SIZE_SERIES) {
            long totalNs = 0L;
            for (int i = 0; i < 1000; i++) {
                String data = randomString(size);
                long start = System.nanoTime();
                Md5Hasher.hashHex(data);
                totalNs += (System.nanoTime() - start);
            }
            double avgNs = totalNs / 1000.0;
            double hashesPerSecond = 1_000_000_000.0 / avgNs;
            result.put(size, new SpeedStat(avgNs, hashesPerSecond));
        }
        return result;
    }

    private static void writeDiffCsv(File file, Map<Integer, Integer> data) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("difference_count,max_common_substring_length");
            for (Map.Entry<Integer, Integer> entry : data.entrySet()) {
                writer.printf(Locale.ROOT, "%d,%d%n", entry.getKey(), entry.getValue());
            }
        }
    }

    private static void writeCollisionCsv(File file, Map<Integer, CollisionStat> data) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("N,has_collisions,collision_count");
            for (Map.Entry<Integer, CollisionStat> entry : data.entrySet()) {
                CollisionStat stat = entry.getValue();
                writer.printf(Locale.ROOT, "%d,%s,%d%n", entry.getKey(), stat.hasCollisions(), stat.collisionCount());
            }
        }
    }

    private static void writeSpeedCsv(File file, Map<Integer, SpeedStat> data) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("input_size_chars,avg_time_ns,hashes_per_second");
            for (Map.Entry<Integer, SpeedStat> entry : data.entrySet()) {
                SpeedStat stat = entry.getValue();
                writer.printf(Locale.ROOT, "%d,%.3f,%.3f%n", entry.getKey(), stat.avgTimeNs(), stat.hashesPerSecond());
            }
        }
    }

    private static void writeSummaryMd(
            File file,
            Map<Integer, Integer> diffStats,
            Map<Integer, CollisionStat> collisionStats,
            Map<Integer, SpeedStat> speedStats) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.println("# Лабораторная 9 (MD5)");
            writer.println();
            writer.println("Отчет сгенерирован: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();
            writer.println("## 1) Влияние количества отличий на хеш");
            writer.println();
            writer.println("| Кол-во отличий | Макс. длина общей последовательности в хешах |");
            writer.println("|---:|---:|");
            for (Map.Entry<Integer, Integer> entry : diffStats.entrySet()) {
                writer.printf(Locale.ROOT, "| %d | %d |%n", entry.getKey(), entry.getValue());
            }
            writer.println();
            writer.println("## 2) Проверка коллизий для N = 10^i, i=2..6");
            writer.println();
            writer.println("| N генераций | Коллизии есть | Кол-во повторов хеша |");
            writer.println("|---:|:---:|---:|");
            for (Map.Entry<Integer, CollisionStat> entry : collisionStats.entrySet()) {
                CollisionStat stat = entry.getValue();
                writer.printf(Locale.ROOT, "| %d | %s | %d |%n",
                        entry.getKey(),
                        stat.hasCollisions() ? "Да" : "Нет",
                        stat.collisionCount());
            }
            writer.println();
            writer.println("## 3) Скорость расчета хеша от размера входа");
            writer.println();
            writer.println("| Размер строки | Среднее время (нс) | Скорость (хеш/с) |");
            writer.println("|---:|---:|---:|");
            for (Map.Entry<Integer, SpeedStat> entry : speedStats.entrySet()) {
                SpeedStat stat = entry.getValue();
                writer.printf(Locale.ROOT, "| %d | %.3f | %.3f |%n",
                        entry.getKey(),
                        stat.avgTimeNs(),
                        stat.hashesPerSecond());
            }
            writer.println();
            writer.println("Графики:");
            writer.println("- `chart_diff_vs_common.png`");
            writer.println("- `chart_size_vs_speed.png`");
        }
    }

    private String randomString(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(chars);
    }

    private String mutateString(String source, int diffCount) {
        char[] chars = source.toCharArray();
        List<Integer> indexes = new ArrayList<>(chars.length);
        for (int i = 0; i < chars.length; i++) {
            indexes.add(i);
        }
        for (int i = 0; i < diffCount && !indexes.isEmpty(); i++) {
            int listPos = random.nextInt(indexes.size());
            int index = indexes.remove(listPos);
            char current = chars[index];
            char next = current;
            while (next == current) {
                next = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
            }
            chars[index] = next;
        }
        return new String(chars);
    }

    private static int longestCommonSubstring(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        int best = 0;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] > best) {
                        best = dp[i][j];
                    }
                }
            }
        }
        return best;
    }

    public record CollisionStat(boolean hasCollisions, int collisionCount) {
    }

    public record SpeedStat(double avgTimeNs, double hashesPerSecond) {
    }

    public record LabResult(
            File outputDir,
            Map<Integer, Integer> diffStats,
            Map<Integer, CollisionStat> collisionStats,
            Map<Integer, SpeedStat> speedStats) {
    }
}
