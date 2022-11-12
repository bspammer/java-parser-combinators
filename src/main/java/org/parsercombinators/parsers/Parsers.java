package org.parsercombinators.parsers;

import org.parsercombinators.data.Pair;
import org.parsercombinators.data.Parser;
import org.parsercombinators.data.result.Failure;
import org.parsercombinators.data.result.Result;
import org.parsercombinators.data.result.Success;
import org.parsercombinators.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    public static <T, U> Parser<U> apply(final Parser<T> parserValue, final Parser<Function<T, U>> parserFunction) {
        return map(concat(parserFunction, parserValue), pair -> pair.left().apply(pair.right()));
    }

    public static <T, U> Parser<U> bind(final Parser<T> parser, final Function<T, Parser<U>> function) {
        return input -> switch(parser.parse(input)) {
            case Success<T> success -> function.apply(success.match()).parse(success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Parser<U> map(final Parser<T> parser, final Function<T, U> function) {
        return input -> switch (parser.parse(input)) {
            case Success<T> success -> new Success<>(function.apply(success.match()), success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Function<Parser<T>, Parser<U>> lift(final Function<T, U> toLift) {
        return parser1 -> apply(parser1, pure(toLift));
    }

    public static <T, U, V> BiFunction<Parser<T>, Parser<U>, Parser<V>> lift2(final BiFunction<T, U, V> toLift) {
        return (parser1, parser2) -> apply(parser2, apply(parser1, pure(a -> b -> toLift.apply(a, b))));
    }

    public static <T, U> Parser<U> foldRight(final Parser<T> parserLeft, final Parser<U> parserRight) {
        return input -> switch (parserLeft.parse(input)) {
            case Success<T> success -> parserRight.parse(success.remaining());
            case Failure<T> failure -> new Failure<>(failure.message());
        };
    }

    public static <T, U> Parser<T> foldLeft(final Parser<T> parserLeft, final Parser<U> parserRight) {
        return input -> switch (parserLeft.parse(input)) {
            case Success<T> successLeft -> switch (parserRight.parse(successLeft.remaining())) {
                case Success<U> successRight -> new Success<>(successLeft.match(), successRight.remaining());
                case Failure<U> failureRight -> new Failure<>(failureRight.message());
            };
            case Failure<T> failureLeft -> new Failure<>(failureLeft.message());
        };
    }

    public static <T, U> Parser<Pair<T, U>> concat(final Parser<T> parserLeft, final Parser<U> parserRight) {
        return input -> switch (parserLeft.parse(input)) {
            case Success<T> successLeft -> switch (parserRight.parse(successLeft.remaining())) {
                case Success<U> successRight -> new Success<>(new Pair<>(successLeft.match(), successRight.match()), successRight.remaining());
                case Failure<U> failureRight -> new Failure<>(failureRight.message());
            };
            case Failure<T> failureLeft -> new Failure<>(failureLeft.message());
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

    public static <T> Parser<Void> not(
        final Parser<T> parser,
        final Function<T, String> failureMessageMapper
    ) {
        return input -> switch (parser.parse(input)) {
            case Success<T> success -> new Failure<>(failureMessageMapper.apply(success.match()));
            case Failure<T> ignored -> new Success<>(null, input);
        };
    }

    public static <T> Parser<Optional<T>> optional(final Parser<T> parser) {
        return or(map(parser, Optional::of), pure(Optional.empty()));
    }

    public static <T> Parser<T> anyOf(final List<Parser<T>> parsers) {
        return parsers.stream()
            .reduce(input -> new Failure<>("anyOf called with empty list of parsers"), Parsers::or);
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
            return new Success<>(out.stream().toList(), remaining);
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

    public static <T> Parser<List<T>> nTimes(final Parser<T> parser, final int n) {
        return transpose(nCopies(n, parser));
    }

    public static <T, U> Parser<T> surrounding(final Parser<T> parser, final Parser<U> surrounding) {
        return surrounding(parser, surrounding, surrounding);
    }

    public static <T, U, V> Parser<T> surrounding(final Parser<T> parser, final Parser<U> left, final Parser<V> right) {
        return foldLeft(foldRight(left, parser), right);
    }

    public static Parser<Character> characterSatisfies(
        final Predicate<Character> matcher,
        final Function<Character, String> failureMessageMapper,
        final Supplier<String> emptyInputError
    ) {
        return input -> {
            if (input.isEmpty()) {
                return new Failure<>(emptyInputError.get());
            }
            final char character = input.charAt(0);
            if (!matcher.test(character)) {
                return new Failure<>(failureMessageMapper.apply(character));
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

    public static Parser<Character> notCharacter(final Character excludedCharacter) {
        return characterSatisfies(Predicate.not(excludedCharacter::equals),
            c -> "Expected any character except '" + excludedCharacter + "' but got it",
            () -> "Expected any character except '" + excludedCharacter + "' but got empty input"
        );
    }

    public static Parser<String> characterAsString(final Character expectedCharacter) {
        return map(character(expectedCharacter), Objects::toString);
    }

    public static Parser<Character> anyCharacterFrom(final List<Character> characters) {
        return characterSatisfies(characters::contains,
            c -> "Expected one of " + characters + " but got '" + c + "'",
            () -> "Expected one of " + characters + " but got empty input"
        );
    }

    public static Parser<Character> whitespaceCharacter() {
        return characterSatisfies(Character::isWhitespace,
            c -> "Expected a whitespace character but got '" + c + "'",
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

    public static Parser<Integer> anyInteger() {
        final List<Character> numerals = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');

        final var signAndBody = concat(
            optional(character('-')),
            or(characterAsString('0'), map(many1(anyCharacterFrom(numerals)), Utils::charsToString))
        );
        return map(signAndBody, pair -> {
            final int integer = Integer.parseInt(pair.right());
            return pair.left().map(ignored -> -integer).orElse(integer);
        });
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
