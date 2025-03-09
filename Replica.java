import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

public class Replica extends UnicastRemoteObject implements ReplicaInterface {

    private List<AuctionItem> items;
    private Map<Integer, String> users;
    private Map<Integer, PublicKey> userPublicKeys;
    private Map<Integer, String> userTokens;
    private Map<Integer, String> currentChallenges;
    private Map<Integer, Integer> highestBidderMap;
    private Map<Integer, Integer> auctionOwnerMap;
    private Set<Integer> closedAuctions;

    private SecretKey secretKey;
    private int userIDCounter = 1;
    private int auctionIDCounter = 1;
    private PrivateKey serverPrivateKey;
    private int replicaID;
    private boolean isPrimary = false;

    public Replica(int replicaID) throws RemoteException {
        super();
        this.replicaID = replicaID;
        items = new ArrayList<>();
        users = new HashMap<>();
        userPublicKeys = new HashMap<>();
        userTokens = new HashMap<>();
        currentChallenges = new HashMap<>();
        highestBidderMap = new HashMap<>();
        auctionOwnerMap = new HashMap<>();
        closedAuctions = new HashSet<>();

        try {
            FileInputStream fis = new FileInputStream("keys/testKey.aes");
            byte[] encodedKey = fis.readAllBytes();
            fis.close();
            secretKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");

            if (Files.exists(Paths.get("keys/server_private.key"))) {
                byte[] keyBytes = Files.readAllBytes(Paths.get("keys/server_private.key"));
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                serverPrivateKey = keyFactory.generatePrivate(spec);
                System.out.println("Loaded existing RSA private key.");
            } else {
                System.out.println("No RSA key pair found, generating a new one...");
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                KeyPair keyPair = keyGen.generateKeyPair();
                serverPrivateKey = keyPair.getPrivate();
                PublicKey serverPublicKey = keyPair.getPublic();

                PKCS8EncodedKeySpec pkcs8Spec = new PKCS8EncodedKeySpec(serverPrivateKey.getEncoded());
                try (FileOutputStream fos = new FileOutputStream("keys/server_private.key")) {
                    fos.write(pkcs8Spec.getEncoded());
                }

                X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(serverPublicKey.getEncoded());
                try (FileOutputStream fos = new FileOutputStream("keys/server_public.key")) {
                    fos.write(x509Spec.getEncoded());
                }

                System.out.println("RSA key pair generated and saved successfully.");
            }

        } catch (Exception e) {
            throw new RemoteException("Failed to load or generate keys.", e);
        }
    }

    public synchronized void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
        if (isPrimary) {
            System.out.println("Replica " + replicaID + " is now the primary.");
        } else {
            System.out.println("Replica " + replicaID + " is no longer the primary.");
        }
    }

    private synchronized void synchronizeReplicas() throws RemoteException {
    if (!isPrimary) {
        return; // Only the primary should synchronize replicas
    }
    System.out.println("Synchronizing replicas...");

    for (int i = 1; i <= 3; i++) { // Assuming there are 3 replicas
        if (i == replicaID) continue; // Skip the current replica
        try {
            ReplicaInterface replica = (ReplicaInterface) Naming.lookup("rmi://localhost/Replica" + i);
            replica.updateState(new HashMap<Integer, AuctionItem>(items.stream().collect(Collectors.toMap(item -> item.itemID, item -> item))),
                                new HashMap<Integer, String>(users),
                                new HashMap<Integer, Integer>(auctionOwnerMap),
                                new HashMap<Integer, Integer>(highestBidderMap));
        } catch (Exception e) {
            System.out.println("Failed to synchronize with Replica " + i + ": " + e.getMessage());
        }
    }
}

    

    @Override
    public synchronized int register(String email) throws RemoteException {
        int userID = userIDCounter++;
        users.put(userID, email);
        return userID;
    }

    @Override
    public synchronized AuctionItem getSpec(int itemID) throws RemoteException {
        for (AuctionItem item : items) {
            if (item.itemID == itemID) {
                return item;
            }
        }
        return null;
    }

    @Override
    public synchronized int newAuction(int userID, AuctionSaleItem saleItem) throws RemoteException {
        if (!isPrimary) {
            throw new RemoteException("Only the primary replica can create new auctions.");
        }
        AuctionItem newItem = new AuctionItem();
        newItem.itemID = auctionIDCounter++;
        newItem.name = saleItem.name;
        newItem.description = saleItem.description;
        newItem.highestBid = 0;
        items.add(newItem);
        auctionOwnerMap.put(newItem.itemID, userID);

        // Synchronize the new auction with other replicas
        synchronizeReplicas();
        return newItem.itemID;
    }

    @Override
    public synchronized AuctionItem[] listItems() throws RemoteException {
        return items.toArray(new AuctionItem[0]);
    }

    @Override
    public synchronized AuctionResult closeAuction(int userID, int itemID) throws RemoteException {
        if (!isPrimary) {
            throw new RemoteException("Only the primary replica can close auctions.");
        }
        if (closedAuctions.contains(itemID)) {
            throw new RemoteException("Auction has already been closed.");
        }
        Integer auctionOwner = auctionOwnerMap.get(itemID);
        if (auctionOwner == null || auctionOwner != userID) {
            throw new RemoteException("You do not have permission to close this auction.");
        }
        AuctionItem auctionItem = null;
        for (AuctionItem item : items) {
            if (item.itemID == itemID) {
                auctionItem = item;
                break;
            }
        }
        if (auctionItem == null) {
            throw new RemoteException("Auction item not found.");
        }
        AuctionResult result = new AuctionResult();
        result.winningEmail = findWinningUserEmail(itemID);
        result.winningPrice = auctionItem.highestBid;
        items.remove(auctionItem);
        highestBidderMap.remove(itemID);
        closedAuctions.add(itemID);

        // Synchronize the closed auction with other replicas
        synchronizeReplicas();
        return result;
    }

    @Override
    public synchronized boolean bid(int userID, int itemID, int price) throws RemoteException {
        if (!isPrimary) {
            throw new RemoteException("Only the primary replica can handle bids.");
        }
        AuctionItem auctionItem = null;
        for (AuctionItem item : items) {
            if (item.itemID == itemID) {
                auctionItem = item;
                break;
            }
        }
        if (auctionItem == null) {
            throw new RemoteException("Auction item not found.");
        }
        if (price > auctionItem.highestBid) {
            auctionItem.highestBid = price;
            highestBidderMap.put(itemID, userID);
            synchronizeReplicas();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getPrimaryReplicaID() throws RemoteException {
        return replicaID;
    }

    @Override
    public void updateState(HashMap<Integer, AuctionItem> itemsMap, HashMap<Integer, String> usersMap,
                        HashMap<Integer, Integer> auctionOwnerMap, HashMap<Integer, Integer> highestBidderMap) throws RemoteException {
    this.items = new ArrayList<>(itemsMap.values());
    this.users = new HashMap<>(usersMap);
    this.auctionOwnerMap = new HashMap<>(auctionOwnerMap);
    this.highestBidderMap = new HashMap<>(highestBidderMap);
    System.out.println("Replica " + replicaID + " state updated.");
    }


    @Override
    public void setPrimaryReplicaID(int newID) throws RemoteException {
        this.isPrimary = (this.replicaID == newID);
        if (isPrimary) {
            System.out.println("Replica " + replicaID + " is now the primary.");
        } else {
            System.out.println("Replica " + replicaID + " is no longer the primary.");
        }
    }

    @Override
    public void updateReplicaIDList(List<Integer> replicaIDs) throws RemoteException {
        System.out.println("Updated replica list for Replica " + replicaID + ": " + replicaIDs);
    }

    private String findWinningUserEmail(int itemID) {
        Integer winningUserID = highestBidderMap.get(itemID);
        if (winningUserID != null) {
            return users.getOrDefault(winningUserID, "unknown@example.com");
        }
        return "unknown@example.com";
    }

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("Usage: java Replica <replicaID>");
                return;
            }

            int replicaID = Integer.parseInt(args[0]);
            Replica server = new Replica(replicaID);
            Naming.rebind("rmi://localhost/Replica" + replicaID, server);
            System.out.println("Replica " + replicaID + " is ready.");
        } catch (Exception e) {
            System.out.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
