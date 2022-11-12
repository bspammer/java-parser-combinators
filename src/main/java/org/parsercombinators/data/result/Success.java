package org.parsercombinators.data.result;

public record Success<T>(T match, String remaining) implements Result<T> {}
