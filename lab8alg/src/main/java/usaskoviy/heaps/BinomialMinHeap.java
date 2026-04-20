package usaskoviy.heaps; // пакет лабораторной по кучам

import java.util.Map; // для карты «степень дерева → корень» при объединении
import java.util.TreeMap; // отсортированная карта по степени (для переноса как в двоичном сложении)

/**
 * Биномиальная min-куча: набор биномиальных деревьев; у каждого корня своя степень k (размер B_k = 2^k).
 * Слияние двух куч — как сложение двоичных чисел, совпадающие степени «связываются» в дерево степени k+1.
 */
public final class BinomialMinHeap {

    /** Узел: ключ; order — степень биномиального дерева с корнем в этом узле; child/sibling — связный список детей и соседних корней. */
    private static final class Node {
        int key; // приоритет (для min-кучи ищем минимальный ключ)
        int order; // степень биномиального дерева (0 = один узел)
        Node child; // указатель на первого ребёнка (поддеревья меньшей степени)
        Node sibling; // следующий корень в списке корней или следующий брат среди детей
    }

    private Node head; // голова списка корней биномиальных деревьев (после операций степени не повторяются)

    public void clear() {
        head = null; // обнуляем список корней — куча пуста
    }

    public boolean isEmpty() {
        return head == null; // пусто, если нет ни одного корня
    }

    public void insert(int key) {
        Node n = new Node(); // новое дерево B_0 — один узел
        n.key = key;
        n.order = 0;
        head = mergeSortedByDegree(head, n); // вливаем в общий отсортированный по степени список корней
        head = unionCarryFromList(head); // устраняем дубликаты степеней (перенос / связывание)
    }

    public int findMin() {
        if (head == null) {
            throw new IllegalStateException("empty"); // в пустой куче минимума нет
        }
        int min = head.key; // кандидат — первый корень в списке
        for (Node cur = head.sibling; cur != null; cur = cur.sibling) {
            if (cur.key < min) {
                min = cur.key; // минимум может быть у любого корня в списке
            }
        }
        return min; // глобальный минимум среди корней (в поддеревьях ключи не меньше по свойству кучи)
    }

    public int deleteMin() {
        if (head == null) {
            throw new IllegalStateException("empty");
        }
        Node prevMin = null; // узел перед минимальным корнем (для вырезания из списка)
        Node minNode = head; // текущий лучший минимум
        Node prev = null; // предыдущий при обходе списка корней
        for (Node cur = head; cur != null; prev = cur, cur = cur.sibling) {
            if (cur.key < minNode.key) {
                minNode = cur; // нашли более малый корень
                prevMin = prev; // запоминаем предыдущего для связи списка
            }
        }
        if (prevMin == null) {
            head = head.sibling; // минимум был первым — сдвигаем голову
        } else {
            prevMin.sibling = minNode.sibling; // выкидываем minNode из списка корней
        }
        Node rest = head; // оставшиеся корни после удаления minNode
        Node childList = minNode.child; // дети удалённого корня — отдельные биномиальные деревья
        minNode.child = null; // обнуляем ссылки у вырезанного узла
        minNode.sibling = null;
        Node childHeap = reverse(childList); // разворачиваем список детей в порядок, удобный для слияния
        head = mergeSortedByDegreeLists(rest, childHeap); // объединяем два набора корней
        head = unionCarryFromList(head); // снова убираем совпадения степеней
        return minNode.key; // возвращаем удалённый минимум
    }

    /** Разворот односвязного списка по полю sibling (дети корня после extract). */
    private static Node reverse(Node node) {
        Node prev = null;
        Node cur = node;
        while (cur != null) {
            Node nxt = cur.sibling; // следующий в цепочке
            cur.sibling = prev; // переворачиваем указатель
            prev = cur;
            cur = nxt;
        }
        return prev; // новая голова развёрнутого списка
    }

    /** Слияние двух списков корней: степени по возрастанию (как при слиянии отсортированных списков). */
    private static Node mergeSortedByDegreeLists(Node a, Node b) {
        if (a == null) {
            return b; // второй список целиком
        }
        if (b == null) {
            return a; // первый список целиком
        }
        Node out; // голова результата
        if (a.order < b.order) {
            out = a; // меньшая степень идёт первой
            a = a.sibling;
        } else {
            out = b;
            b = b.sibling;
        }
        Node tail = out; // хвост для прицепления
        while (a != null && b != null) {
            if (a.order < b.order) {
                tail.sibling = a;
                a = a.sibling;
            } else {
                tail.sibling = b;
                b = b.sibling;
            }
            tail = tail.sibling; // двигаем хвост
        }
        tail.sibling = a != null ? a : b; // дописываем остаток одного из списков
        return out;
    }

    /** Вставка одного нового дерева B_0 в уже отсортированный по степени список корней. */
    private static Node mergeSortedByDegree(Node list, Node single) {
        single.sibling = null; // изолируем одиночный узел
        return mergeSortedByDegreeLists(list, single);
    }

    /**
     * Проход по списку корней: если два дерева одной степени — связываем в одно (меньший ключ — корень нового B_{k+1}).
     * Аналог переноса при сложении: пока есть пара одинаковой степени — объединяем.
     */
    private static Node unionCarryFromList(Node h) {
        if (h == null) {
            return null; // пустой вход
        }
        TreeMap<Integer, Node> byDeg = new TreeMap<>(); // временно: степень → единственный корень этой степени
        Node cur = h;
        while (cur != null) {
            Node next = cur.sibling; // сохраняем следующий до изменения связей
            cur.sibling = null; // отвязываем текущий корень
            insertWithCarry(byDeg, cur); // кладём в «карту переносов»
            cur = next;
        }
        return mapToRootList(byDeg); // собираем обратно связный список корней по возрастанию степени
    }

    /** Добавление дерева: при конфликте степени — link с уже лежащим, повторять пока степень уникальна. */
    private static void insertWithCarry(Map<Integer, Node> byDeg, Node t) {
        int d = t.order; // текущая степень корня t
        while (byDeg.containsKey(d)) {
            Node other = byDeg.remove(d); // второе дерево той же степени
            t = linkTrees(t, other); // получаем одно дерево степени d+1
            d = t.order;
        }
        byDeg.put(d, t); // фиксируем итоговое дерево данной степени
    }

    /** Слияние двух биномиальных деревьев одинаковой степени: меньший ключ — корень, второй — старший ребёнок. */
    private static Node linkTrees(Node a, Node b) {
        if (a.key > b.key) {
            Node tmp = a; // гарантируем: a — узел с меньшим ключом (корень min-кучи)
            a = b;
            b = tmp;
        }
        b.sibling = a.child; // бывшее поддерево цепляем к списку детей a
        a.child = b; // b становится крайним ребёнком
        a.order++; // степень корня выросла на 1
        return a;
    }

    /** Собираем связный список корней из значений TreeMap (уже по возрастанию степени). */
    private static Node mapToRootList(Map<Integer, Node> byDeg) {
        Node head = null;
        Node tail = null;
        for (Node v : byDeg.values()) {
            v.sibling = null; // обнуляем перед склейкой
            if (head == null) {
                head = tail = v; // первый корень
            } else {
                tail.sibling = v; // прицепляем следующий корень
                tail = v;
            }
        }
        return head;
    }
}
