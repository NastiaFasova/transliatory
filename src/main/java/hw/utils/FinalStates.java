package hw.utils;

import lombok.Getter;

@Getter
public enum FinalStates {
    ID(2), INT(5), FLOAT(7), OPERATION(8), COMPARE(10), ASSIGN(11), NEW_LINE(14), BRACKETS(15), STRING_LITERAL(18),
    END_OF_STATEMENT(17), COMMA_IN_FLOAT(101), UNDEFINED(100);
    private int state;

    FinalStates(int state) {
        this.state = state;
    }
}
