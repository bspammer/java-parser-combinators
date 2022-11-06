package org.parsercombinators.parsers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.parsercombinators.data.Failure;
import org.parsercombinators.data.Parser;
import org.parsercombinators.data.Result;
import org.parsercombinators.data.Success;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.parsercombinators.parsers.Parsers.many;
import static org.parsercombinators.parsers.Parsers.many1;
import static org.parsercombinators.parsers.Parsers.character;

class ParsersTest {

    private record TestCase<T>(
        String testName,
        Parser<T> parser,
        String input,
        Result<T> expectedResult
    ) {

    }

    public static Stream<TestCase<?>> tests() {
        return Stream.of(
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
                "many1CharactersEmpty",
                many1(character('a')),
                "",
                new Failure<>("Expecting a but got something else")
            ),
            new TestCase<>(
                "singleCharacterSuccess",
                character('a'),
                "a",
                new Success<>('a', "")
            ),
            new TestCase<>(
                "singleCharacterFailure",
                character('a'),
                "b",
                new Failure<>("Expecting a but got something else")
            )
        );
    }

    @ParameterizedTest
    @MethodSource("tests")
    <T> void runTests(final TestCase<T> testCase) {
        final Result<T> result = testCase.parser.parse(testCase.input);
        assertEquals(result, testCase.expectedResult, testCase.testName);
    }

}
