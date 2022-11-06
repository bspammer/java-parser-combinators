package org.parsercombinators;

import org.parsercombinators.data.result.Failure;
import org.parsercombinators.data.Parser;
import org.parsercombinators.data.result.Result;
import org.parsercombinators.data.result.Success;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello world!");
        final Result<String> result = new Failure<>("testing");

        final Parser<String> stringParser = input -> new Success<>("yes", "");
    }

}
