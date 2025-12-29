package basic;

import builder.test.base1.Adder;

class AdderTest {
    public static void main(String[] args) {
        System.out.println(new Adder().calculate(2, 3));
    }
}