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

import java.nio.CharBuffer;
import java.util.Arrays;

import scheme.EvaluationException;

public class SString extends SValue implements Comparable<SString>, CharSequence {
	private char[] internal;

	public SString(CharSequence sequence) {
		this.internal = sequence.toString().toCharArray();
	}

	public static SString ofSize(int size) {
		return ofSizeWithFill(size, '\0');
	}

	public static SString ofSizeWithFill(int size, char fill) {
		return new SString(String.valueOf(fill).repeat(size));
	}

	public static SString ofChars(char... chars) {
		return new SString(String.valueOf(chars));
	}

	public int length() {
		return internal.length;
	}

	public char ref(int index) throws EvaluationException {
		assertInRange(index);
		return charAt(index);
	}

	public void set(int index, char val) throws EvaluationException {
		assertMutable();
		assertInRange(index);

		internal[index] = val;
	}
	
	public void fill(char val) throws EvaluationException {
		assertMutable();
        for(int i = 0; i < length(); i++) {
        	internal[i] = val;
        }
	}

	public String javaString() {
		return String.valueOf(internal);
	}

	public String toString() {
		return javaString();
	}

	private void assertInRange(int index) throws EvaluationException {
		if (index < 0 || index >= internal.length) {
			throw EvaluationException.format("index %d is out of bounds for string of length %d", index,
					internal.length);
		}
	}

	@Override
	public int compareTo(SString o) {
		return Arrays.compare(internal, o.internal);
	}

	public int compareToCaseInsensitive(SString o) {
		return javaString().compareToIgnoreCase(o.javaString());
	}

	@Override
	public char charAt(int index) {
		assertInRange(index);
		return internal[index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return String.valueOf(internal).subSequence(start, end);
	}
	
	@Override
	public boolean isString() {
		return true;
	}
	
	@Override
	public SString getString() {
		return this;
	}

	@Override
	public String toScheme() {
		// TODO: escape this value
		return String.format("\"%s\"", String.valueOf(internal));
	}
}
