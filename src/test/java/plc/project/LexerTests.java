package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "get-Name_5", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Initial @", "@11111", true),
                Arguments.of("Leading underscore", "_getName", false),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "111fish2fish3fishbluefish", false),
                Arguments.of("Not initial @", "csebfc@@@", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Trailing Zero", "10000", true),
                Arguments.of("Leading Zero", "00001", false),
                Arguments.of("Zero", "-0", false),
                Arguments.of("+Zero", "+0", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-10.0000", true),
                Arguments.of("trailing zero", "1.5000", true),
                Arguments.of("trailing zero", "0.5000", true),
                Arguments.of("Leading zero", "000001.5", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Integer", "1", false),

                Arguments.of("Positive Decimal", "88.88", true),
                Arguments.of("Negative Decimal", "-10.6", true),
                Arguments.of("Less Than One", "0.0000006", true),
                Arguments.of("Trailing Zeros", "1.000000000000", true),
                Arguments.of("Positive Zero", "0.0", true),
                Arguments.of("Negative Zero", "-0.0", true),
                Arguments.of("Leading Zeros", "0000005.4", false),
                Arguments.of("Missing Decimal Point", "34", false),
                Arguments.of("Missing Integer Part", ".5", false),
                Arguments.of("Extra spaces", "0.4    ", false),
                Arguments.of("Zero", "0000000.0000000", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "'c'", true),
                Arguments.of("Escape \b", "'\\b'", true),
                Arguments.of("Escape \n", "'\\n'", true),
                Arguments.of("Escape \r", "\'\\r\'", true),
                Arguments.of("Escape \t", "\'\\t\'", true),
                Arguments.of("Escape \'", "\'\\'\'", true),
                Arguments.of("Escape \\", "\'\\\\'", true),
                Arguments.of("Empty", "\'      \'", false),
                Arguments.of("Multiple", "\'abc\'", false),

                Arguments.of("Single Element", "'a'", true)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("symbol", "\"&\"", true),
                Arguments.of("operator", "\"+\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),

                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("Have spaces", "\"I am a cat\"", true),
                Arguments.of("Escape Characters", "\"\\n\"", true),
                Arguments.of("Numbers", "\"12345\"", true),
                Arguments.of("Symbols", "\"!@#$%^\"", true),
                Arguments.of("Missing Two Quotes", "", false),
                Arguments.of("Missing Start Quote", "heisaboy\"", false),
                Arguments.of("Missing End Quote", "\"hello world!", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Invalid Escape 2", "\"\\c\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Character", "$", true),
                Arguments.of("Character", "+", true),
                Arguments.of("Character", "-", true),
                Arguments.of("Character", "*", true),
                Arguments.of("Character", "/", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Tab2", "\b", false),
                Arguments.of("Tab3", "\n", false),
                Arguments.of("Tab4", "\r", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "0 1 2 3", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.INTEGER, "1", 2),
                        new Token(Token.Type.INTEGER, "2", 4),
                        new Token(Token.Type.INTEGER, "3", 6)
                )),
                Arguments.of("Example 4", "1fish2fish3fishbluefish", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.IDENTIFIER, "fish2fish3fishbluefish", 1)
                )),
                Arguments.of("Example 5", "1.2.3", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "1.2", 0),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.INTEGER, "3", 4)
                )),
                Arguments.of("Example 5", ".3", Arrays.asList(
                        new Token(Token.Type.OPERATOR, ".", 0),
                        new Token(Token.Type.INTEGER, "3", 1)
                )),
                Arguments.of("Example 5", "1.", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.OPERATOR, ".", 1)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }
}
