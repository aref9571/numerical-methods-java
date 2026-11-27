package numericalmethods.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppTest {

    private static String session(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new App(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8)).run();
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void menuDefaultsRunTheExampleProblem() {
        String output = session("1\n2\n\n\n\n\n\nb\nq\n");
        assertTrue(output.contains("Command: bisection x^6 - x - 1 on [1, 2], tol 0.001, max 100"));
        assertTrue(output.contains("x = 1.133789063"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void everyMenuItemRunsWithItsDefaultsWithoutErrors() {
        List<App.Topic> topics = Menus.topics();
        for (int t = 0; t < topics.size(); t++) {
            for (int i = 0; i < topics.get(t).items().size(); i++) {
                App.MenuItem item = topics.get(t).items().get(i);
                String input = (t + 1) + "\n" + (i + 1) + "\n" + "\n".repeat(item.prompts().size()) + "b\nq\n";
                String output = session(input);
                assertFalse(output.contains("Error:"), () -> item.label() + " failed:\n" + output);
                assertTrue(output.contains("Result"), () -> item.label() + " printed no result:\n" + output);
            }
        }
    }

    @Test
    void badInputDoesNotStopTheProgram() {
        String output = session("42\nroot 2x on [0, 1]\n%%%\n1\n9\nb\nq\n");
        assertTrue(output.contains("Please choose a number between 1 and 1."));
        assertTrue(output.contains("Error: Missing operator"));
        assertTrue(output.contains("Error: Unknown command '%%%'"));
        assertTrue(output.contains("Please choose a number between 1 and 7, type a one-line command"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void aOneLineCommandCanBePastedInsideATopicMenu() {
        String output = session("1\nnewton x^2 - 2 from 1\nb\nq\n");
        assertTrue(output.contains("x = 1.414213562"));
        assertFalse(output.contains("Please choose a number"));
    }

    @Test
    void helpIsShownFromTheMainMenu() {
        assertTrue(session("h\nq\n").contains("root x^6 - x - 1 on [1, 2]"));
    }

    @Test
    void endOfInputExitsCleanly() {
        assertTrue(session("1\n1\nx^2 - 2\n").contains("Goodbye."));
        assertTrue(session("").contains("Goodbye."));
    }
}
