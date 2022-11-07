package org.parsercombinators.parsers;

import org.parsercombinators.data.Pair;
import org.parsercombinators.data.Parser;
import org.parsercombinators.data.result.Failure;
import org.parsercombinators.data.result.Result;
import org.parsercombinators.data.result.Success;
import org.parsercombinators.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;

public class Parsers {

    public static <T> Parser<T> pure(final T value) {
        return input -> new Success<>(value, input);
    }

    public static <T, U> Parser<U> apply(final Parser<Function<T, U>> parserFunction, final Parser<T> parserValue) {
        return map(concatEmitPair(parserFunction, parserValue), pair -> pair.left().apply(pair.right()));
    }

    public static <T, U> Parser<U> bind(final Parser<T> parser, final Function<T, Parser<U>> function) {
        return input -> switch(parser.parse(input)) {
            case Success<T> success -> function.apply(success.match()).parse(success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Function<Parser<T>, Parser<U>> lift(final Function<T, U> toLift) {
        return parser1 -> apply(pure(toLift), parser1);
    }

    public static <T, U, V> BiFunction<Parser<T>, Parser<U>, Parser<V>> lift2(final BiFunction<T, U, V> toLift) {
        return (parser1, parser2) -> apply(apply(pure(a -> b -> toLift.apply(a, b)), parser1), parser2);
    }

    public static <T, U> Parser<U> concat(final Parser<T> parser1, final Parser<U> parser2) {
        return input -> switch (parser1.parse(input)) {
            case Success<T> success -> parser2.parse(success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Parser<Pair<T, U>> concatEmitPair(final Parser<T> parser1, final Parser<U> parser2) {
        return input -> switch (parser1.parse(input)) {
            case Success<T> success1 -> switch (parser2.parse(success1.remaining())) {
                case Success<U> success2 -> new Success<>(new Pair<>(success1.match(), success2.match()), success2.remaining());
                case Failure<U> failure2 -> new Failure<>(failure2.message());
            };
            case Failure<T> failure1 -> new Failure<>(failure1.message());
        };
    }

    public static <T> Parser<T> or(final Parser<T> parser1, final Parser<T> parser2) {
        return input -> switch (parser1.parse(input)) {
            case Success<T> success -> success;
            case Failure<T> ignored -> parser2.parse(input);
        };
    }

    public static <T> Parser<T> and(final Parser<T> parser1, final Parser<T> parser2) {
        return input -> switch (parser1.parse(input)) {
            case Success<T> ignored -> parser2.parse(input);
            case Failure<T> failure -> failure;
        };
    }

    public static <T> Parser<T> anyOf(final List<Parser<T>> parsers) {
        return parsers.stream()
            .reduce(Parsers::or)
            .orElseThrow();
    }

    public static <T> Parser<List<T>> transpose(final List<Parser<T>> parsers) {
        return input -> {
            final List<T> out = new ArrayList<>();
            String remaining = input;
            for (Parser<T> parser : parsers) {
                final Result<T> result = parser.parse(remaining);
                if (result instanceof Success<T> success) {
                    out.add(success.match());
                    remaining = success.remaining();
                } else {
                    final Failure<T> failure = (Failure<T>) result;
                    return new Failure<>(failure.message());
                }
            }
            return new Success<>(out, remaining);
        };
    }

    public static <T, U> Parser<U> map(final Parser<T> parser, final Function<T, U> function) {
        return input -> switch (parser.parse(input)) {
            case Success<T> success -> new Success<>(function.apply(success.match()), success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T> Parser<List<T>> many(final Parser<T> parser) {
        return input -> switch (parser.parse(input)) {
            case Success<T> success -> parseZeroOrMore(parser, success);
            case Failure<T> ignored -> new Success<>(emptyList(), input);
        };
    }

    public static <T> Parser<List<T>> many1(final Parser<T> parser) {
        return input -> switch (parser.parse(input)) {
            case Success<T> success -> parseZeroOrMore(parser, success);
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T> Parser<T> nTimes(final Parser<T> parser, final int n) {
        return nCopies(n, parser).stream()
            .reduce(Parsers::concat)
            .orElseThrow();
    }

    public static <T, U> Parser<T> noEmitLeft(final Parser<T> parser, final Parser<U> left) {
        return input -> switch (left.parse(input)) {
            case Success<U> success -> parser.parse(success.remaining());
            case Failure<U> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Parser<T> noEmitRight(final Parser<T> parser, final Parser<U> right) {
        return input -> switch (parser.parse(input)) {
            case Success<T> success -> switch (right.parse(success.remaining())) {
                case Success<U> successR -> new Success<>(success.match(), successR.remaining());
                case Failure<U> failureR -> new Failure<>(failureR.message());
            };
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Parser<T> noEmitSurrounding(final Parser<T> parser, final Parser<U> surrounding) {
        return noEmitSurrounding(parser, surrounding, surrounding);
    }

    public static <T, U, V> Parser<T> noEmitSurrounding(final Parser<T> parser, final Parser<U> left, final Parser<V> right) {
        return input -> switch (left.parse(input)) {
            case Success<U> success -> noEmitRight(parser, right).parse(success.remaining());
            case Failure<U> failure -> new Failure<>(failure.message());
        };
    }

    public static Parser<Character> characterSatisfies(
        final Predicate<Character> matcher,
        final Function<Character, String> errorMessageMapper,
        final Supplier<String> emptyInputError
    ) {
        return input -> {
            if (input.isEmpty()) {
                return new Failure<>(emptyInputError.get());
            }
            final char character = input.charAt(0);
            if (!matcher.test(character)) {
                return new Failure<>(errorMessageMapper.apply(character));
            }

            return new Success<>(character, input.substring(1));
        };
    }

    public static Parser<Character> character(final Character expectedCharacter) {
        return characterSatisfies(expectedCharacter::equals,
            c -> "Expected '" + expectedCharacter + "' but got '" + c +"'",
            () -> "Expected '" + expectedCharacter + "' but got empty input"
        );
    }

    public static Parser<Character> anyCharacterFrom(final List<Character> chars) {
        return characterSatisfies(chars::contains,
            c -> "Unexpected character '" + c + "', expected one of " + chars,
            () -> "Expected one of " + chars + " but got empty input"
        );
    }

    public static Parser<Character> whitespaceCharacter() {
        return characterSatisfies(Character::isWhitespace,
            c -> "Expected '" + c + "' to be a whitespace character",
            () -> "Expected a whitespace character but got empty input"
        );
    }

    public static Parser<String> string(final String string) {
        return map(
            transpose(
                string.chars()
                    .mapToObj(c -> (char) c)
                    .map(Parsers::character)
                    .toList()
            ),
            Utils::charsToString
        );
    }

    private static <T> Success<List<T>> parseZeroOrMore(final Parser<T> parser, final Success<T> success) {
        final String remainingInput = success.remaining();
        final Success<List<T>> result = switch (parser.parse(remainingInput)) {
            case Success<T> success1 -> parseZeroOrMore(parser, success1);
            case Failure<T> ignored -> new Success<>(emptyList(), remainingInput);
        };
        return new Success<>(Stream.concat(
            Stream.of(success.match()),
            result.match().stream()
        ).toList(), result.remaining());
    }
}
