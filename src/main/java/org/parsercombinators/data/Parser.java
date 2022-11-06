package org.parsercombinators.data;

import org.parsercombinators.data.result.Result;

@FunctionalInterface
public interface Parser<T> {

    Result<T> parse(String input);

}
