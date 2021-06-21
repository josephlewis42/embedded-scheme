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

package scheme.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import scheme.EvaluationException;
import scheme.types.numeric.*;
import scheme.types.numeric.Number;

public class SNumber extends SValue implements Comparable<SNumber> {

	public static final SNumber ZERO = of(SInteger.ZERO);
	public static final SNumber ONE = of(SInteger.ONE);

	private final Number<?> wrapped;

	public SNumber(final Number<?> wrapped) {
		this.wrapped = Objects.requireNonNull(wrapped);
	}

	public static SNumber of(final Number<?> value) {
		return new SNumber(value);
	}

	public static SNumber of(final BigDecimal val) {
		return new SNumber(SReal.of(val));
	}

	public static SNumber of(final long num) {
		return of(BigInteger.valueOf(num));
	}

	public static SNumber of(final double num) {
		return of(BigDecimal.valueOf(num));
	}
	
	public static SNumber of(BigInteger obj) {
		return new SNumber(SInteger.of(obj));
	}

	public boolean isComplex() {
		return true; // always true for BigDecimal
	}

	public boolean isReal() {
		return true; // always true for BigDecimal
	}

	public boolean isRational() {
		return true; // always true for BigDecimal
	}

	public boolean isInteger() {
		return wrapped.isInteger();
	}

	public boolean isExact() {
		return Numbers.isExact(wrapped);
	}

	public SNumber add(SNumber other) {
		return of(Numbers.add(wrapped, other.wrapped));
	}
	
	public SNumber subtract(SNumber other) {
		return of(Numbers.subtract(wrapped, other.wrapped));
	}
	
	public SNumber multiply(SNumber other) {
		return of(Numbers.multiply(wrapped, other.wrapped));
	}
	
	public SNumber divide(SNumber other) throws EvaluationException {
		try {
			return of(Numbers.divide(wrapped, other.wrapped));
		} catch(ArithmeticException e) {
			throw new EvaluationException(e);
		}
	}
	
	public SNumber negate() throws EvaluationException {
		return of(Numbers.negate(this.wrapped));
	}

	// TODO fix this
	public String toScheme() {
		return Numbers.displayValue(wrapped);
	}

	public SNumber quotient(SNumber other) throws EvaluationException {
		return SNumber.of(Numbers.quotient(wrapped, other.wrapped));
	}

	public SNumber remainder(SNumber other) throws EvaluationException {
		return SNumber.of(Numbers.remainder(wrapped, other.wrapped));
	}

	public SNumber modulo(SNumber other) throws EvaluationException {
		return SNumber.of(Numbers.modulo(wrapped, other.wrapped));
	}

	public static SNumber parse(String data, int base) throws EvaluationException {
		if(base != 10) {
			throw EvaluationException.format("unsupported base %d", base);
		}

		try {
			return of(new BigInteger(data));
		} catch (NumberFormatException e) {
			// Might not be an integer.
		}

		try {
			return of(new BigDecimal(data));
		} catch (NumberFormatException e) {
			throw new EvaluationException(e);
		}
	}
	
	@Override
	public int compareTo(SNumber o) {
		return Numbers.compareTo(this.wrapped, o.wrapped);
	}
	
	@Override
	public boolean isNumber() {
		return true;
	}
	
	@Override
	public SNumber getNumber() {
		return this;
	}

	public BigInteger toBigInteger() {
		try {
			return Numbers.integerValue(wrapped).toBigInteger();
		} catch (InexactException e) {
			throw new EvaluationException(e);
		}
	}

	public int toInteger() {
		return toBigInteger().intValue();
	}

	public long toLong() {
		return toBigInteger().longValue();
	}

	public BigDecimal toBigDecimal() {
		return Numbers.realValue(wrapped).toBigDecimal();
	}

	public double toDouble() {
		return toBigDecimal().doubleValue();
	}
}
