package jdbld.demo.module.impl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AdderTest {

    @Test
    void test() {
        Adder adder = new Adder();
        assertEquals(3, adder.evaluate(1, 2));
    }
}
