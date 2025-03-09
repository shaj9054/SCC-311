public class AuctionSaleItem implements java.io.Serializable {
    String name;
    String description;
    int reservePrice;

    // Constructor to initialize fields
    public AuctionSaleItem(String name, String description, int reservePrice) {
        this.name = name;
        this.description = description;
        this.reservePrice = reservePrice;
    }
}
