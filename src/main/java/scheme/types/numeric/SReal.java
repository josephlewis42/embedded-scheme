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

import java.math.BigDecimal;
import java.util.Objects;

public class SReal implements Number<SReal>, Field<SReal>, SIntegerDemotable {

	public static final SReal ZERO = of(BigDecimal.ZERO);
	public static final SReal ONE = of(BigDecimal.ONE);

	private final BigDecimal value;

	private SReal(final BigDecimal value) {
		this.value = Objects.requireNonNull(value);
	}

	public static SReal of(final BigDecimal val) {
		return new SReal(val);
	}

	public static SReal of(final SInteger integer) {
		return new SReal(new BigDecimal(integer.toBigInteger()));
	}

	public static SReal of(final SRational rational) {
		var num = new BigDecimal(rational.getNumerator().toBigInteger());
		var den = new BigDecimal(rational.getDenominator().toBigInteger());

		return of(num.divide(den));
	}

	public BigDecimal toBigDecimal() {
		return value;
	}

	public int compareTo(SRational mediant) {
		var num = of(mediant.getNumerator());
		var den = of(mediant.getDenominator());
		
		var decimalValue = num.divide(den).toBigDecimal();

		return value.compareTo(decimalValue);
	}

	public SReal add(SReal other) {
		return of(value.add(other.value));
	}

	public SReal subtract(SReal other) {
		return of(value.subtract(other.value));
	}

	public SReal multiply(SReal other) {
		return of(value.multiply(other.value));
	}

	public SReal divide(SReal other) {
		return of(value.divide(other.value));
	}

	public SReal negate() {
		return of(value.negate());
	}

	public SReal reciprocal() {
		return of(BigDecimal.ONE.divide(value));
	}

	public SReal add(SRealPromotable other) {
		return add(other.toSReal());
	}

	public SReal subtract(SRealPromotable other) {
		return subtract(other.toSReal());
	}

	public SReal multiply(SRealPromotable other) {
		return multiply(other.toSReal());
	}

	public SReal divide(SRealPromotable other) {
		return divide(other.toSReal());
	}


	@Override
	public SInteger toSIntegerExact() throws InexactException {
		try {
			return SInteger.of(value.toBigIntegerExact());
		}catch(ArithmeticException e) {
			throw new InexactException("inexact conversion");
		}
	}

	@Override
	public int compareTo(SReal o) {
		return value.compareTo(o.value);
	}

	public int signum() {
		return value.signum();
	}
}
