package jdbld.demo.module.impl;

import com.google.gson.Gson;
import jdbld.demo.module.api.Evaluator;

@SuppressWarnings("PMD.CommentRequired")
public class Adder implements Evaluator {

    @Override
    public int evaluate(int first, int second) {
        new Gson();
        return first + second;
    }

}
