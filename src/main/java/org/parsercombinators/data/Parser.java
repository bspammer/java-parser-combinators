package org.parsercombinators.data;

import org.parsercombinators.data.result.Result;
import org.parsercombinators.parsers.Parsers;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface Parser<T> {

    Result<T> parse(String input);

    default <U> Parser<U> map(Function<T, U> mapper) {
        return Parsers.map(this, mapper);
    }

    default <U> Parser<U> before(Parser<U> other) {
        return Parsers.foldRight(this, other);
    }

    default <U> Parser<U> after(Parser<U> other) {
        return Parsers.foldLeft(other, this);
    }

    default <U> Parser<T> following(Parser<U> other) {
        return Parsers.foldRight(other, this);
    }

    default <U> Parser<T> followedBy(Parser<U> other) {
        return Parsers.foldLeft(this, other);
    }

    default <U> Parser<Pair<T, U>> with(Parser<U> other) {
        return Parsers.concat(this, other);
    }

    default Parser<T> or(Parser<T> other) {
        return Parsers.or(this, other);
    }

    default Parser<T> and(Parser<T> other) {
        return Parsers.and(this, other);
    }

    default Parser<Void> not() {
        return Parsers.not(this, ignored -> "Should have failed to parse, but succeeded");
    }

    default Parser<Void> not(final Function<T, String> failureMessageMapper) {
        return Parsers.not(this, failureMessageMapper);
    }

    default Parser<Optional<T>> asOptional() {
        return Parsers.optional(this);
    }

    default Parser<List<T>> many() {
        return Parsers.many(this);
    }

    default Parser<List<T>> many1() {
        return Parsers.many1(this);
    }

    default Parser<List<T>> nTimes(final int n) {
        return Parsers.nTimes(this, n);
    }

    default <U> Parser<T> surroundedBy(Parser<U> other) {
        return Parsers.surrounding(this, other);
    }

    default <U, V> Parser<T> surroundedBy(Parser<U> left, Parser<V> right) {
        return Parsers.surrounding(this, left, right);
    }

}
