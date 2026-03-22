/**
 * Общий интерфейс стека.
 * Стек работает по принципу LIFO
 *
 * @param <T> тип элементов, которые хранятся в стеке
 */
public interface SimpleStack<T> extends Iterable<T> {
    void push(T value);
    T pop();
    T peek();
    boolean isEmpty();
    int size();
}

