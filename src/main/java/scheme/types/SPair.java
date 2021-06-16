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

import java.util.LinkedList;
import java.util.Objects;

import main.java.scheme.EvaluationException;

public class SPair extends SValue {
	private SValue car;
	private SValue cdr;

	/**
	 * Empty represents the empty pair.
	 * 
	 * @return
	 */
	public static SValue empty() {
		return SNull.NULL;
	}
	
	/**
	 * listOf creates a proper list e.g. a list where the car of each pair is an
	 * entry and the cdr is another pair.
	 * 
	 * A proper list ends with an empty (null) pair.
	 * 
	 * There MUST be at least one element in values.
	 * 
	 * @param values the entries of the list.
	 * @return
	 * @throws EvaluationException 
	 */
	public static SPair properListOf(SValue... values) throws EvaluationException {
		return improperListOf(empty(), values).getPair();
	}


	/**
	 * listOf creates a proper list e.g. a list where the car of each pair is an
	 * entry and the cdr is another pair.
	 * 
	 * A proper list ends with an empty (null) pair.
	 * 
	 * @param values the entries of the list.
	 * @return
	 */
	public static SValue properListOfNullable(SValue... values) {
		return improperListOf(empty(), values);
	}

	/**
	 * ImproperListOf creates an improper list with the given final value.
	 * 
	 * @param last
	 * @param values
	 * @return
	 */
	public static SValue improperListOf(SValue last, SValue... values) {
		for (var v : values) {
			Objects.requireNonNull(v);
		}

		var result = last;
		for (int i = values.length - 1; i >= 0; i--) {
			result = cons(values[i], result);
		}

		return result;
	}

	public static SPair cons(SValue car, SValue cdr) {
		Objects.requireNonNull(car, "car must not be null");
		Objects.requireNonNull(cdr, "cdr must not be null");
		var tmp = new SPair();
		tmp.car = car;
		tmp.cdr = cdr;
		return tmp;
	}

	public void setCar(SValue v) throws EvaluationException {
		assertMutable();

		Objects.requireNonNull(v, "car must not be null");
		car = v;
	}

	public void setCdr(SValue v) throws EvaluationException {
		assertMutable();

		Objects.requireNonNull(v, "cdr must not be null");
		cdr = v;
	}

	public SValue getCar() throws EvaluationException {
		if(isNull()) {
			throw new EvaluationException("can't take car of ()");
		}

		return car;
	}

	public SValue getCdr() throws EvaluationException {
		if(isNull()) {
			throw new EvaluationException("can't take cdr of ()");
		}

		return cdr;
	}
	
	public SPair reversed() {
		if(isNull()) {
			return this;
		}
		
		SPair head = SNull.NULL;
		SPair element = this;
		while (true) {
			head = cons(element.car, head);		
			
			if(element.cdr.isNull()) {
				break;
			}
			
			element = element.cdr.getPair();
		}
		
		return head;
	}

	public boolean isList() {
		if (isNull()) {
			return true;
		}

		if (cdr.isNull()) {
			return true;
		}

		return cdr.isPair() &&
				cdr.getPair() != this && // don't recurse
				cdr.getPair().isList();
	}

	public String toSchemeBounded(int count) {
		if (isNull()) {
			return "()";
		}

		var out = new StringBuilder();
		out.append("(");

		var next = this;
		for(int i = count; i != 0; i--) {
			out.append(next.car.toScheme());
			out.append(" ");

			if (next.cdr.isPair()) {
				next = next.cdr.getPair();
			} else {
				// don't print final nulls
				if (next.cdr.isNull()) {
					break;
				}

				// if the pair is an irregular list
				// demonstrate it by adding a dot
				// before the final element.
				out.append(". ");
				out.append(next.cdr.toScheme());
				break;
			}
		}
		
		if(count > 0) {
			out.append(" ... ");
		}

		out.append(")");

		return out.toString();

	}

	public String toScheme() {
		return toSchemeBounded(-1);
	}

	public String toString() {
		return toSchemeBounded(20);
	}

	public SValue[] toArray() throws EvaluationException {
		// null check first for performance
		if (isNull()) {
			return new SValue[0];
		}

		if (!isList()) {
			throw EvaluationException.format("%s not a list", toString());
		}

		var tmp = new LinkedList<SValue>();
		var next = this;
		while (true) {
			tmp.add(next.car);

			if (next.cdr.isNull()) {
				break;
			}

			next = next.cdr.getPair();
		}

		return tmp.toArray(SValue[]::new);
	}
	
	@Override
	public boolean isPair() {
		return true;
	}
	
	@Override
	public SPair getPair() {
		return this;
	}
}
