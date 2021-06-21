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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public class SInteger implements Number<SInteger>, SRationalPromotable, SRealPromotable {

	public static final SInteger ONE = new SInteger(BigInteger.ONE);
	public static final SInteger ZERO = new SInteger(BigInteger.ZERO);

	private final BigInteger value;
	private final boolean exact;

	private SInteger(final BigInteger value) {
		this.value = Objects.requireNonNull(value, "value");
		exact = true;
	}

	public static SInteger of(final BigInteger value) {
		return new SInteger(value);
	}

	public BigInteger toBigInteger() {
		return value;
	}

	public SReal toSReal() {
		return SReal.of(this);
	}

	@Override
	public SRational toSRational() {
		return SRational.of(this);
	}

	public SInteger gcd(SInteger other) {
		return of(value.gcd(other.value));
	}

	public SInteger lcm(SInteger other) {
		var upper = value.multiply(other.value).abs();
		var lower = gcd(other);

		return of(upper.divide(lower.value));
	}

	public SInteger add(SInteger other) {
		return of(value.add(other.value));
	}

	public SInteger multiply(SInteger other) {
		return of(value.multiply(other.value));
	}

	public SInteger divide(SInteger other) {
		// lossy
		return of(value.divide(other.value));
	}

	public SInteger subtract(SInteger other) {
		return of(value.subtract(other.value));
	}

	public SInteger remainder(SInteger other) {
		return of(value.remainder(other.value));
	}

	public SInteger getNumerator() {
		return this;
	}

	public SInteger getDenominator() {
		return ONE;
	}

	public SInteger negate() {
		return of(value.negate());
	}

	public int signum() {
		return value.signum();
	}

	public SInteger[] divideAndRemainder(SInteger other) {
		return Arrays.stream(value.divideAndRemainder(other.value))
				.map(SInteger::of)
				.toArray(SInteger[]::new);
	}

	public SRational reciprocal() {
		return SRational.of(ONE, this);
	}

	public boolean equals(Object other) {
		if (other == null || ! (other instanceof SInteger)) {
			return false;
		}

		return ((SInteger) other).value.equals(value);
	}

	public int compareTo(SInteger other) {
		return value.compareTo(other.value);
	}

	@Override
	public SInteger[] divideToIntegralValue(SInteger divisor) {
			BigInteger[] res = this.value.divideAndRemainder(divisor.value);

			return new SInteger[]{of(res[0]), of(res[1])};
	}

	@Override
	public String displayValue() {
		return value.toString();
	}

	@Override
	public SInteger integerValueExact() throws InexactException {
		return this;
	}

	@Override
	public boolean isExact() {
		return exact;
	}
}
