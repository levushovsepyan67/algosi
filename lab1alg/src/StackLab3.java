import java.time.LocalDate;
import java.time.Period;
import java.util.Random;

/**
 * Третья лабораторная работа по алгоритмам.
 * Тема: реализация стека двумя способами (массив и односвязный список)
 * и экспериментальное сравнение их работы.
 */
public class StackLab3 {

    private static final Random RND = new Random(123);

    public static void main(String[] args) {
        System.out.println("Лабораторная №3: стек на массиве и на списке\n");

        testIntegers();
        testStrings();
        testPersonsByBirthDate();
        testInversionWithStacks();
        comparePerformance();
    }

    /**
     * Тест 1.
     * Заполняем стек 1000 случайными целыми числами, считаем сумму, минимум,
     * максимум и среднее. Повторяем для обеих реализаций стека.
     */
    private static void testIntegers() {
        System.out.println("Тест 1: целые числа");
        SimpleStack<Integer> arrayStack = new ArrayStack<>();
        SimpleStack<Integer> linkedStack = new LinkedStack<>();

        // Диапазон значений: от -1000 до 1000 включительно
        int n = 1000;
        int[] data = new int[n];
        for (int i = 0; i < n; i++) {
            data[i] = -1000 + RND.nextInt(2001);
        }

        runIntStackTest("ArrayStack", arrayStack, data);
        runIntStackTest("LinkedStack", linkedStack, data);
        System.out.println();
    }

    private static void runIntStackTest(String title, SimpleStack<Integer> stack, int[] data) {
        for (int value : data) {
            stack.push(value);
        }
        long sum = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        while (!stack.isEmpty()) {
            int v = stack.pop();
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double avg = sum / (double) data.length;
        System.out.printf("%s: n=%d, sum=%d, min=%d, max=%d, avg=%.2f%n",
                title, data.length, sum, min, max, avg);
    }

    /**
     * Тест 2.
     * Демонстрация работы операций push/pop на коллекции из 10 строк.
     */
    private static void testStrings() {
        System.out.println("Тест 2: строки");
        SimpleStack<String> stack = new ArrayStack<>();
        String[] words = {"alpha", "beta", "gamma", "delta", "epsilon",
                "zeta", "eta", "theta", "iota", "kappa"};

        for (String w : words) {
            stack.push(w);
        }

        System.out.println("Содержимое стека (с вершины):");
        for (String s : stack) {
            System.out.println("  " + s);
        }

        System.out.println("Последовательность извлечения (pop):");
        while (!stack.isEmpty()) {
            System.out.println("  pop -> " + stack.pop());
        }
        System.out.println();
    }

    /**
     * Тест 3.
     * Работа с "людьми": ФИО и дата рождения. Из случайно сгенерированного
     * набора людей отбираем тех, кто младше 20 лет и старше 30,
     * используя только операции стека.
     */
    private static void testPersonsByBirthDate() {
        System.out.println("Тест 3: люди и даты рождения");
        SimpleStack<Person> stack = new LinkedStack<>();

        // Сгенерируем 50 "людей" с датой рождения в интервале 1980-01-01 .. 2020-01-01
        LocalDate start = LocalDate.of(1980, 1, 1);
        LocalDate end = LocalDate.of(2020, 1, 1);
        int total = 50;
        for (int i = 0; i < total; i++) {
            LocalDate birth = randomDateBetween(start, end);
            stack.push(new Person("Person" + i, birth));
        }

        int younger20 = 0;
        int older30 = 0;
        LocalDate now = LocalDate.of(2026, 1, 1);

        while (!stack.isEmpty()) {
            Person p = stack.pop();
            int age = Period.between(p.birthDate, now).getYears();
            if (age < 20) younger20++;
            if (age > 30) older30++;
        }

        System.out.printf("Всего людей: %d, младше 20: %d, старше 30: %d%n",
                total, younger20, older30);
        System.out.println();
    }

    private static LocalDate randomDateBetween(LocalDate start, LocalDate end) {
        long days = end.toEpochDay() - start.toEpochDay();
        long offset = (long) (RND.nextDouble() * days);
        return start.plusDays(offset);
    }

    /**
     * Тест 4.
     * Инвертирование содержимого отсортированного контейнера с помощью стека:
     * заполняем стек числами по возрастанию и с помощью второго стека
     * разворачиваем порядок, используя только операции push/pop.
     */
    private static void testInversionWithStacks() {
        System.out.println("Тест 4: инвертирование последовательности");

        SimpleStack<Integer> original = new ArrayStack<>();
        SimpleStack<Integer> inverted = new ArrayStack<>();

        int n = 10;
        for (int i = 1; i <= n; i++) {
            original.push(i); // 1,2,...,10 (10 окажется на вершине)
        }

        // Инверсия: перекладываем элементы из одного стека в другой.
        while (!original.isEmpty()) {
            inverted.push(original.pop());
        }

        System.out.println("После инверсии элементы на вершине идут в порядке 1..10:");
        while (!inverted.isEmpty()) {
            System.out.print(inverted.pop() + " ");
        }
        System.out.println("\n");
    }

    /**
     * Тест 5.
     * Сравнение производительности двух реализаций:
     * измеряем время вставки и удаления 10000 элементов.
     */
    private static void comparePerformance() {
        System.out.println("Тест 5: сравнение производительности (10000 элементов)");

        int n = 10_000;
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = i;
        }

        long arrayTime = measurePushPop(new ArrayStack<Integer>(), values);
        long linkedTime = measurePushPop(new LinkedStack<Integer>(), values);

        System.out.printf("ArrayStack:  push+pop %d элементов заняли %d мс%n", n, arrayTime);
        System.out.printf("LinkedStack: push+pop %d элементов заняли %d мс%n", n, linkedTime);
        System.out.println("Как правило, реализация на массиве немного быстрее из-за лучшей локальности по памяти,\n" +
                "а реализация на списке более гибкая по росту, но имеет накладные расходы на узлы списка.\n");
    }

    private static long measurePushPop(SimpleStack<Integer> stack, int[] values) {
        long start = System.nanoTime();
        for (int v : values) {
            stack.push(v);
        }
        while (!stack.isEmpty()) {
            stack.pop();
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000; // мс
    }

    /**
     * Простая структура для демонстрации работы стека с объектами.
     */
    private static class Person {
        final String name;
        final LocalDate birthDate;

        Person(String name, LocalDate birthDate) {
            this.name = name;
            this.birthDate = birthDate;
        }
    }
}

