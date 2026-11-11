import java.util.Objects;

public final class RestoreStack {
    private Entry top;

    // Stack entries keep their own links so the history links stay untouched.
    private static final class Entry {
        private final Node node;
        private final Entry next;

        private Entry(Node node, Entry next) {
            this.node = node;
            this.next = next;
        }
    }

    public void push(Node node) {
        Objects.requireNonNull(node, "node");
        if (node.owner != null) {
            throw new IllegalArgumentException("Remove the node from history first");
        }
        top = new Entry(node, top);
    }

    public Node peek() {
        return top == null ? null : top.node;
    }

    public Node pop() {
        if (top == null) {
            return null;
        }
        Node node = top.node;
        top = top.next;
        return node;
    }

    public boolean isEmpty() {
        return top == null;
    }

    public void clear() {
        top = null;
    }
}
