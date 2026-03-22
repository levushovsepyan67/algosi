import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Реализация стека на односвязном списке.
 * Каждый элемент обёрнут в узел Node, который хранит ссылку на следующий узел.
 *
 * Все основные операции выполняются за O(1).
 */
public class LinkedStack<T> implements SimpleStack<T> {

    /**
     * Узел односвязного списка: значение + ссылка на следующий узел.
     * Храним внутренним классом, чтобы не "светить" его наружу.
     */
    private static class Node<E> {
        E value;
        Node<E> next;

        Node(E value, Node<E> next) {
            this.value = value;
            this.next = next;
        }
    }

    /** Ссылка на вершину стека. */
    private Node<T> head;
    /** Текущее количество элементов. */
    private int size;

    @Override
    public void push(T value) {
        head = new Node<>(value, head);
        size++;
    }

    @Override
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Стек пуст");
        }
        T value = head.value;
        head = head.next;
        size--;
        return value;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Стек пуст");
        }
        return head.value;
    }

    @Override
    public boolean isEmpty() {
        return head == null;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Итератор также идёт от вершины к основанию,
     * просто проходя по цепочке узлов.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T val = current.value;
                current = current.next;
                return val;
            }
        };
    }
}

