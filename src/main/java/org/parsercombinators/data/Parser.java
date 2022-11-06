package org.parsercombinators.data;

import org.parsercombinators.data.Result;

@FunctionalInterface
public interface Parser<T> {

    Result<T> parse(String input);

}
