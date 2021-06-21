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

import scheme.types.SNumber;

public interface Number<T> extends Comparable<T> {

    /** Returns -1, 0, 1 respectively if the number is negative, zero, or positive. */
    int signum();

    /**
     * Divides this value by other and returns the integer part of the quotient (this / divisor)
     * rounded down followed by the remainder.
     *
     * <p>The remainder will ALWAYS have the same sign as the dividend.
     *
     * @return The integer part of this / divisor followed by the remainder.
     * @throws ArithmeticException if the divisor is zero.
     */
    Number<?>[] divideToIntegralValue(T divisor);

    /**  Converts the number into a human-readable format. */
    String displayValue();

    /** Converts to an {@link SInteger} or throws {@link InexactException} if the conversion is not exact.*/
    SInteger integerValueExact() throws InexactException;

    /** Tests whether {@link ::integerValueExact} will throw an exception. */
    default boolean isInteger() {
        try {
            integerValueExact();
            return true;
        } catch(InexactException e) {
            return false;
        }
    }

    /** Returns whether the number is exact. */
    boolean isExact();
}
