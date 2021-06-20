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
 * SBoolean represents a boolean Scheme value.
 */
public class SBoolean extends SValue {
	
	// The SBoolean constant true. 
	public static final SBoolean TRUE = new SBoolean(true);
	
	// The SBoolean constant false.
	public static final SBoolean FALSE = new SBoolean(false);
		
	/**
	 * Returns an SBoolean whose logical value is equal to the specified boolean.
	 * 
	 * @param value - value of the SBoolean to return.
	 * @return an SBoolean with the specified value.
	 */
	public static SBoolean valueOf(boolean value) {
		return value ? TRUE : FALSE;
	}
	
	private final boolean value;
	
	private SBoolean(boolean value) {
		this.value = value;
		markImmutable(); // sanity check
	}

	/**
	 * Returns the primitive value representation of the SBoolean.
	 * 
	 * @return a boolean with equivalent logical value to the SBoolean.
	 */
	public boolean toBoolean() {
		return value;
	}
	
	@Override
	public boolean isBoolean() {
		return true;
	}
	
	@Override
	public SBoolean getBoolean() {
		return this;
	}
	
	public boolean equals(SBoolean o) {
		return this.value == o.value;
	}

	/**
	 * Returns #t for the TRUE SBoolean, #f for the FALSE SBoolean.
	 */
	@Override
	public String toScheme() {
		return value ? "#t" : "#f";
	}
}
