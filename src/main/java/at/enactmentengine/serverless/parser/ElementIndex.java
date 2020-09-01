package at.enactmentengine.serverless.parser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Provides methods to parse element-index constraints.
 */
public class ElementIndex {

    private ElementIndex() {
    }

    /**
     * Parses the indices that are specified in the given list of colon expressions.
     *
     * @param expressions a list of comma separated colon expressions
     * @return the indices
     */
    public static List<Integer> parseIndices(String expressions) {
        return Arrays.stream(expressions
                .split(","))
                .flatMap(expression ->
                {
                    Integer[] operands = Arrays.stream(expression
                            .split(":"))
                            .map(Integer::parseInt)
                            .toArray(Integer[]::new);

                    int start = operands.length >= 1 ? operands[0] : 0;
                    int end = operands.length >= 2 ? operands[1] : start;
                    int stride = operands.length >= 3 ? operands[2] : 1;

                    if (operands.length < 1 || operands.length > 3 || stride == 0) {
                        throw new IllegalArgumentException("Invalid expression: " + expression);
                    }
                    if (start < 0) {
                        throw new IllegalArgumentException("Start index is negative");
                    }
                    if (end < 0) {
                        throw new IllegalArgumentException("End index is negative");
                    }

                    int sign = Integer.signum(stride);
                    return IntStream.rangeClosed(start * sign, end * sign)
                            .filter(x -> (x - (start * sign)) % (stride * sign) == 0) // apply stride
                            .map(x -> x * sign)
                            .boxed();
                })
                .collect(Collectors.toList());
    }
}
