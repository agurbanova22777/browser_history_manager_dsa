import java.time.Instant;
import java.util.Objects;

public final class Node {
    private final String url;
    private final Instant visitedAt;

    Node prev;
    Node next;
    DoublyLinkedList owner;

    public Node(String url) {
        this(url, Instant.now());
    }

    public Node(String url, Instant visitedAt) {
        this.url = Objects.requireNonNull(url, "url");
        this.visitedAt = Objects.requireNonNull(visitedAt, "visitedAt");
    }

    public String getUrl() {
        return url;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }

    public Node getPrev() {
        return prev;
    }

    public Node getNext() {
        return next;
    }
}
