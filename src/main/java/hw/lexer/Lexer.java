package hw.lexer;

import hw.utils.FinalStates;
import hw.utils.Types;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Lexer {
    private Map<String, Set<String>> tokens = new HashMap<>();
    private Map<Pair<Integer, String>, Integer> stateTransitions = new HashMap<>();
    private Map<String, Integer> types = new HashMap<>();
    {
        types.put("float", 7);
        types.put("int", 5);
        types.put("id", 2);
        tokens.put("keyword", Set.of("main", "do", "while", "by", "for", "if", "then", "fi", "type", "print", "read"));
        tokens.put("function", Set.of("print", "read"));
        tokens.put("type", Set.of("int", "float", "boolean"));
        tokens.put("assign", Set.of("="));
        tokens.put("add_extract", Set.of("+", "-"));
        tokens.put("end_of_stat", Set.of(";"));
        tokens.put("logic_op", Set.of(">", "<", ">=", "<=", "==", "!="));
        tokens.put("mult_divide", Set.of("*", "/"));
        tokens.put("pow", Set.of("^"));
        tokens.put("boolean", Set.of("true", "false"));
        tokens.put("brackets", Set.of("{", "}", "(", ")"));
        tokens.put("new_line", Set.of("\n"));
        tokens.put("white_space", Set.of(" ", "\t"));
        tokens.put("dot", Set.of("."));
        tokens.put("comma", Set.of(","));
        tokens.put("quote", Set.of("\""));
        tokens.put("spec_symbol", Set.of("!"));
        tokens.put("E", Set.of("E"));
        stateTransitions.put(new Pair<>(0, "letter"), 1);
        stateTransitions.put(new Pair<>(1, "letter"), 1);
        stateTransitions.put(new Pair<>(1, "digit"), 1);
        stateTransitions.put(new Pair<>(1, "other"), 2); //ident
        stateTransitions.put(new Pair<>(0, "digit"), 3);
        stateTransitions.put(new Pair<>(3, "digit"), 3);
        stateTransitions.put(new Pair<>(3, "dot"), 4);
        stateTransitions.put(new Pair<>(3, "comma"), 101);
        stateTransitions.put(new Pair<>(3, "other"), 5); //int
        stateTransitions.put(new Pair<>(4, "digit"), 4);
        stateTransitions.put(new Pair<>(4, "other"), 7); //float
        stateTransitions.put(new Pair<>(4, "E"), 20); //exp
        stateTransitions.put(new Pair<>(20, "digit"), 4); //float
        stateTransitions.put(new Pair<>(0, "white_space"), 0);
        stateTransitions.put(new Pair<>(0, "add_extract"), 8);
        stateTransitions.put(new Pair<>(0, "mult_divide"), 8);
        stateTransitions.put(new Pair<>(0, "pow"), 8);
        stateTransitions.put(new Pair<>(0, "brackets"), 15);
        stateTransitions.put(new Pair<>(0, "logic_op"), 16);
        stateTransitions.put(new Pair<>(0, "assign"), 9);
        stateTransitions.put(new Pair<>(9, "assign"), 10);
        stateTransitions.put(new Pair<>(9, "other"), 11);
        stateTransitions.put(new Pair<>(16, "other"), 10);
        stateTransitions.put(new Pair<>(19, "other"), 100);
        stateTransitions.put(new Pair<>(19, "assign"), 10);
        stateTransitions.put(new Pair<>(16, "assign"), 10);
        stateTransitions.put(new Pair<>(0, "spec_symbol"), 19);
        stateTransitions.put(new Pair<>(0, "other"), 100); //undefined
        stateTransitions.put(new Pair<>(3, "comma"), 101);
        stateTransitions.put(new Pair<>(0, "quote"), 12);
        stateTransitions.put(new Pair<>(12, "other"), 12);
        stateTransitions.put(new Pair<>(1, "end_of_stat"), 2);
        stateTransitions.put(new Pair<>(2, "end_of_stat"), 2);
        stateTransitions.put(new Pair<>(3, "end_of_stat"), 5);
        stateTransitions.put(new Pair<>(4, "end_of_stat"), 7);
        stateTransitions.put(new Pair<>(5, "end_of_stat"), 5);
        stateTransitions.put(new Pair<>(6, "end_of_stat"), 8);
        stateTransitions.put(new Pair<>(7, "end_of_stat"), 7);
        stateTransitions.put(new Pair<>(12, "end_of_stat"), 13);
        stateTransitions.put(new Pair<>(12, "quote"), 13);
        stateTransitions.put(new Pair<>(13, "other"), 18);
        stateTransitions.put(new Pair<>(0, "new_line"), 14);
        stateTransitions.put(new Pair<>(0, "end_of_stat"), 17);
    }

    private HashSet<Integer> finalStates;
    private int initialState = 0;
    private int currentLine;
    private StringBuilder currentLexem;
    private Map<String, LexerRow> mapOfIds;
    private Map<String, LexerRow> mapOfConsts;

    public Lexer()
    {
        var alphabet = new HashSet<String>();
        for (char i = 'a'; i < 'z'; i++)
        {
            alphabet.add(Character.toString(i));
            alphabet.add(Character.toString(i).toUpperCase());
        }
        var numbers = new HashSet<String>();
        for (int i = 0; i < 10; i++)
        {
            numbers.add(Integer.toString(i));
        }

        this.finalStates = new HashSet<>();
        for (FinalStates state : FinalStates.values()) {
            this.finalStates.add(state.getState());
        }

        tokens.put("letter", alphabet);
        tokens.put("digit", numbers);
    }

    public LexerAnalyzerResult analyze(String sourceCode) {
        currentLine = 1;
        var currentState = initialState;
        currentLexem = new StringBuilder();
        var result = new ArrayList<LexemaToken>();
        mapOfIds = new HashMap<>();
        mapOfConsts = new HashMap<>();

        try {
            for (int i = 0; i < sourceCode.length(); i++) {
                var character = sourceCode.charAt(i);
                var charClass = getCharacterClass(character);
                currentState = getNextState(currentState, charClass);

                if (finalStates.contains(currentState)) {
                    LexemaToken lexem = processFinalState(currentState, character, i);
                    if (lexem != null) {
                        result.add(lexem);
                    }
                    currentState = 0;
                }
                else if (currentState == initialState) {
                    currentLexem.setLength(0);
                }
                else {
                    currentLexem.append(character);
                }
            }
        }
        catch (LexerException ex) {
            return new LexerAnalyzerResult(new ArrayList<>(), ex.getMessage());
        }
        return new LexerAnalyzerResult(result, mapOfConsts, mapOfIds);
    }

    private String getCharacterClass(char character) {
        for (Map.Entry<String, Set<String>> token : tokens.entrySet()) {
            if (checkToken(token)) {
                if (token.getValue().contains(Character.toString(character))) {
                    return Arrays.stream(new String[]{"logic_op", "brackets", "mult_divide", "add_extract",
                            "assign", "dot", "comma"})
                            .filter( s -> s.contains(token.getKey()))
                            .findFirst().orElse(token.getKey());
                }
            }
        }
        return "other";
    }

    private boolean checkToken(Map.Entry<String, Set<String>> token) {
        List<String> keywords = List.of("keyword", "id", "float", "int");
        Set<String> tokens = token.getValue();
        for (String keyword : keywords) {
            Optional<String> foundedToken = tokens.stream().filter(t -> t.equals(keyword)).findFirst();
            if (foundedToken.isPresent()) {
                return false;
            }
        }
        return true;
    }

    private int getNextState(int state, String charClass) {
        Integer nextState = stateTransitions.get(new Pair<>(state, charClass));
        if (nextState != null) {
            return nextState;
        }
        nextState = stateTransitions.get(new Pair<>(state, "other"));
        if (nextState != null) {
        return nextState;
    }
        throw new LexerException("Undefined lexem");
    }

    private LexemaToken processFinalState(int state, char ch, int charIndex) {
        LexemaToken lexem = null;
        FinalStates finalState = Arrays.stream(FinalStates.values())
                .filter(c -> c.getState() == state)
                .findFirst().orElseThrow(() -> new LexerException("The final state not processed"));
        switch (finalState) {
            case NEW_LINE:
                currentLine++;
                break;
            case ID:
            case INT:
            case FLOAT:
            case STRING_LITERAL:
                charIndex--;
                lexem = getLexem(state);
                if (lexem != null) {
                    if (lexem.getToken().equals("id")) {
                        LexerRow row = mapOfIds.get(lexem.getLexem());
                        if (row == null) {
                            row = new LexerRow();
                            row.setTitle(lexem.getLexem());
                            row.setValueType(Types.UNDEFINED);
                            mapOfIds.put(lexem.getLexem(), row);
                            row.setIndex(mapOfIds.size() - 1);
                        }
                        lexem.setIdOrConstIndex(row.getIndex());
                    }
                }
                break;
            case BRACKETS:
            case ASSIGN:
            case OPERATION:
            case COMPARE:
            case END_OF_STATEMENT:
                if (ch != ' ') {
                    currentLexem.append(ch);
                }
                lexem = getLexem(state);
                break;
            case COMMA_IN_FLOAT:
                throw new LexerException("Using commas in float is forbidden! Line: " + currentLexem);
            case UNDEFINED:
            default:
                throw new LexerException("Undefined symbol! Line: " + ch);
        }

        return lexem;
    }

    private LexemaToken getLexem(int state) {
        String lexem = currentLexem.toString();
        var token = tokens.entrySet()
                .stream()
                .filter(t -> !t.getKey().equals("digit"))
                .filter(t -> !t.getKey().equals("letter"))
                .filter(t -> t.getValue().contains(lexem))
                .findFirst().orElse(null);
        String foundedToken;
        if (token != null) {
            foundedToken = token.getKey();
        }
        else {
            var typeToken = types.entrySet()
                    .stream()
                    .filter(t -> t.getValue() == state)
                    .findFirst()
                    .orElse(null);
            if (typeToken == null) {
                return null;
            } else {
                foundedToken = typeToken.getKey();
            }
        }
        var lexemToken = LexemaToken.builder()
                .lexem(currentLexem.toString())
                .line(currentLine)
                .token(foundedToken)
                .build();

        currentLexem.setLength(0);
        return lexemToken;
    }

}
