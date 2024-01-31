package plc.project;

import java.util.List;
import java.util.ArrayList;


/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) { // While there are characters left to lex
            if (peek("\\s")) { // Skip whitespace
                chars.advance();
                chars.skip();
            }
            else {
                tokens.add(lexToken()); // Add the next token
            }
        }
        return tokens;
    } //TODO

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[a-zA-Z_\\-@]")) {
            return lexIdentifier();
        }
        else if (peek("[0-9]")) {
            return lexNumber();
        }
        else if (peek("-")) {
            if (chars.has(1) && String.valueOf(chars.get(1)).matches("[0-9]")) {
                return lexNumber(); // Now we know the '-' is followed by a digit
            } else {
                throw new ParseException("Expected a digit after '-'", chars.index);
            }
        }
        else if (peek("'")) {
            return lexCharacter();
        }
        else if (peek("\"")) {
            return lexString();
        }
        else if (peek("[!<>]=?|==|&&|\\|\\||[\\(\\)\\[\\]\\{\\}\\.,;]")) {
            return lexOperator();
        }
        else {
            throw new ParseException("Unexpected character: " + chars.get(0), chars.index);
        }
    } //TODO

    public Token lexIdentifier() {
        if (!peek("[a-zA-Z_\\-@]")) {
            throw new ParseException("Expected an identifier", chars.index);
        }
        int startIndex = chars.index;
        chars.advance(); // Consume the first character of the identifier
        // Consume the rest of the identifier
        while (peek("[a-zA-Z0-9_\\-]") && !peek("@")) {
            chars.advance();
        }
        String value = chars.input.substring(startIndex, chars.index);
        if (value.contains("@") && value.charAt(0) != '@') {
            throw new ParseException("Invalid '@' inside identifier", chars.index);
        }
        return chars.emit(Token.Type.IDENTIFIER);
    } //TODO

    public Token lexNumber() {
        StringBuilder number = new StringBuilder();
        boolean isDecimal = false;
        int startIndex = chars.index;
        // Optional leading minus sign
        if (peek("-")) {
            number.append("-");
            chars.advance();
        }
        // Leading digit(s)
        if (!peek("[1-9]", "[0-9]*") && !peek("0")) {
            throw new ParseException("Expected an integer or decimal", chars.index);
        }
        // Consume leading integer part
        while (peek("[0-9]")) {
            number.append(chars.get(0));
            chars.advance();
        }
        // Decimal part
        if (peek("\\.")) {
            isDecimal = true;
            number.append(".");
            chars.advance();
            // There must be at least one digit after the decimal point
            if (!peek("[0-9]")) {
                throw new ParseException("Expected a digit after decimal point", chars.index);
            }
            // Consume fractional part
            while (peek("[0-9]")) {
                number.append(chars.get(0));
                chars.advance();
            }
        }
        if (isDecimal) {
            return chars.emit(Token.Type.DECIMAL);
        }
        else {
            return chars.emit(Token.Type.INTEGER);
        }
    }  //TODO

    public Token lexCharacter() {
        if (!match("'")) { // Start of character literal
            throw new ParseException("Expected start of character literal", chars.index);
        }
        if (peek("\\\\")) { // Escape sequence
            chars.advance(); // Consume backslash
            if (!peek("[bnrt'\\\\]")) { // Check valid escape sequence
                throw new ParseException("Invalid escape sequence", chars.index);
            }
            chars.advance(); // Consume escape character
        }
        else {
            if (!peek("[^']")) { // Check for non-quote character
                throw new ParseException("Empty character literal", chars.index);
            }
            chars.advance(); // Consume character
        }
        if (!match("'")) { // End of character literal
            throw new ParseException("Expected end of character literal", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER);
    } //TODO

    public Token lexString() {
        if (!match("\"")) { // Beginning of string literal
            throw new ParseException("Expected start of string literal", chars.index);
        }
        while (true) {
            if (peek("\"")) { // End of string literal
                break;
            }
            else if (peek("\\\\")) { // Escape sequence
                chars.advance(); // Consume backslash
                if (!peek("[bnrt'\"\\\\]")) { // Check valid escape sequence
                    throw new ParseException("Invalid escape sequence", chars.index);
                }
                chars.advance(); // Consume escape character
            }
            else if (peek("[^\n\r\"\\\\]")) { // Any other character
                chars.advance();
            }
            else { // Invalid character or end of line without closing quote
                throw new ParseException("Unterminated string literal", chars.index);
            }
        }
        match("\""); // Consume the closing quote
        return chars.emit(Token.Type.STRING);
    } //TODO

    public void lexEscape() {
        if (peek("\\\\")) { // If escape sequence
            chars.advance(); // Consume backslash
            if (!peek("[bnrt'\"\\\\]")) { // Valid escape sequences
                throw new ParseException("Invalid escape sequence", chars.index);
            }
            chars.advance(); // Consume the character following the backslash
        }
        else {
            throw new ParseException("Expected escape sequence", chars.index);
        }
    }  //TODO

    public Token lexOperator() {
        if (peek("[!<>]=?|==|&&|\\|\\||[\\(\\)\\[\\]\\{\\}\\.,;]")) {
            // Consume the operator character(s)
            if (peek("==") || peek("!=") || peek("<=") || peek(">=") || peek("&&") || peek("\\|\\|")) {
                chars.advance(); // Consume first character of operator
                chars.advance(); // Consume second character of operator
            }
            else {
                chars.advance(); // Consume single character operator
            }
            return chars.emit(Token.Type.OPERATOR);
        }
        else {
            throw new ParseException("Expected operator", chars.index);
        }
    }  //TODO

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i < patterns.length; i++) {
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    } //TODO (in Lecture)

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek) {
            for(int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    } //TODO (in Lecture)

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}