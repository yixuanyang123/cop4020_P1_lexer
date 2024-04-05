package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */

    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();
        while(peek("LIST") || peek("VAR") || peek("VAL"))
            globals.add(parseGlobal());
        while(peek("FUN"))
            functions.add(parseFunction());
        if (!tokens.has(0))
            return new Ast.Source(globals, functions);
        else
            throw new ParseException("Wrong", tokens.get(0).getIndex());
    } //TODO

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if (peek("LIST")) {
            Ast.Global list = parseList();
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing ;", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (!match(";"))
                throw new ParseException("Missing ;", tokens.get(0).getIndex());
            return list;
        }
        else if (peek("VAR")) {
            Ast.Global list = parseMutable();
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing ;", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (!match(";"))
                throw new ParseException("Missing ;", tokens.get(0).getIndex());
            return list;
        }
        else if (peek("VAL")){
            Ast.Global list = parseImmutable();
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing ;", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (!match(";"))
                throw new ParseException("Missing ;", tokens.get(0).getIndex());
            return list;
        }
        throw new ParseException("Expected global declaration", tokens.get(0).getIndex());
    } //TODO

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected 'LIST'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("LIST"))
            throw new ParseException("Expected 'LIST'", tokens.get(0).getIndex());


        Token nameToken = tokens.get(0);
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected identifier", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected identifier", nameToken.getIndex());
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected '='", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("="))
            throw new ParseException("Expected '='", tokens.get(0).getIndex());
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected '['", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("["))
            throw new ParseException("Expected '['", tokens.get(0).getIndex());


        List<Ast.Expression> expressions = new ArrayList<>();
        while (!peek("]")) {
            expressions.add(parseExpression());
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Expected ',' or ']'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (!match(",")) {
                if (!peek("]")) throw new ParseException("Expected ',' or ']'", tokens.get(0).getIndex());
            }
        }
        if (!match("]")) throw new ParseException("Expected ']'", tokens.get(0).getIndex());

        return new Ast.Global(nameToken.getLiteral(), true, Optional.of(new Ast.Expression.PlcList(expressions)));
    } //TODO

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected 'VAR'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("VAR"))
            throw new ParseException("Expected 'VAR'", tokens.get(0).getIndex());

        Token nameToken = tokens.get(0);
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected identifier", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected identifier", tokens.get(0).getIndex());

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        return new Ast.Global(nameToken.getLiteral(), true, value);
    } //TODO

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected 'VAL'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("VAL"))
            throw new ParseException("Expected 'VAL'", tokens.get(0).getIndex());

        Token nameToken = tokens.get(0);
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected identifier", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected identifier", tokens.get(0).getIndex());
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected '='", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("="))
            throw new ParseException("Expected '='", tokens.get(0).getIndex());

        Ast.Expression value = parseExpression();

        return new Ast.Global(nameToken.getLiteral(), false, Optional.of(value));
    } //TODO

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected 'FUN'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("FUN"))
            throw new ParseException("Expected 'FUN'", tokens.get(0).getIndex());

        Token nameToken = tokens.get(0);
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected identifier", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match(Token.Type.IDENTIFIER))
            throw new ParseException("Expected identifier", tokens.get(0).getIndex());
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected '('", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("("))
            throw new ParseException("Expected '('", tokens.get(0).getIndex());

        List<String> parameters = new java.util.ArrayList<>();
        while (!peek(")")) {
            Token paramToken = tokens.get(0);
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Expected identifier", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (!match(Token.Type.IDENTIFIER))
                throw new ParseException("Expected identifier", tokens.get(0).getIndex());
            parameters.add(paramToken.getLiteral());
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Expected ',' or ')'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (!match(",")) {
                if (!peek(")")) throw new ParseException("Expected ',' or ')'", tokens.get(0).getIndex());
            }
        }
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected ')'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match(")")) throw new ParseException("Expected ')'", tokens.get(0).getIndex());
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected 'DO'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("DO")) throw new ParseException("Expected 'DO'", tokens.get(0).getIndex());

        List<Ast.Statement> statements = parseBlock();

        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("END")) throw new ParseException("Expected 'END'", tokens.get(0).getIndex());

        return new Ast.Function(nameToken.getLiteral(), parameters, statements);
    } //TODO

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new java.util.ArrayList<>();
        while (!peek("END") && !peek("CASE") && !peek("DEFAULT") && !peek("ELSE")) {
            statements.add(parseStatement());
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Expected 'END'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
        }
        return statements;
    } //TODO

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("SWITCH")) {
            return parseSwitchStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else {
            // Directly parsing expression or assignment statement
            Ast.Expression expression = parseExpression();
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (match("=")) {
                Ast.Expression value = parseExpression();
                if(!tokens.has(0)) {
                    tokens.index--;
                    throw new ParseException("Expected ';'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
                }
                if (!match(";"))
                    throw new ParseException("Expected ';'", tokens.get(0).getIndex());

                return new Ast.Statement.Assignment(expression, value);
            }
            else {
                if(!tokens.has(0)) {
                    tokens.index--;
                    throw new ParseException("Expected ';'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
                }
                if (!match(";"))
                    throw new ParseException("Expected ';'", tokens.get(0).getIndex());
                return new Ast.Statement.Expression(expression);
            }
        }
    } //TODO

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if (!match("LET")) {
            throw new ParseException("Expected 'LET'", tokens.has(0) ? tokens.get(0).getIndex() : 0);
        }
        if (!tokens.has(0) || tokens.get(0).getType() != Token.Type.IDENTIFIER) {
            throw new ParseException("Expected identifier after 'LET'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.index);
        }
        Token nameToken = tokens.get(0);
        tokens.advance();
        Optional<String> typeName = Optional.empty();
        if (match(":")) {
            if (!tokens.has(0) || tokens.get(0).getType() != Token.Type.IDENTIFIER) {
                throw new ParseException("Expected type name after ':'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.index);
            }
            Token typeToken = tokens.get(0);
            typeName = Optional.of(typeToken.getLiteral());
            tokens.advance();
        }
        Optional<Ast.Expression> initializer = Optional.empty();
        if (match("=")) {
            initializer = Optional.of(parseExpression());
        }
        if (!match(";")) {
            throw new ParseException("Expected ';' after declaration", tokens.has(0) ? tokens.get(0).getIndex() : tokens.index);
        }
        return new Ast.Statement.Declaration(nameToken.getLiteral(), typeName, initializer);
    } //TODO

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Missing", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("IF"))
            throw new ParseException("Expected 'IF'", tokens.get(0).getIndex());
        Ast.Expression condition = parseExpression();

        if (!match("DO"))
            throw new ParseException("Expected 'DO'", tokens.get(0).getIndex());

        List<Ast.Statement> thenBlock = parseBlock();
        List<Ast.Statement> elseBlock = new java.util.ArrayList<>();
        if (match("ELSE"))
            elseBlock = parseBlock();
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("END"))
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex());

        return new Ast.Statement.If(condition, thenBlock, elseBlock);
    } //TODO

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        if (!match("SWITCH")) {
            throw new ParseException("Expected 'SWITCH'", tokens.get(0).getIndex());
        }

        Ast.Expression condition = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();
        boolean foundDefault = false;

        while (tokens.has(0) && !peek("END")) {
            if (match("CASE")) {
                Ast.Expression caseExpression = parseExpression();
                if (!match(":")) {
                    throw new ParseException("Expected ':' after CASE expression", tokens.get(0).getIndex());
                }
                List<Ast.Statement> caseStatements = parseBlock();
                cases.add(new Ast.Statement.Case(Optional.of(caseExpression), caseStatements));
            } else if (match("DEFAULT")) {
                if (foundDefault) {
                    throw new ParseException("Duplicate 'DEFAULT' case", tokens.get(0).getIndex());
                }
                List<Ast.Statement> defaultStatements = parseBlock();
                cases.add(new Ast.Statement.Case(Optional.empty(), defaultStatements));
                foundDefault = true;
            } else {
                throw new ParseException("Unexpected token in SWITCH statement", tokens.get(0).getIndex());
            }
        }

        if (!foundDefault) {
            throw new ParseException("Expected 'DEFAULT' case", tokens.get(0).getIndex());
        }

        if (!match("END")) {
            throw new ParseException("Expected 'END' after SWITCH statement", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Switch(condition, cases);
    } //TODO

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Missing", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("CASE"))
            throw new ParseException("Expected 'CASE'", tokens.get(0).getIndex());
        Ast.Expression value = parseExpression();
        if (!match(":"))
            throw new ParseException("Expected ':'", tokens.get(0).getIndex());
        List<Ast.Statement> statements = parseBlock();
        return new Ast.Statement.Case(Optional.of(value), statements);
    } //TODO

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if(!tokens.has(0)) {
            tokens.index--;
            throw new ParseException("Missing", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
        }
        if (!match("WHILE"))
            throw new ParseException("Expected 'WHILE'", tokens.get(0).getIndex());
        Ast.Expression condition = parseExpression();  // Parse the condition for the while loop.
        if (!match("DO"))
            throw new ParseException("Expected 'DO'", tokens.get(0).getIndex());
        List<Ast.Statement> statements = parseBlock();  // Parse the block of statements to execute.
        if (!match("END"))
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex());

        return new Ast.Statement.While(condition, statements);
    } //TODO

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        if (!tokens.has(0)) {
            throw new ParseException("Missing expression after 'RETURN'", tokens.index);
        }
        if (!match("RETURN")) {
            throw new ParseException("Expected 'RETURN'", tokens.get(0).getIndex());
        }
        Ast.Expression value = parseExpression();
        if (!match(";")) {
            throw new ParseException("Missing ';'", tokens.index);
        }
        return new Ast.Statement.Return(value);
    } //TODO

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    } //TODO

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression expression = parseComparisonExpression();
        while (peek("&&") || peek("||")) {
            String operator = tokens.get(0).getLiteral();
            tokens.advance(); // Consume the operator
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing Operand", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            Ast.Expression right = parseComparisonExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    } //TODO

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression expression = parseAdditiveExpression();
        while (peek("<") || peek(">") || peek("==") || peek("!=")) {
            String operator = tokens.get(0).getLiteral();
            tokens.advance(); // Consume the operator
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing Operand", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            Ast.Expression right = parseAdditiveExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    } //TODO

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expression = parseMultiplicativeExpression();
        while (peek("+") || peek("-")) {
            String operator = tokens.get(0).getLiteral();
            tokens.advance(); // Consume the operator
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing Operand", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            Ast.Expression right = parseMultiplicativeExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    } //TODO

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expression = parsePrimaryExpression();
        while (peek("*") || peek("/") || peek("^")) {
            String operator = tokens.get(0).getLiteral();
            tokens.advance(); // Consume the operator
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing Operand", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            Ast.Expression right = parsePrimaryExpression();
            expression = new Ast.Expression.Binary(operator, expression, right);
        }
        return expression;
    } //TODO

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL")) {
            tokens.advance();
            return new Ast.Expression.Literal(null);
        }
        else if (peek("TRUE") || peek("FALSE")) {
            Token token = tokens.get(0);
            tokens.advance();
            return new Ast.Expression.Literal(Boolean.parseBoolean(token.getLiteral()));
        }
        else if (peek(Token.Type.INTEGER)) {
            Token token = tokens.get(0);
            tokens.advance();
            return new Ast.Expression.Literal(new BigInteger(token.getLiteral()));
        }
        else if (peek(Token.Type.DECIMAL)) {
            Token token = tokens.get(0);
            tokens.advance();
            return new Ast.Expression.Literal(new BigDecimal(token.getLiteral()));
        }
        else if (peek(Token.Type.CHARACTER)) {
            Token token = tokens.get(0);
            String literalString = token.getLiteral().substring(1,token.getLiteral().length()-1);
            String convertedString = literalString
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\b", "\b")
                    .replace("\\r", "\r")
                    .replace("\\f", "\f")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            tokens.advance();
            return new Ast.Expression.Literal(convertedString.charAt(0)); // Assuming character is single-quoted
        }
        else if (peek(Token.Type.STRING)) {
            Token token = tokens.get(0);
            String literalString = token.getLiteral().substring(1,token.getLiteral().length()-1);
            String convertedString = literalString
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\b", "\b")
                    .replace("\\r", "\r")
                    .replace("\\f", "\f")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            tokens.advance();
            return new Ast.Expression.Literal(convertedString); // Assuming string is double-quoted
        }
        else if (peek("(")) {
            tokens.advance();
            Ast.Expression expression = parseExpression();
            if(!tokens.has(0)) {
                tokens.index--;
                throw new ParseException("Missing ',' or ')'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
            }
            if (!match(")"))
                throw new ParseException("Expected ',' or ')'", tokens.get(0).getIndex());
            tokens.advance();
            return new Ast.Expression.Group(expression);
        }

         else if (peek(Token.Type.IDENTIFIER)) {
            Token token = tokens.get(0);
            tokens.advance();
            if (match("(")) {
                List<Ast.Expression> arguments = new java.util.ArrayList<Ast.Expression>();
                while (!peek(")")) {
                    if(!tokens.has(0)) {
                        tokens.index--;
                        throw new ParseException("Missing ')'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
                    }
                    arguments.add(parseExpression());
                    if (!peek(")")) {
                        if (!match(","))
                            throw new ParseException("Expected ',' or ')'", tokens.get(0).getIndex());
                        else
                        {
                            if(peek(")"))
                                throw new ParseException("Wrong", tokens.get(0).getIndex());
                        }
                    }
                    else
                        break;
                }
                tokens.advance();
                return new Ast.Expression.Function(token.getLiteral(),arguments);
            }
            else if (match("[")) {
                Ast.Expression arguments ;
                while (!peek("]")) {
                    if(!tokens.has(0)) {
                        tokens.index--;
                        throw new ParseException("Missing ']'", tokens.get(0).getIndex()+tokens.get(0).getLiteral().length());
                    }
                    arguments = parseExpression();
                    if (!peek("]")) {
                        if (!peek(","))
                            throw new ParseException("Expected ',' or ']'", tokens.get(0).getIndex());
                    }
                    else {
                        tokens.advance();
                        return new Ast.Expression.Access(Optional.of(arguments), token.getLiteral());
                    }
                }
            }
            else {
                // Variable access
                return new Ast.Expression.Access(Optional.empty(), token.getLiteral());
            }
        }
        throw new ParseException("Expected a primary expression", tokens.get(0).getIndex());
    }
 //TODO

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    } //TODO (in lecture)

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    } //TODO (in lecture)

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }
    }
}
