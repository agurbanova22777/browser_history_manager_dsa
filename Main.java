import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner (System.in);

        System.out.print("Enter the Homepage URL: ");
        String homepage = input.nextLine();

        BrowserHistory browser = new BrowserHistory(homepage);

        while (true) {
            System.out.println("=====BROWSER HISTORY MANAGER=====");
            System.out.println("1. Visit a page");
            System.out.println("2. Go back");
            System.out.println("3. Go forward");
            System.out.println("4. Show current page");
            System.out.println("5. Show full history");
            System.out.println("6. Delete browsing data");
            System.out.println("7. Exit");

            System.out.print("Enter your choice: ");
            int choice = input.nextInt();

            switch (choice) {
                case 1:
                    input.nextLine();
                    System.out.print("Enter the URL: ");
                    String url = input.nextLine();

                    browser.visit(url);

                    System.out.println("Page opened successfully.");
                    System.out.println();
                    break;
                case 2:
                    System.out.print("How many pages back? ");
                    int backSteps = input.nextInt();

                    String backFinalPage = browser.back(backSteps);

                    System.out.println("Current page: " + backFinalPage);
                    break;
                case 3:
                    System.out.print("How many pages ahead? ");
                    int forwSteps = input.nextInt();

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
                    browser.Delete_histroy();
                    System.out.println("Browser history deleted successfully.");
                    break;
                case 7:
                    System.out.println("Thank you for utilizing our Browser History Manager!");
                    input.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please select again.");
                    break;
            }
        }
    }
}
