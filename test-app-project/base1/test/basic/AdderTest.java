package basic;

import builder.test.base1.Adder;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AdderTest {
    @Test
    void test() {
        Adder adder = new Adder();
        assertEquals(3, adder.calculate(1, 2));
    }
}