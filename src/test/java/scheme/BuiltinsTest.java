package scheme;


import static org.junit.Assert.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import scheme.bind.Procedure;
import org.junit.Test;
import org.junit.runner.RunWith;
import scheme.bind.ExampleCall;
import scheme.bind.ExampleCalls;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(JUnitParamsRunner.class)
public class BuiltinsTest {

    @Test
    public void correctNumberOfBuiltins() {
        assertEquals(118, getAnnotatedMethods().size());
    }

    @Test
    @Parameters(method = "getAnnotatedMethods")
    public void allBuiltinsHaveExamples(Method method) {
        var annotationPresent = method.isAnnotationPresent(ExampleCall.class);
        annotationPresent = annotationPresent || method.isAnnotationPresent(ExampleCalls.class);

        assertTrue(method.getName() + " needs @ExampleCall annotation", annotationPresent);
    }

    @Test(timeout = 1000L)
    @Parameters(method = "getExampleTestCases")
    public void testBuiltinExamples(ExampleTestCase testCase) {
        var call = testCase.call;
        var s = Scheme.headless();

        if (call.error()) {
            assertThrows(EvaluationException.class, () -> s.loadString(call.in()));
            return;
        }

        var inEvaluation = s.loadString(call.in());
        var outEvaluation = s.loadString(call.out());

        assertTrue(inEvaluation.isPresent());
        assertEquals(outEvaluation.get().toScheme(), inEvaluation.get().toScheme());
    }

    private static class ExampleTestCase {
        Method method;
        ExampleCall call;

        public ExampleTestCase(Method method, ExampleCall call) {
            this.method = method;
            this.call = call;
        }

        public String toString() {
            return String.format("%s::%s[eval=%s]",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    call.in());
        }
    }

    private static final List<ExampleTestCase> getExampleTestCases() {
        return getAnnotatedMethods().stream().flatMap(
                method -> Arrays.stream(method.getAnnotationsByType(ExampleCall.class))
                        .filter(ExampleCall::deterministic)
                        .map(c -> new ExampleTestCase(method, c)))
                .collect(Collectors.toList());
    }

    private static final List<Method> getAnnotatedMethods() {
        var builtins = new Builtins();
       return  Arrays.stream(builtins.getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(Procedure.class))
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());
    }

}
