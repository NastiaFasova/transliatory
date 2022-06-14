import hw.lexer.Lexer;
import hw.parser.Parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        StringBuilder sourceCode = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("myProgram.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sourceCode.append(line).append("\n");
            }
        }
        var res = new Lexer().analyze(sourceCode.toString());
        var output = new StringBuilder();
        if (res.success) {
            output.append("Lexer: Success");
        } else {
            output.append("Lexer: Error\n").append(res.getErrorMessage());
        }
        if (res.tokens != null) {
            for (var t : res.tokens) {
                var id = t.getIdOrConstIndex() != null
                        ? t.getIdOrConstIndex() : "";
                output.append(String.format("\n[{%d, %s}]: %s", t.getLine(), t.getToken(), t.getLexem()));
            }
            var parserResult = new Parser().parse(res);
            if (parserResult.success) {
                output.append("\nParser: Success");
            } else {
                output.append("\nParser: Error\n").append(res.getErrorMessage());
            }
            if (res.success) {
                BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
                writer.write(output.toString());
            }
            System.out.println(output);
            res.mapOfIds.forEach((key, value) -> System.out.println(key + " " + value));
        }
    }
}
