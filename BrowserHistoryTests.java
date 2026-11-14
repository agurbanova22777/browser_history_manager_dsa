import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

/** System-level tests for the whole BrowserHistory. Run:  java BrowserHistoryTests */
public class BrowserHistoryTests {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else    { failed++; System.out.println("  FAIL  " + name); }
    }
    static void eq(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(ok ? name : name + "  (expected " + expected + ", got " + actual + ")", ok);
    }
    static String h(BrowserHistory b) { return String.join(",", b.historyUrls()); }
    static BrowserHistory make(int capacity, String... urls) {
        BrowserHistory b = new BrowserHistory(urls[0], capacity);
        for (int i = 1; i < urls.length; i++) b.visit(urls[i]);
        return b;
    }
    static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }
    static String captureDisplay(BrowserHistory b) {
        PrintStream old = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try { b.display_history(); } finally { System.setOut(old); }
        return buf.toString().replace("\r", "");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("-- constructor / visit");
        BrowserHistory b = new BrowserHistory("A");
        eq("homepage is the only page", "A", h(b));
        b.visit("B"); b.visit("C");
        eq("visit appends", "A,B,C", h(b));
        check("current_page shows newest URL", b.current_page().contains("URL: C"));
        check("current_page shows metadata", b.current_page().contains("Visited at:"));
        check("current_page does not change state", b.current_page().equals(b.current_page()) && h(b).equals("A,B,C"));
        check("blank URL rejected", throwsIAE(() -> b.visit("   ")));
        check("null URL rejected", throwsIAE(() -> b.visit(null)));
        check("bad homepage rejected", throwsIAE(() -> new BrowserHistory("")));
        check("bad capacity rejected", throwsIAE(() -> new BrowserHistory("A", 0)));

        System.out.println("-- back / forward");
        eq("back(1)", "B", b.back(1));
        eq("back(1) again", "A", b.back(1));
        eq("back past the oldest page stops at oldest", "A", b.back(5));
        eq("forward(1)", "B", b.forward(1));
        eq("forward past the newest page stops at newest", "C", b.forward(100));
        eq("back(0) stays", "C", b.back(0));
        eq("forward(0) stays", "C", b.forward(0));
        check("negative steps rejected", throwsIAE(() -> b.back(-1)) && throwsIAE(() -> b.forward(-1)));
        BrowserHistory two = make(50, "A", "B");
        String last = null;
        for (int i = 0; i < 100; i++) last = two.back(1);
        eq("back() called 100 times on 2 pages", "A", last);
        eq("back(100) on 2 pages", "A", two.back(100));
        eq("back(Integer.MAX_VALUE) is safe", "A", two.back(Integer.MAX_VALUE));
        eq("forward(Integer.MAX_VALUE) is safe", "B", two.forward(Integer.MAX_VALUE));
        eq("history unchanged by navigation", "A,B", h(two));
        BrowserHistory one = new BrowserHistory("only");
        eq("back on a single page", "only", one.back(3));
        eq("forward on a single page", "only", one.forward(3));

        System.out.println("-- branching");
        BrowserHistory br = make(50, "A", "B", "C");
        br.back(2);
        br.visit("X");
        eq("visit from the middle drops forward history", "A,X", h(br));
        eq("nothing to go forward to", "X", br.forward(5));
        eq("can go back to the branch point", "A", br.back(1));
        BrowserHistory br2 = make(50, "A", "B", "C", "D");
        br2.back(1);
        br2.visit("E");
        eq("branch from second-to-last page", "A,B,C,E", h(br2));
        BrowserHistory br3 = make(50, "A", "B", "C");
        br3.back(10);
        br3.visit("Z");
        eq("branch from the oldest page", "A,Z", h(br3));

        System.out.println("-- capacity");
        BrowserHistory cap = make(3, "A", "B", "C", "D", "E");
        eq("oldest pages pruned", "C,D,E", h(cap));
        eq("cannot go back further than the pruned history", "C", cap.back(10));
        BrowserHistory cap1 = make(1, "A", "B", "C");
        eq("capacity 1 keeps only the newest", "C", h(cap1));
        eq("capacity 1 cursor valid", "C", cap1.back(1));
        BrowserHistory cap2 = make(3, "A", "B", "C");
        cap2.back(1);
        cap2.visit("D");
        eq("branching inside a full history", "A,B,D", h(cap2));
        cap2.visit("E");
        eq("then the oldest is pruned", "B,D,E", h(cap2));
        BrowserHistory def = new BrowserHistory("p0");
        for (int i = 1; i <= 120; i++) def.visit("p" + i);
        eq("default capacity is 50", 50, def.size());
        eq("newest kept", "p120", def.historyUrls()[49]);
        eq("oldest kept", "p71", def.historyUrls()[0]);

        System.out.println("-- display_history");
        BrowserHistory d = make(50, "A", "B", "C");
        d.back(1);
        eq("cursor marker on the active page", "A\n[<] B\nC\n", captureDisplay(d));
        BrowserHistory e = new BrowserHistory("A");
        e.Delete_histroy();
        eq("display on empty history", "(history is empty)\n", captureDisplay(e));

        System.out.println("-- Delete_histroy");
        BrowserHistory del = make(50, "A", "B", "C");
        del.Delete_histroy();
        eq("history empty", 0, del.size());
        eq("current_page when empty", "No current page.", del.current_page());
        check("back/forward return null when empty", del.back(1) == null && del.forward(1) == null);
        del.visit("N");
        eq("browser works after deleting everything", "N", h(del));

        System.out.println("-- delete a URL & restore");
        BrowserHistory dr = make(50, "A", "B", "C");
        eq("deleteUrl count", 1, dr.deleteUrl("B"));
        eq("after delete", "A,C", h(dr));
        check("cursor stays on C", dr.current_page().contains("URL: C"));
        eq("restore returns the page", "B", dr.restore().getUrl());
        eq("restored as newest page", "A,C,B", h(dr));
        check("restore with empty stack returns null", dr.restore() == null);
        eq("deleting a missing URL", 0, dr.deleteUrl("nope"));
        BrowserHistory dc = make(50, "A", "B", "C");
        dc.back(1);
        dc.deleteUrl("B");
        check("deleting the active page moves the cursor to the previous page", dc.current_page().contains("URL: A"));
        eq("forward from there", "C", dc.forward(1));
        BrowserHistory lifo = make(50, "A", "B", "C", "D");
        lifo.deleteUrl("B"); lifo.deleteUrl("C");
        eq("last deleted restored first", "C", lifo.restore().getUrl());
        eq("then the earlier one", "B", lifo.restore().getUrl());
        BrowserHistory lone = new BrowserHistory("A");
        lone.deleteUrl("A");
        eq("deleting the only page", 0, lone.size());
        lone.restore();
        check("restoring into an empty history makes it active", lone.current_page().contains("URL: A"));
        BrowserHistory ds = make(50, "A", "B", "C");
        ds.deleteUrl("B");
        ds.Delete_histroy();
        check("Delete_histroy also clears the restore stack", ds.restore() == null);
        BrowserHistory full = make(3, "A", "B", "C");
        full.deleteUrl("B");
        full.visit("D"); full.visit("E");
        full.restore();
        eq("restore into a full history keeps the capacity", 3, full.size());

        System.out.println("-- export / import");
        File f = File.createTempFile("hist", ".txt");
        f.deleteOnExit();
        BrowserHistory ex = make(50, "A", "B", "C");
        ex.back(1);
        ex.exportHistory(f.getPath());
        BrowserHistory im = new BrowserHistory("Z");
        im.importHistory(f.getPath());
        eq("pages round trip", "A,B,C", h(im));
        check("active page round trip", im.current_page().contains("URL: B"));
        eq("navigation works after import", "C", im.forward(1));
        eq("and back again", "A", im.back(10));
        im.visit("N");
        eq("visit after import", "A,N", h(im));
        BrowserHistory small = new BrowserHistory("Z", 2);
        small.importHistory(f.getPath());
        eq("import respects capacity", "B,C", h(small));
        BrowserHistory withStack = make(50, "A", "B", "C");
        withStack.deleteUrl("B");
        withStack.importHistory(f.getPath());
        check("import clears the restore stack", withStack.restore() == null);
        try (FileWriter w = new FileWriter(f)) { w.write("#CURRENT=0\nA\t2026-01-01T10:00:00Z\nbroken line\n"); }
        boolean threw = false;
        try { im.importHistory(f.getPath()); } catch (IOException ex2) { threw = true; }
        check("malformed file rejected", threw);
        eq("history untouched after a failed import", "A,N", h(im));
        threw = false;
        try { im.importHistory("/no/such/file.txt"); } catch (IOException ex2) { threw = true; }
        check("missing file reported", threw);
        BrowserHistory emptyEx = new BrowserHistory("A");
        emptyEx.Delete_histroy();
        emptyEx.exportHistory(f.getPath());
        BrowserHistory emptyIm = new BrowserHistory("Z");
        emptyIm.importHistory(f.getPath());
        eq("empty history round trip", 0, emptyIm.size());

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
