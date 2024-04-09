package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        test(tokens, expected, Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Zero Statements",
                        Arrays.asList(),
                        new Ast.Source(Arrays.asList(), Arrays.asList())
                ),
                Arguments.of("Global - Immutable",
                        Arrays.asList(
                                //VAL name = expr;
                                new Token(Token.Type.IDENTIFIER, "VAL", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Global("name", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Function",
                        Arrays.asList(
                                //FUN name() DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(new Ast.Function("name", Arrays.asList(), Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                                )))
                        )
                ),
                Arguments.of("List",
                        Arrays.asList(
                                // LIST list = [expr];
                                new Token(Token.Type.IDENTIFIER, "LIST", 0),
                                new Token(Token.Type.IDENTIFIER, "list", 5),
                                new Token(Token.Type.OPERATOR, "=", 10),
                                new Token(Token.Type.OPERATOR, "[", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 13),
                                new Token(Token.Type.OPERATOR, "]", 17),
                                new Token(Token.Type.OPERATOR, ";", 18)
                        ),
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("list", true, Optional.of(
                                                new Ast.Expression.PlcList(Arrays.asList(
                                                        new Ast.Expression.Access(Optional.empty(), "expr")
                                                ))
                                        ))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Global Function",
                        Arrays.asList(
                                // VAL name = expr; FUN name() DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "VAL", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "FUN", 17),
                                new Token(Token.Type.IDENTIFIER, "name", 21),
                                new Token(Token.Type.OPERATOR, "(", 25),
                                new Token(Token.Type.OPERATOR, ")", 26),
                                new Token(Token.Type.IDENTIFIER, "DO", 28),
                                new Token(Token.Type.IDENTIFIER, "stmt", 31),
                                new Token(Token.Type.OPERATOR, ";", 35),
                                new Token(Token.Type.IDENTIFIER, "END", 37)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Global("name", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                                Arrays.asList(new Ast.Function("name", Arrays.asList(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))))
                        )
                ),
                Arguments.of("Multiple Globals",
                        Arrays.asList(
                                // VAR x = expr; VAR y = expr; VAR z = expr;
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr", 8),
                                new Token(Token.Type.OPERATOR, ";", 12),
                                new Token(Token.Type.IDENTIFIER, "VAR", 14),
                                new Token(Token.Type.IDENTIFIER, "y", 18),
                                new Token(Token.Type.OPERATOR, "=", 20),
                                new Token(Token.Type.IDENTIFIER, "expr", 22),
                                new Token(Token.Type.OPERATOR, ";", 26),
                                new Token(Token.Type.IDENTIFIER, "VAR", 28),
                                new Token(Token.Type.IDENTIFIER, "z", 32),
                                new Token(Token.Type.OPERATOR, "=", 34),
                                new Token(Token.Type.IDENTIFIER, "expr", 36),
                                new Token(Token.Type.OPERATOR, ";", 40)
                        ),
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("x", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))),
                                        new Ast.Global("y", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))),
                                        new Ast.Global("z", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Mixed Globals",
                        Arrays.asList(
                                // LIST list = [e1, e2, e3]; VAR y = expr; VAL z = expr;
                                new Token(Token.Type.IDENTIFIER, "LIST", 0),
                                new Token(Token.Type.IDENTIFIER, "list", 5),
                                new Token(Token.Type.OPERATOR, "=", 10),
                                new Token(Token.Type.OPERATOR, "[", 12),
                                new Token(Token.Type.IDENTIFIER, "e1", 13),
                                new Token(Token.Type.OPERATOR, ",", 16),
                                new Token(Token.Type.IDENTIFIER, "e2", 18),
                                new Token(Token.Type.OPERATOR, ",", 21),
                                new Token(Token.Type.IDENTIFIER, "e3", 23),
                                new Token(Token.Type.OPERATOR, "]", 25),
                                new Token(Token.Type.OPERATOR, ";", 26),
                                new Token(Token.Type.IDENTIFIER, "VAR", 28),
                                new Token(Token.Type.IDENTIFIER, "y", 32),
                                new Token(Token.Type.OPERATOR, "=", 34),
                                new Token(Token.Type.IDENTIFIER, "expr", 36),
                                new Token(Token.Type.OPERATOR, ";", 40),
                                new Token(Token.Type.IDENTIFIER, "VAL", 42),
                                new Token(Token.Type.IDENTIFIER, "z", 46),
                                new Token(Token.Type.OPERATOR, "=", 48),
                                new Token(Token.Type.IDENTIFIER, "expr", 50),
                                new Token(Token.Type.OPERATOR, ";", 54)
                        ),
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("list", true, Optional.of(
                                                new Ast.Expression.PlcList(Arrays.asList(
                                                        new Ast.Expression.Access(Optional.empty(), "e1"),
                                                        new Ast.Expression.Access(Optional.empty(), "e2"),
                                                        new Ast.Expression.Access(Optional.empty(), "e3")
                                                ))
                                        )),
                                        new Ast.Global("y", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))),
                                        new Ast.Global("z", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Multiple Functions",
                        Arrays.asList(
                                // FUN x() DO stmt; END FUN y() DO stmt; END FUN z() DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, "(", 5),
                                new Token(Token.Type.OPERATOR, ")", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "END", 17),
                                new Token(Token.Type.IDENTIFIER, "FUN", 21),
                                new Token(Token.Type.IDENTIFIER, "y", 25),
                                new Token(Token.Type.OPERATOR, "(", 26),
                                new Token(Token.Type.OPERATOR, ")", 27),
                                new Token(Token.Type.IDENTIFIER, "DO", 29),
                                new Token(Token.Type.IDENTIFIER, "stmt", 32),
                                new Token(Token.Type.OPERATOR, ";", 36),
                                new Token(Token.Type.IDENTIFIER, "END", 38),
                                new Token(Token.Type.IDENTIFIER, "FUN", 42),
                                new Token(Token.Type.IDENTIFIER, "z", 46),
                                new Token(Token.Type.OPERATOR, "(", 47),
                                new Token(Token.Type.OPERATOR, ")", 48),
                                new Token(Token.Type.IDENTIFIER, "DO", 50),
                                new Token(Token.Type.IDENTIFIER, "stmt", 53),
                                new Token(Token.Type.OPERATOR, ";", 57),
                                new Token(Token.Type.IDENTIFIER, "END", 59)
                        ),
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("x", Arrays.asList(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))),
                                        new Ast.Function("y", Arrays.asList(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))),
                                        new Ast.Function("z", Arrays.asList(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))))
                                )
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                ),
                Arguments.of("Variable",
                        Arrays.asList(
                                //expr;
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, ";", 4)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "expr"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Statement.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Definition",
                        Arrays.asList(
                                //LET name;
                                new Token(Token.Type.IDENTIFIER, "LET", -1),
                                new Token(Token.Type.IDENTIFIER, "name", -1),
                                new Token(Token.Type.OPERATOR, ";", -1)
                        ),
                        new Ast.Statement.Declaration("name", Optional.empty())
                ),
                Arguments.of("Initialization",
                        Arrays.asList(
                                //LET name = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                ),
                Arguments.of("List",
                        Arrays.asList(
                                //list[offset] = expr;
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "offset", 5),
                                new Token(Token.Type.OPERATOR, "]", 11),
                                new Token(Token.Type.OPERATOR, "=", 13),
                                new Token(Token.Type.IDENTIFIER, "expr", 15),
                                new Token(Token.Type.OPERATOR, ";", 19)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "offset")), "list"),
                                new Ast.Expression.Access(Optional.empty(), "expr")
                        )
                )
        );
    }


    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, List<Token> tokens, Ast.Statement.If expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        Arrays.asList(
                                //IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "END", 17)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Else",
                        Arrays.asList(
                                //IF expr DO stmt1; ELSE stmt2; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.OPERATOR, ";", 16),
                                new Token(Token.Type.IDENTIFIER, "ELSE", 18),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 23),
                                new Token(Token.Type.OPERATOR, ";", 28),
                                new Token(Token.Type.IDENTIFIER, "END", 30)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSwitchStatement(String test, List<Token> tokens, Ast.Statement expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Switch",
                        Arrays.asList(
                                // SWITCH expr CASE expr : stmt1; DEFAULT stmt2; END
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 17),
                                new Token(Token.Type.OPERATOR, ":", 22),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 24),
                                new Token(Token.Type.OPERATOR, ";", 29),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 31),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 39),
                                new Token(Token.Type.OPERATOR, ";", 44),
                                new Token(Token.Type.IDENTIFIER, "END", 46)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")))
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                                        )
                                )
                        )
                ),
                Arguments.of("Multiple Statements",
                        Arrays.asList(
                                // SWITCH expr CASE expr : stmt1; stmt2; stmt3; DEFAULT stmt4; END
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 17),
                                new Token(Token.Type.OPERATOR, ":", 22),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 24),
                                new Token(Token.Type.OPERATOR, ";", 29),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 31),
                                new Token(Token.Type.OPERATOR, ";", 36),
                                new Token(Token.Type.IDENTIFIER, "stmt3", 38),
                                new Token(Token.Type.OPERATOR, ";", 43),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 45),
                                new Token(Token.Type.IDENTIFIER, "stmt4", 53),
                                new Token(Token.Type.OPERATOR, ";", 58),
                                new Token(Token.Type.IDENTIFIER, "END", 60)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")),
                                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")),
                                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt3"))
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt4")))
                                        )
                                )
                        )
                ),
                Arguments.of("Multiple Cases",
                        Arrays.asList(
                                // SWITCH expr CASE expr1 : stmt1; CASE expr2 : stmt2; CASE expr3 : stmt3; DEFAULT stmt4; END
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                                new Token(Token.Type.IDENTIFIER, "expr1", 17),
                                new Token(Token.Type.OPERATOR, ":", 23),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 25),
                                new Token(Token.Type.OPERATOR, ";", 31),
                                new Token(Token.Type.IDENTIFIER, "CASE", 33),
                                new Token(Token.Type.IDENTIFIER, "expr2", 38),
                                new Token(Token.Type.OPERATOR, ":", 44),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 46),
                                new Token(Token.Type.OPERATOR, ";", 51),
                                new Token(Token.Type.IDENTIFIER, "CASE", 53),
                                new Token(Token.Type.IDENTIFIER, "expr3", 58),
                                new Token(Token.Type.OPERATOR, ":", 64),
                                new Token(Token.Type.IDENTIFIER, "stmt3", 66),
                                new Token(Token.Type.OPERATOR, ";", 71),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 73),
                                new Token(Token.Type.IDENTIFIER, "stmt4", 81),
                                new Token(Token.Type.OPERATOR, ";", 86),
                                new Token(Token.Type.IDENTIFIER, "END", 88)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr1")),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")))
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr2")),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr3")),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt3")))
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt4")))
                                        )
                                )
                        )
                ),
                Arguments.of("No Case",
                        Arrays.asList(
                                // SWITCH expr DEFAULT stmt; END
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Collections.singletonList(
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Collections.singletonList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                                                )
                                        )
                                )
                        )
                ),
                Arguments.of("No Statements",
                        Arrays.asList(
                                // SWITCH expr DEFAULT END
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Collections.singletonList(
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Collections.emptyList() // No statements in the DEFAULT block
                                        )
                                )
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhileStatement(String test, List<Token> tokens, Ast.Statement.While expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("While",
                        Arrays.asList(
                                //WHILE expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
                        )
                ),
                Arguments.of("Multiple Statements",
                        Arrays.asList(
                                //WHILE expr DO stmt1; stmt2; stmt3; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 14),
                                new Token(Token.Type.OPERATOR, ";", 19),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 21),
                                new Token(Token.Type.OPERATOR, ";", 26),
                                new Token(Token.Type.IDENTIFIER, "stmt3", 28),
                                new Token(Token.Type.OPERATOR, ";", 33),
                                new Token(Token.Type.IDENTIFIER, "END", 35)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")),
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")),
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt3"))
                                )
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStatement(String test, List<Token> tokens, Ast.Statement.Return expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
                Arguments.of("Return Statement",
                        Arrays.asList(
                                //RETURN expr;
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "expr"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Nil Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                        new Ast.Expression.Literal(null)
                ),
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\t'", 0)),
                        new Ast.Expression.Literal('\t')
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\\t\"", 0)),
                        new Ast.Expression.Literal("\t")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access",
                        Arrays.asList(
                                //list[expr]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, "]", 9)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                )
        );
    }
    @ParameterizedTest
    @MethodSource
    void testException(String test, List<Token> tokens, ParseException expected) {
        Assertions.assertThrows(ParseException.class, () -> {
            Parser parser = new Parser(tokens);
            parser.parseStatement();
        });
    }

    private static Stream<Arguments> testException() {
        return Stream.of(
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                // f
                                new Token(Token.Type.IDENTIFIER, "f", 0)
                        ),
                        new ParseException("Missing ';'", 1)
                ),
                Arguments.of("Missing Value",
                        Arrays.asList(
                                // name = ;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new ParseException("Missing value after '='", 6)
                ),
                Arguments.of("Missing DO",
                        Arrays.asList(
                                // IF expr stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "stmt", 8),
                                new Token(Token.Type.OPERATOR, ";", 12),
                                new Token(Token.Type.IDENTIFIER, "END", 14)
                        ),
                        new ParseException("Missing 'DO' keyword", 8)
                ),
                Arguments.of("Missing END",
                        Arrays.asList(
                                // WHILE expr DO stmt;
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18)
                        ),
                        new ParseException("Missing 'END' keyword", 19)
                ),
                Arguments.of("Missing Value for RETURN",
                        Arrays.asList(
                                // RETURN;
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new ParseException("Missing value for return statement", 6)
                ),
                Arguments.of("Function Global",
                        Arrays.asList(
                                //FUN name() DO stmt; END VAR name = expr;
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20),
                                new Token(Token.Type.IDENTIFIER, "VAR", 24),
                                new Token(Token.Type.IDENTIFIER, "name", 28),
                                new Token(Token.Type.OPERATOR, "=", 33),
                                new Token(Token.Type.IDENTIFIER, "expr", 35),
                                new Token(Token.Type.OPERATOR, ";", 39)
                        ),
                        new ParseException("Global before function", 24)
                ),
                Arguments.of("Missing Expression",
                        Arrays.asList(
                                // LIST list = [];
                                new Token(Token.Type.IDENTIFIER, "LIST", 0),
                                new Token(Token.Type.IDENTIFIER, "list", 5),
                                new Token(Token.Type.OPERATOR, "=", 10),
                                new Token(Token.Type.OPERATOR, "[", 12),
                                new Token(Token.Type.OPERATOR, "]", 13),
                                new Token(Token.Type.OPERATOR, ";", 14)
                        ),
                        new ParseException("Missing expression for list initialization", 13)
                ),
                Arguments.of("Missing End",
                        Arrays.asList(
                                // FUN name() DO stmt;
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 9),
                                new Token(Token.Type.OPERATOR, ")", 10),
                                new Token(Token.Type.IDENTIFIER, "DO", 12),
                                new Token(Token.Type.IDENTIFIER, "stmt", 15),
                                new Token(Token.Type.OPERATOR, ";", 19)
                        ),
                        new ParseException("Missing 'END' keyword", 20)
                ),
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                // LET name
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4)
                        ),
                        new ParseException("Missing ';'", 5)
                ),
                Arguments.of("Missing END",
                        Arrays.asList(
                                // IF expr DO stmt;
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new ParseException("Missing 'END' keyword", 16)
                ),
                Arguments.of("No Default",
                        Arrays.asList(
                                // SWITCH expr CASE expr : stmt1; END
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 17),
                                new Token(Token.Type.OPERATOR, ":", 22),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 24),
                                new Token(Token.Type.OPERATOR, ";", 29),
                                new Token(Token.Type.IDENTIFIER, "END", 31)
                        ),
                        new ParseException("Expected 'DEFAULT' case", 31)
                ),
                Arguments.of("No Expression",
                        Arrays.asList(
                                // SWITCH DEFAULT END
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 7),
                                new Token(Token.Type.IDENTIFIER, "END", 15)
                        ),
                        new ParseException("Missing expression after 'SWITCH'", 7)
                ),
                Arguments.of("No End",
                        Arrays.asList(
                                // SWITCH expr DEFAULT stmt2;
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 20),
                                new Token(Token.Type.OPERATOR, ";", 25)
                        ),
                        new ParseException("Expected 'END' keyword", 25)
                ),
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                // RETURN expr
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7)
                        ),
                        new ParseException("Missing ';'", 8)
                ),
                Arguments.of("Missing DO",
                        Arrays.asList(
                                // IF expr
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3)
                        ),
                        new ParseException("Missing 'DO' keyword", 4)
                )
        );
    }

    @Test
    void testExample1() {
        List<Token> input = Arrays.asList(
                /* VAR first = 1;
                 * FUN main() DO
                 *     WHILE first != 10 DO
                 *         print(first);
                 *         first = first + 1;
                 *     END
                 * END
                 */
                //VAR first = 1;
                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                new Token(Token.Type.IDENTIFIER, "first", 4),
                new Token(Token.Type.OPERATOR, "=", 10),
                new Token(Token.Type.INTEGER, "1", 12),
                new Token(Token.Type.OPERATOR, ";", 13),
                //FUN main() DO
                new Token(Token.Type.IDENTIFIER, "FUN", 15),
                new Token(Token.Type.IDENTIFIER, "main", 19),
                new Token(Token.Type.OPERATOR, "(", 23),
                new Token(Token.Type.OPERATOR, ")", 24),
                new Token(Token.Type.IDENTIFIER, "DO", 26),
                //    WHILE first != 10 DO
                new Token(Token.Type.IDENTIFIER, "WHILE", 33),
                new Token(Token.Type.IDENTIFIER, "first", 39),
                new Token(Token.Type.OPERATOR, "!=", 45),
                new Token(Token.Type.INTEGER, "10", 48),
                new Token(Token.Type.IDENTIFIER, "DO", 51),
                //        print(first);
                new Token(Token.Type.IDENTIFIER, "print", 62),
                new Token(Token.Type.OPERATOR, "(", 67),
                new Token(Token.Type.IDENTIFIER, "first", 68),
                new Token(Token.Type.OPERATOR, ")", 73),
                new Token(Token.Type.OPERATOR, ";", 74),
                //        first = first + 1;
                new Token(Token.Type.IDENTIFIER, "first", 84),
                new Token(Token.Type.OPERATOR, "=", 90),
                new Token(Token.Type.IDENTIFIER, "first", 92),
                new Token(Token.Type.OPERATOR, "+", 98),
                new Token(Token.Type.INTEGER, "1", 100),
                new Token(Token.Type.OPERATOR, ";", 101),
                //    END
                new Token(Token.Type.IDENTIFIER, "END", 107),
                //END
                new Token(Token.Type.IDENTIFIER, "END", 111)
        );
        Ast.Source expected = new Ast.Source(
                Arrays.asList(new Ast.Global("first", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(
                        new Ast.Statement.While(
                                new Ast.Expression.Binary("!=",
                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                        new Ast.Expression.Literal(BigInteger.TEN)
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function("print", Arrays.asList(
                                                        new Ast.Expression.Access(Optional.empty(), "first"))
                                                )
                                        ),
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "first"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                                        new Ast.Expression.Literal(BigInteger.ONE)
                                                )
                                        )
                                )
                        )
                ))
        ));
    }

    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        T exp = expected;
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
