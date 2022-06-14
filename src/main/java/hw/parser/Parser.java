package hw.parser;

import hw.lexer.LexemaToken;
import hw.lexer.LexerAnalyzerResult;
import hw.lexer.LexerRow;
import hw.utils.Types;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Parser {
    private Set<String> typeLexems = Set.of("boolean", "int", "float");
    private Set<String> numberTokens = Set.of("int", "float");
    private Set<String> boolConst = Set.of("true", "false" );
    private Set<String> multOperations = Set.of("mult_divide_op", "pow_op");
    private Set<String> figureBrackets = Set.of("{", "}" );
    private List<PostfixElement> postfixElements;
    private Map<String, Integer> labels;
    private Map<String, LexerRow> mapOfIds;

    public ParserResult parse(LexerAnalyzerResult analyzeResult) {
        if (!analyzeResult.success || analyzeResult.tokens.isEmpty()) {
            return new ParserResult("Tokens are missing!");
        }
        labels = new HashMap<>();
        postfixElements = new ArrayList<>();
        var tokensQueue = new ArrayDeque<>(analyzeResult.tokens);
        mapOfIds = analyzeResult.mapOfIds;

        try {
            parseToken("main", "keyword", tokensQueue, true);
            parseDoSection(tokensQueue);
        }
        catch (ParserException ex) {
            return new ParserResult(ex.getMessage());
        }

        return ParserResult.builder()
                .postfixElements(postfixElements)
                .labelsTable(labels)
                .build();
    }

    private boolean parseToken(String expectedLexem, String expectedToken, Queue<LexemaToken> tokens,
                               boolean throwEx) {
        var token = tokens.peek();
        if (!expectedLexem.equals(token.getLexem()) || !expectedToken.equals(token.getToken())) {
            if (throwEx) {
                throw new ParserException("Wrong token on line {token.Line}!\n Expected token: " +
                        "{expectedToken}, provided: {token.Token}");
            }
            return false;
        }
        tokens.poll();
        return true;
    }

    private boolean parseToken(String expectedToken, Queue<LexemaToken> tokens, boolean throwEx) {
        var token = tokens.peek();
        if (!expectedToken.equals(token.getToken())) {
            if (throwEx) {
                throw new ParserException("Wrong token on line {token.Line}!\n Expected token: "
                        + "{expectedToken}, provided: {token.Token}");
            }
            return false;
        }
        tokens.remove();
        return true;
    }

    private boolean parseDoSection(Queue<LexemaToken> tokens) {
        parseToken("brackets", tokens, false);
        parseStatementList(tokens);
        parseToken("brackets", tokens, false);
        return true;
    }

    private void parseStatementList(Queue<LexemaToken> tokens) {
        while (!figureBrackets.contains(tokens.peek().getLexem())) {
            parseStatement(tokens);
        }
//        LexemaToken token;
//        while ((token = tokens.peek()) != null) {
//            var lexem = token.getLexem();
//            while (!figureBrackets.contains(lexem) &&
//                    parseStatement(tokens)) {
//            }
//        }
    }

    private boolean parseStatement(Queue<LexemaToken> tokens) {
        var statementStart = false;
        LexemaToken token;
        while ((token = tokens.peek()) != null) {
            if (token.getToken().equals("end_of_stat")) {
                tokens.poll();
                return true;
            }
            else if (statementStart) {
                throw new ParserException("Unexpected symbol \"{token.Lexem}\" on line {token.Line}." +
                        " Expected symbol: \";\"");
            } else {
                statementStart = true;
                if (token.getToken().equals("type")) {
                    parseDeclaration(tokens);
                } else if (token.getToken().equals("id")) {
                    var idToken = tokens.poll();
                    postfixElements.add(PostfixElement.builder()
                            .token(idToken.getToken())
                            .line(idToken.getLine())
                            .lexem(idToken.getLexem())
                            .build());
                    parseAssign(tokens);
                } else if (token.getToken().equals("keyword")) {
                    if (token.getLexem().equals("if")) {
                        parseCondition(tokens);
                    }
                    else if (token.getLexem().equals("for")) {
                        parseLoop(tokens);
                    } else {
                        throw new ParserException("Unexpected symbol \"{token.Lexem}\" on line {token.Line}");
                    }
                } else if (token.getToken().equals("function")) {
                    if (token.getLexem().equals("print")) {
                        parsePrintFunction(tokens);
                    } else if (token.getLexem().equals("read")) {
                        parseReadFunction(tokens);
                    } else {
                        throw new ParserException("Unexpected symbol \"{token.Lexem}\" on line {token.Line}");
                    }
                }
                else {
                    throw new ParserException("Unexpected symbol \"{token.Lexem}\" on line {token.Line}");
                }
            }
        }
        return false;
    }

    private void parseDeclaration(Queue<LexemaToken> tokens) {
        var type = parseType(tokens.poll());
        var idToken = tokens.peek();
        LexerRow lexerRow;
        if ((lexerRow = mapOfIds.get(idToken.getLexem())) != null) {
            if (lexerRow.getValueType() != Types.UNDEFINED) {
                throw new ParserException("Variable is already defined!");
            }
            lexerRow.setValueType(type);
        } else {
            throw new ParserException("Undefined variable");
        }

        parseToken("id", tokens, false);
        if (tokens.peek().getToken().equals("assign")) {
            postfixElements.add(PostfixElement.builder()
                    .line(idToken.getLine())
                    .token(idToken.getToken())
                    .lexem(idToken.getLexem())
                    .build()); ;

            parseAssign(tokens);
        }
    }

    private Types parseType(LexemaToken token) {
        if (!token.getToken().equals("type")) {
            throw new ParserException("Unexpected token {token.Token} on line {token.Line}");
        } else if (!typeLexems.contains(token.getLexem())) {
            throw new ParserException("Undefined type {token.Token} on line {token.Line}");
        }
        return Arrays.stream(Types.values())
                .filter(c -> c.getValue().equals(token.getLexem()))
                .findFirst().orElseThrow();
    }

    private void parseAssign(Queue<LexemaToken> tokens) {
        var assignToken = tokens.peek();
        parseToken("=", "assign", tokens, true);
        var token = tokens.peek();
        if (token != null && boolConst.contains(token.getLexem())) {
            token = tokens.poll();
            postfixElements.add(PostfixElement.builder()
                    .lexem(token.getLexem())
                    .token("boolean")
                    .line(token.getLine())
                    .build());
        } else if (token.getToken().equals("float")) {
            LexerRow lexerRow;
            if ((lexerRow = mapOfIds.get(token.getLexem())) != null) {
                if (lexerRow.getValueType() != Types.UNDEFINED) {
                    throw new ParserException("Variable is already defined!");
                }
                lexerRow.setValue(token.getLexem());
            }
            token = tokens.poll();
            postfixElements.add(PostfixElement.builder()
                    .lexem(token.getLexem())
                    .token("float")
                    .line(token.getLine())
                    .build());
        } else if (token.getToken().equals("int")) {
                LexerRow lexerRow;
                if ((lexerRow = mapOfIds.get(token.getLexem())) != null) {
                    if (lexerRow.getValueType() != Types.UNDEFINED) {
                        throw new ParserException("Variable is already defined!");
                    }
                    lexerRow.setValue(token.getLexem());
                }
                token = tokens.poll();
                postfixElements.add(PostfixElement.builder()
                        .lexem(token.getLexem())
                        .token("int")
                        .line(token.getLine())
                        .build());
        } else if (parseArithmExpression(tokens)) {
            token = tokens.peek();
            if (parseToken("compare", tokens, false)) {
                parseArithmExpression(tokens);
                postfixElements.add(PostfixElement.builder()
                        .lexem(token.getLexem())
                        .token(token.getToken())
                        .line(token.getLine())
                        .build());
            }
        }
        else if (parseReadFunction(tokens)) {
        }
        else {
            throw new ParserException("Unexpected symbol on line {tokens.Peek().Line}");
        }

        postfixElements.add(PostfixElement.builder()
                .lexem("=")
                .token("assign")
                .line(assignToken.getLine())
                .build());
    }

    private boolean parseArithmExpression(Queue<LexemaToken> tokens) {
        if (!parseTerm(tokens)) {
            return false;
        }
        while (tokens.peek().getToken().equals("add_extract_op")) {
            var addToken = tokens.poll();
            parseTerm(tokens);

            postfixElements.add(PostfixElement.builder()
                    .lexem(addToken.getLexem())
                    .token(addToken.getToken())
                    .line(addToken.getLine())
                    .build());
        }
        return true;
    }

    private boolean parseTerm(Queue<LexemaToken> tokens) {
        LexemaToken signToken = null;
        LexemaToken token = tokens.peek();
        if (token != null) {
            if (token.getToken().equals("add_extract_op")) {
                signToken = tokens.poll();
            }
        }

        if (!parseFactor(tokens)) {
            return false;
        }
        while (multOperations.contains(tokens.peek().getToken())) {
            var multToken = tokens.poll();
            parseFactor(tokens);

            postfixElements.add(PostfixElement.builder()
                    .token(multToken.getToken())
                    .lexem(multToken.getLexem())
                    .line(multToken.getLine())
                    .build());
        }

        if (signToken != null && signToken.getLexem().equals("-")) {
            postfixElements.add(PostfixElement.builder()
                    .token("negative")
                    .line(signToken.getLine()).build());
        }
        return true;
    }

    private boolean parseFactor(Queue<LexemaToken> tokens) {
        var token = tokens.peek();
        if (token.getToken().equals("id") || numberTokens.contains(token.getToken())) {
            postfixElements.add(PostfixElement.builder()
                    .token(token.getToken())
                    .line(token.getLine())
                    .lexem(token.getLexem())
                    .build());
            tokens.poll();
            return true;
        }
        else {
            if (!parseToken(")", "brackets", tokens, false)) {
                return false;
            }
            parseArithmExpression(tokens);
            return parseToken("(", "brackets", tokens, true);
        }
    }

    private boolean parseCondition(Queue<LexemaToken> tokens) {
        parseToken("if", "keyword", tokens, true);
        parseToken("(", "brackets", tokens, true);

        parseBoolExpression(tokens);

        parseToken(")", "brackets", tokens, true);
        parseToken("then", "keyword", tokens, true);
        var lblName = String.format("label{%d}", labels.size());
        addLabel(String.format("{%s}&if", lblName), tokens.peek().getLine(), 1);
        parseDoSection(tokens);

        labels.put(String.format("{%s}&fi", lblName), postfixElements.size() - 1);
        return parseToken("endif", "keyword", tokens, true);
    }

    private boolean parseBoolExpression(Queue<LexemaToken> tokens) {
        if (boolConst.contains(tokens.peek().getLexem())) {
            var token = tokens.poll();
            postfixElements.add(PostfixElement.builder()
                    .token(token.getToken())
                    .line(token.getLine())
                    .lexem(token.getLexem())
                    .build());
            return true;
        } else {
            if (!parseArithmExpression(tokens)) {
                return false;
            }
            var compareToken = tokens.peek();
            parseToken("compare_op", tokens, true);
            var result = parseArithmExpression(tokens);

            postfixElements.add(PostfixElement.builder()
                    .token(compareToken.getToken())
                    .line(compareToken.getLine())
                    .lexem(compareToken.getLexem())
                    .build());

            return result;
        }
    }

    private boolean parseReadFunction(Queue<LexemaToken> tokens) {
        var token = tokens.peek();
        if (!parseToken("read", "function", tokens, false)) {
            return false;
        }
        parseToken("(", "brackets", tokens, true);

        postfixElements.add(PostfixElement.builder()
                .lexem("read")
                .token("function")
                .line(token.getLine())
                .build());

        return parseToken(")", "brackets", tokens, true);
    }

    private boolean parsePrintFunction(Queue<LexemaToken> tokens) {
        var writeToken = tokens.peek();
        parseToken("print", "function", tokens, true);
        parseToken("(", "brackets", tokens, true);
        var token = tokens.peek();
        if (parseToken("id", tokens, false)) {
            postfixElements.add(PostfixElement.builder()
                    .lexem(token.getLexem())
                    .line(token.getLine())
                    .token(token.getToken())
                    .build());
        }
        else if (typeLexems.contains(tokens.peek().getToken())) {
            postfixElements.add(PostfixElement.builder()
                    .lexem(token.getLexem())
                    .line(token.getLine())
                    .token(token.getToken())
                    .build());
            tokens.poll();
        }
        else {
            throw new RuntimeException("Wrong token on line {token.Line}!");
        }

        postfixElements.add(PostfixElement.builder()
                .lexem(writeToken.getLexem())
                .line(writeToken.getLine())
                .token(writeToken.getToken())
                .build());
        return parseToken(")", "brackets", tokens, true);
    }

    private boolean parseLoop(Queue<LexemaToken> tokens)  {
        parseToken("for", "keyword", tokens, true);
        var labelIndex = postfixElements.size() - 1;

        parseToken("by", "keyword", tokens, true);
        parseToken("while", "keyword", tokens, true);
        parseToken("(", "bracket_op", tokens, true);
        parseBoolExpression(tokens);
        parseToken(")", "brackets", tokens, true);
        addLabel(String.format("label{%d}", labels.size()), tokens.peek().getLine(), labelIndex);
        parseToken("do", "keyword", tokens, true);
        parseDoSection(tokens);
        return parseDoSection(tokens);
    }

    private void addLabel(String name, int line, int index) {
        if (index == -1) {
            index = postfixElements.size();
        }
        labels.put(name, index);
        postfixElements.add(PostfixElement.builder()
                .lexem(name)
                .token("label")
                .line(line)
                .build());
    }

}
