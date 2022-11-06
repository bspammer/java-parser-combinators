package org.parsercombinators.data;

public record Failure<T> (String message) implements Result<T> {}
