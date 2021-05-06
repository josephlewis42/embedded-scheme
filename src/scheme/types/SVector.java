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

import java.util.Arrays;
import java.util.stream.Collectors;

import scheme.EvaluationException;

public class SVector extends SValue {

	private SValue[] internal;

	private SVector(final int size, final SValue fill) throws EvaluationException {
		if (size < 0) {
			throw new EvaluationException(String.format("%d isn't a valid vector length", size));
		}

		internal = new SValue[size];
		Arrays.fill(internal, fill);
	}

	private SVector(SValue[] contents) {
		internal = Arrays.copyOf(contents, contents.length);
	}

	/**
	 * Creates a new vector of the given size populated with false booleans.
	 * 
	 * @param size the size of the vector.
	 * @return new vector of the given size populated with false values.
	 * @throws EvaluationException
	 */
	public static SVector ofSize(final int size) {
		return new SVector(size, SBoolean.FALSE);
	}

	/**
	 * Creates a new vector of the given size populated with the fill value.
	 * 
	 * @param size the size of the vector.
	 * @param fill the value to fill with.
	 * @return a new vector of the given size and fill.
	 * @throws EvaluationException
	 */
	public static SVector ofSizeWithFill(final int size, final SValue fill) {
		return new SVector(size, fill);
	}

	/**
	 * Creates a new vector of the given size populated with the fill value.
	 * 
	 * @param size the size of the vector.
	 * @param fill the value to fill with.
	 * @return a new Vector filled with the contents of the list.
	 * @throws EvaluationException
	 */
	public static SVector fromList(final SPair list) {
		var contents = list.toArray();
		return new SVector(contents);
	}

	/**
	 * Creates a new vector from the given values.
	 * 
	 * @param values an ordered list of values.
	 * @return A new vector with the given values.
	 */
	public static SVector of(SValue... values) {
		return new SVector(values);
	}

	/**
	 * Returns the number of elements in vector
	 * 
	 * @return The length of the vector.
	 */
	public int length() {
		return internal.length;
	}

	/**
	 * Fetches an item reference from the given index of the vector.
	 * 
	 * @param index the index to fetch from
	 * @return The item at the index
	 * @throws EvaluationException if
	 */
	public SValue ref(int index) throws EvaluationException {
		assertInRange(index);
		return internal[index];
	}

	public SValue set(int index, SValue val) throws EvaluationException {
		assertMutable();
		assertInRange(index);

		var old = internal[index];
		internal[index] = val;
		return old;
	}

	public void fill(SValue val) {
		assertMutable();

		Arrays.fill(internal, val);
	}

	private void assertInRange(int index) {
		if (index < 0 || index >= internal.length) {
			throw EvaluationException.format("index %d is out of bounds for vector of length %d", index,
					internal.length);
		}
	}

	public SValue[] toArray() {
		return Arrays.copyOf(internal, internal.length);
	}
	
	@Override
	public boolean isVector() {
		return true;
	}
	
	@Override
	public SVector getVector() {
		return this;
	}

	@Override
	public String toScheme() {
		return Arrays.stream(internal).map(SValue::toScheme).collect(Collectors.joining(" ", "#(", ")"));
	}
}
