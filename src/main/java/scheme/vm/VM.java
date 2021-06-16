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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

import main.java.scheme.Environment;
import main.java.scheme.EvaluationException;
import main.java.scheme.Promise;
import main.java.scheme.types.SBoolean;
import main.java.scheme.types.SClosure;
import main.java.scheme.types.SNull;
import main.java.scheme.types.SPair;
import main.java.scheme.types.SProcedure;
import main.java.scheme.types.SSymbol;
import main.java.scheme.types.SValue;

/**
 * Implements a stack based VM for Scheme that supports evaluating keywords.
 * 
 * <p>
 * The VM splits up the operations that make up each keyword into a sequence of
 * opcodes that can be executed atomically between which user code can be
 * evaluated. This allows suspending, resuming, and capturing the state of the
 * VM at any point allowing the {@code call/cc} to work correctly. High-level
 * Scheme implementations typically use exceptions and the host language's stack
 * meaning continuations are only valid until the {@code call/cc} function exits
 * which prohibits certain use-cases.
 * 
 * <p>
 * The VM also correctly handles tail-call recursion.
 */
public class VM {

	private VM() {
		// Private utility class constructor.
	}

	private static class StackFrame {
		// op is the operation to run
		final Opcode op;
		// Arguments of the operation.
		final SValue[] args;
		// The environment to operate in.
		final Environment env;

		public StackFrame(Environment env, Opcode op, SValue... args) {
			this.op = op;
			this.args = args;
			this.env = env;
		}
	}

	public static SValue eval(final Environment env, final SValue val) {
		Objects.requireNonNull(val, "node must not be null");

		return vmInit(env, Opcode.OP_EVAL, val);
	}

	public static SValue evalClosure(final Environment env, final SClosure val, final SValue... callArgs) {
		Objects.requireNonNull(val, "closure must not be null");

		return vmInit(env, Opcode.OP_EVAL_CLOSURE, val, SPair.properListOf(callArgs));
	}


	private static SValue vmInit(final Environment env, Opcode op, final SValue... args) {
		Objects.requireNonNull(env, "env must not be null");
		Objects.requireNonNull(op, "opcode must not be null");
		Objects.requireNonNull(args, "args must not be null");

		// Initialize new stack
		var stack = new ArrayDeque<StackFrame>();

		// each future call gets shoved onto the stack

		stack.push(new StackFrame(env, Opcode.OP_RETURN));
		stack.push(new StackFrame(env, op, args));

		return vmEval(stack);
	}

	private static boolean enableTracing = false;

	/**
	 * vmEval executes the values of the stack until returning. This style of
	 * evaluation allows tail call recursion to be implemented correctly as well as
	 * call/cc; a special form that allows turning the current state of the VM into
	 * a closure that can later be called repeatedly.
	 * 
	 * Calls to this function should prime the stack by first pushing an OP_RETURN
	 * frame then an additional frame with the function they wish to call.
	 * 
	 * Calling an opcode without a symbol name is an error and will lead to either
	 * crashes or undefined results.
	 * 
	 * Operations within the VM are broken into atomic operations such that no more
	 * than one OP_EVAL will be triggered per operation, ensuring that if OP_EVAL
	 * triggers a call/cc a valid continuation can be generated.
	 */
	private static SValue vmEval(ArrayDeque<StackFrame> stack) {

		Optional<SValue> result = Optional.empty();

		// the stack should ALWAYS be > 0, otherwise there's
		// a break in the interpreter.
		while (!stack.isEmpty()) {

			var sf = stack.pop();

			if (enableTracing) {
				StringBuilder sb = new StringBuilder();
				sb.append("TRACE: ");
				sb.append(" ".repeat(stack.size()));
				sb.append(sf.op);
				sb.append(" args: ");
				sb.append(Arrays.deepToString(sf.args));
				sb.append(" ret: ");
				sb.append(result);
				System.err.println(sb.toString());
			}

			switch (sf.op) {
			/**
			 * return is a special opcode which exits the evaluation loop returning the
			 * value in the current stack frame's return register.
			 */
			case OP_RETURN: {
				// OP_RETURN returns the result
				return result.orElse(SNull.NULL);
			}

			/**
			 * (quote arg) returns the argument unevaluated, effectively turning code into
			 * data.
			 */
			case OP_QUOTE: {
				// TODO set flag to disable manipulation
				result = Optional.of(first(sf.args));
				break;
			}


			/**
			 * (if test consequent alternate?) evaluates test, if truthy returns the
			 * evaluated consequent, else returns the evaluated alternate.
			 * 
			 * This implementation of main.java.scheme returns the null if no alternate is provided,
			 * although the behavior is unspecified in R5RS.
			 */

			case OP_IF_INIT: {

				var test = first(sf.args);
				var rest = rest(sf.args);

				pushRet(stack, sf.env, Opcode.OP_IF_TERM, rest);
				jmp(stack, sf.env, Opcode.OP_EVAL, test);
				break;
			}

			case OP_IF_TERM: {
				var res = result.orElseThrow(() -> new IllegalStateException("no ret to and"));
				if (res.isTruthy()) {
					jmp(stack, sf.env, Opcode.OP_EVAL, first(sf.args));
					break;
				}

				var tail = rest(sf.args);
				if (tail.length == 0) { // no alternate
					result = Optional.of(SNull.NULL);
					break;
				}

				// evaluate alternate
				jmp(stack, sf.env, Opcode.OP_EVAL, first(tail));
				break;
			}


			/**
			 * (set! var expr) evaluates expr and assigns the value to the location which
			 * var is already bound.
			 * 
			 * This implementation of main.java.scheme returns the previously bound value, although
			 * the behavior is unspecified in R5RS.
			 * 
			 * OP_SET is the entrypoint. OP_SET_TERM is the termination state, it receives
			 * the result of the evaluation in result and the variable as an arg.
			 */
			case OP_SET: {
				var key = sf.args[0];
				var expr = sf.args[1];

				pushRet(stack, sf.env, Opcode.OP_SET_TERM, key);
				jmp(stack, sf.env, Opcode.OP_EVAL, expr);
				break;
			}

			case OP_SET_TERM: {
				var key = first(sf.args);
				var res = result.orElseThrow(() -> new IllegalStateException("no ret to and"));

				// return value is the replaced value
				result = Optional.of(sf.env.replace(key.getSymbol(), res));
				break;
			}

			case OP_EVAL:
				var node = first(sf.args);
				// Resolve symbols
				if (node.isSymbol()) {
					result = Optional.of(sf.env.lookupSymbol(node.getSymbol()));
					break;
				}
				// Execute pairs, except null pairs.
				if (node.isPair()) {
					var pair = node.getPair();
					var firstArg = pair.getCar();

					SValue[] eargs;
					var cdr = pair.getCdr();
					if (cdr.isPair() || cdr.isNull()) {
						eargs = cdr.getPair().toArray();
					} else {
						throw new EvaluationException("can't evaluate pairs, only lists");
					}

					// Evaluate special forms
					if (firstArg.isSymbol()) {
						var op = Opcode.lookup(firstArg.getSymbol());

						if (op.isPresent()) {
							jmp(stack, sf.env, op.get(), eargs);
							break;
						}
					}

					jmp(stack, sf.env, Opcode.OP_CALL_INIT, pair);
					break;
				}

				// All other types get returned
				result = Optional.of(node);
				break;

			case OP_BEGIN: {
				if (sf.args.length == 0) {
					result = Optional.of(SNull.VOID);
					break;
				}

				if (sf.args.length > 1) {
					pushRet(stack, sf.env, Opcode.OP_BEGIN, rest(sf.args));
				}

				jmp(stack, sf.env, Opcode.OP_EVAL, first(sf.args));
				break;
			}

			///////////////////////////////////////////////////////////////////
			// and
			///////////////////////////////////////////////////////////////////
			case OP_AND_TEST: {
				var res = result.orElseThrow(() -> new IllegalStateException("no ret to and"));
				if (!res.isTruthy()) {
					result = Optional.of(res);
					break;
				}
				// fall through to continue computing AND for the tail
			}
			case OP_AND: {
				// break early if possible
				if (sf.args.length == 0) {
					result = Optional.of(SBoolean.TRUE);
					break;
				}

				if (sf.args.length > 1) {
					pushRet(stack, sf.env, Opcode.OP_AND_TEST, rest(sf.args));
				}

				jmp(stack, sf.env, Opcode.OP_EVAL, first(sf.args));
				break;
			}

			/**
			 * or evaluates expressions from first to last, and the value of the first
			 * expression that evaluates to a true value is returned. Any remaining
			 * expressions are not evaluated. If all expressions evaluate to false values,
			 * the value of the last expression is returned. If there are no expressions
			 * then #f is returned.
			 * 
			 * OP_OR is the entrypoint, it pops the first expression off the front of the
			 * argument list and evaluates it. Evaluation flow returns to OP_OR_TEST which
			 * checks the evaluation result, and either returns it or continues to the next
			 * argument.
			 */
			case OP_OR_TEST: {
				var res = result.orElseThrow(() -> new IllegalStateException("no ret to or"));
				if (res.isTruthy()) {
					result = Optional.of(res);
					break;
				}

				// fall through to continue computing or for the tail.
			}
			case OP_OR: {
				// base case for or is false.
				if (sf.args.length == 0) {
					result = Optional.of(SBoolean.FALSE);
					break;
				}

				if (sf.args.length > 1) {
					pushRet(stack, sf.env, Opcode.OP_OR_TEST, rest(sf.args));
				}

				jmp(stack, sf.env, Opcode.OP_EVAL, first(sf.args));
				break;
			}

			/**
			 * (lambda formals body) creates a procedure. Formals are the parameters to the
			 * procedure and can take one of the following forms:
			 * 
			 * 1) A proper list of symbols, each symbol represents a single param. 2) An
			 * improper list of symbols, each symbol except the last represents a single
			 * param and the last is a variadic receiver. 3) A single symbol, representing a
			 * variadic receiver.
			 * 
			 * The lambda is a closure on on the environment is was executed in. The body of
			 * a lambda contains one or more expressions, the results of the last expression
			 * are returned. The body of the lambda supports tail-call recursion.
			 */
			case OP_LAMBDA: {
				var formals = first(sf.args);
				var body = rest(sf.args);

				// TODO: support the third syntax with an improper list:
				// https://www.cs.cmu.edu/Groups/AI/html/r4rs/r4rs_6.html#SEC30
				SValue[] paramList = null;
				boolean isVaradic = false;
				if (formals.isSymbol()) {
					paramList = new SValue[] { formals };
					isVaradic = true;
				} else if (formals.isNull()) {
					paramList = new SValue[0];
				} else if (formals.isPair()) {
					var pair = formals.getPair();

					if (pair.isList()) {
						paramList = pair.toArray();
					} else {
						throw new EvaluationException("improper lists not yet supported");
					}
				} else {
					throw new EvaluationException(
							String.format("%s not allowed as first argument to lambda", formals.toString()));
				}

				var paramNames = new SSymbol[paramList.length];
				for (int i = 0; i < paramList.length; i++) {
					paramNames[i] = paramList[i].getSymbol();
				}

				var closed = new SClosure(sf.env, paramNames, isVaradic, body);
				result = Optional.of(closed);
				break;
			}

			case OP_TRACE:
				enableTracing = !enableTracing;
				result = Optional.of(SBoolean.valueOf(enableTracing));
				break;

			case OP_DEFINE_INIT: {
				var vars = first(sf.args);
				var expr = rest(sf.args);

				if (vars.isSymbol()) {
					pushRet(stack, sf.env, Opcode.OP_DEFINE_TERM, vars);
					jmp(stack, sf.env, Opcode.OP_EVAL, expr);
					break;
				}

				var pair = vars.getPair();
				var sym = pair.getCar();
				var formals = pair.getCdr();

				pushRet(stack, sf.env, Opcode.OP_DEFINE_TERM, sym);
				var al = new LinkedList<SValue>();
				al.add(formals);
				al.addAll(Arrays.asList(expr));
				jmp(stack, sf.env, Opcode.OP_LAMBDA, al.toArray(SValue[]::new));
				break;
			}

			case OP_DEFINE_TERM: {
				var symbol = first(sf.args).getSymbol();
				sf.env.define(symbol, result.get());
				result = Optional.of(symbol);
				break;
			}

			// similar to OP_DEFINE_TERM, but validates the term wasn't
			// previously defined.
			case OP_MUST_DEFINE_TERM: {
				var symbol = first(sf.args).getSymbol();
				if (null != sf.env.define(symbol, result.get())) {
					throw EvaluationException.format("can't define %s more than once", symbol);
				}
				result = Optional.of(symbol);
				break;
			}

			case OP_DELAY: {
				var body = first(sf.args);
				result = Optional.of(new Promise(sf.env, body));
				break;
			}

			case OP_COND_TEST: {
				var cond = first(sf.args);
				var testRes = result.get();

				if (testRes.isTruthy()) {
					var condBody = cond.getPair().toArray();
					if (condBody.length == 1) {
						result = Optional.of(testRes);
						break;
					}

					if (condBody[1].equals(SSymbol.of("=>"))) {
						// call the lambda with the result as the arg
						var invocation = SPair.properListOf(condBody[2], result.get());

						jmp(stack, sf.env, Opcode.OP_EVAL, invocation);
						break;
					}

					jmp(stack, sf.env, Opcode.OP_BEGIN, rest(condBody));
					break;
				}

				// if no match, continue
				var args = rest(sf.args);
				if (args.length == 0) {
					result = Optional.of(SNull.NULL);
					break;
				}

				jmp(stack, sf.env, Opcode.OP_COND_INIT, args);
				break;
			}

			case OP_COND_INIT: {
				var cond = first(sf.args);
				if (!cond.isPair()) {
					throw new EvaluationException("malformed cond");
				}

				var condArr = cond.getPair().toArray();
				if (condArr.length == 0) {
					throw new EvaluationException("cond missing test");
				}

				var testExpr = first(condArr);

				if (SSymbol.of("else").equals(testExpr)) {
					if (sf.args.length > 1) {
						throw new EvaluationException("else must be final test of cond");
					}

					var condBody = rest(condArr);
					if (condBody.length == 0) {
						throw new EvaluationException("else missing expressions");
					}

					jmp(stack, sf.env, Opcode.OP_BEGIN, condBody);
					break;
				}


				pushRet(stack, sf.env, Opcode.OP_COND_TEST, sf.args);
				jmp(stack, sf.env, Opcode.OP_EVAL, testExpr);
				break;
			}

			case OP_LET: {
				// (let bindings body) => 2 arg min
				// (let variable bindings body) => 3 arg min
				var bodyEnv = new Environment(Optional.of(sf.env));

				if (sf.args[0].isPair()) {
					var defns = first(sf.args).getPair();
					var body = rest(sf.args);
					pushRet(stack, bodyEnv, Opcode.OP_BEGIN, body);
					resolveLet(stack, sf.env, bodyEnv, false, false, defns);
					break;
				}

				if (sf.args[0].isSymbol()) {
					var varSym = first(sf.args).getSymbol();
					var defns = sf.args[1].getPair();
					var body = Arrays.copyOfRange(sf.args, 2, sf.args.length);

					pushRet(stack, bodyEnv, Opcode.OP_BEGIN, body);
					var letVars = resolveLet(stack, sf.env, bodyEnv, false, false, defns);
					var closure = new SClosure(bodyEnv, letVars, false, body);
					bodyEnv.define(varSym, closure);
					break;
				}

				throw new EvaluationException("second arg must be a symbol or list");
			}

			case OP_LET_SEQ: {
				var bodyEnv = new Environment(Optional.of(sf.env));
				var defns = first(sf.args).getPair();
				var body = rest(sf.args);

				pushRet(stack, bodyEnv, Opcode.OP_BEGIN, body);
				resolveLet(stack, bodyEnv, bodyEnv, true, false, defns);
				break;
			}

			case OP_LETREC: {
				var bodyEnv = new Environment(Optional.of(sf.env));
				var defns = first(sf.args).getPair();
				var body = rest(sf.args);

				pushRet(stack, bodyEnv, Opcode.OP_BEGIN, body);
				resolveLet(stack, bodyEnv, bodyEnv, true, true, defns);
				break;
			}


			case OP_DO: {
				var initExprList = sf.args[0].getPair();
				var untilCond = sf.args[1].getPair();
				var commandList = Arrays.copyOfRange(sf.args, 2, sf.args.length);

				var testExpr = untilCond.getCar();
				var resExprs = untilCond.getCdr();

				var bindingExprs = initExprList.toArray();
				var updateExprs = new SValue[bindingExprs.length];
				var initExprs = new SValue[bindingExprs.length];
				var names = new SSymbol[bindingExprs.length];

				for (int i = 0; i < initExprs.length; i++) {
					var binding = bindingExprs[i].getPair().toArray();
					names[i] = binding[0].getSymbol();
					initExprs[i] = binding[1];
					updateExprs[i] = (binding.length == 3) ? binding[2] : binding[0];
				}

				var loopsym = SSymbol.unique("do-");

				var lambda = list(SSymbol.of("lambda"), SPair.properListOf(names), list(SSymbol.of("if"), testExpr,
						// If true, run the result expressions
						SPair.cons(SSymbol.of("begin"), resExprs),
						// Otherwise, run the body and update the vars
						list(SSymbol.of("begin"), list(SSymbol.of("begin"), commandList),
								// Call the loopback
								list(loopsym, updateExprs))));


				// define loopsym to be the loop contents
				var defns = list(list(loopsym, lambda));

				// call the loopsym as the result
				var body = list(loopsym, initExprs);

				// call the procedure
				jmp(stack, sf.env, Opcode.OP_LETREC, defns, body);
				break;
			}

			case OP_CALL_INIT: {
				// makes the call based on the return value
				pushRet(stack, sf.env, Opcode.OP_CALL_TERM);

				// push the evaluation
				var toEval = sf.args[0].getPair();

				var first = toEval.getCar();
				var rest = toEval.getCdr();

				pushRet(stack, sf.env, Opcode.OP_CALL_LOOP, rest, SNull.NULL);
				jmp(stack, sf.env, Opcode.OP_EVAL, first);
				break;
			}

			case OP_CALL_LOOP: {
				var unevaluated = sf.args[0];
				var out = SPair.cons(result.get(), sf.args[1]);

				// if we've run out of unevaluated things, return
				if (unevaluated.isNull()) {
					// reverse list because it's evaluated backwards on the stack.
					result = Optional.of(out.reversed());
					break;
				}

				pushRet(stack, sf.env, Opcode.OP_CALL_LOOP, unevaluated.getPair().getCdr(), out);
				jmp(stack, sf.env, Opcode.OP_EVAL, unevaluated.getPair().getCar());
				break;
			}

			case OP_CALL_TERM: {
				var callList = result.get().getPair().toArray();

				var possibleProcedure = first(callList);
				if (!possibleProcedure.isProcedure()) {
					throw new EvaluationException(String.format("%s can't be evaluated", possibleProcedure.toString()));
				}

				var procedure = possibleProcedure.getProcedure();
				var operands = rest(callList);

				if (procedure instanceof ContinuationProcedure) {
					jmp(stack, sf.env, Opcode.OP_EVAL_CALL_CC, procedure, SPair.properListOf(operands));
					break;
				}

				if (procedure.isClosure()) {
					jmp(stack, sf.env, Opcode.OP_EVAL_CLOSURE, procedure, SPair.properListOf(operands));
					break;
				}


				result = Optional.of(procedure.eval(sf.env, operands));
				break;
			}

			case OP_EVAL_CLOSURE: {
				// closure, operands

				var closure = sf.args[0].getClosure();
				var callArgs = sf.args[1].getPair().toArray();


				// the procedure references are from the original scope
				var procScope = new Environment(Optional.of(closure.getDefnScope()));

				// add in the values of each argument
				var requiredArgCount = closure.getParamNames().length;
				if (closure.isVaradic()) {
					requiredArgCount--;
				}

				var gotArgCount = callArgs.length;
				if (gotArgCount < requiredArgCount) {
					throw EvaluationException.format("expected at least %d args, got %d", requiredArgCount,
							gotArgCount);
				}
				if (gotArgCount > requiredArgCount && !closure.isVaradic()) {
					throw EvaluationException.format("expected at most %d args, got %d", requiredArgCount, gotArgCount);
				}

				for (int i = 0; i < requiredArgCount; i++) {
					procScope.define(closure.getParamNames()[i], callArgs[i]);
				}

				if (closure.isVaradic()) {
					var varArgs = Arrays.copyOfRange(callArgs, requiredArgCount, callArgs.length);
					procScope.define(closure.getParamNames()[requiredArgCount], SPair.properListOf(varArgs));
				}

				jmp(stack, procScope, Opcode.OP_BEGIN, closure.getBody());
				break;
			}

			case OP_EVAL_CALL_CC: {
				var proc = (ContinuationProcedure) sf.args[0];
				var callArgs = sf.args[1].getPair().toArray();

				stack.clear();
				stack.addAll(proc.continuation);
				result = Optional.of(callArgs[0]);
				break;
			}

			case OP_CALL_CC_ALIAS: // fall-through
			case OP_CALL_CC: {
				// save the stack
				var continuation = new ContinuationProcedure(new ArrayDeque<>(stack));
				jmp(stack, sf.env, Opcode.OP_EVAL, list(sf.args[0], continuation));
				break;
			}

			/**
			 */

			case OP_QQ_INIT: {
				var tmp = expandQQ(sf.args[0], 0);
				System.err.println(tmp.toScheme());
				jmp(stack, sf.env, Opcode.OP_EVAL, tmp);
				break;
			}


			default:
				throw new RuntimeException(String.format("unknown operation: %s", sf.op.toString()));
			}
		}

		throw new RuntimeException("stack underflow");
	}

	public static void jmp(Deque<StackFrame> stack, Environment env, Opcode op, SValue... args) {
		stack.push(new StackFrame(env, op, args));
	}

	public static void pushRet(Deque<StackFrame> stack, Environment env, Opcode op, SValue... args) {
		stack.push(new StackFrame(env, op, args));
	}

	public static SValue first(SValue... args) {
		return args[0];
	}

	public static SValue[] rest(SValue... args) {
		return Arrays.copyOfRange(args, 1, args.length);
	}

	private static SSymbol[] resolveLet(
			ArrayDeque<StackFrame> stack,
			Environment resolveIn,
			Environment defineIn,
			boolean allowDuplicates,
			boolean preDeclare,
			SPair defns) {
		var entries = new LinkedList<SPair>();
		var definedSet = new LinkedList<SSymbol>();

		for (var defnPair : defns.toArray()) {
			var defn = defnPair.getPair().toArray();

			requireNArgs(2, defn);
			var symbol = defn[0].getSymbol();
			var value = defn[1];
			definedSet.add(symbol);
			entries.add(SPair.cons(symbol, value));
		}

		// reverse the order of the list because we're going to be pushing
		// their values on to the stack to be evaluated.
		Collections.reverse(entries);

		// Push the let definitions onto the stack
		for (var entry : entries) {
			var key = entry.getCar().getSymbol();

			if (allowDuplicates) {
				pushRet(stack, defineIn, Opcode.OP_DEFINE_TERM, key);
			} else {
				pushRet(stack, defineIn, Opcode.OP_MUST_DEFINE_TERM, key);
			}

			pushRet(stack, resolveIn, Opcode.OP_EVAL, entry.getCdr());
		}


		// If the let wants variables pre-declared to empty states
		// in the environment, evaluate each.
		if (preDeclare) {
			for (var entry : entries) {
				var key = entry.getCar().getSymbol();
				pushRet(stack, defineIn, Opcode.OP_DEFINE_TERM, key);
				pushRet(stack, defineIn, Opcode.OP_EVAL, SNull.NULL);
			}
		}

		return definedSet.toArray(SSymbol[]::new);
	}


	private static SValue list(SValue head, SValue... args) {
		return SPair.cons(head, SPair.properListOfNullable(args));
	}

	private static class ContinuationProcedure extends SProcedure {

		private final ArrayDeque<StackFrame> continuation;

		public ContinuationProcedure(final ArrayDeque<StackFrame> continuation) {
			this.continuation = continuation;
		}

		@Override
		public SValue eval(Environment env, SValue[] operands) throws EvaluationException {
			throw new EvaluationException("can't be invoked directly");
		}

		public String toScheme() {
			return "#[continuation]";
		}
	}

	/**
	 * expandQQ expands the quasiquote syntax.
	 * 
	 * Quasiquote expressions are useful for constructing a list or vector structure
	 * when most but not all of the desired structure is known in advance. If no
	 * commas appear within the (qq template), the result of evaluating `(qq
	 * template) is equivalent to the result of evaluating '(qq template). If a
	 * comma appears within the (qq template), however, the expression following the
	 * comma is evaluated ("unquoted") and its result is inserted into the structure
	 * instead of the comma and the expression. If a comma appears followed
	 * immediately by an "@," then the following expression must evaluate to a list;
	 * the opening and closing parentheses of the list are then "stripped away" and
	 * the elements of the list are inserted in place of the comma at-sign
	 * expression sequence. A comma at-sign should only appear within a list or
	 * vector (qq template).
	 * 
	 * @param  template the thing to quote
	 * @param  depth    the current depth
	 * @return          An expanded form ready to be evaluated
	 */
	private static SValue expandQQ(final SValue template, final int depth) {		
		// no quoting needed
		
		if (!template.isPair() && !template.isVector()) {
			return quote(template);
		}
		
		if(template.isVector()) {
			var expanded = expandQQ(SPair.properListOf(template.getVector().toArray()), depth);
			return SPair.cons(SSymbol.of("vector"), expanded);
		}
		

		var pair = template.getPair();
		var car = pair.getCar();

		// if the pair is another call to quasiquote, recurse.
		if (car.equals(SSymbol.QUASIQUOTE)) {
			return fixupCons(template, quote(car), expandQQ(pair.getCdr(), depth + 1));
		}

		// if we're at the root level
		if (depth == 0) {
			// unquote returns the value directly to be evaluated
			if (car.equals(SSymbol.UNQUOTE)) {
				
				return pair.getCdr().getPair().getCar();
			}
			
			if(car.equals(SSymbol.UNQUOTE_SPLICING)) {
				throw new EvaluationException("can't splice into a non-list");
			}
			
			// Check if we need to splice in
			{
				if(car.isPair() && car.getPair().getCar().equals(SSymbol.of("unquote-splicing"))){
					var usArg = car.getPair().toArray()[1];
					return mergeQQ(template, usArg, expandQQ(pair.getCdr(), depth));
				}
				
			}

			
			// TODO if the CAR is a pair and the cons is a pair
			
			return fixupCons(template, expandQQ(car, depth), expandQQ(pair.getCdr(), depth));
		} else {
			if (car.equals(SSymbol.UNQUOTE)) {
				return fixupCons(template, quote(car), expandQQ(pair.getCdr(), depth - 1));
			}
			
			if (car.equals(SSymbol.UNQUOTE_SPLICING)) {
				return fixupCons(template, quote(car), expandQQ(pair.getCdr(), depth - 1));
			}
			
			return fixupCons(template, expandQQ(car, depth), expandQQ(pair.getCdr(), depth));
		}
	}
	
	private static boolean isQuotedPair(SValue possible) {
		return possible.isPair() && possible.getPair().getCar().equals(SSymbol.of("quote"));
	}
	
	private static SValue fixupCons(SValue template, SValue left, SValue right) {
		if(isQuotedPair(left) && isQuotedPair(right) && template.isPair()) {
			var tp = template.getPair();
			var lp = left.getPair().getCdr().getPair();
			var rp = right.getPair().getCdr().getPair();
			
			if (lp.getCar().equals(tp.getCar()) && 
					rp.getCar().equals(tp.getCdr())) {
				return quote(template);
			}	
		}
		
		// TODO add support for vectors here
		
		return list(SSymbol.of("cons"), left, right);
	}
	
	private static SValue mergeQQ(SValue template, SValue left, SValue right) {
		var templateIsEnd = template.isPair() && template.getPair().getCdr().equals(SNull.NULL);
		var rightIsEnd = isQuotedPair(right) && right.getPair().getCdr().getPair().getCar().equals(SNull.NULL);
		
		
		if( templateIsEnd || rightIsEnd) {
			return left;
		}
		
		return list(SSymbol.of("append"), left, right);
	}
	
	private static SValue quote(SValue quoted) {
		if(quoted.isBoolean() || quoted.isCharacter() || quoted.isString() || quoted.isNumber() || quoted.isProcedure()) {
			return quoted;
		}

		return list(SSymbol.QUOTE, quoted);
	}

	public static void requireNArgs(final int n, final Object[] args) throws EvaluationException {
		if (args.length != n) {
			throw new EvaluationException(String.format("expected %d arg(s), got %d", n, args.length));
		}
	}

}
