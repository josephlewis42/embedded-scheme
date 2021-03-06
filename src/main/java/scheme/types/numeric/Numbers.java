// Copyright (c) 2021 Google LLC
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package scheme.types.numeric;

import scheme.EvaluationException;
import java.util.Arrays;
import java.lang.Class;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Numbers {
    private static <R> BiFunction<Number, Number, R> binaryDispatcher(
            BiFunction<SInteger, SInteger, R> intImpl,
            BiFunction<SRational, SRational, R> rationalImpl,
            BiFunction<SReal, SReal, R> realImpl) {
        return (a, b) -> {
            if (convertibleTo(SInteger.class, a, b)) {
                return intImpl.apply(convert(SInteger.class, a), convert(SInteger.class, b));
            }

            if (convertibleTo(SRational.class, a, b)) {
                return rationalImpl.apply(convert(SRational.class, a), convert(SRational.class, b));
            }

            if (convertibleTo(SReal.class, a, b)) {
                return realImpl.apply(convert(SReal.class, a), convert(SReal.class, b));
            }

            throw new AssertionError(String.format("incompatible values: %s, %s", a.getClass(), b.getClass()));
        };
    }

    private static <R> Function<Number, R> unaryDispatch(
            Function<SInteger, R> intImpl,
            Function<SRational, R> rationalImpl,
            Function<SReal, R> realImpl) {
        return (a) -> {
            if (convertibleTo(SInteger.class, a)) {
                return intImpl.apply(convert(SInteger.class, a));
            }

            if (convertibleTo(SRational.class, a)) {
                return rationalImpl.apply(convert(SRational.class, a));
            }

            if (convertibleTo(SReal.class, a)) {
                return realImpl.apply(convert(SReal.class, a));
            }

            throw new AssertionError(String.format("incompatible values: %s", a.getClass()));
        };
    }

    private static final BiFunction<Number, Number, Number> ADD_DISPATCH = binaryDispatcher(
            SInteger::add,
            SRational::add,
            SReal::add
    );

    public static final Number add(Number a, Number b) {
        return ADD_DISPATCH.apply(a,b);
    }

    private static final Function<Number, Number> NEGATE_DISPATCH = unaryDispatch(
            SInteger::negate,
            SRational::negate,
            SReal::negate
    );

    public static final Number negate(Number a) {
        return NEGATE_DISPATCH.apply(a);
    }

    private static final BiFunction<Number, Number, Number> SUBTRACT_DISPATCH = binaryDispatcher(
            SInteger::subtract,
            SRational::subtract,
            SReal::subtract
    );

    public static final Number subtract(Number a, Number b) {
        return SUBTRACT_DISPATCH.apply(a,b);
    }

    private static final BiFunction<Number, Number, Number> MULTIPLY_DISPATCH = binaryDispatcher(
            SInteger::multiply,
            SRational::multiply,
            SReal::multiply
    );

    public static final Number multiply(Number a, Number b) {
        return MULTIPLY_DISPATCH.apply(a,b);
    }

    private static final BiFunction<Number, Number, Number> DIVIDE_DISPATCH = binaryDispatcher(
            SRational::of, // int / int -> rational to preserve precision
            SRational::divide,
            SReal::divide
    );

    public static final Number divide(Number a, Number b) {
        return DIVIDE_DISPATCH.apply(a,b);
    }

    private static final Function<Number, Number> RECIPROCAL_DISPATCH = unaryDispatch(
            SInteger::reciprocal,
            SRational::reciprocal,
            SReal::reciprocal
    );

    public static final Number reciprocal(Number a) {
        return RECIPROCAL_DISPATCH.apply(a);
    }

    private static final BiFunction<Number, Number, Integer> COMPARE_DISPATCH = binaryDispatcher(
            SInteger::compareTo,
            SRational::compareTo,
            SReal::compareTo
    );

    public static final int compareTo(Number a, Number b) {
        return COMPARE_DISPATCH.apply(a, b);
    }

    private static final BiFunction<Number, Number, Number[]> DIVIDE_TO_INTEGRAL_VALUE_DISPATCH = binaryDispatcher(
            SInteger::divideToIntegralValue,
            SRational::divideToIntegralValue,
            SReal::divideToIntegralValue
    );

    public static final Number[] divideToIntegralValue(Number dividend, Number divisor) {
        return DIVIDE_TO_INTEGRAL_VALUE_DISPATCH.apply(dividend, divisor);
    }

    public static final Number quotient(Number dividend, Number divisor) {
        return divideToIntegralValue(dividend, divisor)[0];
    }

    public static final Number remainder(Number dividend, Number divisor) {
        return divideToIntegralValue(dividend, divisor)[1];
    }

    public static final Number modulo(Number dividend, Number divisor) throws EvaluationException {
        var res = remainder(dividend, divisor);
        if (divisor.signum() < 0){
            if (compareTo(res, SInteger.ZERO) <= 0){
                return res;
            } else {
                return add(res, divisor);
            }
        } else {
            if (res.signum() >= 0){
                return res;
            } else {
                return add(res, divisor);
            }
        }
    }

    private static final Function<Number, String> DISPLAY_VALUE_DISPATCH = unaryDispatch(
            SInteger::displayValue,
            SRational::displayValue,
            SReal::displayValue
    );

    public static final String displayValue(Number a) {
        return DISPLAY_VALUE_DISPATCH.apply(a);
    }

    private static final Function<Number, Boolean> EXACT_DISPATCH = unaryDispatch(
            SInteger::isExact,
            SRational::isExact,
            SReal::isExact
    );

    public static final boolean isExact(Number a) {
        return EXACT_DISPATCH.apply(a);
    }

    public static SInteger integerValue(Number a) throws InexactException {
        return a.integerValueExact();
    }

    public static SReal realValue(Number a) {
        return convert(SReal.class, a);
    }

    public static SRational rationalValue(Number a) {
        return convert(SRational.class, a);
    }

    private static final <T> boolean convertibleTo(Class<T> clazz, Number...nums) {
        return Arrays.stream(nums).allMatch(num ->
                (num.getClass().isAssignableFrom(clazz)) ||
                (clazz == SRational.class && num instanceof SRationalPromotable) ||
                (clazz == SReal.class && num instanceof SRealPromotable));
    }

    private static final <T> T convert(Class<T> clazz, Number num) {
        if (num.getClass().isAssignableFrom(clazz)) {
            return (T)num;
        }

        if (clazz == SRational.class && SRationalPromotable.class.isAssignableFrom(num.getClass())) {
            return (T)((SRationalPromotable) num).toSRational();
        }

        if (clazz == SReal.class && SRealPromotable.class.isAssignableFrom(num.getClass())) {
            return (T)((SRealPromotable) num).toSReal();
        }

        throw new ArithmeticException(String.format("can't convert %s to %s", num.getClass(), clazz));
    }
}
