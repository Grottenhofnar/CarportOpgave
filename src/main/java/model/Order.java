package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Order {
    public int orderId;
    public Integer userId;
    public String username;
    public Timestamp orderDate;
    public CarportSpec spec;
    public Contact contact;
    public String status = "REQUESTED";
    public BigDecimal totalPrice;
    public String adminNote;
    public boolean paid;
    public boolean sketchReleased;

    public String statusLabel() {
        return switch (status) {
            case "REQUESTED" -> "Afventer behandling";
            case "IN_PROGRESS" -> "Under behandling";
            case "QUOTED" -> "Tilbud klar";
            case "PAID" -> "Betalt";
            case "COMPLETED" -> "Afsluttet";
            default -> status;
        };
    }
}
