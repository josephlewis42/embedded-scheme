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

import java.util.ArrayList;
import java.util.Arrays;

import scheme.bind.ExampleCall;
import scheme.bind.Init;
import scheme.bind.Procedure;
import scheme.types.SCharacter;
import scheme.types.SNull;
import scheme.types.SNumber;
import scheme.types.SPair;
import scheme.types.SProcedure;
import scheme.types.SString;
import scheme.types.SSymbol;
import scheme.types.SValue;
import scheme.types.SVector;
import scheme.vm.VM;

/**
 * Builtins provides native implementations of many required functions.
 */
public class Builtins {

	///////////////////////////////////////////////////////
	// Equivalence predicates
	///////////////////////////////////////////////////////

	@Procedure(name = "eq?")
	@ExampleCall(in = "(eq? 'a 'b)", out = "#f")
	@ExampleCall(in = "(eq? 'a 'a)", deterministic = false)
	@ExampleCall(in = "(eq? (list 'a) (list 'a))", out = "#f")
	@ExampleCall(in = "(eq? \"a\" \"a\")", deterministic = false)
	@ExampleCall(in = "(eq? \"\" \"\")", deterministic = false)
	@ExampleCall(in = "(eq? '() '())", out = "#t")
	@ExampleCall(in = "(eq? 2 2)", deterministic = false)
	@ExampleCall(in = "(eq? car car)", out = "#t")
	@ExampleCall(in = "(let ((n (+ 2 3)))\n\t(eq? n n))", out = "#t")
	public static boolean eq(SValue lhs, SValue rhs) throws EvaluationException {
		return lhs == rhs;
	}

	@Procedure(name = "eqv?")
	@ExampleCall(in = "(eqv? (string->symbol \"TRUE\") (string->symbol \"true\"))", out = "#f")
	public static boolean eqv(SValue lhs, SValue rhs) {
		// From R5RS: The eqv? procedure defines a useful equivalence relation
		// on objects.

		// obj1 and obj2 are both #t or both #f.
		if (lhs.isBoolean() && rhs.isBoolean()) {
			return lhs.getBoolean().equals(rhs);
		}

		// obj1 and obj2 are both symbols and
		// (string=? (symbol->string obj1)
		// (symbol->string obj2)) => #t
		if (lhs.isSymbol() && rhs.isSymbol()) {
			return stringEq(symbolToString(lhs.getSymbol()), symbolToString(rhs.getSymbol()));
		}

		// obj1 and obj2 are both characters and are the same
		// character according to the char=? procedure.
		if (lhs.isCharacter() && rhs.isCharacter()) {
			return charEQ(lhs.getCharacter().toChar(), rhs.getCharacter().toChar());
		}

		// obj1 and obj2 are both numbers, are numerically equal
		// and are both exact or both inexact.
		if (lhs.isNumber() && rhs.isNumber()) {
			return isExact(lhs.getNumber()) == isExact(rhs.getNumber())
					&& lhs.getNumber().compareTo(rhs.getNumber()) == 0;
		}

		// both obj1 and obj2 are the empty list.
		if (lhs.isNull() && rhs.isNull()) {
			return true;
		}

		// obj1 and obj2 are pairs, vectors, or strings that denote
		// the same locations in the store.
		// obj1 and obj2 are procedures whose location tags are equal.
		return lhs == rhs;
	}

	/**
	 * Equal? recursively compares the contents of pairs, vectors, and strings,
	 * applying eqv? on other objects such as numbers and symbols. A rule of thumb
	 * is that objects are generally equal? if they print the same. Equal? may fail
	 * to terminate if its arguments are circular data structures.
	 */
	@Procedure(name = "equal?")
	@ExampleCall(in="(equal? 'a 'a)", out="#t")
	@ExampleCall(in="(equal? '(a) '(a))", out="#t")
	@ExampleCall(in="(equal? '(a (b) c)\n" +
			"\t'(a (b) c))", out="#t")
	@ExampleCall(in="(equal? \"abc\" \"abc\")", out="#t")
	@ExampleCall(in="(equal? 2 2)", out="#t")
	@ExampleCall(in="(equal? (make-vector 5 'a)\n" +
			"\t(make-vector 5 'a))", out="#t")
	@ExampleCall(in="(equal? (lambda (x) x)\n" +
			"\t(lambda (y) y))", out="#f")
	public static boolean isEqual(SValue lhs, SValue rhs) {
		// short circuit if possible.
		if (lhs == rhs) {
			return true;
		}

		if (lhs.isPair() && rhs.isPair()) {
			var lp = lhs.getPair();
			var rp = rhs.getPair();

			return isEqual(lp.getCar(), rp.getCar()) && isEqual(lp.getCdr(), rp.getCdr());
		}

		if (lhs.isVector() && rhs.isVector()) {
			var lv = lhs.getVector();
			var rv = rhs.getVector();

			if (lv.length() != rv.length()) {
				return false;
			}

			for (int i = 0; i < lv.length(); i++) {
				if (!isEqual(lv.ref(i), rv.ref(i))) {
					return false;
				}
			}

			return true;
		}

		if (lhs.isString() && rhs.isString()) {
			return lhs.getString().compareTo(rhs.getString()) == 0;
		}

		return eqv(lhs, rhs);
	}

	///////////////////////////////////////////////////////
	// Extra procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "error")
	@ExampleCall(in="(error \"some message\")", error = true)
	public static void error(String message) throws EvaluationException {
		throw new EvaluationException(message);
	}

	@Procedure(name = "eval")
	@ExampleCall(in="(eval '(* 7 3))", out="21")
	@ExampleCall(in="(let ((f (eval '(lambda (f x) (f x x))))) (f + 10))", out="20")
	public static SValue eval(Environment env, SValue val) {
		return VM.eval(env, val);
	}


	///////////////////////////////////////////////////////
	// Numeric procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "number?")
	@ExampleCall(in="(number? 1)", out="#t")
	@ExampleCall(in="(number? #t)", out="#f")
	public static boolean isNumber(SValue val) {
		return val.isNumber();
	}

	@Procedure(name = "+")
	@ExampleCall(in="(+ 3 4)", out="7")
	@ExampleCall(in="(+ 3)", out="3")
	@ExampleCall(in="(+)", out="0")
	public static SNumber sum(SNumber... args) {
		var res = SNumber.ZERO;

		for (var arg : args) {
			res = res.add(arg);
		}

		return res;
	}

	@Procedure(name = "*")
	@ExampleCall(in="(* 3 4)", out="12")
	@ExampleCall(in="(* 3)", out="3")
	@ExampleCall(in="(*)", out="1")
	public static SNumber multiply(SNumber... args) {
		var res = SNumber.ONE;

		for (var arg : args) {
			res = res.multiply(arg);
		}

		return res;
	}

	@Procedure(name = "-")
	@ExampleCall(in="(- 3 4)", out="-1")
	@ExampleCall(in="(- 3 4 5)", out="-6")
	@ExampleCall(in="(- 3)", out="-3")
	public static SNumber subtract(SNumber first, SNumber... rest) throws EvaluationException {
		if (rest.length == 0) {
			return first.negate();
		}

		var res = first;

		for (var arg : rest) {
			res = res.subtract(arg);
		}

		return res;
	}

	@Procedure(name = "/")
	// TODO update these to mach r5rs once we support rationals.
	@ExampleCall(in="(/ 3 4 5)", out="(/ 3 20)")
	@ExampleCall(in="(/ 3)", out="(/ 1 3)")
	public static SNumber divide(SNumber first, SNumber... rest) throws EvaluationException {
		if (rest.length == 0) {
			return SNumber.ONE.divide(first);
		}

		var res = first;

		for (var arg : rest) {
			res = res.divide(arg);
		}

		return res;
	}

	@Procedure(name = ">")
	@ExampleCall(in="(> 1)", out="#t")
	@ExampleCall(in="(> 2 1)", out="#t")
	@ExampleCall(in="(> 1 2)", out="#f")
	@ExampleCall(in="(> 3 2 2)", out="#f")
	public static boolean gt(SNumber first, SNumber... rest) {
		if (rest.length == 0) {
			return true;
		}

		var prev = first;
		for (var curr : rest) {
			if (prev.compareTo(curr) > 0) {
				prev = curr;
			} else {
				return false;
			}
		}

		return true;
	}

	@Procedure(name = ">=")
	@ExampleCall(in="(>= 1)", out="#t")
	@ExampleCall(in="(>= 2 1)", out="#t")
	@ExampleCall(in="(>= 1 2)", out="#f")
	@ExampleCall(in="(>= 3 2 2)", out="#t")
	public static boolean gte(SNumber first, SNumber... rest) {
		if (rest.length == 0) {
			return true;
		}

		var prev = first;
		for (var curr : rest) {
			if (prev.compareTo(curr) >= 0) {
				prev = curr;
			} else {
				return false;
			}
		}

		return true;
	}

	@Procedure(name = "<=")
	@ExampleCall(in="(<= 1)", out="#t")
	@ExampleCall(in="(<= 2 1)", out="#f")
	@ExampleCall(in="(<= 1 2)", out="#t")
	@ExampleCall(in="(<= 1 1 2 3)", out="#t")
	public static boolean lte(SNumber first, SNumber... rest) {
		if (rest.length == 0) {
			return true;
		}

		var prev = first;
		for (var curr : rest) {
			if (prev.compareTo(curr) <= 0) {
				prev = curr;
			} else {
				return false;
			}
		}

		return true;
	}

	@Procedure(name = "<")
	@ExampleCall(in="(< 1)", out="#t")
	@ExampleCall(in="(< 2 1)", out="#f")
	@ExampleCall(in="(< 1 1)", out="#f")
	@ExampleCall(in="(< 1 2 3)", out="#t")
	public static boolean lt(SNumber first, SNumber... rest) {
		if (rest.length == 0) {
			return true;
		}

		var prev = first;
		for (var curr : rest) {
			if (prev.compareTo(curr) < 0) {
				prev = curr;
			} else {
				return false;
			}
		}

		return true;
	}

	@Procedure(name = "=")
	@ExampleCall(in="(= 1)", out="#t")
	@ExampleCall(in="(= 1 2)", out="#f")
	@ExampleCall(in="(= 1 1 1)", out="#t")
	public static boolean eq(SNumber first, SNumber... rest) {
		if (rest.length == 0) {
			return true;
		}

		var prev = first;
		for (var curr : rest) {
			if (prev.compareTo(curr) == 0) {
				prev = curr;
			} else {
				return false;
			}
		}

		return true;
	}



	@Procedure(name = "min")
	@ExampleCall(in="(min -1)", out="-1")
	@ExampleCall(in="(min 3 4)", out="3")
	@ExampleCall(in="(min 3.9 4)", out="3.9")
	public static SNumber min(SNumber first, SNumber... rest) throws EvaluationException {
		if (rest.length == 0) {
			return first;
		}

		var res = first;

		for (var arg : rest) {
			if (res.compareTo(arg) > 0) {
				res = arg;
			}
		}

		return res;
	}

	@Procedure(name = "max")
	@ExampleCall(in="(max -1)", out="-1")
	@ExampleCall(in="(max 3 4)", out="4")
	@ExampleCall(in="(max 3.9 4)", out="4")
	public static SNumber max(SNumber first, SNumber... rest) throws EvaluationException {
		if (rest.length == 0) {
			return first;
		}

		var res = first;

		for (var arg : rest) {
			if (res.compareTo(arg) < 0) {
				res = arg;
			}
		}

		return res;
	}

	@Procedure(name = "zero?")
	@ExampleCall(in="(zero? -1)", out="#f")
	@ExampleCall(in="(zero? 0)", out="#t")
	@ExampleCall(in="(zero? 1)", out="#f")
	public static boolean isZero(SNumber num) {
		return num.compareTo(SNumber.ZERO) == 0;
	}

	@Procedure(name = "positive?")
	@ExampleCall(in="(positive? -1)", out="#f")
	@ExampleCall(in="(positive? 0)", out="#f")
	@ExampleCall(in="(positive? 1)", out="#t")
	public static boolean isPositive(SNumber num) {
		return num.compareTo(SNumber.ZERO) > 0;
	}

	@Procedure(name = "negative?")
	@ExampleCall(in="(negative? -1)", out="#t")
	@ExampleCall(in="(negative? 0)", out="#f")
	@ExampleCall(in="(negative? 1)", out="#f")
	public static boolean isNegative(SNumber num) {
		return num.compareTo(SNumber.ZERO) < 0;
	}

	@Procedure(name = "even?")
	@ExampleCall(in="(even? -2)", out="#t")
	@ExampleCall(in="(even? 0)", out="#t")
	@ExampleCall(in="(even? 1)", out="#f")
	@ExampleCall(in="(even? 2)", out="#t")
	public static boolean isEven(SNumber num) throws EvaluationException {
		return num.modulo(SNumber.of(2)).compareTo(SNumber.ZERO) == 0;
	}

	@Procedure(name = "odd?")
	@ExampleCall(in="(odd? -1)", out="#t")
	@ExampleCall(in="(odd? 0)", out="#f")
	@ExampleCall(in="(odd? 1)", out="#t")
	@ExampleCall(in="(odd? 2)", out="#f")
	public static boolean isOdd(SNumber num) throws EvaluationException {
		return num.modulo(SNumber.of(2)).compareTo(SNumber.ZERO) != 0;
	}

	@Procedure(name = "integer?")
	@ExampleCall(in="(integer? 1)", out="#t")
	//@ExampleCall(in="(integer? 1/2)", out="#f")
	@ExampleCall(in="(integer? 1.1)", out="#f")
	//@ExampleCall(in="(integer? 1i)", out="#f")
	public static boolean isInteger(SNumber num) throws EvaluationException {
		return num.isInteger();
	}

	@Procedure(name = "rational?")
	@ExampleCall(in="(integer? 1)", out="#t")
	// TODO fix with rationals
	//@ExampleCall(in="(integer? 1/2)", out="#f")
	@ExampleCall(in="(integer? 1.1)", out="#f")
	//@ExampleCall(in="(integer? 1i)", out="#f")
	public static boolean isRational(SNumber num) throws EvaluationException {
		return num.isRational();
	}

	@Procedure(name = "real?")
	@ExampleCall(in="(real? 1)", out="#t")
	//@ExampleCall(in="(real? 1/2)", out="#t")
	@ExampleCall(in="(real? 1.1)", out="#t")
	//@ExampleCall(in="(integer? 1i)", out="#f")
	public static boolean isReal(SNumber num) throws EvaluationException {
		return num.isReal();
	}

	@Procedure(name = "complex?")
	@ExampleCall(in="(real? 1)", out="#t")
	//@ExampleCall(in="(real? 1/2)", out="#t")
	@ExampleCall(in="(real? 1.1)", out="#t")
	//@ExampleCall(in="(integer? 1i)", out="#t")
	public static boolean isComplex(SNumber num) throws EvaluationException {
		return num.isComplex();
	}

	@Procedure(name = "exact?")
	@ExampleCall(in="(exact? 1)", out="#t")
	@ExampleCall(in="(exact? 1.1)", out="#f")
	public static boolean isExact(SNumber num) {
		return num.isExact();
	}

	@Procedure(name = "inexact?")
	@ExampleCall(in="(inexact? 1)", out="#f")
	@ExampleCall(in="(inexact? 1.1)", out="#t")
	public static boolean isInxact(SNumber num) {
		return !isExact(num);
	}

	@Procedure(name = "quotient")
	@ExampleCall(in="(quotient 5 3)", out="1")
	@ExampleCall(in="(quotient 5 2)", out="2")
	@ExampleCall(in="(quotient 4 2)", out="2")
	@ExampleCall(in="(quotient 11 4)", out="2")
	public static SNumber quotient(SNumber lhs, SNumber rhs) throws EvaluationException {
		return lhs.quotient(rhs);
	}

	@Procedure(name = "remainder")
	@ExampleCall(in="(remainder 13 4)", out="1")
	@ExampleCall(in="(remainder -13 4)", out="-1")
	@ExampleCall(in="(remainder 13 -4)", out="1")
	@ExampleCall(in="(remainder -13 -4)", out="-1")
	@ExampleCall(in="(remainder -13 -4.0)", out="-1.0")
	public static SNumber remainder(SNumber lhs, SNumber rhs) throws EvaluationException {
		return lhs.remainder(rhs);
	}

	@Procedure(name = "modulo")
	@ExampleCall(in="(modulo 13 4)", out="1")
	@ExampleCall(in="(modulo -13 4)", out="3")
	@ExampleCall(in="(modulo 13 -4)", out="-3")
	@ExampleCall(in="(modulo -13 -4)", out="-1")
	public static SNumber modulo(SNumber lhs, SNumber rhs) throws EvaluationException {
		return lhs.modulo(rhs);
	}

	@Procedure(name = "abs")
	@ExampleCall(in="(abs -7)", out="7")
	public static SNumber abs(SNumber val) throws EvaluationException {
		return isPositive(val) ? val : val.negate();
	}

	@Procedure(name = "string->number", maxVaradicArgs = 1)
	@ExampleCall(in="(string->number \"100\")", out="100")
	//@ExampleCall(in="(string->number \"100\" 16)", out="256")
	@ExampleCall(in="(string->number \"1e2\")", out="1E+2")
	//@ExampleCall(in="(string->number \"15##\")", out="1500.0")
	public static SNumber stringToNumber(SString val, SNumber... optBase) throws EvaluationException {
		var base = 10;

		if (optBase.length == 1) {
			base = optBase[0].toInteger();
		}

		return SNumber.parse(val.javaString(), base);
	}

	@Procedure(name = "number->string")
	@ExampleCall(in="(number->string 100)", out="\"100\"")
	public static SString numberToString(SNumber num) throws EvaluationException {
		// TODO add radix support
		return new SString(num.toString());
	}

	///////////////////////////////////////////////////////
	// Boolean procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "not")
	@ExampleCall(in="(not #f)", out="#t")
	@ExampleCall(in="(not #t)", out="#f")
	@ExampleCall(in="(not 0)", out="#f")
	@ExampleCall(in="(not 1)", out="#f")
	public static boolean not(final SValue boxed) throws EvaluationException {
		// Not returns #t if obj is false, and returns #f otherwise.
		// https://www.cs.cmu.edu/Groups/AI/html/r4rs/r4rs_8.html#SEC46
		return !boxed.isTruthy();
	}

	@Procedure(name = "boolean?")
	@ExampleCall(in="(boolean? #f)", out="#t")
	@ExampleCall(in="(boolean? 1)", out="#f")
	public static boolean isBoolean(final SValue boxed) {
		return boxed.isBoolean();
	}

	///////////////////////////////////////////////////////
	// Vector procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "vector?")
	@ExampleCall(in="(vector? #(1 2 3))", out="#t")
	@ExampleCall(in="(vector? '(1 2 3))", out="#f")
	public static boolean isVector(SValue boxed) {
		return boxed.isVector();
	}

	@Procedure(name = "make-vector", maxVaradicArgs = 1)
	@ExampleCall(in="(make-vector 3)", out="#(#f #f #f)")
	@ExampleCall(in="(make-vector 3 #\\a)", out="#(#\\a #\\a #\\a)")
	public static SVector makeVector(int size, SValue... args) throws EvaluationException {
		if (args.length == 0) {
			return SVector.ofSize(size);
		}

		return SVector.ofSizeWithFill(size, args[0]);
	}

	@Procedure(name = "vector")
	@ExampleCall(in="(vector 'a 'b 'c)", out="#(a b c)")
	public static SVector vector(SValue... contents) throws EvaluationException {
		return SVector.of(contents);
	}

	@Procedure(name = "vector-length")
	@ExampleCall(in="(vector-length (vector 'a 'b 'c))", out="3")
	public static int vectorLength(SVector self) throws EvaluationException {
		return self.length();
	}

	@Procedure(name = "vector-ref")
	@ExampleCall(in="(vector-ref '#(1 1 2 3 5 8 13 21) 5)", out="8")
	public static SValue vectorRef(SVector vec, int idx) throws EvaluationException {
		return vec.ref(idx);
	}

	@Procedure(name = "vector-set!")
	@ExampleCall(in="(define x '#(1 1 1)) (vector-set! x 1 4) x", out="'#(1 4 1)")
	public static SValue vectorSet(SVector vec, int idx, SValue val) throws EvaluationException {
		return vec.set(idx, val);
	}

	@Procedure(name = "vector->list")
	@ExampleCall(in="(vector->list '#(dah dah didah))", out="'(dah dah didah)")
	public static SValue vectorToList(SVector vec) throws EvaluationException {
		return SPair.properListOfNullable(vec.toArray());
	}

	@Procedure(name = "list->vector")
	@ExampleCall(in="(list->vector '(dididit dah))", out="'#(dididit dah)")
	public static SVector listToVector(SPair list) throws EvaluationException {
		return SVector.of(list.toArray());
	}

	@Procedure(name = "vector-fill!")
	@ExampleCall(in="(define x '#(1 1 1)) (vector-fill! x 2) x", out="'#(2 2 2)")
	public static void vectorFill(SVector vec, SValue val) throws EvaluationException {
		vec.fill(val);
	}

	///////////////////////////////////////////////////////
	// List procedures
	///////////////////////////////////////////////////////
	@Procedure(name = "null?")
	@ExampleCall(in = "(null? ())", out = "#t")
	@ExampleCall(in = "(null? '(1 2 3))", out = "#f")
	@ExampleCall(in = "(null? 'foo)", out = "#f")
	public static boolean isNull(SValue val) {
		return val.isNull();
	}

	@Procedure(name = "pair?")
	@ExampleCall(in = "(pair? '(1 . 2))", out = "#t")
	@ExampleCall(in = "(pair? ())", out = "#f")
	public static boolean isPair(SValue val) {
		return val.isPair();
	}

	@Procedure(name = "list?")
	@ExampleCall(in = "(list? '())", out = "#t")
	@ExampleCall(in = "(list? '(1 2 3))", out = "#t")
	@ExampleCall(in = "(list? 'foo)", out = "#f")
	@ExampleCall(in = "(list? '(a . b))", out = "#f")
	public static boolean isList(SValue boxed) throws EvaluationException {
		return boxed.isNull() || (boxed.isPair() && boxed.getPair().isList());
	}

	@Procedure(name = "cons")
	@ExampleCall(in = "(cons 1 2)", out = "'(1 . 2)")
	@ExampleCall(in = "(cons 1 ())", out = "'(1)")
	public static SPair cons(SValue car, SValue cdr) throws EvaluationException {
		return SPair.cons(car, cdr);
	}

	@Procedure(name = "car")
	@ExampleCall(in="(car '(a b c))", out="'a")
	@ExampleCall(in="(car '((a) b c d))", out="'(a)")
	@ExampleCall(in="(car '(1 . 2))", out="1")
	@ExampleCall(in="(car '())", error=true)
	public static SValue car(SValue pair) throws EvaluationException {
		return pair.getPair().getCar();
	}

	@Procedure(name = "cdr")
	@ExampleCall(in="(cdr '((a) b c d))", out="'(b c d)")
	@ExampleCall(in="(cdr '(1 . 2))", out="2")
	@ExampleCall(in="(cdr '())", error=true)
	public static SValue cdr(SValue pair) throws EvaluationException {
		return pair.getPair().getCdr();
	}

	@Procedure(name = "set-car!")
	@ExampleCall(in="(define x '(1 . 2)) (set-car! x 3) x", out="'(3 . 2)")
	public static SPair setCar(SPair pair, SValue car) throws EvaluationException {
		pair.setCar(car);
		return pair;
	}

	@Procedure(name = "set-cdr!")
	@ExampleCall(in="(define x '(1 . 2)) (set-cdr! x 3) x", out="'(1 . 3)")
	public static SPair setCdr(SPair pair, SValue cdr) throws EvaluationException {
		pair.setCdr(cdr);
		return pair;
	}

	@Procedure(name = "length")
	@ExampleCall(in="(length '(a b c))", out="3")
	@ExampleCall(in="(length '(a (b) (c d e)))", out="3")
	@ExampleCall(in="(length '())", out="0")
	public static int length(SPair pair) throws EvaluationException {
		return pair.toArray().length;
	}

	@Procedure(name = "list")
	@ExampleCall(in = "(list)", out = "'()")
	@ExampleCall(in = "(list 1 2 3)", out = "'(1 2 3)")
	public static SValue list(SValue... contents) throws EvaluationException {
		return SPair.properListOfNullable(contents);
	}

	@Procedure(name = "append")
	@ExampleCall(in = "(append)", out = "()")
	@ExampleCall(in = "(append '(a b) '(c d))", out = "'(a b c d)")
	@ExampleCall(in = "(append '(a) '(b . c))", out = "'(a b . c)")
	@ExampleCall(in = "(append '() 'final)", out = "'final")
	public static SValue append(SValue... lists) throws EvaluationException {
		if (lists.length == 0) {
			return SNull.NULL;
		}

		var valueList = new ArrayList<>();
		for (int i = 0; i < lists.length - 1; i++) {
			if (lists[i].isNull()) {
				continue;
			}

			valueList.addAll(Arrays.asList(lists[i].getPair().toArray()));
		}

		var lastValue = lists[lists.length - 1];
		if (valueList.size() == 0) {
			return lastValue;
		}

		return SPair.improperListOf(lastValue, valueList.toArray(SValue[]::new));
	}

	@Procedure(name = "reverse")
	@ExampleCall(in = "(reverse '(a b c))", out = "'(c b a)")
	public static SValue reverse(SPair list) throws EvaluationException {
		return list.reversed();
	}

	@Procedure(name = "list-tail")
	@ExampleCall(in = "(list-tail 'a 0)", out = "'a")
	@ExampleCall(in = "(list-tail '() 0)", out = "'()")
	@ExampleCall(in = "(list-tail '(a . b) 1)", out = "'b")
	@ExampleCall(in = "(list-tail '(a b c) 2)", out = "'(c)")
	@ExampleCall(in = "(list-tail '(a b c) 3)", out = "'()")
	public static SValue listTail(SValue list, int idx) throws EvaluationException {
		while (idx > 0) {
			if (list.isNull()) {
				throw new EvaluationException("index not in range");
			}

			list = list.getPair().getCdr();
			idx--;
		}

		return list;
	}

	@Procedure(name = "list-ref")
	@ExampleCall(in = "(list-ref '(1 2 3) 1)", out = "2")
	public static SValue listRef(SValue list, int index) throws EvaluationException {
		var tail = listTail(list, index);
		return car(tail);
	}


	@Procedure(name = "apply")
	@ExampleCall(in = "(apply + '(1 2 3))", out = "6")
	@ExampleCall(in = "(apply - 100 '(10 20))", out = "70")
	@ExampleCall(in = "(apply +)", out = "0")
	public static SValue apply(Environment env, SProcedure proc, SValue... args) throws EvaluationException {

		switch (args.length) {
		case 0:
			return proc.eval(env, new SValue[0]);
		case 1:
			return proc.eval(env, args[0].getPair().toArray());
		default:
			var al = new ArrayList<SValue>(args.length);
			for (int i = 0; i < args.length - 1; i++) {
				al.add(args[i]);
			}

			var tail = args[args.length - 1].getPair().toArray();
			for (var e : tail) {
				al.add(e);
			}

			return proc.eval(env, al.toArray(SValue[]::new));
		}
	}

	@Procedure(name = "map")
	@ExampleCall(in = "(map + '(1 2 3))", out = "'(1 2 3)")
	@ExampleCall(in = "(map + '(1 2 3) '(3 2 1))", out = "'(4 4 4)")
	@ExampleCall(in = "(map cons '(1 2 3) '(10 20 30))", out = "'((1 . 10) (2 . 20) (3 . 30))")
	public static SValue map(Environment env, SProcedure proc, SPair argList, SPair... addlArgs)
			throws EvaluationException {
		var argLists = new ArrayList<SValue[]>(addlArgs.length + 1);
		var firstArray = argList.toArray();
		argLists.add(firstArray);
		for (var pair : addlArgs) {
			var tmp = pair.toArray();
			if (tmp.length != firstArray.length) {
				throw new EvaluationException("mismatched list lengths");
			}

			argLists.add(tmp);
		}

		var out = new SValue[firstArray.length];
		for (int i = 0; i < firstArray.length; i++) {
			var args = new SValue[argLists.size()];
			for (int j = 0; j < argLists.size(); j++) {
				args[j] = argLists.get(j)[i];
			}

			out[i] = apply(env, proc, SPair.properListOfNullable(args));
		}

		return SPair.properListOfNullable(out);
	}

	// for-each is like map, but with guaranteed order and no return value.
	@Procedure(name = "for-each")
	@ExampleCall(in="(let ((v (make-vector 5))) (for-each (lambda (i) (vector-set! v i (* i i))) '(0 1 2 3 4)) v)", out="#(0 1 4 9 16)")
	public static void forEach(Environment env, SProcedure proc, SPair argList, SPair... addlArgs)
			throws EvaluationException {
		var argLists = new ArrayList<SValue[]>(addlArgs.length + 1);
		var firstArray = argList.toArray();
		argLists.add(firstArray);
		for (var pair : addlArgs) {
			var tmp = pair.toArray();
			if (tmp.length != firstArray.length) {
				throw new EvaluationException("mismatched list lengths");
			}

			argLists.add(tmp);
		}

		var out = new SValue[firstArray.length];
		for (int i = 0; i < firstArray.length; i++) {
			var args = new SValue[argLists.size()];
			for (int j = 0; j < argLists.size(); j++) {
				args[j] = argLists.get(j)[i];
			}

			out[i] = apply(env, proc, SPair.properListOfNullable(args));
		}
	}

	///////////////////////////////////////////////////////
	// Symbol procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "symbol?")
	@ExampleCall(in="(symbol? 'foo)", out="#t")
	@ExampleCall(in="(symbol? (car '(a b)))", out="#t")
	@ExampleCall(in="(symbol? \"bar\")", out="#f")
	@ExampleCall(in="(symbol? 'nil)", out="#t")
	@ExampleCall(in="(symbol? '())", out="#f")
	@ExampleCall(in="(symbol? #f)", out="#f")
	public boolean isSymbol(SValue val) {
		return val.isSymbol();
	}

	@Procedure(name = "string->symbol")
	@ExampleCall(in="(eq? 'mISSISSIppi 'mississippi)", out="#t")
	@ExampleCall(in="(string->symbol \"mISSISSIppi\")", deterministic = false)
	@ExampleCall(in="(eq? 'bitBlt (string->symbol \"bitBlt\"))", out="#f")
	@ExampleCall(in="(eq? 'JollyWog (string->symbol (symbol->string 'JollyWog)))", out="#t")
	@ExampleCall(in="(string=? \"K. Harper, M.D.\" (symbol->string (string->symbol \"K. Harper, M.D.\")))", out="#t")
	public static SSymbol stringToSymbol(SString value) {
		return SSymbol.fromSString(value);
	}

	@Procedure(name = "symbol->string")
	@ExampleCall(in="(symbol->string 'flying-fish)", out="\"flying-fish\"")
	@ExampleCall(in="(symbol->string 'Martin)", out="\"martin\"")
	@ExampleCall(in="(symbol->string (string->symbol \"Malvina\"))", out="\"Malvina\"")
	public static SString symbolToString(SSymbol value) {
		return value.getStringRepresentation();
	}

	///////////////////////////////////////////////////////
	// String procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "string?")
	@ExampleCall(in="(string? \"foo\")", out="#t")
	@ExampleCall(in="(string? 'bar)", out="#f")
	public boolean isString(SValue val) {
		return val.isString();
	}

	@Procedure(name = "make-string", maxVaradicArgs = 1)
	@ExampleCall(in="(make-string 5 #\\a)", out="\"aaaaa\"")
	public static SString makeString(int size, char... args) throws EvaluationException {
		if (args.length == 0) {
			return new SString("\0".repeat(size));
		}

		return new SString(String.valueOf(args[0]).repeat(size));
	}

	@Procedure(name = "string")
	@ExampleCall(in="(string #\\a #\\b #\\c)", out="\"abc\"")
	public static SString string(char... contents) throws EvaluationException {
		return new SString(String.valueOf(contents));
	}

	@Procedure(name = "string-length")
	@ExampleCall(in="(string-length \"abcde\")", out="5")
	public static int stringLength(SString string) throws EvaluationException {
		return string.length();
	}

	@Procedure(name = "string-ref")
	@ExampleCall(in="(string-ref \"abcde\" 4)", out="#\\e")
	public static char stringRef(SString string, int idx) throws EvaluationException {
		return string.ref(idx);
	}

	@Procedure(name = "string-set!")
	@ExampleCall(in="(define x \"abc\") (string-set! x 1 #\\z) x", out="\"azc\"")
	public static void stringSet(SString str, int idx, char val) throws EvaluationException {
		str.set(idx, val);
	}

	@Procedure(name = "string=?")
	@ExampleCall(in="(string=? \"def\" \"abc\")", out="#f")
	@ExampleCall(in="(string=? \"abc\" \"abc\")", out="#t")
	@ExampleCall(in="(string=? \"abc\" \"def\")", out="#f")
	public static boolean stringEq(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) == 0;
	}

	@Procedure(name = "string>?")
	@ExampleCall(in="(string>? \"def\" \"abc\")", out="#t")
	@ExampleCall(in="(string>? \"abc\" \"abc\")", out="#f")
	@ExampleCall(in="(string>? \"abc\" \"def\")", out="#f")
	public static boolean stringGt(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) > 0;
	}

	@Procedure(name = "string>=?")
	@ExampleCall(in="(string>=? \"def\" \"abc\")", out="#t")
	@ExampleCall(in="(string>=? \"abc\" \"abc\")", out="#t")
	@ExampleCall(in="(string>=? \"abc\" \"def\")", out="#f")
	public static boolean stringGE(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) >= 0;
	}

	@Procedure(name = "string<?")
	@ExampleCall(in="(string<? \"def\" \"abc\")", out="#f")
	@ExampleCall(in="(string<? \"abc\" \"abc\")", out="#f")
	@ExampleCall(in="(string<? \"abc\" \"def\")", out="#t")
	public static boolean stringLt(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) < 0;
	}

	@Procedure(name = "string<=?")
	@ExampleCall(in="(string<=? \"def\" \"abc\")", out="#f")
	@ExampleCall(in="(string<=? \"abc\" \"abc\")", out="#t")
	@ExampleCall(in="(string<=? \"abc\" \"def\")", out="#t")
	public static boolean stringLE(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) <= 0;
	}

	@Procedure(name = "string-ci=?")
	@ExampleCall(in="(string-ci=? \"def\" \"ABC\")", out="#f")
	@ExampleCall(in="(string-ci=? \"abc\" \"ABC\")", out="#t")
	@ExampleCall(in="(string-ci=? \"abc\" \"DEF\")", out="#f")
	public static boolean stringCIEq(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) == 0;
	}

	@Procedure(name = "string-ci>?")
	@ExampleCall(in="(string-ci>? \"def\" \"ABC\")", out="#t")
	@ExampleCall(in="(string-ci>? \"abc\" \"ABC\")", out="#f")
	@ExampleCall(in="(string-ci>? \"abc\" \"DEF\")", out="#f")
	public static boolean stringCIGt(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) > 0;
	}

	@Procedure(name = "string-ci>=?")
	@ExampleCall(in="(string-ci>=? \"def\" \"ABC\")", out="#t")
	@ExampleCall(in="(string-ci>=? \"abc\" \"ABC\")", out="#t")
	@ExampleCall(in="(string-ci>=? \"abc\" \"DEF\")", out="#f")
	public static boolean stringCIGE(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) >= 0;
	}

	@Procedure(name = "string-ci<?")
	@ExampleCall(in="(string-ci<? \"def\" \"ABC\")", out="#f")
	@ExampleCall(in="(string-ci<? \"abc\" \"ABC\")", out="#f")
	@ExampleCall(in="(string-ci<? \"abc\" \"DEF\")", out="#t")
	public static boolean stringCILt(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) < 0;
	}

	@Procedure(name = "string-ci<=?")
	@ExampleCall(in="(string-ci<=? \"def\" \"ABC\")", out="#f")
	@ExampleCall(in="(string-ci<=? \"abc\" \"ABC\")", out="#t")
	@ExampleCall(in="(string-ci<=? \"abc\" \"DEF\")", out="#t")
	public static boolean stringCILE(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) <= 0;
	}

	@Procedure(name = "substring")
	@ExampleCall(in="(substring \"abcde\" 1 3)", out="\"bc\"")
	public static SString substring(SString string, int start, int end) {
		return new SString(string.subSequence(start, end));
	}

	@Procedure(name = "string-append")
	@ExampleCall(in="(string-append \"abc\" \"def\" \"ghi\")", out="\"abcdefghi\"")
	public static SString stringAppend(SString... strings) {
		var sb = new StringBuilder();
		Arrays.stream(strings).forEach(sb::append);
		return new SString(sb);
	}

	@Procedure(name = "string->list")
	@ExampleCall(in="(string->list \"abc\")", out="'(#\\a #\\b #\\c)")
	public static SValue stringToList(SString string) {

		var boxed = new SValue[string.length()];
		for (int i = 0; i < string.length(); i++) {
			boxed[i] = new SCharacter(string.charAt(i));
		}

		return SPair.properListOfNullable(boxed);
	}

	@Procedure(name = "list->string")
	@ExampleCall(in="(list->string '(#\\a #\\b #\\c))", out="\"abc\"")
	public static SString listToString(SPair list) throws EvaluationException {
		var sb = new StringBuilder();

		for (var val : list.toArray()) {
			sb.append(val.getCharacter().toChar());
		}

		return new SString(sb);
	}

	@Procedure(name = "string-copy")
	// TODO assert eq? is false
	@ExampleCall(in="(string-copy \"abc\")", out="\"abc\"")
	public static SString stringCopy(SString string) {
		var sb = new StringBuilder();
		sb.append(string);
		return new SString(sb);
	}

	@Procedure(name = "string-fill!")
	@ExampleCall(in="(define x \"aaa\") (string-fill! x #\\z) x", out="\"zzz\"")
	public static void stringFill(SString str, char fill) throws EvaluationException {
		str.fill(fill);
	}

	///////////////////////////////////////////////////////
	// Control procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "procedure?")
	@ExampleCall(in="(procedure? car)", out="#t")
	@ExampleCall(in="(procedure? 'car)", out="#f")
	@ExampleCall(in="(procedure? (lambda (x) (* x x)))", out="#t")
	@ExampleCall(in="(procedure? '(lambda (x) (* x x)))", out="#f")
	@ExampleCall(in="(call-with-current-continuation procedure?)", out="#t")
	public static boolean isProcedure(SValue boxed) {
		return boxed.isProcedure();
	}

	///////////////////////////////////////////////////////
	// Char procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "char?")
	@ExampleCall(in="(char? #\\a)", out="#t")
	@ExampleCall(in="(char? #t)", out="#f")
	public static boolean isChar(SValue val) {
		return val.isCharacter();
	}

	@Procedure(name = "char=?")
	@ExampleCall(in="(char=? #\\a #\\b)", out="#f")
	@ExampleCall(in="(char=? #\\a #\\a)", out="#t")
	@ExampleCall(in="(char=? #\\b #\\a)", out="#f")
	public static boolean charEQ(char lhs, char rhs) {
		return Character.compare(lhs, rhs) == 0;
	}

	@Procedure(name = "char<?")
	@ExampleCall(in="(char<? #\\a #\\b)", out="#t")
	@ExampleCall(in="(char<? #\\a #\\a)", out="#f")
	@ExampleCall(in="(char<? #\\b #\\a)", out="#f")
	public static boolean charLT(char lhs, char rhs) {
		return Character.compare(lhs, rhs) < 0;
	}

	@Procedure(name = "char>?")
	@ExampleCall(in="(char>? #\\a #\\b)", out="#f")
	@ExampleCall(in="(char>? #\\a #\\a)", out="#f")
	@ExampleCall(in="(char>? #\\b #\\a)", out="#t")
	public static boolean charGT(char lhs, char rhs) {
		return Character.compare(lhs, rhs) > 0;
	}

	@Procedure(name = "char>=?")
	@ExampleCall(in="(char>=? #\\a #\\b)", out="#f")
	@ExampleCall(in="(char>=? #\\a #\\a)", out="#t")
	@ExampleCall(in="(char>=? #\\b #\\a)", out="#t")
	public static boolean charGE(char lhs, char rhs) {
		return Character.compare(lhs, rhs) >= 0;
	}

	@Procedure(name = "char<=?")
	@ExampleCall(in="(char<=? #\\a #\\b)", out="#t")
	@ExampleCall(in="(char<=? #\\a #\\a)", out="#t")
	@ExampleCall(in="(char<=? #\\b #\\a)", out="#f")
	public static boolean charLE(char lhs, char rhs) {
		return Character.compare(lhs, rhs) <= 0;
	}

	private static int charCI(char lhs, char rhs) {
		return String.valueOf(lhs).compareToIgnoreCase(String.valueOf(rhs));
	}

	@Procedure(name = "char-ci=?")
	@ExampleCall(in="(char-ci=? #\\a #\\B)", out="#f")
	@ExampleCall(in="(char-ci=? #\\a #\\A)", out="#t")
	@ExampleCall(in="(char-ci=? #\\b #\\A)", out="#f")
	public static boolean charCIEQ(char lhs, char rhs) {
		return charCI(lhs, rhs) == 0;
	}

	@Procedure(name = "char-ci<?")
	@ExampleCall(in="(char-ci<? #\\a #\\B)", out="#t")
	@ExampleCall(in="(char-ci<? #\\a #\\A)", out="#f")
	@ExampleCall(in="(char-ci<? #\\b #\\A)", out="#f")
	public static boolean charCILT(char lhs, char rhs) {
		return charCI(lhs, rhs) < 0;
	}

	@Procedure(name = "char-ci>?")
	@ExampleCall(in="(char-ci>? #\\a #\\B)", out="#f")
	@ExampleCall(in="(char-ci>? #\\a #\\A)", out="#f")
	@ExampleCall(in="(char-ci>? #\\b #\\A)", out="#t")
	public static boolean charCIGT(char lhs, char rhs) {
		return charCI(lhs, rhs) > 0;
	}

	@Procedure(name = "char-ci>=?")
	@ExampleCall(in="(char-ci>=? #\\a #\\B)", out="#f")
	@ExampleCall(in="(char-ci>=? #\\a #\\A)", out="#t")
	@ExampleCall(in="(char-ci>=? #\\b #\\A)", out="#t")
	public static boolean charCIGE(char lhs, char rhs) {
		return charCI(lhs, rhs) >= 0;
	}

	@Procedure(name = "char-ci<=?")
	@ExampleCall(in="(char-ci<=? #\\a #\\B)", out="#t")
	@ExampleCall(in="(char-ci<=? #\\a #\\A)", out="#t")
	@ExampleCall(in="(char-ci<=? #\\b #\\A)", out="#f")
	public static boolean charCILE(char lhs, char rhs) {
		return charCI(lhs, rhs) <= 0;
	}

	@Procedure(name = "char-alphabetic?")
	@ExampleCall(in="(char-alphabetic? #\\a)", out="#t")
	@ExampleCall(in="(char-alphabetic? #\\?)", out="#f")
	public static boolean isCharAlphabetic(char c) {
		return Character.isAlphabetic(c);
	}

	@Procedure(name = "char-numeric?")
	@ExampleCall(in="(char-numeric? #\\0)", out="#t")
	@ExampleCall(in="(char-numeric? #\\?)", out="#f")
	public static boolean isCharNumeric(char c) {
		return Character.isDigit(c);
	}

	@Procedure(name = "char-whitespace?")
	@ExampleCall(in="(char-whitespace? #\\ )", out="#t")
	@ExampleCall(in="(char-whitespace? #\\?)", out="#f")
	public static boolean isCharWhitespace(char c) {
		return Character.isWhitespace(c);
	}

	@Procedure(name = "char-upper-case?")
	@ExampleCall(in="(char-upper-case? #\\A)", out="#t")
	@ExampleCall(in="(char-upper-case? #\\a)", out="#f")
	public static boolean isCharUpperCase(char c) {
		return Character.isUpperCase(c);
	}

	@Procedure(name = "char-lower-case?")
	@ExampleCall(in="(char-lower-case? #\\a)", out="#t")
	@ExampleCall(in="(char-lower-case? #\\A)", out="#f")
	public static boolean isCharLowerCase(char c) {
		return Character.isLowerCase(c);
	}

	@Procedure(name = "char->integer")
	@ExampleCall(in="(char->integer #\\a)", out="97")
	public static int charToInt(char c) {
		return (int) (c);
	}

	@Procedure(name = "integer->char")
	@ExampleCall(in="(integer->char 97)", out="#\\a")
	public static char intToChar(int c) {
		return (char) c;
	}

	@Procedure(name = "char-upcase")
	@ExampleCall(in="(char-upcase #\\a)", out="#\\A")
	public static char charUpcase(char c) {
		return Character.toUpperCase(c);
	}

	@Procedure(name = "char-downcase")
	@ExampleCall(in="(char-downcase #\\A)", out="#\\a")
	public static char charDowncase(char c) {
		return Character.toLowerCase(c);
	}

	@Procedure(name = "force")
	@ExampleCall(in="(force (delay (+ 1 2)))", out="3")
	public static SValue force(Environment env, SProcedure toEval) throws EvaluationException {
		return toEval.eval(env, new SValue[0]);
	}

	@Procedure(name = "gensym")
	@ExampleCall(in="(gensym)", out="'gensym-1", deterministic=false)
	public static SValue gensym() {
		return SSymbol.unique("gensym-");
	}

	@Procedure(name = "void")
	@ExampleCall(in="(void)", out="#<void>", deterministic = false)
	public static SValue void_(SValue... args) {
		return SNull.VOID;
	}

	@Procedure(name = "void?")
	@ExampleCall(in="(void? (void))", out="#t")
	@ExampleCall(in="(void? ())", out="#f")
	public static boolean isVoid(SValue arg) {
		return arg == SNull.VOID;
	}

	///////////////////////////////////////////////////////
	// Port procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "port?")
	@ExampleCall(in="(port? (current-input-port))", out="#t")
	@ExampleCall(in="(port? ())", out="#f")
	public static boolean isPort(SValue arg) {
		return arg.isPort();
	}

	@Procedure(name = "input-port?")
	@ExampleCall(in="(input-port? (current-input-port))", out="#t")
	@ExampleCall(in="(input-port? (current-output-port))", out="#f")
	public static boolean isInputPort(SValue arg) {
		return arg.isPort() && arg.getPort().isInputPort();
	}

	@Procedure(name = "output-port?")
	@ExampleCall(in="(output-port? (current-output-port))", out="#t")
	@ExampleCall(in="(output-port? (current-input-port))", out="#f")
	public static boolean isOutputPort(SValue arg) {
		return arg.isPort() && arg.getPort().isOutputPort();
	}

	@Procedure(name = "eof")
	@ExampleCall(in="(eof)", out="#<EOF>", deterministic=false)
	public static SValue eof(SValue... args) {
		return SNull.EOF;
	}

	@Procedure(name = "eof-object?")
	@ExampleCall(in="(eof-object? (eof))", out="#t")
	public static boolean isEOFObject(final SValue value) {
		return value == SNull.EOF;
	}

	///////////////////////////////////////////////////////
	// Other procedures
	///////////////////////////////////////////////////////

	@Init
	public static void init(Scheme scm) {
		scm.loadString(Builtins.LIBRARY);
	}


	// Arbitrary compositions (of car/cdr), up to four deep, are provided.
	// There are twenty-eight of these procedures in all.
	// @formatter:off
	private static final String LIBRARY = "" +
	        "(define (caar x) (car (car x))) " + 
			"(define (cadr x) (car (cdr x))) " + 
			"(define (cdar x) (cdr (car x))) " + 
			"(define (cddr x) (cdr (cdr x))) " + 
			"(define (caaar x) (car (car (car x)))) " + 
			"(define (caadr x) (car (car (cdr x)))) " + 
			"(define (cadar x) (car (cdr (car x)))) " + 
			"(define (caddr x) (car (cdr (cdr x)))) " + 
			"(define (cdaar x) (cdr (car (car x)))) " + 
			"(define (cdadr x) (cdr (car (cdr x)))) " + 
			"(define (cddar x) (cdr (cdr (car x)))) " + 
			"(define (cdddr x) (cdr (cdr (cdr x)))) " + 
			"(define (caaaar x) (car (car (car (car x))))) " + 
			"(define (caaadr x) (car (car (car (cdr x))))) " + 
			"(define (caadar x) (car (car (cdr (car x))))) " + 
			"(define (caaddr x) (car (car (cdr (cdr x))))) " + 
			"(define (cadaar x) (car (cdr (car (car x))))) " + 
			"(define (cadadr x) (car (cdr (car (cdr x))))) " + 
			"(define (caddar x) (car (cdr (cdr (car x))))) " + 
			"(define (cadddr x) (car (cdr (cdr (cdr x))))) " + 
			"(define (cdaaar x) (cdr (car (car (car x))))) " + 
			"(define (cdaadr x) (cdr (car (car (cdr x))))) " + 
			"(define (cdadar x) (cdr (car (cdr (car x))))) " + 
			"(define (cdaddr x) (cdr (car (cdr (cdr x))))) " + 
			"(define (cddaar x) (cdr (cdr (car (car x))))) " + 
			"(define (cddadr x) (cdr (cdr (car (cdr x))))) " + 
			"(define (cdddar x) (cdr (cdr (cdr (car x))))) " + 
			"(define (cddddr x) (cdr (cdr (cdr (cdr x))))) " +

			"(define (memq obj lst)" + 
			"  (cond ((null? lst) #f)" + 
			"        ((eq? obj (car lst)) lst)" + 
			"        (else (memq obj (cdr lst)))))" + 

			"(define (memv obj lst)" + 
			"  (cond ((null? lst) #f)" + 
			"        ((eqv? obj (car lst)) lst)" + 
			"        (else (memv obj (cdr lst)))))" + 

			"(define (member obj lst)" + 
			"  (cond ((null? lst) #f)" + 
			"        ((equal? obj (car lst)) lst)" + 
			"        (else (member obj (cdr lst)))))" +

			"(define (assq obj lst)" + 
			"  (cond ((null? lst) #f)" + 
			"        ((eq? obj (caar lst)) (car lst))" + 
			"        (else (assq obj (cdr lst)))))" + 

			"(define (assv obj lst)" + 
			"  (cond ((null? lst) #f)" + 
			"        ((eqv? obj (caar lst)) (car lst))" + 
			"        (else (assv obj (cdr lst)))))" + 

			"(define (assoc obj lst)" + 
			"  (cond ((null? lst) #f)" + 
			"        ((equal? obj (caar lst)) (car lst))" + 
			"        (else (assoc obj (cdr lst)))))";
	// @formatter:on
}
