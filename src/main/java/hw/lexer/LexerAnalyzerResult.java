package hw.lexer;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class LexerAnalyzerResult {
    public List<LexemaToken> tokens;
    public String errorMessage;
    public boolean success;

    public Map<String, LexerRow> mapOfIds;
    public Map<String, LexerRow> mapOfConst;

    public LexerAnalyzerResult(List<LexemaToken> tokens, Map<String, LexerRow> mapOfConst,
                               Map<String, LexerRow> mapOfIds) {
        this.tokens = tokens;
        success = true;
        this.mapOfConst = mapOfConst;
        this.mapOfIds = mapOfIds;
    }

    public LexerAnalyzerResult(List<LexemaToken> tokens, String errorMessage) {
        this.errorMessage = errorMessage;
        this.tokens = tokens;
    }
}
