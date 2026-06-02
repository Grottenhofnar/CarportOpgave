package model;

import java.math.BigDecimal;
import java.util.List;

public class BillOfMaterials {
    public CarportSpec spec;
    public List<BomLine> lines;
    public BigDecimal total;

    public BillOfMaterials(CarportSpec spec, List<BomLine> lines) {
        this.spec = spec;
        this.lines = lines;
        this.total = lines.stream().map(l -> l.lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
