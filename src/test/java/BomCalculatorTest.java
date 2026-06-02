
import model.CarportSpec;
import model.Material;
import org.junit.jupiter.api.Test;
import service.BomCalculator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BomCalculatorTest {

    private BomCalculator calculatorWithOneMaterial() {
        Map<String, Material> catalog = new HashMap<>();
        catalog.put("45x195 mm spærtræ ubh.",
                new Material(1, "spaer", "45x195 mm spærtræ ubh.", new BigDecimal("189"), 600, null));
        return new BomCalculator(catalog);
    }

    @Test
    void rafterCountAt780Is15() {
        assertEquals(15, BomCalculator.rafterCount(780));
    }

    @Test
    void calculateReturnsATotal() {
        BomCalculator calc = calculatorWithOneMaterial();
        BigDecimal total = calc.calculate(new CarportSpec("plast", 600, 780, false, 0, 0)).total;
        assertTrue(total.compareTo(BigDecimal.ZERO) > 0);
    }
}