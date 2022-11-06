package org.parsercombinators.utils;

import java.util.List;
import java.util.stream.Collector;

public class Utils {

    public static String charsToString(final List<Character> characters) {
        return characters.stream()
            .collect(Collector.of(
                StringBuilder::new,
                StringBuilder::append,
                StringBuilder::append,
                StringBuilder::toString
            ));
    }

}
