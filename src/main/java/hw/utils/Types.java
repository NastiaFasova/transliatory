package hw.utils;

public enum Types {
    STRING("function"), INT("int"), FLOAT("float"), BOOLEAN("boolean"), UNDEFINED("");

    private String value;

    Types(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
