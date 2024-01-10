/*
 * COURSE: COSC455_102
 * Assignment: Program 1
 * 
 * Name: Curry, Michelle
 */

package compiler;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.file.Files.lines;
import static java.text.MessageFormat.format;

public class MAIN {

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!! Toggle to display Graphviz prompt. !!!!!!!

    // NOTE: this can be overridden in the Parser class.
    public static boolean PROMPT_FOR_GRAPHVIZ;

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public static void main(String[] args) {
        // Check for an input file argument
        if (args.length != 1) {
            System.err.println("Must Provide an input filename!!");
            System.exit(1);
        }

        final File file = new File(args[0]);

        if (!file.exists() || !file.isFile() || !file.canRead()) {
            System.err.printf("Input file not found: %s%n", file.toPath());
            System.exit(2);
        }

        try {
            final String compiledCode = ScanAndParse(file);

            // Display the graphviz test page, if desired.
            if (PROMPT_FOR_GRAPHVIZ) {
                GraphViewer.openWebGraphViz(compiledCode);
            }
        } catch (IOException ex) {
            final String msg = format("Could not read the file!!! {0}", ex.getMessage());
            Logger.getGlobal().log(Level.SEVERE, msg);
            System.exit(3);
        }
    }

    private static String ScanAndParse(final File inputFile) throws IOException {
        // Create the code generator and lexical analyzer.
        final CodeGenerator codeGenerator = new CodeGenerator();
        final LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer(inputFile);

        // Compile the program from the input supplied by the lexical analyzer.
        final Parser parser = new Parser(lexicalAnalyzer, codeGenerator);
        parser.analyze();

        return codeGenerator.getGeneratedCodeBuffer();
    }
}

// *********************************************************************************************************

class LexicalAnalyzer {

    // TOKENIZED input.
    private Queue<TokenString> tokenList;

    /**
     * Construct a lexer over an input string.
     *
     * @param inputString The input file as a string.
     */
    LexicalAnalyzer(final String inputString) {
        this.tokenize(inputString);
    }

    /**
     * Construct a Lexer over the contents of a file. Filters out lines starting
     * with a '#' Symbol.
     * Removes EOL markers; otherwise, our grammar would have to deal with them.
     *
     * @param inputFile The file to read from.
     */
    LexicalAnalyzer(final File inputFile) throws IOException {
        Path filePath = inputFile.toPath();

        try (var lines = lines(filePath)) {
            this.tokenize(lines
                    .map(String::trim) // trim leading/trailing whitespace
                    .filter(x -> !x.startsWith("#")) // filter out comments
                    .map(String::trim) // trim again, since there may be whitespace after the comment.
                    .collect(Collectors.joining(" "))); // join into a single string
        }
    }

    /**
     * Convert the line to a series of tokens.
     */
    private void tokenize(final String line) {
        // Using Java 8's "Function Streams"
        this.tokenList = Arrays.stream(line.trim().split("\\s+")) // split string on spaces
                .map(TokenString::new) // map to a new compiler.Token
                .collect(Collectors.toCollection(LinkedList::new)); // collect into a new list
    }

    /**
     * Method to return the current lexeme.
     * If the tokenList is empty or the current token is $$, the function will
     * return "EOF".
     *
     * @return the Lexeme as an Optional string since an empty list has no tokens.
     */
    public String getCurrentLexeme() {
        return (this.tokenList.isEmpty() || this.currentToken() == Token.$$) ? "EOF" : this.tokenList.peek().lexeme;
    }

    /**
     * Get just the current token.
     *
     * @return the current token.
     */
    public Token currentToken() {
        return this.tokenList.isEmpty() ? Token.$$ : this.tokenList.peek().token;
    }

    /**
     * Advance to next token, making it current.
     */
    public void advanceToken() {
        if (!this.tokenList.isEmpty()) {
            this.tokenList.remove();
        }
    }

    /**
     * To string for debugging.
     * <p>
     * (Converts the current token list to a string for debugging.)
     */
    @Override
    public String toString() {
        return this.tokenList.toString();
    }

    /**
     * Nested class: a "Pair Tuple/Struct" for the token type and original string.
     */
    private static class TokenString {

        private final String lexeme;
        private final Token token;

        TokenString(final String lexeme) {
            this.lexeme = lexeme;
            this.token = Token.fromLexeme(lexeme);
        }

        @Override
        public String toString() {
            return String.format("{lexeme=%s, token=%s}", lexeme, token);
        }
    }
}

// *********************************************************************************************************

class CodeGenerator {

    // Buffer for generated code
    private final StringBuffer generatedCodeBuffer;

    // Constructor
    CodeGenerator() {
        this.generatedCodeBuffer = new StringBuffer();
    }

    // Write generated code to both the screen AND the buffer.
    void outputGeneratedCode(final String msg) {
        System.out.print(msg);
        this.generatedCodeBuffer.append(msg);
    }

    // Show the terminals as ovals...
    public void addTerminalToTree(final TreeNode fromNode, final String lexeme) {
        final var node = new TreeNode(lexeme);
        final var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=oval]};%n", fromNode, node, lexeme);

        this.outputGeneratedCode(msg);
    }

    /**
     * Add an "inner node" to the parse tree.
     * <p>
     * The following code employs a bit of a trick to automatically build the
     * calling method name.
     * The "getStackTrace()" method returns information about the entire active
     * stack. Element[0] is
     * the actual "getStackTrace()" method (it doesn't eliminate itself from the
     * array), element[1]
     * is THIS method (since we called "getStackTrace()") and element[2] is the
     * method that called
     * us, etc.
     *
     * @param parentNode the parent of the node being added to the tree
     * @return the newly added node as ParseNode object.
     */
    public TreeNode addNonTerminalToTree(final TreeNode parentNode) {
        // This uses a Java "Trick" to return the name of the function that called this
        // method.
        final var fromMethodName = Thread
                .currentThread()
                .getStackTrace()[2]
                .getMethodName()
                .toUpperCase();

        // Build a node name
        final var toNode = this.buildNode("<" + fromMethodName + ">");

        this.addNonTerminalToTree(parentNode, toNode);

        return toNode;
    }

    /**
     * Show the non-terminals as boxes...
     *
     * @param fromNode the parent node
     * @param toNode   the child node
     * @return the child node
     */
    public TreeNode addNonTerminalToTree(final TreeNode fromNode, final TreeNode toNode) {
        final var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=rect]};%n", fromNode, toNode,
                toNode.getNodeName());

        this.outputGeneratedCode(msg);
        return toNode;
    }

    /**
     * Add the "from node" to the tree and return a new "next node" object.
     *
     * @param fromNode The node to add to the tree.
     * @return the newly added node as ParseNode object.
     */
    public TreeNode addNonTerminalToTree(final TreeNode fromNode, final String toNodeString) {
        final var toNode = this.buildNode(toNodeString);
        return this.addNonTerminalToTree(fromNode, toNode);
    }

    // Show the terminals as ovals...
    public void addEmptyToTree(final TreeNode fromNode) {
        final var node = new TreeNode("EMPTY");
        final var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=none]};%n", fromNode, node,
                "&epsilon;");

        this.outputGeneratedCode(msg);
    }

    // Call this if a syntax error occurs...
    public void syntaxError(final String err, TreeNode fromNode) throws ParseException {
        final var msg = String.format("\t\"%s\" -> {\"%s\"};%n}%n", fromNode, err);

        this.outputGeneratedCode(msg);
        throw new ParseException(err);
    }

    // Build a node name, so it can be later "deconstructed" for the output.
    private TreeNode buildNode(final String name) {
        return new TreeNode(name);
    }

    // "Real" executable code generally has a header. See:
    // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format
    // (There are some good diagrams at the link)
    public void writeHeader(TreeNode node) {
        // The header for the "compiled" output
        var msg = String.format("""
                digraph ParseTree {
                \t"%s" [label="%s", shape=diamond];
                """, node, node.getNodeName());

        this.outputGeneratedCode(msg);
    }

    public TreeNode writeHeader(final String nodeString) {
        // The header for the "compiled" output
        final var headerNode = this.buildNode(nodeString);

        this.writeHeader(headerNode);
        return headerNode;
    }

    // Our output requires a footer as well.
    public void writeFooter() {
        final var msg = "}\n";

        this.outputGeneratedCode(msg);
    }

    public String getGeneratedCodeBuffer() {
        return generatedCodeBuffer.toString();
    }
}

// *********************************************************************************************************

/**
 * A "3-Tuple" for the node name and id number.
 */
class TreeNode {

    private static Integer currentNodeID = 0;
    private final String nodeName;
    private final Integer nodeId;

    TreeNode(final String nodeName) {
        this.nodeName = nodeName;
        this.nodeId = currentNodeID++;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", this.getNodeName(), this.getNodeId());
    }
}

// *********************************************************************************************************

/**
 * Code to invoke the online graph viewer.
 */
class GraphViewer {

    /**
     * To open a browser window...
     * <p>
     * FEEL FREE TO IGNORE THIS!!! It's just for opening the default browser, if
     * desired.
     */
    @SuppressWarnings("deprecation")
    static void openWebGraphViz(final String graph) {
        /*
         * Online/Web versions of Graphviz
         * http://www.webgraphviz.com
         * http://viz-js.com
         * https://dreampuf.github.io/GraphvizOnline
         */
        final var WEBGRAPHVIZ_HOME = "https://dreampuf.github.io/GraphvizOnline/";

        final var MSG = ("To visualize the output you may Copy/Paste the parser output into:%n" +
                "%s%n" +
                "(or any other online graphviz tool)").formatted(WEBGRAPHVIZ_HOME);

        // For some reason, the URL encoder always uses "+" instead of "%20" for
        // spaces...which are not always accepted.
        String encodedURL = URLEncoder.encode(graph, StandardCharsets.UTF_8).replace("+", "%20");

        // URI Length limit reached.
        if (WEBGRAPHVIZ_HOME.length() + encodedURL.length() >= 32_000) {
            System.err.println("Can't use remote graphviz; the output is too long for a \"GET\" Request");
            System.err.println("But you can still manually Copy/Paste instead.");

            return;
        }

        // Can we open a browser?
        if (Desktop.isDesktopSupported()) {
            // Try to set the default skin
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException e) {
                // Ignore the error, but notify.
                System.out.println("Using Default Skin");
            }

            // Open the default browser with the url:
            try {
                final URL webGraphvizURI = new URL(WEBGRAPHVIZ_HOME + "#" + encodedURL);
                final Desktop desktop = Desktop.getDesktop();

                // Can we launch a browser?
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    final var response = JOptionPane.showConfirmDialog(
                            null,
                            String.format("%s%n%nOpen %s?", MSG, WEBGRAPHVIZ_HOME),
                            "Open Web Graphviz Page",
                            JOptionPane.YES_NO_OPTION);

                    // Open Browser?
                    if (response == JOptionPane.YES_OPTION) {
                        desktop.browse(webGraphvizURI.toURI());
                    } else {
                        System.out.println(MSG);
                    }
                }
            } catch (IOException | URISyntaxException ex) {
                java.util.logging.Logger.getAnonymousLogger()
                        .log(java.util.logging.Level.WARNING, "Could not open browser (URL \"GET\" May be too long!!!)",
                                ex);
            }
        }
    }
}

// *********************************************************************************************************

/**
 * An exception to be raised if parsing fails due to a "syntax error" in the
 * input file.
 */
final class ParseException extends RuntimeException implements Serializable {

    private final String ExceptionID = "ZU3vkDLZRB3pOms4woQh";

    public ParseException(String errMsg) {
        super(errMsg);
    }
}
