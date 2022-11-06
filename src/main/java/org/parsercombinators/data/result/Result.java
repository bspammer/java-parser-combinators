package org.parsercombinators.data.result;

public sealed interface Result<T> permits Success, Failure {}
