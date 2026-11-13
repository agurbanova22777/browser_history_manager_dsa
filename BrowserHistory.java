public class BrowserHistory {
    private DoublyLinkedList history;
    private Node current;
    private int capacity = 50;

    public BrowserHistory (String homepage) {
        history = new DoublyLinkedList();

        current = new Node(homepage);

        history.append(current);

    }

    public void visit (String url) {
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

    public void Delete_histroy() { // grammatically following the project requirements (pdf: page 2, first line)
        history.clear();
        current = null;
    }

}

