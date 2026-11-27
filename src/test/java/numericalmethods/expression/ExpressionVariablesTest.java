package numericalmethods.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ExpressionVariablesTest {

    @Test
    void collectsEveryVariableOnceInAlphabeticalOrder() {
        assertEquals(Set.of("t", "x", "y1"), ExpressionVariables.of(ExpressionParser.parse("-sin(x*y1) + x^t")));
        assertEquals("[t, x, y1]", ExpressionVariables.of(ExpressionParser.parse("y1 + x + t + x")).toString());
    }

    @Test
    void constantsAndNumbersAreNotVariables() {
        assertEquals(Set.of(), ExpressionVariables.of(ExpressionParser.parse("2*pi + e - sqrt(3)")));
    }
}
