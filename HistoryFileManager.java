import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Export / import of the history as a simple text file.
 *
 * File format:
 *   #CURRENT=<index of the active page, 0 = oldest, -1 = none>
 *   <url><TAB><timestamp>          (one line per page, oldest first)
 */
public final class HistoryFileManager {

    private HistoryFileManager() { }

    /** What import returns: the new list and which node in it is the active page. */
    public static final class LoadedHistory {
        public final DoublyLinkedList list;
        public final Node current;

        private LoadedHistory(DoublyLinkedList list, Node current) {
            this.list = list;
            this.current = current;
        }
    }

    public static void export(DoublyLinkedList list, Node current, String path) throws IOException {
        Objects.requireNonNull(list, "list");
        int currentIndex = -1;
        int index = 0;
        for (Node n = list.getHead(); n != null; n = n.getNext(), index++) {
            if (n.getUrl().contains("\t") || n.getUrl().contains("\n") || n.getUrl().contains("\r")) {
                throw new IllegalArgumentException("URL contains a tab or line break: " + n.getUrl());
            }
            if (n == current) {
                currentIndex = index;
            }
        }
        try (BufferedWriter out = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8)) {
            out.write("#CURRENT=" + currentIndex);
            out.newLine();
            for (Node n = list.getHead(); n != null; n = n.getNext()) {
                out.write(n.getUrl() + "\t" + n.getVisitedAt());
                out.newLine();
            }
        }
    }

    /**
     * Read a history file into a NEW list (nothing existing is touched, so a bad file
     * can never damage the live history). If the file has more pages than 'capacity',
     * the oldest ones are dropped. BrowserHistory should replace its list and cursor
     * with the returned ones.
     */
    public static LoadedHistory load(String path, int capacity) throws IOException {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        DoublyLinkedList list = new DoublyLinkedList();
        int currentIndex = -2;                    // -2 = header missing
        Node current = null;
        int index = 0;

        try (BufferedReader in = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = in.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("#CURRENT=")) {
                    try {
                        currentIndex = Integer.parseInt(line.substring("#CURRENT=".length()).trim());
                    } catch (NumberFormatException e) {
                        throw new IOException("Line " + lineNo + ": invalid #CURRENT header");
                    }
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length != 2 || parts[0].isBlank()) {
                    throw new IOException("Line " + lineNo + ": expected <url><TAB><timestamp>");
                }
                Instant visitedAt;
                try {
                    visitedAt = Instant.parse(parts[1].trim());
                } catch (DateTimeParseException e) {
                    throw new IOException("Line " + lineNo + ": invalid timestamp");
                }
                Node node = new Node(parts[0], visitedAt);
                list.append(node);
                if (index == currentIndex) {
                    current = node;
                }
                index++;
            }
        }

        // Respect the capacity: keep the newest pages.
        while (list.size() > capacity) {
            Node dropped = list.removeFirst();
            if (dropped == current) {
                current = list.getHead();
            }
        }
        if (current == null) {
            current = list.getTail();             // no / invalid header: newest page is active
        }
        return new LoadedHistory(list, current);
    }
}
