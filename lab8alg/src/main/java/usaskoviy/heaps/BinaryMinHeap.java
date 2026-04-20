package usaskoviy.heaps; // пакет лабораторной по кучам

/**
 * Бинарная min-куча на массиве: родитель не больше детей, минимум всегда в корне [0].
 */
public final class BinaryMinHeap {

    private int[] heap; // хранилище: полное бинарное дерево «слоями» в массиве
    private int size; // сколько элементов сейчас в куче (индексы 0..size-1)

    public BinaryMinHeap(int capacity) {
        this.heap = new int[Math.max(16, capacity)]; // запас по размеру, минимум 16
        this.size = 0; // изначально пусто
    }

    public int size() {
        return size; // текущее число элементов
    }

    public void clear() {
        size = 0; // «очистка» без перевыделения массива — просто считаем кучу пустой
    }

    public void insert(int key) {
        ensure(size + 1); // при необходимости расширяем массив под новый элемент
        heap[size] = key; // новый ключ кладём в конец (лист)
        siftUp(size); // проталкиваем вверх, пока не выполнится порядок min-кучи
        size++; // увеличиваем число элементов после фиксации позиции
    }

    public int findMin() {
        if (size == 0) {
            throw new IllegalStateException("empty"); // минимума нет в пустой куче
        }
        return heap[0]; // в min-куче минимум всегда в корне массива
    }

    public int deleteMin() {
        if (size == 0) {
            throw new IllegalStateException("empty");
        }
        int min = heap[0]; // запоминаем минимум (корень)
        size--; // уменьшаем размер: последний элемент станет новым корнем
        if (size > 0) {
            heap[0] = heap[size]; // переносим последний элемент в корень
            siftDown(0); // просеиваем вниз до восстановления свойства кучи
        }
        return min; // возвращаем старый минимум
    }

    private void siftUp(int i) {
        while (i > 0) { // пока не дошли до корня
            int p = (i - 1) >>> 1; // индекс родителя в массиве (целочисленное деление на 2)
            if (heap[i] >= heap[p]) {
                break; // свойство min-кучи уже выполняется
            }
            swap(i, p); // меняем с родителем
            i = p; // поднимаемся выше
        }
    }

    private void siftDown(int i) {
        while (true) {
            int l = i * 2 + 1; // левый потомок в куче на массиве
            int r = l + 1; // правый потомок
            int smallest = i; // индекс узла с наименьшим ключом среди i и детей
            if (l < size && heap[l] < heap[smallest]) {
                smallest = l; // минимум среди текущего и левого ребёнка
            }
            if (r < size && heap[r] < heap[smallest]) {
                smallest = r; // учитываем правого ребёнка
            }
            if (smallest == i) {
                break; // уже ниже опускаться не нужно
            }
            swap(i, smallest); // текущий узел опускаем, меньший поднимается
            i = smallest; // продолжаем просеивание с новой позиции
        }
    }

    private void swap(int i, int j) {
        int t = heap[i]; // обмен двух элементов массива (без отдельной функции Objects)
        heap[i] = heap[j];
        heap[j] = t;
    }

    private void ensure(int need) {
        if (need <= heap.length) {
            return; // места хватает
        }
        int n = heap.length; // текущая ёмкость
        while (n < need) {
            n <<= 1; // удваиваем, пока не покроем need
        }
        int[] next = new int[n]; // новый массив большего размера
        System.arraycopy(heap, 0, next, 0, size); // копируем только занятые ячейки
        heap = next; // переключаемся на новый буфер
    }
}
