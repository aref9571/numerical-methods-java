package numericalmethods;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainTest {

    private static String run(String... args) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
            Main.main(args);
        } finally {
            System.setOut(originalOut);
        }
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    @Test
    void argumentsRunAOneLineCommand() {
        String output = run("newton", "x^2", "-", "2", "from", "1");
        assertTrue(output.contains("Newton"));
        assertTrue(output.contains("1.414213562"));
    }

    @Test
    void helpListsCommands() {
        assertTrue(run("help").contains("root x^6 - x - 1 on [1, 2]"));
    }
}
