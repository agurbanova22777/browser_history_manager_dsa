import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner (System.in);

        BrowserHistory browser = null;
        while (browser == null) {
            System.out.print("Enter the Homepage URL: ");
            String homepage = input.nextLine();
            try {
                browser = new BrowserHistory(homepage);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        while (true) {
            System.out.println("=====BROWSER HISTORY MANAGER=====");
            System.out.println("1. Visit a page");
            System.out.println("2. Go back");
            System.out.println("3. Go forward");
            System.out.println("4. Show current page");
            System.out.println("5. Show full history");
            System.out.println("6. Delete a page from history");
            System.out.println("7. Restore last deleted page");
            System.out.println("8. Delete all browsing data");
            System.out.println("9. Export history to a file");
            System.out.println("10. Import history from a file");
            System.out.println("11. Exit");

            int choice = readInt(input, "Enter your choice: ");

            try {
                switch (choice) {
                    case 1:
                        System.out.print("Enter the URL: ");
                        String url = input.nextLine();

                        browser.visit(url);

                        System.out.println("Page opened successfully.");
                        System.out.println();
                        break;
                    case 2:
                        int backSteps = readInt(input, "How many pages back? ");

                        String backFinalPage = browser.back(backSteps);

                        System.out.println("Current page: " + backFinalPage);
                        break;
                    case 3:
                        int forwSteps = readInt(input, "How many pages ahead? ");

                        String forwFinalPage = browser.forward(forwSteps);

                        System.out.println("Current page: " + forwFinalPage);
                        break;
                    case 4:
                        System.out.println(browser.current_page());
                        break;
                    case 5:
                        browser.display_history();
                        break;
                    case 6:
                        System.out.print("Enter the URL to delete: ");
                        String toDelete = input.nextLine();

                        int removed = browser.deleteUrl(toDelete);

                        if (removed == 0) {
                            System.out.println("That URL is not in the history.");
                        } else {
                            System.out.println("Deleted " + removed + " page(s). You can restore them with option 7.");
                        }
                        break;
                    case 7:
                        Node restored = browser.restore();

                        if (restored == null) {
                            System.out.println("There is nothing to restore.");
                        } else {
                            System.out.println("Restored: " + restored.getUrl());
                        }
                        break;
                    case 8:
                        browser.Delete_histroy();
                        System.out.println("Browser history deleted successfully.");
                        break;
                    case 9:
                        System.out.print("File name to export to (e.g. history.txt): ");
                        browser.exportHistory(input.nextLine().trim());
                        System.out.println("History exported successfully.");
                        break;
                    case 10:
                        System.out.print("File name to import from (e.g. history.txt): ");
                        browser.importHistory(input.nextLine().trim());
                        System.out.println("History imported successfully.");
                        break;
                    case 11:
                        System.out.println("Thank you for utilizing our Browser History Manager!");
                        input.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Please select again.");
                        break;
                }
            } catch (IllegalArgumentException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Reads a whole line and converts it to an int; asks again if the user types something else.
    private static int readInt(Scanner input, String message) {
        while (true) {
            System.out.print(message);
            String line = input.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }
}
