import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.rmi.Naming;
import java.security.*;
import java.util.Scanner;

public class AuctionClient {

    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    public static void main(String[] args) {
        try {
            // Connect to the Front-End server instead of individual replicas
            Auction auction = (Auction) Naming.lookup("rmi://localhost/FrontEnd");
            
            // Load secret key for communication encryption
            SecretKey secretKey;
            try (FileInputStream keyFile = new FileInputStream("keys/testKey.aes")) {
                byte[] encodedKey = keyFile.readAllBytes();
                secretKey = new SecretKeySpec(encodedKey, "AES");
            }

            // Generate RSA key pair for future signing (if needed)
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter your email to register:");
            String email = scanner.nextLine();
            int userID = auction.register(email);
            System.out.println("Registered with userID: " + userID);

            while (true) {
                System.out.println("1. Create Auction\n2. List Items\n3. Bid on Item\n4. Close Auction\n5. Exit");
                int choice = scanner.nextInt();
                switch (choice) {
                    case 1 -> {
                        scanner.nextLine(); // Consume newline
                        System.out.println("Enter item name:");
                        String name = scanner.nextLine();
                        System.out.println("Enter item description:");
                        String description = scanner.nextLine();
                        System.out.println("Enter reserve price:");
                        int reservePrice = scanner.nextInt();

                        AuctionSaleItem saleItem = new AuctionSaleItem(name, description, reservePrice);
                        int itemID = auction.newAuction(userID, saleItem);
                        System.out.println("Auction created with Item ID: " + itemID);
                    }
                    case 2 -> {
                        AuctionItem[] items = auction.listItems();
                        for (AuctionItem item : items) {
                            displayAuctionItem(item);
                        }
                    }
                    case 3 -> {
                        System.out.println("Enter the item ID to bid on:");
                        int itemID = scanner.nextInt();
                        System.out.println("Enter your bid:");
                        int price = scanner.nextInt();

                        if (auction.bid(userID, itemID, price)) {
                            System.out.println("Bid placed successfully.");
                        } else {
                            System.out.println("Bid too low. Try again.");
                        }
                    }
                    case 4 -> {
                        System.out.println("Enter the item ID to close:");
                        int itemID = scanner.nextInt();

                        AuctionResult result = auction.closeAuction(userID, itemID);
                        System.out.println("Auction closed. Winner: " + result.winningEmail + " with bid: $" + result.winningPrice);
                    }
                    case 5 -> {
                        return; // Exit the application
                    }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            }

        } catch (Exception e) {
            System.out.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void displayAuctionItem(AuctionItem item) {
        System.out.println("Auction Item Details:");
        System.out.println("Item ID: " + item.itemID);
        System.out.println("Name: " + item.name);
        System.out.println("Description: " + item.description);
        System.out.println("Highest Bid: $" + item.highestBid);
    }
}
