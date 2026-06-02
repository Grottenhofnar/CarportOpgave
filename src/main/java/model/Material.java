package model;

import java.math.BigDecimal;

public class Material {
    public int materialId;
    public String type;
    public String name;
    public BigDecimal price;
    public Integer lengthCm;
    public Integer packSize;

    public Material(int materialId, String type, String name, BigDecimal price, Integer lengthCm, Integer packSize) {
        this.materialId = materialId;
        this.type = type;
        this.name = name;
        this.price = price;
        this.lengthCm = lengthCm;
        this.packSize = packSize;
    }
}
