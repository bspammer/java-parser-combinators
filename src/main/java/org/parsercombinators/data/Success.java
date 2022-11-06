package org.parsercombinators.data;

public record Success<T> (T match, String remaining) implements Result<T> {}
