package numericalmethods.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextTableTest {

    @Test
    void alignsColumnsUnderARule() {
        List<String> lines = new TextTable("#", "x").row("1", "1.5").row("10", "2").lines();
        assertEquals(List.of("#  | x", "---+----", "1  | 1.5", "10 | 2"), lines);
    }

    @Test
    void rejectsARowWithTheWrongNumberOfCells() {
        assertThrows(IllegalArgumentException.class, () -> new TextTable("a", "b").row("1"));
    }
}
