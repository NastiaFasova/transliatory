package hw.parser;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PostfixElement {
    public String token;
    public String lexem;
    public int line;
}
