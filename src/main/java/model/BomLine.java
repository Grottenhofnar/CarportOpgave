package model;

import java.math.BigDecimal;

public class BomLine {
    public int materialId;
    public String type;
    public String description;
    public int lengthCm;
    public int quantity;
    public String unit;
    public String usage;
    public BigDecimal unitPrice;
    public BigDecimal lineTotal;

    public BomLine(int materialId, String type, String description, int lengthCm,
                   int quantity, String unit, String usage, BigDecimal unitPrice) {
        this.materialId = materialId;
        this.type = type;
        this.description = description;
        this.lengthCm = lengthCm;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;
        this.unitPrice = unitPrice;
        this.lineTotal = (unitPrice == null) ? BigDecimal.ZERO
                : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
