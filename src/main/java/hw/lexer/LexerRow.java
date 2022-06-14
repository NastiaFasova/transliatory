package hw.lexer;

import hw.utils.Types;
import lombok.Data;

@Data
public class LexerRow {
    public int index;
    public String value;
    public String title;
    public Types ValueType;
}
