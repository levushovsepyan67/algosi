/**
 * Реализация сортировки слиянием для массивов {@code double[]} с
 * дополнительным сбором статистики: количество рекурсивных вызовов,
 * максимальная глубина рекурсии и объём вспомогательной памяти.
 *
 * Идея алгоритма:
 * 1) рекурсивно делим массив пополам до отрезков длины 1;
 * 2) затем последовательно "поднимаемся" вверх, на каждом шаге сливая
 *    два уже отсортированных подмассива во временный буфер {@code temp};
 * 3) содержимое буфера копируется обратно в исходный массив.
 */
public class MergeSort {

    public static class Result {
        /** Общее количество вызовов рекурсивной функции mergeSort (включая самый первый). */
        public final long recursiveCalls;
        /** Максимальная достигнутая глубина рекурсии (1 — верхний уровень). */
        public final int maxDepth;
        /**
         * Максимальное количество байт дополнительной памяти,
         * выделенной алгоритмом во время сортировки (байты под вспомогательный массив).
         */
        public final long maxExtraBytes;

        public Result(long recursiveCalls, int maxDepth, long maxExtraBytes) {
            this.recursiveCalls = recursiveCalls;
            this.maxDepth = maxDepth;
            this.maxExtraBytes = maxExtraBytes;
        }
    }

    /**
     * Обёртка над рекурсивной сортировкой слиянием.
     * Сортирует массив по возрастанию и возвращает метрики.
     *
     * @param a массив для сортировки
     * @return результат с количеством рекурсивных вызовов, глубиной рекурсии и доп. памятью
     */
    public static Result sort(double[] a) {
        // Пустой массив уже отсортирован, метрики равны нулю
        if (a.length == 0) {
            return new Result(0, 0, 0);
        }

        // Вспомогательный массив для операций слияния.
        // Создаётся один раз на весь процесс сортировки.
        double[] temp = new double[a.length];
        // В Counter сразу записываем "верхнюю оценку" на доп. память: размер temp в байтах.
        Counter counter = new Counter(a.length * (long) Double.BYTES);
        mergeSort(a, temp, 0, a.length - 1, 1, counter);
        return new Result(counter.calls, counter.maxDepth, counter.maxExtraBytes);
    }

    /** Вспомогательный счётчик для аккуратной передачи статистики между рекурсивными вызовами. */
    private static class Counter {
        long calls = 0;
        int maxDepth = 0;
        long maxExtraBytes;

        Counter(long maxExtraBytes) {
            this.maxExtraBytes = maxExtraBytes;
        }
    }

    /**
     * Рекурсивная часть сортировки слиянием.
     *
     * @param a       исходный массив
     * @param temp    общий временный буфер для слияний
     * @param left    левая граница (включительно)
     * @param right   правая граница (включительно)
     * @param depth   текущая глубина рекурсивного вызова
     * @param counter объект, аккумулирующий статистику по всем вызовам
     */
    private static void mergeSort(double[] a, double[] temp, int left, int right, int depth, Counter counter) {
        // Фиксируем, что мы зашли в ещё один рекурсивный вызов
        counter.calls++;
        if (depth > counter.maxDepth) {
            counter.maxDepth = depth;
        }

        // База рекурсии: подмассив из одного элемента уже отсортирован
        if (left >= right) {
            return;
        }

        // Делим текущий отрезок пополам и рекурсивно сортируем левую и правую части
        int mid = (left + right) >>> 1;
        mergeSort(a, temp, left, mid, depth + 1, counter);
        mergeSort(a, temp, mid + 1, right, depth + 1, counter);
        // После того как обе половины отсортированы, сливаем их во временный буфер
        merge(a, temp, left, mid, right);
        // Дополнительная память выделена один раз под temp, поэтому maxExtraBytes задаётся в конструкторе Counter.
    }

    /**
     * Сливает два соседних отсортированных подмассива {@code [left, mid]} и {@code [mid+1, right]}
     * в общий временный буфер {@code temp}, а затем копирует результат обратно в {@code a}.
     */
    private static void merge(double[] a, double[] temp, int left, int mid, int right) {
        int i = left;
        int j = mid + 1;
        int k = left;

        // Основной цикл: выбираем наименьший из "голов" двух подмассивов
        while (i <= mid && j <= right) {
            if (a[i] <= a[j]) {
                temp[k++] = a[i++];
            } else {
                temp[k++] = a[j++];
            }
        }
        // Если остались элементы в левой части — дописываем их
        while (i <= mid) {
            temp[k++] = a[i++];
        }
        // Если остались элементы в правой части — дописываем их
        while (j <= right) {
            temp[k++] = a[j++];
        }

        // Переносим отсортированный отрезок обратно в исходный массив
        for (int idx = left; idx <= right; idx++) {
            a[idx] = temp[idx];
        }
    }
}

