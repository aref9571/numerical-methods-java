package edu.vuas.numericalmethods;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void mainPrintsWelcomeMessage() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
            Main.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(Main.WELCOME_MESSAGE, capturedOutput.toString(StandardCharsets.UTF_8).strip());
    }
}
