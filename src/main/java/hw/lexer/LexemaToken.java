package hw.lexer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LexemaToken {
    private Integer line;
    private String token;
    private String lexem;
    private Integer idOrConstIndex;
}
