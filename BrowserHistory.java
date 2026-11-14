import java.io.IOException;

public class BrowserHistory {
    private DoublyLinkedList history;
    private Node current;
    private int capacity = 50;
    private final RestoreStack restoreStack = new RestoreStack(); // deleted pages, last deleted = first restored

    public BrowserHistory (String homepage) {
        history = new DoublyLinkedList();

        requireUrl(homepage);
        current = new Node(homepage);

        history.append(current);

    }

    // Extra constructor so a smaller capacity can be used for demos and tests (e.g. 3 pages).
    public BrowserHistory (String homepage, int capacity) {
        this(homepage);
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        this.capacity = capacity;
    }

    public void visit (String url) {
        requireUrl(url);
        if (current != null) {
            history.clearAfter(current);
        }
        Node newNode = new Node(url);
        history.append(newNode);
        current = newNode;

        if (history.size() > capacity) {
            history.removeFirst();
        }
    }

    public String back (int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("Steps cannot be negative.");
        }

        if (current == null) { // to not have NullPointerException
            return null;
        }

        if (steps == 0 || current.getPrev() == null) { 
            return current.getUrl(); 
        }

        current = current.getPrev();
        
        return back(steps - 1);
    }

    public String forward (int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("Steps cannot be negative.");
        }

        if (current == null) { // to not have NullPointerException
            return null;
        }

        if (steps == 0 || current.getNext() == null) {
            return current.getUrl();
        }

        current = current.getNext();

        return forward(steps - 1);
    }

    public String current_page () {
        if (current == null) {
            return "No current page.";
        }
        return "URL: " + current.getUrl() 
        + "\nVisited at: " + current.getVisitedAt();
    }

    public void display_history () {
        if (history.isEmpty()) {
            System.out.println("(history is empty)");
            return;
        }
        displayRecursive(history.getHead());
    }

    private void displayRecursive (Node node) {
        if (node == null) { return; }

        if (node == current) {
            System.out.println("[<] " + node.getUrl());
        } 
        else {
            System.out.println(node.getUrl());
        }
        displayRecursive(node.getNext());
    }

    public void Delete_histroy() { // grammatically following the project requirements (pdf: page 2, line 6)
        HistoryEditor.deleteAll(history, restoreStack); // clears the list AND the restore stack
        current = null;
    }

    // ---------------- delete specific URLs & restore ----------------

    /** Deletes every page with this URL (each one can be restored). Returns how many were deleted. */
    public int deleteUrl (String url) {
        requireUrl(url);
        HistoryEditor.DeleteResult result = HistoryEditor.deleteUrl(history, restoreStack, current, url);
        current = result.current;
        return result.removed;
    }

    /** Restores the most recently deleted page as the newest page. Returns it, or null if nothing to restore. */
    public Node restore () {
        HistoryEditor.RestoreResult result = HistoryEditor.restore(history, restoreStack, current, capacity);
        current = result.current;
        return result.restored;
    }

    // ---------------- export / import (txt file) ----------------

    public void exportHistory (String path) throws IOException {
        HistoryFileManager.export(history, current, path);
    }

    public void importHistory (String path) throws IOException {
        // load() builds a brand-new list first, so a bad file throws before anything is changed
        HistoryFileManager.LoadedHistory loaded = HistoryFileManager.load(path, capacity);
        history.clear();
        restoreStack.clear();
        history = loaded.list;
        current = loaded.current;
    }

    // ---------------- helpers (used by the tests) ----------------

    public int size () {
        return history.size();
    }

    /** URLs from oldest to newest. */
    public String[] historyUrls () {
        String[] urls = new String[history.size()];
        int i = 0;
        for (Node n = history.getHead(); n != null; n = n.getNext()) {
            urls[i++] = n.getUrl();
        }
        return urls;
    }

    private static void requireUrl (String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty.");
        }
    }

}
