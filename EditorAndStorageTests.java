import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Random;

/** Tests for delete/restore/delete-all and export/import. Run:  java EditorAndStorageTests */
public class EditorAndStorageTests {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else    { failed++; System.out.println("  FAIL  " + name); }
    }
    static void eq(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(ok ? name : name + "  (expected " + expected + ", got " + actual + ")", ok);
    }

    // ---- helpers ----
    static DoublyLinkedList listOf(String... urls) {
        DoublyLinkedList l = new DoublyLinkedList();
        for (String u : urls) l.append(new Node(u));
        return l;
    }
    static String str(DoublyLinkedList l) {
        StringBuilder sb = new StringBuilder();
        for (Node n = l.getHead(); n != null; n = n.getNext()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(n.getUrl());
        }
        return sb.toString();
    }
    static Node find(DoublyLinkedList l, String url) {
        for (Node n = l.getHead(); n != null; n = n.getNext()) if (n.getUrl().equals(url)) return n;
        return null;
    }
    static String url(Node n) { return n == null ? null : n.getUrl(); }

    /** Walks forward and backward and checks size, owner and every prev/next pair. */
    static boolean intact(DoublyLinkedList l) {
        int count = 0;
        Node before = null;
        for (Node n = l.getHead(); n != null; n = n.getNext()) {
            if (n.getPrev() != before || n.owner != l) return false;
            before = n;
            if (++count > l.size()) return false;
        }
        if (before != l.getTail() || count != l.size()) return false;
        count = 0;
        Node after = null;
        for (Node n = l.getTail(); n != null; n = n.getPrev()) {
            if (n.getNext() != after) return false;
            after = n;
            if (++count > l.size()) return false;
        }
        return after == l.getHead() && count == l.size();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("-- deleteUrl");
        DoublyLinkedList l = listOf("A", "B", "C");
        RestoreStack s = new RestoreStack();
        Node cur = find(l, "C");
        HistoryEditor.DeleteResult d = HistoryEditor.deleteUrl(l, s, cur, "B");
        eq("delete middle page", "A,C", str(l));
        eq("removed count", 1, d.removed);
        eq("cursor unchanged", "C", url(d.current));
        check("list intact", intact(l));
        check("node pushed to stack", !s.isEmpty() && s.peek().getUrl().equals("B"));

        l = listOf("A", "B", "C"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, find(l, "B"), "B");
        eq("deleting active page moves cursor to previous", "A", url(d.current));
        l = listOf("A", "B", "C"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, find(l, "A"), "A");
        eq("deleting active head moves cursor to next", "B", url(d.current));
        l = listOf("A", "B", "C"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, find(l, "C"), "C");
        eq("deleting active tail moves cursor to previous", "B", url(d.current));
        eq("tail deleted", "A,B", str(l));
        check("intact after tail delete", intact(l));

        l = listOf("A"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, find(l, "A"), "A");
        check("deleting the only page leaves empty list and null cursor", l.isEmpty() && d.current == null && intact(l));

        l = listOf("A", "B"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, find(l, "B"), "nope");
        check("deleting a missing URL changes nothing", d.removed == 0 && str(l).equals("A,B") && s.isEmpty());

        l = listOf("A", "B", "A", "C", "A"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, find(l, "C"), "A");
        eq("all duplicates deleted", "B,C", str(l));
        eq("duplicate count", 3, d.removed);
        eq("cursor stays on C", "C", url(d.current));
        check("intact after duplicates", intact(l));

        l = listOf("A", "A", "A"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, l.getHead().getNext(), "A");
        check("cursor never left on a removed node (all pages deleted)", d.current == null && l.isEmpty());

        boolean threw = false;
        try { HistoryEditor.deleteUrl(l, s, null, null); } catch (NullPointerException e) { threw = true; }
        check("null url rejected", threw);

        System.out.println("-- restore");
        l = listOf("A", "B", "C"); s = new RestoreStack();
        cur = find(l, "C");
        HistoryEditor.deleteUrl(l, s, cur, "B");
        HistoryEditor.RestoreResult r = HistoryEditor.restore(l, s, cur, 50);
        eq("restored page returned", "B", url(r.restored));
        eq("restored as newest page", "A,C,B", str(l));
        eq("cursor does not move", "C", url(r.current));
        check("intact after restore", intact(l));
        r = HistoryEditor.restore(l, s, cur, 50);
        check("restore on empty stack does nothing", r.restored == null && str(l).equals("A,C,B") && r.current == cur);

        l = listOf("A", "B", "C", "D"); s = new RestoreStack();
        cur = find(l, "D");
        HistoryEditor.deleteUrl(l, s, cur, "B");
        HistoryEditor.deleteUrl(l, s, cur, "C");
        r = HistoryEditor.restore(l, s, cur, 50);
        eq("LIFO: last deleted restored first", "C", url(r.restored));
        r = HistoryEditor.restore(l, s, cur, 50);
        eq("then the earlier one", "B", url(r.restored));
        eq("order after both restores", "A,D,C,B", str(l));

        l = listOf("A"); s = new RestoreStack();
        d = HistoryEditor.deleteUrl(l, s, find(l, "A"), "A");
        r = HistoryEditor.restore(l, s, d.current, 50);
        check("restore into empty history makes it active", "A".equals(url(r.current)) && l.size() == 1);

        l = listOf("A", "B", "C"); s = new RestoreStack();
        cur = find(l, "C");
        HistoryEditor.deleteUrl(l, s, cur, "B");        // A,C  (stack: B)
        l.append(new Node("D"));                         // A,C,D  -> full for capacity 3
        r = HistoryEditor.restore(l, s, cur, 3);
        eq("full history prunes oldest on restore", "C,D,B", str(l));
        eq("size respects capacity", 3, l.size());
        check("intact after pruning restore", intact(l));

        l = listOf("A", "B", "C"); s = new RestoreStack();
        cur = find(l, "A");
        HistoryEditor.deleteUrl(l, s, find(l, "B"), "B");   // A,C  (stack: B)
        l.append(new Node("D"));                             // A,C,D
        r = HistoryEditor.restore(l, s, cur, 3);
        eq("cursor on pruned oldest page moves forward", "C", url(r.current));
        check("cursor is in the list", r.current.owner == l);

        l = listOf("A"); s = new RestoreStack();
        cur = find(l, "A");
        s.push(new Node("Z"));
        r = HistoryEditor.restore(l, s, cur, 1);
        eq("capacity 1: restored page replaces the old one", "Z", str(l));
        eq("capacity 1: cursor on the only page", "Z", url(r.current));
        check("bad capacity rejected", throwsIAE(() -> HistoryEditor.restore(listOf("A"), new RestoreStack(), null, 0)));

        System.out.println("-- deleteAll");
        l = listOf("A", "B", "C"); s = new RestoreStack();
        HistoryEditor.deleteUrl(l, s, null, "B");
        HistoryEditor.deleteAll(l, s);
        check("list empty", l.isEmpty() && l.getHead() == null && l.getTail() == null);
        check("restore stack cleared", s.isEmpty());
        l.append(new Node("N"));
        check("list usable after deleteAll", str(l).equals("N") && intact(l));

        System.out.println("-- export / import");
        File f = File.createTempFile("hist", ".txt");
        f.deleteOnExit();
        l = listOf("https://a.com", "https://b.com/x?y=1&z=2", "https://c.com");
        Node active = find(l, "https://b.com/x?y=1&z=2");
        HistoryFileManager.export(l, active, f.getPath());
        HistoryFileManager.LoadedHistory h = HistoryFileManager.load(f.getPath(), 50);
        eq("pages round trip", str(l), str(h.list));
        eq("cursor round trip", "https://b.com/x?y=1&z=2", url(h.current));
        check("timestamps round trip", h.list.getHead().getVisitedAt().equals(l.getHead().getVisitedAt()));
        check("loaded list intact", intact(h.list) && h.current.owner == h.list);

        h = HistoryFileManager.load(f.getPath(), 2);
        eq("import keeps newest pages when over capacity", "https://b.com/x?y=1&z=2,https://c.com", str(h.list));
        eq("cursor kept when it survives pruning", "https://b.com/x?y=1&z=2", url(h.current));
        HistoryFileManager.export(l, find(l, "https://a.com"), f.getPath());
        h = HistoryFileManager.load(f.getPath(), 2);
        eq("cursor on pruned page moves to oldest kept page", "https://b.com/x?y=1&z=2", url(h.current));
        h = HistoryFileManager.load(f.getPath(), 1);
        check("capacity 1 import", str(h.list).equals("https://c.com") && h.current == h.list.getHead());

        HistoryFileManager.export(new DoublyLinkedList(), null, f.getPath());
        h = HistoryFileManager.load(f.getPath(), 50);
        check("empty history round trip", h.list.isEmpty() && h.current == null);

        write(f, "A\t" + Instant.now() + "\nB\t" + Instant.now() + "\n");
        h = HistoryFileManager.load(f.getPath(), 50);
        eq("missing header: newest page active", "B", url(h.current));
        write(f, "#CURRENT=99\nA\t" + Instant.now() + "\nB\t" + Instant.now() + "\n");
        h = HistoryFileManager.load(f.getPath(), 50);
        eq("out-of-range header: newest page active", "B", url(h.current));
        write(f, "\n#CURRENT=0\n\nA\t" + Instant.now() + "\n\n");
        h = HistoryFileManager.load(f.getPath(), 50);
        eq("blank lines ignored", "A", str(h.list));

        check("line without timestamp rejected", loadFails(f, "#CURRENT=0\nA\n"));
        check("bad timestamp rejected", loadFails(f, "#CURRENT=0\nA\tyesterday\n"));
        check("bad header rejected", loadFails(f, "#CURRENT=abc\nA\t" + Instant.now() + "\n"));
        check("extra column rejected", loadFails(f, "A\t" + Instant.now() + "\textra\n"));
        threw = false;
        try { HistoryFileManager.load("/no/such/file.txt", 50); } catch (IOException e) { threw = true; }
        check("missing file reported as IOException", threw);
        check("bad capacity rejected on load", throwsIAE(() -> { try { HistoryFileManager.load(f.getPath(), 0); } catch (IOException e) { } }));
        DoublyLinkedList bad = listOf("has\ttab");
        check("URL with tab rejected on export", throwsIAE(() -> { try { HistoryFileManager.export(bad, null, f.getPath()); } catch (IOException e) { } }));

        System.out.println("-- randomized stress test (3000 operations)");
        Random rnd = new Random(7);
        DoublyLinkedList sl = listOf("start");
        RestoreStack ss = new RestoreStack();
        Node sc = sl.getHead();
        int capacity = 6;
        boolean ok = true;
        for (int i = 0; i < 3000 && ok; i++) {
            switch (rnd.nextInt(5)) {
                case 0: case 1: {                          // simulate visit
                    if (sc != null) sl.clearAfter(sc);
                    Node n = new Node("u" + rnd.nextInt(8));
                    sl.append(n);
                    sc = n;
                    while (sl.size() > capacity) sl.removeFirst();
                    break;
                }
                case 2: sc = HistoryEditor.deleteUrl(sl, ss, sc, "u" + rnd.nextInt(8)).current; break;
                case 3: sc = HistoryEditor.restore(sl, ss, sc, capacity).current; break;
                default:
                    if (rnd.nextInt(40) == 0) { HistoryEditor.deleteAll(sl, ss); sc = null; }
                    break;
            }
            ok = intact(sl) && sl.size() <= capacity && (sl.isEmpty() ? sc == null : sc != null && sc.owner == sl);
        }
        check("list intact, cursor valid, capacity respected after every operation", ok);

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void write(File f, String text) throws IOException {
        try (FileWriter w = new FileWriter(f)) { w.write(text); }
    }
    static boolean loadFails(File f, String content) throws IOException {
        write(f, content);
        try { HistoryFileManager.load(f.getPath(), 50); return false; } catch (IOException e) { return true; }
    }
    static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }
}
