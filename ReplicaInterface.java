import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public interface ReplicaInterface extends Remote {

    int register(String email) throws RemoteException;

    AuctionItem getSpec(int itemID) throws RemoteException;

    int newAuction(int userID, AuctionSaleItem item) throws RemoteException;

    AuctionItem[] listItems() throws RemoteException;

    AuctionResult closeAuction(int userID, int itemID) throws RemoteException;

    boolean bid(int userID, int itemID, int price) throws RemoteException;

    int getPrimaryReplicaID() throws RemoteException;

    // Updated to match the field types in the Replica class
    void updateState(HashMap<Integer, AuctionItem> itemsMap, HashMap<Integer, String> usersMap,
                     HashMap<Integer, Integer> auctionOwnerMap, HashMap<Integer, Integer> highestBidderMap) throws RemoteException;

    void setPrimaryReplicaID(int newID) throws RemoteException;

    void updateReplicaIDList(List<Integer> replicaIDs) throws RemoteException;
}
