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
import java.math.MathContext;
import java.util.Objects;
import java.util.function.Supplier;

import scheme.EvaluationException;

public class SNumber extends SValue implements Comparable<SNumber> {

	public static final SNumber ZERO = of(BigDecimal.ZERO);
	public static final SNumber ONE = of(BigDecimal.ONE);

	// This is an oversimplification of types and won't
	// properly represent all values that Scheme supports
	// specifically complex numbers;
	private final BigDecimal internal;

	public SNumber(final BigDecimal internal) {
		Objects.requireNonNull(internal);

		this.internal = internal;
	}

	public static SNumber of(final BigDecimal val) {
		return new SNumber(val);
	}

	public static SNumber of(final long num) {
		return new SNumber(BigDecimal.valueOf(num));
	}

	public static SNumber of(final double num) {
		return new SNumber(BigDecimal.valueOf(num));
	}
	
	public static SNumber of(BigInteger obj) {
		return new SNumber(new BigDecimal(obj));
	}


	public BigInteger toBigInteger() throws EvaluationException {
		return catchInexact(internal::toBigIntegerExact);
	}
	
	public BigDecimal toBigDecimal() throws EvaluationException {
		return internal;
	}
	
	public double toDouble() throws EvaluationException {
		return catchInexact(internal::doubleValue);
	}
	
	public int toInteger() throws EvaluationException {
		return catchInexact(toBigInteger()::intValueExact);
	}
	
	public long toLong() throws EvaluationException {
		return catchInexact(toBigInteger()::longValueExact);
	}

	private <T> T catchInexact(Supplier<T> supplier)throws EvaluationException {
		try {
			return supplier.get();
		} catch(ArithmeticException e) {
			throw new EvaluationException("number can't be converted exactly");
		}
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
		// https://stackoverflow.com/a/12748321
		return internal.stripTrailingZeros().scale() <= 0;
	}
	
	public boolean isExact() {
		return isInteger();
	}
		
	public SNumber sqrt() throws EvaluationException {
		try {
			return new SNumber(internal.sqrt(MathContext.DECIMAL64));
		} catch(ArithmeticException e) {
			throw new EvaluationException(e);
		}
	}
	
	public SNumber add(SNumber other) {
		return new SNumber(this.internal.add(other.internal));
	}
	
	public SNumber subtract(SNumber other) {
		return new SNumber(this.internal.subtract(other.internal));
	}
	
	public SNumber multiply(SNumber other) {
		return new SNumber(this.internal.multiply(other.internal));
	}
	
	public SNumber divide(SNumber other) throws EvaluationException {
		try {
			return new SNumber(this.internal.divide(other.internal, MathContext.DECIMAL64));
		} catch(ArithmeticException e) {
			throw new EvaluationException(e);
		}
	}
	
	public SNumber negate() throws EvaluationException {
		return new SNumber(this.internal.negate(MathContext.DECIMAL64));
	}
	
	public String toScheme() {
		return internal.toString();
	}
	
	public SNumber quotient(SNumber other) throws EvaluationException {
		return SNumber.of(internal.divideToIntegralValue(other.internal));
	}
	
	public SNumber remainder(SNumber other) throws EvaluationException {
		return SNumber.of(internal.divideAndRemainder(other.internal)[1]);
	}
	
	public SNumber modulo(SNumber other) throws EvaluationException {
		SNumber res = remainder(other);
		if (other.compareTo(SNumber.ZERO) < 0){
			if (res.compareTo(SNumber.ZERO) <= 0){
				return res;
			} else {
				return res.add(other);
			}
		} else {
			if (res.compareTo(SNumber.ZERO) >= 0){
				return res;
			} else {
				return res.add(other);
			}
		}
	}

	public static SNumber parse(String data, int base) throws EvaluationException {
		if(base != 10) {
			throw EvaluationException.format("unsupported base %d", base);
		}
		
		try {
			return of(new BigDecimal(data));
		} catch(NumberFormatException e) {
			throw new EvaluationException(e);
		}
	}
	
	@Override
	public int compareTo(SNumber o) {
		return internal.compareTo(o.internal);
	}
	
	@Override
	public boolean isNumber() {
		return true;
	}
	
	@Override
	public SNumber getNumber() {
		return this;
	}
}
