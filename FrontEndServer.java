import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;

public class FrontEndServer extends UnicastRemoteObject implements Auction {

    private List<ReplicaInterface> replicas; // Changed to use ReplicaInterface
    private int primaryReplicaID;

    protected FrontEndServer() throws RemoteException {
        super();
        initializeReplicas();
    }

    private void initializeReplicas() {
        try {
            // Assuming there are three replicas
            replicas = List.of(
                (ReplicaInterface) Naming.lookup("rmi://localhost/Replica1"),
                (ReplicaInterface) Naming.lookup("rmi://localhost/Replica2"),
                (ReplicaInterface) Naming.lookup("rmi://localhost/Replica3")
            );

            // Set an initial primary replica
            primaryReplicaID = new Random().nextInt(replicas.size());
            setPrimaryReplica(primaryReplicaID);
            System.out.println("Initial primary replica set to Replica" + (primaryReplicaID + 1));

        } catch (Exception e) {
            System.out.println("Failed to initialize replicas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ReplicaInterface getPrimaryReplica() {
        try {
            // Check if the current primary replica is responsive
            replicas.get(primaryReplicaID).getPrimaryReplicaID(); // Any method that confirms the replica is active
            return replicas.get(primaryReplicaID);
        } catch (RemoteException e) {
            System.out.println("Primary replica " + (primaryReplicaID + 1) + " is not available. Attempting to find a new primary.");
            switchPrimaryReplica();
            return getPrimaryReplica(); // Recursively find a working primary
        }
    }

    private void switchPrimaryReplica() {
        int initialPrimaryID = primaryReplicaID;
        boolean foundNewPrimary = false;

        // Loop through the replicas to find a responsive one
        do {
            primaryReplicaID = (primaryReplicaID + 1) % replicas.size();
            try {
                // Check if the new candidate is responsive
                replicas.get(primaryReplicaID).getPrimaryReplicaID();
                setPrimaryReplica(primaryReplicaID); // Promote the new primary
                System.out.println("Primary replica switched to Replica" + (primaryReplicaID + 1));
                foundNewPrimary = true;
                break;
            } catch (RemoteException e) {
                System.out.println("Replica " + (primaryReplicaID + 1) + " is not available. Checking next replica.");
            }
        } while (primaryReplicaID != initialPrimaryID);

        if (!foundNewPrimary) {
            throw new IllegalStateException("No available replicas to set as primary.");
        }
    }

    private void setPrimaryReplica(int newPrimaryReplicaID) {
        // Set all replicas to not primary
        for (int i = 0; i < replicas.size(); i++) {
            try {
                replicas.get(i).setPrimaryReplicaID(-1); // Set to not primary by passing -1 or any non-matching ID
            } catch (Exception e) {
                System.out.println("Failed to set primary status for Replica " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        // Set the new primary replica
        try {
            replicas.get(newPrimaryReplicaID).setPrimaryReplicaID(newPrimaryReplicaID + 1);
        } catch (Exception e) {
            System.out.println("Failed to set primary status for the new primary Replica " + (newPrimaryReplicaID + 1) + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int register(String email) throws RemoteException {
        return getPrimaryReplica().register(email);
    }

    @Override
    public AuctionItem getSpec(int itemID) throws RemoteException {
        return getPrimaryReplica().getSpec(itemID);
    }

    @Override
    public int newAuction(int userID, AuctionSaleItem saleItem) throws RemoteException {
        try {
            return getPrimaryReplica().newAuction(userID, saleItem);
        } catch (RemoteException e) {
            System.out.println("Primary replica failed during new auction. Attempting failover.");
            switchPrimaryReplica();
            return getPrimaryReplica().newAuction(userID, saleItem);
        }
    }

    @Override
    public AuctionItem[] listItems() throws RemoteException {
        return getPrimaryReplica().listItems();
    }

    @Override
    public AuctionResult closeAuction(int userID, int itemID) throws RemoteException {
        try {
            return getPrimaryReplica().closeAuction(userID, itemID);
        } catch (RemoteException e) {
            System.out.println("Primary replica failed during close auction. Attempting failover.");
            switchPrimaryReplica();
            return getPrimaryReplica().closeAuction(userID, itemID);
        }
    }

    @Override
    public boolean bid(int userID, int itemID, int price) throws RemoteException {
        try {
            return getPrimaryReplica().bid(userID, itemID, price);
        } catch (RemoteException e) {
            System.out.println("Primary replica failed during bid. Attempting failover.");
            switchPrimaryReplica();
            return getPrimaryReplica().bid(userID, itemID, price);
        }
    }

    @Override
    public int getPrimaryReplicaID() throws RemoteException {
        return primaryReplicaID + 1;
    }

    public static void main(String[] args) {
        try {
            FrontEndServer frontEndServer = new FrontEndServer();
            Naming.rebind("rmi://localhost/FrontEnd", frontEndServer);
            System.out.println("Front-End Server is ready.");
        } catch (Exception e) {
            System.out.println("Front-End Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
