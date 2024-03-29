package org.parsercombinators.parsers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.parsercombinators.data.Pair;
import org.parsercombinators.data.Parser;
import org.parsercombinators.data.result.Failure;
import org.parsercombinators.data.result.Result;
import org.parsercombinators.data.result.Success;
import org.parsercombinators.utils.Utils;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.parsercombinators.parsers.Parsers.and;
import static org.parsercombinators.parsers.Parsers.anyCharacterFrom;
import static org.parsercombinators.parsers.Parsers.anyInteger;
import static org.parsercombinators.parsers.Parsers.anyOf;
import static org.parsercombinators.parsers.Parsers.character;
import static org.parsercombinators.parsers.Parsers.characterAsString;
import static org.parsercombinators.parsers.Parsers.concat;
import static org.parsercombinators.parsers.Parsers.foldLeft;
import static org.parsercombinators.parsers.Parsers.foldRight;
import static org.parsercombinators.parsers.Parsers.many;
import static org.parsercombinators.parsers.Parsers.many1;
import static org.parsercombinators.parsers.Parsers.map;
import static org.parsercombinators.parsers.Parsers.nTimes;
import static org.parsercombinators.parsers.Parsers.not;
import static org.parsercombinators.parsers.Parsers.notCharacter;
import static org.parsercombinators.parsers.Parsers.optional;
import static org.parsercombinators.parsers.Parsers.or;
import static org.parsercombinators.parsers.Parsers.string;
import static org.parsercombinators.parsers.Parsers.surrounding;
import static org.parsercombinators.parsers.Parsers.transpose;
import static org.parsercombinators.parsers.Parsers.untilCharacter;
import static org.parsercombinators.parsers.Parsers.whitespaceCharacter;
import static org.parsercombinators.parsers.Parsers.noMoreInput;

class ParsersTest {

    private record TestCase<T>(
        String testName,
        Parser<T> parser,
        String input,
        Result<T> expectedResult
    ) {}

    public static List<TestCase<?>> tests() {
        final List<TestCase<?>> tests = List.of(
            new TestCase<>(
                "concatCharacters",
                foldRight(character('a'), character('b')),
                "abaaa",
                new Success<>('b', "aaa")
            ),
            new TestCase<>(
                "concatCharactersFailure",
                foldRight(character('a'), character('b')),
                "a",
                new Failure<>("Expected 'b' but got empty input")
            ),
            new TestCase<>(
                "concatEmitPairCharacters",
                concat(character('a'), character('b')),
                "abaaa",
                new Success<>(new Pair<>('a', 'b'), "aaa")
            ),
            new TestCase<>(
                "concatEmitPairCharactersFailure",
                concat(character('a'), character('b')),
                "a",
                new Failure<>("Expected 'b' but got empty input")
            ),
            new TestCase<>(
                "orCharactersA",
                or(character('a'), character('b')),
                "abbb",
                new Success<>('a', "bbb")
            ),
            new TestCase<>(
                "orCharactersB",
                or(character('a'), character('b')),
                "baaa",
                new Success<>('b', "aaa")
            ),
            new TestCase<>(
                "orCharactersFailure",
                or(character('a'), character('b')),
                "cccc",
                new Failure<>("Expected 'b' but got 'c'")
            ),
            new TestCase<>(
                "optional",
                optional(character('a')),
                "abcd",
                new Success<>(Optional.of('a'), "bcd")
            ),
            new TestCase<>(
                "optionalEmpty",
                optional(character('a')),
                "",
                new Success<>(Optional.empty(), "")
            ),
            new TestCase<>(
                "optionalNoMatch",
                optional(character('a')),
                "bcde",
                new Success<>(Optional.empty(), "bcde")
            ),
            new TestCase<>(
                "andCharacters",
                and(character('a'), character('a')),
                "abbb",
                new Success<>('a', "bbb")
            ),
            new TestCase<>(
                "notCombinatorCharacters",
                not(character('a'), c -> "Expected not 'a' but got 'a'"),
                "baaa",
                new Success<>(null, "baaa")
            ),
            new TestCase<>(
                "notCombinatorCharactersFailure",
                not(character('a'), c -> "Expected not 'a' but got 'a'"),
                "aaaa",
                new Failure<>("Expected not 'a' but got 'a'")
            ),
            new TestCase<>(
                "andCharactersFailure",
                and(character('a'), character('b')),
                "aaaa",
                new Failure<>("Expected 'b' but got 'a'")
            ),
            new TestCase<>(
                "anyOfCharactersA",
                anyOf(List.of(character('a'), character('b'))),
                "abbb",
                new Success<>('a', "bbb")
            ),
            new TestCase<>(
                "anyOfCharactersB",
                anyOf(List.of(character('a'), character('b'))),
                "baaa",
                new Success<>('b', "aaa")
            ),
            new TestCase<>(
                "anyOfCharactersFailure",
                anyOf(List.of(character('a'), character('b'))),
                "caaa",
                new Failure<>("Expected 'b' but got 'c'")
            ),
            new TestCase<>(
                "transposeCharacters",
                transpose(List.of(character('a'), character('b'))),
                "abaa",
                new Success<>(List.of('a', 'b'), "aa")
            ),
            new TestCase<>(
                "transposeCharactersFailure",
                transpose(List.of(character('a'), character('b'))),
                "acaa",
                new Failure<>("Expected 'b' but got 'c'")
            ),
            new TestCase<>(
                "transposeCharactersEmptyFailure",
                transpose(List.of(character('a'), character('b'))),
                "",
                new Failure<>("Expected 'a' but got empty input")
            ),
            new TestCase<>(
                "mapCharactersToNumeric",
                map(Character::getNumericValue, character('a')),
                "abbb",
                new Success<>(10, "bbb")
            ),
            new TestCase<>(
                "mapCharactersToNumericFailure",
                map(Character::getNumericValue, character('a')),
                "bbbb",
                new Failure<>("Expected 'a' but got 'b'")
            ),
            new TestCase<>(
                "manyCharactersAll",
                many(character('a')),
                "aaaaa",
                new Success<>(List.of('a', 'a', 'a', 'a', 'a'), "")
            ),
            new TestCase<>(
                "manyCharactersPartial",
                many(character('a')),
                "aaabb",
                new Success<>(List.of('a', 'a', 'a'), "bb")
            ),
            new TestCase<>(
                "manyCharactersEmpty",
                many(character('a')),
                "bbbbb",
                new Success<>(emptyList(), "bbbbb")
            ),
            new TestCase<>(
                "many1CharactersAll",
                many1(character('a')),
                "aaaaa",
                new Success<>(List.of('a', 'a', 'a', 'a', 'a'), "")
            ),
            new TestCase<>(
                "many1CharactersPartial",
                many1(character('a')),
                "aaabb",
                new Success<>(List.of('a', 'a', 'a'), "bb")
            ),
            new TestCase<>(
                "many1CharactersEmptyFailure",
                many1(character('a')),
                "",
                new Failure<>("Expected 'a' but got empty input")
            ),
            new TestCase<>(
                "nTimesCharacters",
                nTimes(character('a'), 5),
                "aaaaabb",
                new Success<>(List.of('a', 'a', 'a', 'a', 'a'), "bb")
            ),
            new TestCase<>(
                "nTimesCharactersShortFailure",
                nTimes(character('a'), 5),
                "aaa",
                new Failure<>("Expected 'a' but got empty input")
            ),
            new TestCase<>(
                "nTimesCharactersWrongCharacterFailure",
                nTimes(character('a'), 5),
                "aaabbb",
                new Failure<>("Expected 'a' but got 'b'")
            ),
            new TestCase<>(
                "noEmitRightCharacters",
                foldLeft(character('a'), character('b')),
                "ababab",
                new Success<>('a', "abab")
            ),
            new TestCase<>(
                "noEmitRightCharactersFailure",
                foldLeft(character('a'), character('b')),
                "acabab",
                new Failure<>("Expected 'b' but got 'c'")
            ),
            new TestCase<>(
                "noEmitSurroundingCharacters",
                surrounding(character('a'), character('"')),
                "\"a\"abab",
                new Success<>('a', "abab")
            ),
            new TestCase<>(
                "noEmitSurroundingCharactersFailure",
                surrounding(character('a'), character('"')),
                "a\"abab",
                new Failure<>("Expected '\"' but got 'a'")
            ),
            new TestCase<>(
                "noEmitSurroundingOverloadCharacters",
                surrounding(character('a'), character('<'), character('>')),
                "<a>abab",
                new Success<>('a', "abab")
            ),
            new TestCase<>(
                "noEmitSurroundingOverloadCharactersFailure",
                surrounding(character('a'), character('<'), character('>')),
                ">a>abab",
                new Failure<>("Expected '<' but got '>'")
            ),
            new TestCase<>(
                "character",
                character('a'),
                "a",
                new Success<>('a', "")
            ),
            new TestCase<>(
                "characterFailure",
                character('a'),
                "b",
                new Failure<>("Expected 'a' but got 'b'")
            ),
            new TestCase<>(
                "notCharacter",
                notCharacter('a'),
                "bcdef",
                new Success<>('b', "cdef")
            ),
            new TestCase<>(
                "notCharacterFailure",
                notCharacter('a'),
                "abcde",
                new Failure<>("Expected any character except 'a' but got it")
            ),
            new TestCase<>(
                "notCharacterFailureEmpty",
                notCharacter('a'),
                "",
                new Failure<>("Expected any character except 'a' but got empty input")
            ),
            new TestCase<>(
                "characterAsString",
                characterAsString('a'),
                "abab",
                new Success<>("a", "bab")
            ),
            new TestCase<>(
                "characterAsStringFailure",
                characterAsString('a'),
                "cbab",
                new Failure<>("Expected 'a' but got 'c'")
            ),
            new TestCase<>(
                "anyCharacterFrom",
                anyCharacterFrom(List.of('a', 'b', 'c')),
                "caaaa",
                new Success<>('c', "aaaa")
            ),
            new TestCase<>(
                "anyCharacterFromFailure",
                anyCharacterFrom(List.of('a', 'b', 'c')),
                "daaaa",
                new Failure<>("Expected one of [a, b, c] but got 'd'")
            ),
            new TestCase<>(
                "anyCharacterFromEmptyFailure",
                anyCharacterFrom(List.of('a', 'b', 'c')),
                "",
                new Failure<>("Expected one of [a, b, c] but got empty input")
            ),
            new TestCase<>(
                "string",
                string("ababab"),
                "abababab",
                new Success<>("ababab", "ab")
            ),
            new TestCase<>(
                "stringEmpty",
                string(""),
                "abababab",
                new Success<>("", "abababab")
            ),
            new TestCase<>(
                "stringFailure",
                string("ababab"),
                "abcabcabc",
                new Failure<>("Expected 'a' but got 'c'")
            ),
            new TestCase<>(
                "untilCharacter",
                untilCharacter('\n'),
                "some long string then\nanother one",
                new Success<>("some long string then", "another one")
            ),
            new TestCase<>(
                "untilCharacterFailure",
                untilCharacter('\n'),
                "some long string then",
                new Failure<>("Expected '\n' but got empty input")
            ),
            new TestCase<>(
                "whitespaceCharacter",
                whitespaceCharacter(),
                " ab",
                new Success<>(' ', "ab")
            ),
            new TestCase<>(
                "whitespaceCharacterFailure",
                whitespaceCharacter(),
                "ab",
                new Failure<>("Expected a whitespace character but got 'a'")
            ),
            new TestCase<>(
                "whitespaceCharacterFailureEmpty",
                whitespaceCharacter(),
                "",
                new Failure<>("Expected a whitespace character but got empty input")
            ),
            new TestCase<>(
                "manyWhitespace",
                map(Utils::charsToString, many(whitespaceCharacter())),
                " \r\n\t\fab",
                new Success<>(" \r\n\t\f", "ab")
            ),
            new TestCase<>(
                "anyInteger",
                anyInteger(),
                "187654329abc",
                new Success<>(187654329, "abc")
            ),
            new TestCase<>(
                "anyIntegerTwice",
                concat(anyInteger(), anyInteger()),
                "187654329-101010cde",
                new Success<>(new Pair<>(187654329, -101010), "cde")
            ),
            new TestCase<>(
                "anyIntegerLeadingZero",
                anyInteger(),
                "01234",
                new Success<>(0, "1234")
            ),
            new TestCase<>(
                "quotedInteger",
                surrounding(anyInteger(), character('"')),
                "\"54321\"asdf",
                new Success<>(54321, "asdf")
            ),
            new TestCase<>(
                "noMoreInput",
                noMoreInput(),
                "",
                new Success<>("", "")
            ),
            new TestCase<>(
                "noMoreInputFailure",
                noMoreInput(),
                "hello",
                new Failure<>("Expected end of string but was non-empty")
            )
        );
        assertThat(tests.stream().map(TestCase::testName)).doesNotHaveDuplicates();

        return tests;
    }

    @ParameterizedTest
    @MethodSource("tests")
    <T> void tests(final TestCase<T> testCase) {
        final Result<T> result = testCase.parser.parse(testCase.input);
        assertEquals(testCase.expectedResult, result, testCase.testName);
    }

}
