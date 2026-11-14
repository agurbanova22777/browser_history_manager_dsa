import java.util.Objects;

/**
 * Delete / restore / delete-all operations on the history.
 *
 * These methods work directly on the DoublyLinkedList and the RestoreStack.
 * The "current page" cursor lives in BrowserHistory, so every method that can
 * affect the cursor takes the current node and returns the NEW current node.
 */
public final class HistoryEditor {

    private HistoryEditor() { }   // utility class, no instances

    /** Result of deleteUrl: how many pages were removed and where the cursor is now. */
    public static final class DeleteResult {
        public final int removed;
        public final Node current;

        private DeleteResult(int removed, Node current) {
            this.removed = removed;
            this.current = current;
        }
    }

    /** Result of restore: the restored page (null if nothing to restore) and the cursor. */
    public static final class RestoreResult {
        public final Node restored;
        public final Node current;

        private RestoreResult(Node restored, Node current) {
            this.restored = restored;
            this.current = current;
        }
    }

    /**
     * Delete EVERY page whose URL equals 'url'. Each removed node is pushed onto the
     * restore stack. If the active page is deleted, the cursor moves to the previous
     * page (or to the next page when there is no previous one; null if the list is empty).
     */
    public static DeleteResult deleteUrl(DoublyLinkedList list, RestoreStack stack,
                                         Node current, String url) {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(url, "url");

        int removed = 0;
        Node node = list.getHead();
        while (node != null) {
            Node next = node.getNext();          // save BEFORE remove() clears the links
            if (node.getUrl().equals(url)) {
                if (node == current) {
                    current = node.getPrev() != null ? node.getPrev() : next;
                }
                list.remove(node);
                stack.push(node);
                removed++;
            }
            node = next;
        }
        return new DeleteResult(removed, current);
    }

    /**
     * Restore the most recently deleted page by adding it back as the NEWEST page (tail).
     * If the history is already full, the oldest page is pruned first.
     * The cursor does not move, unless the history was empty (then the restored page becomes active).
     */
    public static RestoreResult restore(DoublyLinkedList list, RestoreStack stack,
                                        Node current, int capacity) {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(stack, "stack");
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }

        Node node = stack.pop();
        if (node == null) {
            return new RestoreResult(null, current);
        }
        while (list.size() >= capacity) {
            Node oldest = list.getHead();
            if (oldest == current) {
                current = oldest.getNext();      // never leave the cursor on a removed node
            }
            list.removeFirst();
        }
        list.append(node);
        if (current == null) {
            current = node;
        }
        return new RestoreResult(node, current);
    }

    /** Delete_history(): remove all pages and forget the restore stack. The cursor becomes null. */
    public static void deleteAll(DoublyLinkedList list, RestoreStack stack) {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(stack, "stack");
        list.clear();
        stack.clear();
    }
}
