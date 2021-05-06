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

/**
 * SCharacter represents a character Scheme value.
 */
public class SCharacter extends SValue implements Comparable<SCharacter>{
	// value is the wrapped character
	private final char value;

	public SCharacter(final char val) {
		this.value = val;
		markImmutable();
	}
	
	/**
	 * Returns the primitive value representation of the SCharacter.
	 * 
	 * @return a char with equivalent value to the SCharacter.
	 */
	public char toChar() {
		return value;
	}

	@Override
	public String toScheme() {
		switch (value) {
		case ' ':
			return "#\\space";
		case '\n':
			return "#\\newline";
		default:
			if(Character.isLetterOrDigit(value)) {
				return String.format("#\\%c", value);
			}
			
			return String.format("#\\U+%02x", (int)value);
		}
	}

	@Override
	public int compareTo(final SCharacter o) {
		return Character.compare(value, o.value);
	}
	
	public int compareToIgnoreCase(final SCharacter o) {
		var thisLower = Character.toLowerCase(value);
		var otherLower = Character.toLowerCase(o.value);
		
		return Character.compare(thisLower, otherLower);
	}
	
	@Override
	public boolean isCharacter() {
		return true;
	}

	@Override
	public SCharacter getCharacter() {
		return this;
	}
}
