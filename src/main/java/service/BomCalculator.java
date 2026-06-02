package service;

import model.BillOfMaterials;
import model.BomLine;
import model.CarportSpec;
import model.Material;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BomCalculator {

    private static final int MAX_RAFTER_GAP = 60;
    private static final int RAFTER_FASCIA_DEDUCT = 5;
    private static final int POST_SPAN = 250;
    private static final double CLADDING_COVER = 9.5;
    private static final int ROOF_PLATE_COVER = 105;
    private static final int SHED_HEIGHT = 210;
    private static final int SHED_LOESHOLT_ROWS = 3;

    private static final String RAFTER = "45x195 mm spærtræ ubh.";
    private static final String POST = "97x97 mm trykimp. stolpe";
    private static final String FASCIA_UNDER = "25x200 mm trykimp. brædt";
    private static final String FASCIA_OVER = "25x125 mm trykimp. brædt";
    private static final String WATERBOARD = "19x100 mm trykimp. brædt";
    private static final String LOESHOLT = "45x95 mm reglar ub.";
    private static final String ROOF_PLATE = "Plastmo Ecolite blåtonet";
    private static final String BRACKET_R = "Universal 190 mm højre";
    private static final String BRACKET_L = "Universal 190 mm venstre";
    private static final String BOLT = "Bræddebolt 10x120 mm";
    private static final String WASHER = "Firkantskiver 40x40x11 mm";
    private static final String PERFBAND = "Hulbånd 1x20 mm (10 m)";
    private static final String ANGLE_BRACKET = "Vinkelbeslag 35";
    private static final String SCREW_ROOF = "Plastmo bundskruer (200 stk)";
    private static final String SCREW_BRACKET = "4,0x50 mm beslagskruer (250 stk)";
    private static final String SCREW_FASCIA = "4,5x60 mm skruer (200 stk)";
    private static final String SCREW_OUTER = "4,5x70 mm skruer (400 stk)";
    private static final String SCREW_INNER = "4,5x50 mm skruer (300 stk)";

    private final Map<String, Material> catalog;

    public BomCalculator(Map<String, Material> catalogByName) {
        this.catalog = catalogByName;
    }

    private static int ceilDiv(double n) { return (int) Math.ceil(n - 1e-9); }

    public static int rafterCount(int lengthCm) {
        int gaps = ceilDiv((double) lengthCm / MAX_RAFTER_GAP);
        if ((double) lengthCm / gaps >= MAX_RAFTER_GAP - 0.001) gaps++;
        return gaps + 1;
    }

    public BillOfMaterials calculate(CarportSpec spec) {
        List<BomLine> lines = new ArrayList<>();
        int L = spec.lengthCm, B = spec.widthCm;
        boolean roof = spec.roofType != null && !spec.roofType.contains("without") && !"rooftype".equals(spec.roofType);

        int rafters = rafterCount(L);
        add(lines, "spaer", RAFTER, B - RAFTER_FASCIA_DEDUCT, rafters, "stk", "Spær, monteres på rem");

        int posts = (ceilDiv((double) L / POST_SPAN) + 1) * 2 + (spec.withShed ? 1 : 0);
        add(lines, "stolpe", POST, 300, posts, "stk", "Nedgraves ca. 90 cm i jord");

        add(lines, "rem", RAFTER, Math.min(L, 600), 2, "stk", "Remme i sider, sadles ned i stolper");
        if (L > 600) add(lines, "rem", RAFTER, L - 600, 2, "stk", "Rem-forlængelse, samles over stolpe");

        add(lines, "stern", FASCIA_UNDER, B, 2, "stk", "Understern for- & bagende");
        add(lines, "stern", FASCIA_UNDER, L, 2, "stk", "Understern til siderne");
        add(lines, "stern", FASCIA_OVER, B, 1, "stk", "Overstern forende");
        add(lines, "stern", FASCIA_OVER, L, 2, "stk", "Overstern til siderne");
        add(lines, "vandbraet", WATERBOARD, L, 2, "stk", "Vandbrædt på stern i sider");
        add(lines, "vandbraet", WATERBOARD, B, 1, "stk", "Vandbrædt på stern i forende");

        if (spec.withShed) {
            int sw = spec.shedWidthCm, sl = spec.shedLengthCm;
            add(lines, "loesholt", LOESHOLT, sw, SHED_LOESHOLT_ROWS * 2, "stk", "Løsholter, skurets to gavle");
            add(lines, "loesholt", LOESHOLT, sl, SHED_LOESHOLT_ROWS, "stk", "Løsholter, skurets side");
            int cladding = ceilDiv((2 * sw + sl) / CLADDING_COVER);
            add(lines, "beklaedning", WATERBOARD, SHED_HEIGHT, cladding, "stk", "Beklædning af skur, 1 på 2");
        }

        if (roof) {
            int roofBays = ceilDiv((double) L / ROOF_PLATE_COVER);
            add(lines, "tagplade", ROOF_PLATE, B, roofBays, "stk", "Tagplader på spær");
        }

        add(lines, "beslag", BRACKET_R, 0, rafters, "stk", "Spær på rem");
        add(lines, "beslag", BRACKET_L, 0, rafters, "stk", "Spær på rem");
        int bolts = posts * 2 + (L > 600 ? 2 : 0);
        add(lines, "beslag", BOLT, 0, bolts, "stk", "Rem på stolper");
        add(lines, "beslag", WASHER, 0, bolts, "stk", "Rem på stolper");
        add(lines, "beslag", PERFBAND, 0, 2, "rl", "Vindkryds på spær");

        double areaM2 = (L * (double) B) / 10000.0;
        if (roof) add(lines, "skrue", SCREW_ROOF, 0, ceilDiv(areaM2 / 15), "pk", "Til tagplader");
        add(lines, "skrue", SCREW_BRACKET, 0, ceilDiv((double) rafters / 6) + 1, "pk", "Beslag + hulbånd");
        add(lines, "skrue", SCREW_FASCIA, 0, 1, "pk", "Stern & vandbrædt");

        if (spec.withShed) {
            add(lines, "skrue", SCREW_OUTER, 0, 2, "pk", "Yderste beklædning");
            add(lines, "skrue", SCREW_INNER, 0, 2, "pk", "Inderste beklædning");
            add(lines, "beslag", ANGLE_BRACKET, 0, SHED_LOESHOLT_ROWS * 3 * 2, "stk", "Løsholter i skur");
        }

        return new BillOfMaterials(spec, lines);
    }

    private void add(List<BomLine> lines, String type, String name, int lengthCm,
                     int quantity, String unit, String usage) {
        Material m = catalog.get(name);
        int id = (m != null) ? m.materialId : -1;
        BigDecimal price = (m != null && m.price != null) ? m.price : BigDecimal.ZERO;
        lines.add(new BomLine(id, type, name, lengthCm, quantity, unit, usage, price));
    }
}
