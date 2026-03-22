public class ShakerSort {
    public static class Result {
        /** Количество проходов по массиву (каждый проход влево или вправо считается отдельно). */
        public final int passes;
        /** Количество операций обмена двух элементов (swap). */
        public final int swaps;

        /** Конструктор: сохраняем переданные значения в поля (они потом не меняются — final). */
        public Result(int passes, int swaps) {
            this.passes = passes;
            this.swaps = swaps;
        }
    }

    /**
     * Сортирует массив, а по возрастанию (от меньшего к большему) и возвращает метрики.
     * Массив изменяется «на месте» — дополнительный массив не создаётся.
     *
     * @param a массив чисел с плавающей запятой для сортировки
     * @return Result с полями passes (проходы) и swaps (обмены)
     */
    public static Result sort(double[] a) {
        int swaps = 0;
        int passes = 0;
        int left = 0;
        int right = a.length - 1;

        // Цикл пока границы не сошлись — тогда весь массив отсортирован.
        while (left < right) {
            for (int i = left; i < right; i++) {
                if (a[i] > a[i + 1]) {
                    swap(a, i, i + 1);
                    swaps++;
                }
            }
            right--;
            passes++;

            if (left >= right) break;

            for (int i = right; i > left; i--) {
                if (a[i - 1] > a[i]) {
                    swap(a, i - 1, i);
                    swaps++;
                }
            }
            left++;
            passes++;
        }

        return new Result(passes, swaps);
    }

    private static void swap(double[] a, int i, int j) {
        double t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}