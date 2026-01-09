package jdbld.demo.library.impl;

import jdbld.demo.library.api.Evaluator;

@SuppressWarnings("PMD.CommentRequired")
public class Adder implements Evaluator {

    @Override
    public int evaluate(int first, int second) {
        return first + second;
    }

}
