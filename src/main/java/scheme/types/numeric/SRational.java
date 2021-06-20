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

public class SRational implements Field<SRational>, SRealPromotable, SIntegerDemotable, Number<SRational> {

	private final SInteger numerator;
	private final SInteger denominator;

	private SRational(SInteger numerator, SInteger denominator) {
		var tmpNum = Objects.requireNonNull(numerator, "numerator");
		var tmpDen = Objects.requireNonNull(denominator, "denominator");
		var gcd = tmpNum.gcd(tmpDen);

		this.numerator = numerator.divide(gcd);
		this.denominator = denominator.divide(gcd);
	}

	public static SRational of(SInteger integer) {
		return new SRational(integer, SInteger.ONE);
	}

	public static SRational of(SInteger numerator, SInteger denominator) {
		return new SRational(numerator, denominator);
	}

	public SRational of(SReal real) {
		var res = real.toBigDecimal().divideAndRemainder(BigDecimal.ONE);
		var quotient = SInteger.of(res[0].toBigIntegerExact());
		var remainder = SReal.of(res[1]);

		var lhs = of(SInteger.ZERO, SInteger.ONE);
		var rhs = of(SInteger.ONE, SInteger.ONE);
		SRational mediant;
		while (true) {
			mediant = lhs.mediant(rhs);
			var comparison = remainder.compareTo(mediant);
			if (comparison == 0) {
				break;
			} else if (comparison > 0) {
				lhs = mediant;
			} else {
				rhs = mediant;
			}
		}

		return of(
				mediant.numerator.add(mediant.denominator.multiply(quotient)),
				mediant.denominator);

	}

	/**
	 * Computes the mediant of the two rationals by adding the numerators and
	 * denominators. The resulting value will be strictly between the two inputs.
	 */
	public SRational mediant(SRational other) {
		var num = numerator.add(other.numerator);
		var denom = denominator.add(other.denominator);

		return of(num, denom);
	}

	public SInteger getNumerator() {
		return numerator;
	}

	public SInteger getDenominator() {
		return denominator;
	}

	@Override
	public SReal toSReal() {
		return SReal.of(this);
	}

	@Override
	public SInteger toSIntegerExact() throws InexactException {
		if (denominator.equals(SInteger.ONE)) {
			return numerator;
		}

		throw new InexactException("can't convert to an integer");
	}

	@Override
	public SRational add(SRational value) {
		var lcm = denominator.lcm(value.denominator);
		var lhsNumerator = numerator.multiply(lcm);
		var rhsNumerator = value.numerator.multiply(lcm);
		
		return of(lhsNumerator.add(rhsNumerator), denominator.multiply(lcm));
	}

	@Override
	public SRational divide(SRational a) {
		return multiply(a.reciprocal());
	}

	public SRational multiply(SInteger value) {
		return of(numerator.multiply(value), denominator);
	}

	@Override
	public SRational multiply(SRational value) {
		return of(
				numerator.multiply(value.numerator),
				denominator.multiply(value.denominator));
	}

	@Override
	public SRational negate() {
		return multiply(SInteger.ONE.negate());
	}

	@Override
	public SRational reciprocal() {
		return of(denominator, numerator);
	}

	@Override
	public SRational subtract(SRational value) {
		return add(value.negate());
	}

	public int signum() {
		var ns = numerator.signum();
		var ds = denominator.signum();
		
		if (ns == 0) {
			return 0;
		}

		if (ns == ds) {
			return 1;
		}

		return -1;
	}

	@Override
	public int compareTo(SRational other) {
		var lcm = denominator.lcm(other.denominator);
		var lhsNumerator = numerator.multiply(lcm);
		var rhsNumerator = other.numerator.multiply(lcm);

		return lhsNumerator.compareTo(rhsNumerator);
	}
}
