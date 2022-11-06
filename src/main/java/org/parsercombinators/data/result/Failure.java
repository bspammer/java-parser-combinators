package org.parsercombinators.data.result;

public record Failure<T> (String message) implements Result<T> {}
