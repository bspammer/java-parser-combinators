package org.parsercombinators.data;

public sealed interface Result<T> permits Success, Failure {}
