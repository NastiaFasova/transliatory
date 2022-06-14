package hw.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParserResult {
    public boolean success;
    public String errorMessage;
    public List<PostfixElement> postfixElements;

    public Map<String, Integer> labelsTable;

    public ParserResult(String message) {
        errorMessage = message;
    }
}
