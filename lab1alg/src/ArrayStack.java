import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Реализация стека на массиве.
 * Внутри хранится обычный динамически расширяемый массив Object[].
 *
 * Операции push/pop работают за O(1) амортизированно.
 */
public class ArrayStack<T> implements SimpleStack<T> {

    /** Массив для хранения элементов стека. */
    private Object[] data;
    /** Индекс следующей свободной позиции (также равен размеру стека). */
    private int size;

    public ArrayStack() {
        this.data = new Object[16];
        this.size = 0;
    }

    @Override
    public void push(T value) {
        // При переполнении массива увеличиваем его в два раза
        if (size == data.length) {
            Object[] newData = new Object[data.length * 2];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
        data[size++] = value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Стек пуст");
        }
        size--;
        T value = (T) data[size];
        data[size] = null; // помогаем GC
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Стек пуст");
        }
        return (T) data[size - 1];
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Итератор обходит стек от вершины к основанию.
     * Это удобно для отладки и демонстрации.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = size - 1;

            @Override
            public boolean hasNext() {
                return index >= 0;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return (T) data[index--];
            }
        };
    }
}

