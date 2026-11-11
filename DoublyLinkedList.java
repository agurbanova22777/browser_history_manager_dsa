public final class DoublyLinkedList {
    private Node head;
    private Node tail;
    private int size;

    // History runs from the oldest visit at head to the newest at tail.
    public Node getHead() {
        return head;
    }

    public Node getTail() {
        return tail;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void append(Node node) {
        insertAfter(tail, node);
    }

    // A null previous node inserts at the head.
    public void insertAfter(Node previous, Node node) {
        if (node == null || node.owner != null) {
            throw new IllegalArgumentException("Expected a detached node");
        }
        if (previous != null) {
            requireMember(previous);
        }

        Node next = previous == null ? head : previous.next;
        node.prev = previous;
        node.next = next;
        node.owner = this;

        if (previous == null) {
            head = node;
        } else {
            previous.next = node;
        }
        if (next == null) {
            tail = node;
        } else {
            next.prev = node;
        }
        size++;
    }

    public Node remove(Node node) {
        requireMember(node);
        Node previous = node.prev;
        Node next = node.next;

        if (previous == null) {
            head = next;
        } else {
            previous.next = next;
        }
        if (next == null) {
            tail = previous;
        } else {
            next.prev = previous;
        }

        // A detached node can be put on the restore stack and inserted later.
        node.prev = null;
        node.next = null;
        node.owner = null;
        size--;
        return node;
    }

    public Node removeFirst() {
        return head == null ? null : remove(head);
    }

    public void clearAfter(Node node) {
        requireMember(node);
        while (node.next != null) {
            remove(node.next);
        }
    }

    public void clear() {
        while (head != null) {
            remove(head);
        }
    }

    private void requireMember(Node node) {
        if (node == null || node.owner != this) {
            throw new IllegalArgumentException("Node is not in this list");
        }
    }
}
