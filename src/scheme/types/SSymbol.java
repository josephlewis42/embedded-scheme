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

import java.util.HashMap;
import java.util.Objects;

public class SSymbol extends SValue implements Comparable<SSymbol>  {

	private static final HashMap<String, SSymbol> INTERNED = new HashMap<>();
	
	public static final SSymbol UNQUOTE_SPLICING = SSymbol.of("unquote-splicing");
	public static final SSymbol QUOTE = SSymbol.of("quote");
	public static final SSymbol QUASIQUOTE = SSymbol.of("quasiquote");
	public static final SSymbol UNQUOTE = SSymbol.of("unquote");

	private final String symbol;
	private final boolean uninterned;

	private SSymbol(String value, boolean unique) {
		Objects.requireNonNull(value, "value must not be null");
		symbol = value;
		this.uninterned = unique;
	}
	
	public static SSymbol of(String value) {
		var normalized = value.toLowerCase();
		
		var tmp = INTERNED.get(normalized);
		if(tmp != null) {
			return tmp;
		}
		
		tmp = new SSymbol(normalized, false);
		INTERNED.put(normalized, tmp);
		return tmp;
	}
	
	public static SSymbol fromSString(SString value) {
		var normalized = value.javaString();
		
		var tmp = INTERNED.get(normalized);
		if(tmp != null) {
			return tmp;
		}
		
		tmp = new SSymbol(normalized, false);
		INTERNED.put(normalized, tmp);
		return tmp;
	}

		
	private static int uninternCounter = 0;
	public static SSymbol unique(String prefix) {
		var name = String.format("%s%d", prefix, uninternCounter++);
		return new SSymbol(name, true);
	}

	public SString getStringRepresentation() {
		return new SString(symbol);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		// If the symbol is declared uninterned, it will
		// only compare with itself.
		if (this.uninterned) {
			return false;
		}
		
		if (obj == null) {
			return false;
		}
		
		if( !(obj instanceof SSymbol)) {
			return false;
		}
		
		return compareTo((SSymbol) obj) == 0;
	}
	
	@Override
	public int hashCode() {
		return symbol.hashCode();
	}

	@Override
	public int compareTo(SSymbol o) {
		Objects.requireNonNull(o, "other must not be null");
		
		return symbol.compareTo(o.symbol);
	}
	
	@Override
	public boolean isSymbol() {
		return true;
	}
	
	@Override
	public SSymbol getSymbol() {
		return this;
	}

	@Override
	public String toScheme() {
		if(uninterned) {
			return String.format("#<uninterned-symbol %s>", symbol);
		}
		
		return symbol;
	}
}
