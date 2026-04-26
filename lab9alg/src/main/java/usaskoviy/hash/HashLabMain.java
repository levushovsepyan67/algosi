package usaskoviy.hash;

import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Точка входа для лабораторной работы 9.
 */
public final class HashLabMain {

    private HashLabMain() {
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        System.out.println("Лабораторная 9: реализация MD5");

        if (args.length > 0 && "--run-lab".equals(args[0])) {
            runLab();
            return;
        }

        String input;
        if (args.length > 0) {
            input = args[0];
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Введите строку: ");
            input = scanner.nextLine();
        }

        String hash = Md5Hasher.hashHex(input);
        System.out.println("MD5: " + hash);
        System.out.println("Для полного прогона лабораторной: --run-lab");
    }

    private static void runLab() {
        try {
            HashLabRunner runner = new HashLabRunner();
            HashLabRunner.LabResult result = runner.runAll();

            System.out.println("Все эксперименты выполнены.");
            System.out.println("Папка результатов: " + result.outputDir().getAbsolutePath());

            System.out.println();
            System.out.println("Эксперимент 1 (max общая последовательность в хешах):");
            for (Map.Entry<Integer, Integer> entry : result.diffStats().entrySet()) {
                System.out.printf("diff=%d -> max=%d%n", entry.getKey(), entry.getValue());
            }

            System.out.println();
            System.out.println("Эксперимент 2 (коллизии):");
            for (Map.Entry<Integer, HashLabRunner.CollisionStat> entry : result.collisionStats().entrySet()) {
                HashLabRunner.CollisionStat stat = entry.getValue();
                System.out.printf("N=%d -> hasCollisions=%s, collisionCount=%d%n",
                        entry.getKey(),
                        stat.hasCollisions(),
                        stat.collisionCount());
            }

            System.out.println();
            System.out.println("Эксперимент 3 (скорость):");
            for (Map.Entry<Integer, HashLabRunner.SpeedStat> entry : result.speedStats().entrySet()) {
                HashLabRunner.SpeedStat stat = entry.getValue();
                System.out.printf("size=%d -> avgNs=%.3f, hashesPerSec=%.3f%n",
                        entry.getKey(),
                        stat.avgTimeNs(),
                        stat.hashesPerSecond());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка выполнения лабораторных экспериментов", e);
        }
    }
}
