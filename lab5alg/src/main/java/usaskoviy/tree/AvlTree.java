package usaskoviy.tree; // пакет с AVL и BST для лабораторной работы №6

/**
 * AVL-дерево: сбалансированное BST; высота O(log N), глубина рекурсии приемлема при N до 2^20.
 */
public final class AvlTree {

    /** Узел AVL: ключ, высота поддерева в этом узле, левый и правый потомки. */
    private static final class Node {
        int key; // ключ сортировки
        int height; // высота поддерева с корнем в этом узле (лист = 1)
        Node left; // левое поддерево
        Node right; // правое поддерево

        Node(int key) {
            this.key = key; // сохраняем ключ
            this.height = 1; // новый узел — лист, высота 1 (дети null считаются высотой 0)
        }
    }

    private Node root; // корень AVL-дерева

    /** Делает дерево пустым. */
    public void clear() {
        root = null; // отвязываем корень
    }

    /** Высота поддерева: у null — 0, иначе поле height в узле. */
    private static int height(Node n) {
        return n == null ? 0 : n.height; // null не хранит высоту — считаем 0
    }

    /** Пересчитывает height узла n по высотам детей: 1 + max(лево, право). */
    private static void updateHeight(Node n) {
        n.height = 1 + Math.max(height(n.left), height(n.right)); // классическая формула высоты узла
    }

    /** Разница высот левого и правого поддеревьев (фактор баланса по определению AVL). */
    private static int balance(Node n) {
        return n == null ? 0 : height(n.left) - height(n.right); // для null баланс 0
    }

    /**
     * Правый поворот вокруг y (малый поворот при перекосе влево).
     * x = y.left; поддерево перецепляется так, чтобы сохранить порядок ключей BST.
     */
    private static Node rotateRight(Node y) {
        Node x = y.left; // новый «верхний» узел после поворота
        Node t2 = x.right; // правое поддерево x временно отрываем
        x.right = y; // y становится правым ребёнком x
        y.left = t2; // бывшее правое поддерево x вешаем слева от y
        updateHeight(y); // пересчёт высот снизу вверх
        updateHeight(x);
        return x; // возвращаем новый корень поддерева
    }

    /**
     * Левый поворот вокруг x (зеркально rotateRight).
     */
    private static Node rotateLeft(Node x) {
        Node y = x.right; // новый корень поддерева после поворота
        Node t2 = y.left; // левое поддерево y отрываем
        y.left = x; // x становится левым ребёнком y
        x.right = t2; // бывшее левое поддерево y — справа от x
        updateHeight(x);
        updateHeight(y);
        return y; // новый корень
    }

    /**
     * Восстанавливает свойство AVL у узла n после вставки/удаления в потомках.
     * Рассматриваются случаи LL, LR, RR, RL по знаку баланса и балансу детей.
     */
    private static Node rebalance(Node n) {
        updateHeight(n); // сначала актуализируем высоту n
        int b = balance(n); // текущий перекос
        if (b > 1 && balance(n.left) >= 0) { // LL: левое поддерево выше, внутри левого — левый наклон
            return rotateRight(n); // один правый поворот
        }
        if (b > 1) { // LR: левое выше, но левый ребёнок наклонён вправо
            n.left = rotateLeft(n.left); // сначала левый поворот у левого ребёнка
            return rotateRight(n); // затем правый у n
        }
        if (b < -1 && balance(n.right) <= 0) { // RR: зеркально LL
            return rotateLeft(n);
        }
        if (b < -1) { // RL: зеркально LR
            n.right = rotateRight(n.right);
            return rotateLeft(n);
        }
        return n; // баланс в допустимых пределах — повороты не нужны
    }

    /** Публичная вставка: перезаписывает корень результатом рекурсивной вставки. */
    public void insert(int key) {
        root = insert(root, key); // вставка с балансировкой от корня
    }

    /** Рекурсивная вставка в поддерево с корнем node; возвращает (возможно новый) корень поддерева. */
    private static Node insert(Node node, int key) {
        if (node == null) {
            return new Node(key); // пустое место — создаём лист
        }
        if (key < node.key) {
            node.left = insert(node.left, key); // идём влево
        } else if (key > node.key) {
            node.right = insert(node.right, key); // идём вправо
        } else {
            return node; // дубликат — дерево не меняем
        }
        return rebalance(node); // на пути вверх балансируем предков
    }

    /** Итеративный поиск (как в BST), глубина O(log N). */
    public boolean contains(int key) {
        Node cur = root; // текущий узел
        while (cur != null) { // спуск до листа или нахождения ключа
            if (key < cur.key) {
                cur = cur.left; // влево
            } else if (key > cur.key) {
                cur = cur.right; // вправо
            } else {
                return true; // найдено
            }
        }
        return false; // ключ не встретился
    }

    /** Удаление ключа из дерева. */
    public void delete(int key) {
        root = delete(root, key); // результат — новый корень
    }

    /** Минимальный ключ в поддереве с корнем n — самый левый узел. */
    private static Node minNode(Node n) {
        while (n.left != null) { // идём влево пока возможно
            n = n.left;
        }
        return n; // узел с минимальным ключом в поддереве
    }

    /**
     * Рекурсивное удаление: стандартные три случая BST + rebalance на обратном пути.
     */
    private static Node delete(Node node, int key) {
        if (node == null) {
            return null; // поддерево пустое — удалять нечего
        }
        if (key < node.key) {
            node.left = delete(node.left, key); // удаление только в левом поддереве
        } else if (key > node.key) {
            node.right = delete(node.right, key); // только справа
        } else {
            // Узел с ключом key найден
            if (node.left == null) {
                return node.right; // нет левого — подтягиваем правое поддерево
            }
            if (node.right == null) {
                return node.left; // нет правого — подтягиваем левое
            }
            Node succ = minNode(node.right); // преемник — минимум справа
            node.key = succ.key; // заменяем ключ на преемника (удаление по значению в этой вершине)
            node.right = delete(node.right, succ.key); // удаляем дубликат ключа в правом поддереве
        }
        return rebalance(node); // после изменения высот — балансировка
    }
}
