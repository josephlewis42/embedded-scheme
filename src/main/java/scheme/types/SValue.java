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

package main.java.scheme.types;

import main.java.scheme.EvaluationException;

/**
 * SObject is the base-class for Scheme objects.
 */
public abstract class SValue {	
	private boolean immutable = false;
	
	/**
	 * assertMutable validates that the object is marked as 
	 * mutable.
	 */
	protected void assertMutable() {
		if(immutable) {
			throw new EvaluationException("object is immutable");
		}
	}
	
	/**
	 * markImmutable marks the object as immutable.
	 * 
	 * This property MUST be enforced by the object's methods.
	 */
	public void markImmutable() {
		immutable = true;
	}
	
	/**
	 * isTruthy checks whether this value should evaluate to 
	 * true in test expressions. R5RS states that every value
	 * except #f is "true".
	 * 
	 * @return true if the value isn't explicitly #f
	 */
	public boolean isTruthy() {
		if (isBoolean()) {
			return getBoolean().toBoolean();
		}

		return true;
	}
	
	/**
	 * toScheme returns the value of this
	 * type formatted as a string that would
	 * produce the same object (if possible).
	 *  
	 * @return a Scheme representation of the
	 * object.
	 */
	public abstract String toScheme();
	
	@Override
	public String toString() {
		return toScheme();
	}

	/**
	 * The following functions are utilities for building Scheme
	 * they allow nice(r) conversions of built-in types than Java
	 * would otherwise provide.
	 */	

	protected EvaluationException wrongTypeException(String typeName) {
		return new EvaluationException(String.format("value %s isn't a %s, it's a %s", toString(), typeName, this.getClass().getCanonicalName()));
	}

	public boolean isBoolean() {
		return false;
	}
	
	public SBoolean getBoolean() {
		throw wrongTypeException("boolean"); 
	}
	
	public boolean isCharacter() {
		return false;
	}
	
	public SCharacter getCharacter() {
		throw wrongTypeException("character"); 
	}
	
	public boolean isProcedure() {
		return false;
	}
	
	public SProcedure getProcedure() {
		throw wrongTypeException("procedure"); 
	}
	
	public boolean isClosure() {
		return false;
	}
	
	public SClosure getClosure() {
		throw wrongTypeException("closure");
	}

	public boolean isString() {
		return false;
	}
	
	public SString getString() {
		throw wrongTypeException("string"); 
	}
	
	public boolean isNumber() {
		return false;
	}
	
	public SNumber getNumber() {
		throw wrongTypeException("number"); 
	}
	
	public boolean isSymbol() {
		return false;
	}
	
	public SSymbol getSymbol() {
		throw wrongTypeException("symbol"); 
	}
	
	public boolean isNull() {
		return false;
	}
	
	public SPair getNull() {
		throw wrongTypeException("null"); 
	}

	public boolean isVector() {
		return false;
	}

	public SVector getVector() {
		throw wrongTypeException("vector"); 
	}

	public boolean isPair() {
		return false;
	}
	
	public SPair getPair() {
		throw wrongTypeException("pair");
	}

	public boolean isPort() {
		return false;
	}

	public SPort getPort() {
		throw wrongTypeException("port");
	}
}
