package model;

public class CarportSpec {
    public String roofType;
    public int widthCm;
    public int lengthCm;
    public boolean withShed;
    public int shedWidthCm;
    public int shedLengthCm;

    public CarportSpec() {}

    public CarportSpec(String roofType, int widthCm, int lengthCm, boolean withShed, int shedWidthCm, int shedLengthCm) {
        this.roofType = roofType;
        this.widthCm = widthCm;
        this.lengthCm = lengthCm;
        this.withShed = withShed;
        this.shedWidthCm = shedWidthCm;
        this.shedLengthCm = shedLengthCm;
    }

    public void validate() {
        if (widthCm <= 0 || lengthCm <= 0)
            throw new IllegalArgumentException("Vælg bredde og længde");
        if (widthCm % 30 != 0 || lengthCm % 30 != 0)
            throw new IllegalArgumentException("Mål skal angives i spring af 30 cm");
        if (withShed) {
            if (shedWidthCm % 30 != 0 || shedLengthCm % 30 != 0)
                throw new IllegalArgumentException("Skurmål skal angives i spring af 30 cm");
            if (shedLengthCm > lengthCm || shedWidthCm > widthCm)
                throw new IllegalArgumentException("Skuret kan ikke være større end carporten");
        }
    }
}
