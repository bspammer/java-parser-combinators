package org.parsercombinators.parsers;

import org.parsercombinators.data.Failure;
import org.parsercombinators.data.Pair;
import org.parsercombinators.data.Parser;
import org.parsercombinators.data.Result;
import org.parsercombinators.data.Success;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;

public class Parsers {

    public static <T, U> Parser<U> concat(final Parser<T> parser1, final Parser<U> parser2) {
        return input -> switch (parser1.parse(input)) {
            case Success<T> success -> parser2.parse(success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Parser<Pair<T, U>> concatPair(final Parser<T> parser1, final Parser<U> parser2) {
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

    public static <T, R> Parser<R> map(final Parser<T> parser, final Function<T, R> function) {
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

    public static Parser<Character> character(final Character expectedCharacter) {
        return input -> {
            if (input.isEmpty()) {
                return new Failure<>("Expected " + expectedCharacter + " but got empty input");
            }
            final char actualCharacter = input.charAt(0);
            if (actualCharacter != expectedCharacter) {
                return new Failure<>("Expected " + expectedCharacter + " but got " + actualCharacter);
            }

            return new Success<>(expectedCharacter, input.substring(1));
        };
    }

    public static Parser<Character> anyCharacterFrom(final List<Character> chars) {
        return anyOf(chars.stream()
            .map(Parsers::character)
            .collect(Collectors.toList()));
    }

    public static Parser<String> string(final String string) {
        return map(
            transpose(
                string.chars()
                    .mapToObj(Character.class::cast)
                    .map(Parsers::character)
                    .toList()
            ),
            cs -> cs.stream()
                .map(c -> Character.toString(c))
                .reduce("", String::concat)
        );
    }

    public static <T> Success<List<T>> parseZeroOrMore(final Parser<T> parser, final Success<T> success) {
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

    public static <T, U> Parser<T> parseButIgnoreRight(final Parser<T> parser, final Parser<U> toIgnore) {
        return input -> switch (parser.parse(input)) {
            case Success<T> success -> switch (toIgnore.parse(success.remaining())) {
                case Success<U> successR -> new Success<>(success.match(), successR.remaining());
                case Failure<U> failureR -> new Failure<>(failureR.message());
            };
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Parser<T> parseButIgnoreLeft(final Parser<T> parser, final Parser<U> toIgnore) {
        return input -> switch (toIgnore.parse(input)) {
            case Success<T> success -> parser.parse(success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

}
