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

package main.java.scheme.vm;

import java.util.Objects;
import java.util.Optional;

import main.java.scheme.EvaluationException;
import main.java.scheme.types.SSymbol;

public enum Opcode {
	OP_RETURN("", 0, 0, false),
	OP_EVAL("", 1, 1, false),
	OP_BEGIN("begin", 0, Integer.MAX_VALUE, false),
	OP_QUOTE("quote", 1, 1, false),
	OP_AND("and", 0, Integer.MAX_VALUE, false),
	OP_AND_TEST("", 0, Integer.MAX_VALUE, true),
	OP_OR("or", 0, Integer.MAX_VALUE, false),
	OP_OR_TEST("", 0, Integer.MAX_VALUE, true),
	OP_SET("set!", 2, 2, false),
	OP_SET_TERM("", 1, 1, true),
	OP_IF_INIT("if", 2, 3, false),
	OP_IF_TERM("", 2, 3, true),
	OP_LAMBDA("lambda", 2, Integer.MAX_VALUE, false),
	OP_TRACE("trace!", 0, 0, false),
	OP_DEFINE_INIT("define", 2, Integer.MAX_VALUE, false),
	OP_DEFINE_TERM("", 1, 1, true),
	OP_MUST_DEFINE_TERM("", 1, 1, true),
	OP_DELAY("delay", 1, 1, false),
	OP_COND_INIT("cond", 1, Integer.MAX_VALUE, false),
	OP_COND_TEST("", 1, Integer.MAX_VALUE, true),
	OP_LET("let", 2, Integer.MAX_VALUE, false),
	OP_LET_SEQ("let*", 2, Integer.MAX_VALUE, false),
	OP_LETREC("letrec", 2, Integer.MAX_VALUE, false),
	OP_DO("do", 2, Integer.MAX_VALUE, false),
	OP_CALL_INIT("", 1, 1, false),
	OP_CALL_LOOP("", 2, 2, false),
	OP_CALL_TERM("", 0, 0, true),
	OP_EVAL_CLOSURE("", 2, 2, false),
	OP_CALL_CC_ALIAS("call/cc", 1, 1, false),
	OP_CALL_CC("call-with-current-continuation", 1, 1, false),
	OP_EVAL_CALL_CC("", 2, 2, false),
	OP_QQ_INIT("quasiquote", 1, 1, false),
	OP_QQ_LOOP("", 2, 2, false);

	private SSymbol opVal;
	private int minArgs;
	private int maxArgs;
	private boolean needsRet;

	private Opcode(final String symName, int minArgs, int maxArgs, boolean needsRet) {
		if (!symName.isEmpty()) {
			opVal = SSymbol.of(symName);
		}
		this.minArgs = minArgs;
		this.maxArgs = maxArgs;
		this.needsRet = needsRet;
	}

	public static Optional<Opcode> lookup(final SSymbol sym) {
		Objects.requireNonNull(sym);

		for (var op : Opcode.values()) {
			if (op.opVal != null && op.opVal.equals(sym)) {
				return Optional.of(op);
			}
		}

		return Optional.empty();
	}

	public void assertArgs(int count, boolean retDefined) {
		var name = this.toString();
		if (opVal != null) {
			name = opVal.toString();
		}

		if (count < minArgs) {
			throw EvaluationException.format("%s requires at least %d args, got %d", name, minArgs, count);
		}

		if (count > maxArgs) {
			throw EvaluationException.format("%s requires at most %d args, got %d", name, maxArgs, count);
		}

		if (needsRet && !retDefined) {
			throw EvaluationException.format("bad state: %s needs ret? %b got? %b", name, needsRet, retDefined);
		}
	}
}