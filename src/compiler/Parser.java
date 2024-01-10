/*
 * COURSE: COSC455_102
 * Assignment: Program 1
 * 
 * Name: Curry, Michelle
 */

 package compiler;

import java.util.logging.Logger;

public class Parser {

    // The lexer which will provide the tokens
    private final LexicalAnalyzer lexer;

    // The actual "code generator"
    private final CodeGenerator codeGenerator;

    /**
     * This is the constructor for the Parser class which
     * accepts a LexicalAnalyzer and a CodeGenerator object as parameters.
     *
     * @param lexer         The Lexer Object
     * @param codeGenerator The CodeGenerator Object
     */
    public Parser(LexicalAnalyzer lexer, CodeGenerator codeGenerator) {
        this.lexer = lexer;
        this.codeGenerator = codeGenerator;

        // Change this to automatically prompt to see the Open WebGraphViz dialog or
        // not.
        MAIN.PROMPT_FOR_GRAPHVIZ = true;
    }

    /*
     * Since the "Compiler" portion of the code knows nothing about the start rule,
     * the "analyze" method must invoke the start rule.
     *
     * Begin analyzing...
     */
    void analyze() {
        try {
            // Generate header for our output
            TreeNode startNode = codeGenerator.writeHeader("PARSE TREE");

            // THIS IS OUR START RULE
            this.BEGIN_PARSING(startNode);

            // generate footer for our output
            codeGenerator.writeFooter();

        } catch (ParseException ex) {
            final String msg = String.format("%s\n", ex.getMessage());
            Logger.getAnonymousLogger().severe(msg);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This is just an intermediate method to make it easy to change the start rule
     * of the grammar.
     *
     * @param parentNode The parent node for the parse tree
     * @throws ParseException If there is a syntax error
     */
    void BEGIN_PARSING(final TreeNode parentNode) throws ParseException {
        this.Program(parentNode);
    }

    // <PROGRAM> ::= <STMT_LIST> 
    void Program(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        this.STMT_LIST(thisNode);

        if (lexer.currentToken() != Token.$$) {
            this.raiseException(Token.$$, thisNode);
        }
    }

    // <STMT_LIST> ::= <STMT> <STMT_LIST> | E 
    void STMT_LIST(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.currentToken() == Token.ID || 
            lexer.currentToken() == Token.READ || 
            lexer.currentToken() == Token.WRITE || 
            lexer.currentToken() == Token.WHILE ||
            lexer.currentToken() == Token.DO ||
            lexer.currentToken() == Token.IF) {

            this.STMT(thisNode);
            this.STMT_LIST(thisNode);
        } else {
            this.EMPTY(thisNode);
        }
    }

    // <STMT> ::= <id> <:=> <EXPR> | read <id> | write <EXPR> 
    void STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    
        // while
        if (lexer.currentToken() == Token.WHILE) {
            this.WHILE_LOOP(thisNode);

        // do
        } else if (lexer.currentToken() == Token.DO) {
            this.DO_LOOP(thisNode);
        
        // if
        } else if (lexer.currentToken() == Token.IF) {
            this.IF_STMT(thisNode);
        
        // <id> <:=> <EXPR>
        } else if (lexer.currentToken() == Token.ID ) {

            this.MATCH(thisNode, Token.ID);
            this.MATCH(thisNode, Token.ASSIGN);
            this.EXPR(thisNode);

        // read <id>
        } else if (lexer.currentToken() == Token.READ) {
            this.MATCH(thisNode, Token.READ);
            this.MATCH(thisNode, Token.ID);

        // write <EXPR>
        } else if (lexer.currentToken() == Token.WRITE) {
            this.MATCH(thisNode, Token.WRITE);
            this.EXPR(thisNode);
        }

         
    }

    // <EXPR> ::= <TERM> <TERM_TAIL>
     void EXPR(final TreeNode parentNode) throws ParseException {
         final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        this.TERM(thisNode);
        this.TERM_TAIL(parentNode);
     }

     // while loop ::= [while <CONDITION>] do <STMT_LIST> od
     void WHILE_LOOP(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        //while
        this.MATCH(thisNode, Token.WHILE);
       
        //condition
        this.CONDITION(thisNode);
       
        //do
        this.MATCH(thisNode, Token.DO);

        //statements
        this.STMT_LIST(thisNode);

        //end loop
        this.MATCH(thisNode, Token.WHILE_END);
    }

    // do loop ::= do <STMT_LIST> until <CONDITION>
    void DO_LOOP(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);        

        //do
        this.MATCH(thisNode, Token.DO);

        //statements
        this.STMT_LIST(thisNode);

        //until
        this.MATCH(thisNode, Token.UNTIL);

        //condition
        this.CONDITION(thisNode);
    }


    // <IF_STMT> ::= if <CONDITION> then <STMT_LIST> [else <STMT_LIST>] fi
    void IF_STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        // if
        this.MATCH(thisNode, Token.IF);

        // condition
        this.CONDITION(thisNode);

        // then
        this.MATCH(thisNode, Token.THEN);

        // statement
        this.STMT_LIST(thisNode);

        // optional else
        if (lexer.currentToken() == Token.ELSE) {
            this.MATCH(thisNode, Token.ELSE);
            this.STMT_LIST(thisNode);
        }

        // fi
        this.MATCH(thisNode, Token.IF_END);
    }

    // <CONDITION> ::= <EXPR> <RELATION> <EXPR>
    void CONDITION(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        
        this.EXPR(thisNode);
        this.MATCH(thisNode, Token.RELATION);
        this.EXPR(thisNode);
    }

    // <TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | E
    void TERM_TAIL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.currentToken() == Token.ADD_OP) {
            this.MATCH(thisNode, Token.ADD_OP);
            this.TERM(thisNode);
            this.TERM_TAIL(thisNode);

        } else {
            this.EMPTY(thisNode);
        }
    }

    // <TERM> ::= <FACTOR> <FACTOR_TAIL>
    void TERM(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        this.FACTOR(thisNode);
        this.FACTOR_TAIL(thisNode);
    }

    // <FACTOR_TAIL> ::= <MULT_OP> <FACTOR> <FACTOR_TAIL> | E
    void FACTOR_TAIL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.currentToken() == Token.MULT_OP) {
            this.MATCH(thisNode, Token.MULT_OP);
            this.FACTOR(thisNode);
            this.FACTOR_TAIL(thisNode);
        } else {
            this.EMPTY(thisNode);
        }
    }

    // <FACTOR> ::= ( <EXPR> ) | <id> | <num>
    void FACTOR(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        // ( <EXPR> )
        if (lexer.currentToken() == Token.LPAREN) {
            this.MATCH(thisNode, Token.LPAREN);
            this.EXPR(thisNode);
            this.MATCH(thisNode, Token.RPAREN);

        // <id>    
        } else if (lexer.currentToken() == Token.ID) {
            this.MATCH(thisNode, Token.ID);

        // <num>  
        } else if (lexer.currentToken() == Token.NUMBER) {
            this.MATCH(thisNode, Token.NUMBER);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add a an EMPTY terminal node (result of an Epsilon Production) to the parse
     * tree.
     * Mainly, this is just done for better visualizing the complete parse tree.
     *
     * @param parentNode The parent of the terminal node.
     */
    void EMPTY(final TreeNode parentNode) {
        codeGenerator.addEmptyToTree(parentNode);
    }

    /**
     * Match the current token with the expected token.
     * If they match, add the token to the parse tree, otherwise throw an exception.
     *
     * @param parentNode    The parent of the terminal node.
     * @param expectedToken The token to be matched.
     * @throws ParseException Thrown if the token does not match the expected token.
     */
    void MATCH(final TreeNode parentNode, final Token expectedToken) throws ParseException {
        final Token currentToken = lexer.currentToken();

        if (currentToken == expectedToken) {
            var currentLexeme = lexer.getCurrentLexeme();
            this.addTerminalToTree(parentNode, currentToken, currentLexeme);
            lexer.advanceToken();
        } else {
            this.raiseException(expectedToken, parentNode);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add a terminal node to the parse tree.
     *
     * @param parentNode    The parent of the terminal node.
     * @param currentToken  The token to be added.
     * @param currentLexeme The lexeme of the token beign added.
     * @throws ParseException Throws a ParseException if the token cannot be added
     *                        to the tree.
     */
    void addTerminalToTree(final TreeNode parentNode, final Token currentToken, final String currentLexeme)
            throws ParseException {
        var nodeLabel = "<%s>".formatted(currentToken);
        var terminalNode = codeGenerator.addNonTerminalToTree(parentNode, nodeLabel);

        codeGenerator.addTerminalToTree(terminalNode, currentLexeme);
    }

    /**
     * Raise a ParseException if the input cannot be parsed as defined by the
     * grammar.
     *
     * @param expected   The expected token
     * @param parentNode The token's parent node
     */
    private void raiseException(Token expected, TreeNode parentNode) throws ParseException {
        final var template = "SYNTAX ERROR: '%s' was expected but '%s' was found.";
        final var errorMessage = template.formatted(expected.name(), lexer.getCurrentLexeme());
        codeGenerator.syntaxError(errorMessage, parentNode);
    }
}
