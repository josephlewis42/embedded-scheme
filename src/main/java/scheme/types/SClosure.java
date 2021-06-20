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
import java.util.Objects;
import java.util.stream.Collectors;

import scheme.Environment;
import scheme.EvaluationException;
import scheme.vm.VM;

public class SClosure extends SProcedure {
		
	public final Environment defnScope;
	public final SSymbol[] paramNames;
	public final SValue[] body;
	public final boolean varadic;

	public SClosure(final Environment scope, final SSymbol[] paramNames, final boolean varadic, final SValue[] body) {
		Objects.requireNonNull(scope);
		Objects.requireNonNull(paramNames);
		Objects.requireNonNull(body);
		
		if(body.length == 0) {
			throw new EvaluationException("body can't be blank");
		}
		
		this.defnScope = scope;
		this.varadic = varadic;
		this.paramNames = paramNames;
		this.body = body;
		
		markImmutable();
	}

	@Override
	public SValue eval(Environment callEnv, SValue[] callArgs) throws EvaluationException {
		return VM.evalClosure(defnScope, this, callArgs);
	}

	public Environment getDefnScope() {
		return defnScope;
	}

	public SSymbol[] getParamNames() {
		return paramNames;
	}

	public SValue[] getBody() {
		return body;
	}

	public boolean isVaradic() {
		return varadic;
	}
	
	public SClosure getClosure() {
		return this;
	}
	
	public boolean isClosure() {
		return true;
	}

	@Override
	public String toScheme() {
		var tmp = Arrays.stream(paramNames)
				.map(SSymbol::toString)
				.collect(Collectors.joining(" "));
		
		return String.format("#[closure (lambda (%s) ...)]", tmp);
	}
}
