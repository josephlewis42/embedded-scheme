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

package scheme;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import scheme.types.SSymbol;
import scheme.types.SValue;

/**
 * Environment captures the set variables within a scope.
 */
public class Environment {
	private final Optional<Environment> parent;
	private final Map<SSymbol, SValue> env;

	public Environment(Optional<Environment> parent) {
		this.parent = parent;
		env = new HashMap<>();
	}

	/**
	 * Sets the value associated with the given symbol in the environment.
	 * 
	 * @return true the first time the value was defined.
	 */
	public SValue define(final SSymbol symbol, final SValue value) {
		Objects.requireNonNull(symbol);
		Objects.requireNonNull(value);

		return env.put(symbol, value);
	}
	
	/**
	 * Replaces an existing definition of a symbol in an environment.
	 * 
	 * <p>If multiple definitions exist for a symbol, the one in the 
	 * innermost scope is replaced.
	 * 
	 * @return the previous value associated with the symbol.
	 * @throws EvaluationException - if the symbol was not previously defined.
	 */
	public SValue replace(final SSymbol symbol, final SValue value) throws EvaluationException {
		Objects.requireNonNull(symbol);
		Objects.requireNonNull(value);

		if(env.containsKey(symbol)) {
			return env.replace(symbol, value);
		}
		
		if(parent.isEmpty()) {
			throw new EvaluationException(String.format("symbol %s not defined", symbol));
		}
		
		return parent.get().replace(symbol, value);
	}

	/**
	 * Returns the value of a symbol in the environment traversing from innermost to
	 * outermost scope.
	 * 
	 * @throws EvaluationException - if the symbol wasn't defined.
	 */
	public SValue lookupSymbol(final SSymbol symbol) throws EvaluationException {
		Objects.requireNonNull(symbol);

		var existing = env.get(symbol);
		if (existing != null) {
			return existing;
		}

		if (parent.isPresent()) {
			return parent.get().lookupSymbol(symbol);
		}

		throw new EvaluationException(String.format("symbol %s not defined", symbol));
	}
	
	/**
	 * Returns whether the symbol was defined in the current or any prevous scope.
	 */
	public boolean isDefined(final SSymbol symbol) {
		if (env.containsKey(symbol)) {
			return true;
		}

		if (parent.isEmpty()) {
			return false;
		}

		return parent.get().isDefined(symbol);
	}

	/**
	 * Returns the set of all defined symbols.
	 */
	public Set<SSymbol> keys() {
		var declaredVars = new HashSet<>(env.keySet());
		if (parent.isPresent()) {
			declaredVars.addAll(parent.get().keys());
		}

		return declaredVars;
	}

	/**
	 * Returns the count of all defined symbols.
	 */
	public int size() {
		return keys().size();
	}

	@Override
	public String toString() {
		return "Environment [env=" + env + "]";
	}
}
