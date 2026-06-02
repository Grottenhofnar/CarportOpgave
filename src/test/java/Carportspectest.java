
import model.CarportSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CarportSpecTest {

    @Test
    void validDimensionsAreAccepted() {
        CarportSpec spec = new CarportSpec("plast", 600, 780, false, 0, 0);
        assertDoesNotThrow(spec::validate);
    }

    @Test
    void dimensionsNotMultipleOf30AreRejected() {
        CarportSpec spec = new CarportSpec("plast", 615, 600, false, 0, 0);
        assertThrows(IllegalArgumentException.class, spec::validate);
    }

    @Test
    void shedLargerThanCarportIsRejected() {
        CarportSpec spec = new CarportSpec("plast", 330, 630, true, 360, 510);
        assertThrows(IllegalArgumentException.class, spec::validate);
    }
}