/*
 * COURSE: COSC455_102
 * Assignment: Program 1
 * 
 * Name: Curry, Michelle
 */

 package compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
Ignore
public enum Token {
    $$, // End of file
    ID, // Could be "ID" in a "real programming language"
    NUMBER,// A sequence of digits.
    ADD_OP("+", "-"),
    MULT_OP("*", "/"),
    RELATION("<",">","<=",">=","=","!="),
    READ("read"),
    WRITE("write"),
    ASSIGN(":="),
    LPAREN("("),
    RPAREN(")"),
    WHILE("while"),
    DO("do"),
    THEN("then"),
    ELSE("else"),
    UNTIL("until"),
    IF("if"),
    IF_END("fi"),
    WHILE_END("od"),;
    
    /**
     * A list of all lexemes for each token.
     */
    private final List<String> lexemeList;

    Token(final String... tokenStrings) {
        lexemeList = new ArrayList<>(tokenStrings.length);
        lexemeList.addAll(Arrays.asList(tokenStrings));
    }

    /**
     * Get a compiler.Token object from the Lexeme string.
     *
     * @param string The String (lexeme) to convert to a compiler.Token
     * @return A compiler.Token object based on the input String (lexeme)
     */
    public static Token fromLexeme(final String string) {
        // Just to be safe...
        final var lexeme = string.trim();

        // An empty string/lexeme should mean no more tokens to process.
        if (lexeme.isEmpty()) {
            return $$;
        }

        // Regex for one or more digits optionally followed by and more digits.
        // (doesn't handle "-", "+" etc., only digits)
        if (lexeme.matches("\\d+(?:\\.\\d+)?")) {
            return NUMBER;
        }

        // Search through ALL lexemes looking for a match with early bailout.
        for (var token : Token.values()) {
            if (token.lexemeList.contains(lexeme)) {
                // early bailout from for loop.
                return token;
            }
        }

        // NOTE: UNKNOWN could represent an ID, for example.
        return ID;
    }
}





