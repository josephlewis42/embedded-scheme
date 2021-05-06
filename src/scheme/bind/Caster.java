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

package scheme.bind;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

import scheme.types.SBoolean;
import scheme.types.SCharacter;
import scheme.types.SInputPort;
import scheme.types.SNull;
import scheme.types.SNumber;
import scheme.types.SOutputPort;
import scheme.types.SPair;
import scheme.types.SPort;
import scheme.types.SProcedure;
import scheme.types.SString;
import scheme.types.SSymbol;
import scheme.types.SValue;
import scheme.types.SVector;

/**
 * Converts values between Java and Scheme types.
 */
public final class Caster {

	private Caster() {
		// Private utility class constructor.
	}

	public static Function<Object, SValue> toValue(Class<?> clazz) throws BindException {

		// Void (undefined results) are implicitly turned into the empty list.
		if (Void.TYPE.isAssignableFrom(clazz)) {
			return (obj) -> SNull.NULL;
		}

		// Native Scheme types
		if (SValue.class.isAssignableFrom(clazz)) {
			return (obj) -> (SValue) obj;
		}

		// Java casts, these come after tests for built-in types
		// and are sorted most to least specific.
		if (boolean.class.isAssignableFrom(clazz)) {
			return (obj) -> SBoolean.valueOf((boolean) obj);
		}

		if (int.class.isAssignableFrom(clazz)) {
			return (obj) -> SNumber.of((int) obj);
		}

		if (long.class.isAssignableFrom(clazz)) {
			return (obj) -> SNumber.of((long) obj);
		}

		if (BigInteger.class.isAssignableFrom(clazz)) {
			return (obj) -> SNumber.of((BigInteger) obj);
		}

		if (BigDecimal.class.isAssignableFrom(clazz)) {
			return (obj) -> SNumber.of((BigDecimal) obj);
		}

		// CharSequence is an interface that CharBuffer, Segment, String,
		// StringBuffer, and StringBuilder implement.
		// SString also implements so this check MUST be done below it.
		if (CharSequence.class.isAssignableFrom(clazz)) {
			return (obj) -> {
				return new SString((CharSequence) obj);
			};
		}

		if (char.class.isAssignableFrom(clazz)) {
			return (obj) -> new SCharacter((char) obj);
		}

		throw new BindException(String.format("%s can't be converted to a value", clazz.getCanonicalName()));
	}

	public static Function<SValue, Object> fromValue(Class<?> clazz) throws BindException {
		// Identity
		if (clazz.isAssignableFrom(SValue.class)) {
			return box -> box;
		}

		if (clazz.isAssignableFrom(SVector.class)) {
			return (box) -> box.getVector();
		}

		if (clazz.isAssignableFrom(SPair.class)) {
			return (box) -> box.getPair();
		}

		if (clazz.isAssignableFrom(SSymbol.class)) {
			return (box) -> box.getSymbol();
		}

		if (clazz.isAssignableFrom(SString.class)) {
			return (box) -> box.getString();
		}

		if (clazz.isAssignableFrom(SNumber.class)) {
			return (box) -> box.getNumber();
		}

		if (clazz.isAssignableFrom(SBoolean.class)) {
			return (box) -> box.getBoolean();
		}

		if (clazz.isAssignableFrom(SProcedure.class)) {
			return (box) -> box.getProcedure();
		}

		if (clazz.isAssignableFrom(SCharacter.class)) {
			return (box) -> box.getCharacter();
		}

		if (clazz.isAssignableFrom(SPort.class)) {
			return (box) -> box.getPort();
		}

		if (clazz.isAssignableFrom(SOutputPort.class)) {
			return (box) -> box.getPort().getOutputPort();
		}

		if (clazz.isAssignableFrom(SInputPort.class)) {
			return (box) -> box.getPort().getInputPort();
		}

		// Java casts
		if (clazz.isAssignableFrom(BigInteger.class)) {
			return (box) -> box.getNumber().toBigInteger();
		}

		if (clazz.isAssignableFrom(Integer.class)) {
			return (box) -> box.getNumber().toInteger();
		}

		if (clazz.isAssignableFrom(int.class)) {
			return (box) -> box.getNumber().toInteger();
		}

		if (clazz.isAssignableFrom(Long.class)) {
			return (box) -> box.getNumber().toLong();
		}

		if (clazz.isAssignableFrom(long.class)) {
			return (box) -> box.getNumber().toLong();
		}

		if (clazz.isAssignableFrom(double.class)) {
			return (box) -> box.getNumber().toDouble();
		}

		if (clazz.isAssignableFrom(BigDecimal.class)) {
			return (box) -> box.getNumber().toBigDecimal();
		}

		if (clazz.isAssignableFrom(String.class)) {
			return (box) -> box.getString().javaString();
		}

		if (clazz.isAssignableFrom(char.class)) {
			return (box) -> box.getCharacter().toChar();
		}

		throw new BindException(String.format("value can't be converted to %s", clazz.getCanonicalName()));
	}
}
