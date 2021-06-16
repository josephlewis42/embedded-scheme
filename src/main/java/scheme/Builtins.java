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

package main.java.scheme;

import java.util.ArrayList;
import java.util.Arrays;

import main.java.scheme.bind.ExampleCall;
import main.java.scheme.bind.Init;
import main.java.scheme.bind.Procedure;
import main.java.scheme.types.SCharacter;
import main.java.scheme.types.SNull;
import main.java.scheme.types.SNumber;
import main.java.scheme.types.SPair;
import main.java.scheme.types.SProcedure;
import main.java.scheme.types.SString;
import main.java.scheme.types.SSymbol;
import main.java.scheme.types.SValue;
import main.java.scheme.types.SVector;
import main.java.scheme.vm.VM;

/**
 * Builtins provides native implementations of many required functions.
 */
public class Builtins {

	///////////////////////////////////////////////////////
	// Equivalence predicates
	///////////////////////////////////////////////////////

	@Procedure(name = "eq?")
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

	@Procedure(name = "exit")
	public static void exit() throws EvaluationException {
		System.exit(0);
	}

	@Procedure(name = "error")
	public static void error(String message) throws EvaluationException {
		throw new EvaluationException(message);
	}

	@Procedure(name = "eval")
	public static SValue eval(Environment env, SValue val) {
		return VM.eval(env, val);
	}


	///////////////////////////////////////////////////////
	// Numeric procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "number?")
	public static boolean isNumber(SValue val) {
		return val.isNumber();
	}

	@Procedure(name = "+")
	public static SNumber sum(SNumber... args) {
		var res = SNumber.ZERO;

		for (var arg : args) {
			res = res.add(arg);
		}

		return res;
	}

	@Procedure(name = "*")
	public static SNumber multiply(SNumber... args) {
		var res = SNumber.ONE;

		for (var arg : args) {
			res = res.multiply(arg);
		}

		return res;
	}

	@Procedure(name = "-")
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
	public static boolean isZero(SNumber num) {
		return num.compareTo(SNumber.ZERO) == 0;
	}

	@Procedure(name = "positive?")
	public static boolean isPositive(SNumber num) {
		return num.compareTo(SNumber.ZERO) > 0;
	}

	@Procedure(name = "negative?")
	public static boolean isNegative(SNumber num) {
		return num.compareTo(SNumber.ZERO) < 0;
	}

	@Procedure(name = "even?")
	public static boolean isEven(SNumber num) throws EvaluationException {
		return num.toLong() % 2 == 0;
	}

	@Procedure(name = "odd?")
	public static boolean isOdd(SNumber num) throws EvaluationException {
		return num.toLong() % 2 != 0;
	}

	@Procedure(name = "integer?")
	public static boolean isInteger(SNumber num) throws EvaluationException {
		return num.isInteger();
	}

	@Procedure(name = "rational?")
	public static boolean isRational(SNumber num) throws EvaluationException {
		return num.isRational();
	}

	@Procedure(name = "real?")
	public static boolean isReal(SNumber num) throws EvaluationException {
		return num.isReal();
	}

	@Procedure(name = "complex?")
	public static boolean isComplex(SNumber num) throws EvaluationException {
		return num.isComplex();
	}

	@Procedure(name = "exact?")
	public static boolean isExact(SNumber num) {
		return num.isExact();
	}

	@Procedure(name = "inexact?")
	public static boolean isInxact(SNumber num) {
		return !isExact(num);
	}

	@Procedure(name = "sqrt")
	public static SNumber sqrt(SNumber num) throws EvaluationException {
		return num.sqrt();
	}

	@Procedure(name = "quotient")
	public static SNumber quotient(SNumber lhs, SNumber rhs) throws EvaluationException {
		return lhs.quotient(rhs);
	}

	@Procedure(name = "remainder")
	public static SNumber remainder(SNumber lhs, SNumber rhs) throws EvaluationException {
		return lhs.remainder(rhs);
	}

	@Procedure(name = "modulo")
	public static SNumber modulo(SNumber lhs, SNumber rhs) throws EvaluationException {
		return lhs.modulo(rhs);
	}

	@Procedure(name = "abs")
	public static SNumber abs(SNumber val) throws EvaluationException {
		return isPositive(val) ? val : val.negate();
	}

	@Procedure(name = "string->number", maxVaradicArgs = 1)
	public static SNumber stringToNumber(SString val, SNumber... optBase) throws EvaluationException {
		var base = 10;

		if (optBase.length == 1) {
			base = optBase[0].toInteger();
		}

		return SNumber.parse(val.javaString(), base);
	}

	@Procedure(name = "number->string")
	public static SString numberToString(SNumber num) throws EvaluationException {
		// TODO add radix support
		return new SString(num.toString());
	}

//	library procedure:  (gcd n1 ...) 
//	library procedure:  (lcm n1 ...) 
//	These procedures return the greatest common divisor or least common multiple of their arguments. The result is always non-negative.
//
//	(gcd 32 -36)                    ===>  4
//	(gcd)                           ===>  0
//	(lcm 32 -36)                    ===>  288
//	(lcm 32.0 -36)                  ===>  288.0  ; inexact
//	(lcm)                           ===>  1
//
//	procedure:  (numerator q) 
//	procedure:  (denominator q) 
//	These procedures return the numerator or denominator of their argument; the result is computed as if the argument was represented as a fraction in lowest terms. The denominator is always positive. The denominator of 0 is defined to be 1.
//
//	(numerator (/ 6 4))          ===>  3
//	(denominator (/ 6 4))          ===>  2
//	(denominator
//	  (exact->inexact (/ 6 4)))         ===> 2.0
//
//	procedure:  (floor x) 
//	procedure:  (ceiling x) 
//	procedure:  (truncate x) 
//	procedure:  (round x) 
//	
//	These procedures return integers. Floor returns the largest integer not larger than x. Ceiling returns the smallest integer not smaller than x. Truncate returns the integer closest to x whose absolute value is not larger than the absolute value of x. Round returns the closest integer to x, rounding to even when x is halfway between two integers.
//
//			Rationale:   Round rounds to even for consistency with the default rounding mode specified by the IEEE floating point standard.
//			Note:   If the argument to one of these procedures is inexact, then the result will also be inexact. If an exact value is needed, the result should be passed to the inexact->exact procedure.

	///////////////////////////////////////////////////////
	// Boolean procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "not")
	public static boolean not(final SValue boxed) throws EvaluationException {
		// Not returns #t if obj is false, and returns #f otherwise.
		// https://www.cs.cmu.edu/Groups/AI/html/r4rs/r4rs_8.html#SEC46
		return !boxed.isTruthy();
	}

	@Procedure(name = "boolean?")
	public static boolean isBoolean(final SValue boxed) {
		return boxed.isBoolean();
	}

	///////////////////////////////////////////////////////
	// Vector procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "vector?")
	public static boolean isVector(SValue boxed) {
		return boxed.isVector();
	}

	@Procedure(name = "make-vector", maxVaradicArgs = 1)
	public static SVector makeVector(int size, SValue... args) throws EvaluationException {
		if (args.length == 0) {
			return SVector.ofSize(size);
		}

		return SVector.ofSizeWithFill(size, args[0]);
	}

	@Procedure(name = "vector")
	public static SVector vector(SValue... contents) throws EvaluationException {
		return SVector.of(contents);
	}

	@Procedure(name = "vector-length")
	public static int vectorLength(SVector self) throws EvaluationException {
		return self.length();
	}

	@Procedure(name = "vector-ref")
	public static SValue vectorRef(SVector vec, int idx) throws EvaluationException {
		return vec.ref(idx);
	}

	@Procedure(name = "vector-set!")
	public static SValue vectorSet(SVector vec, int idx, SValue val) throws EvaluationException {
		return vec.set(idx, val);
	}

	@Procedure(name = "vector->list")
	public static SValue vectorToList(SVector vec) throws EvaluationException {
		return SPair.properListOfNullable(vec.toArray());
	}

	@Procedure(name = "list->vector")
	public static SVector listToVector(SPair list) throws EvaluationException {
		return SVector.of(list.toArray());
	}

	@Procedure(name = "vector-fill!")
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
	public static boolean isPair(SValue val) {
		return val.isPair();
	}

	@Procedure(name = "list?")
	@ExampleCall(in = "(list? ())", out = "#t")
	@ExampleCall(in = "(list '(1 2 3))", out = "#t")
	@ExampleCall(in = "(list 'foo)", out = "#f")
	@ExampleCall(in = "(list? '(a . b))", out = "#f")
	public static boolean isList(SPair boxed) throws EvaluationException {
		return boxed.isList();
	}

	@Procedure(name = "cons")
	public static SPair cons(SValue car, SValue cdr) throws EvaluationException {
		return SPair.cons(car, cdr);
	}

	@Procedure(name = "car")
	public static SValue car(SValue pair) throws EvaluationException {
		return pair.getPair().getCar();
	}

	@Procedure(name = "cdr")
	public static SValue cdr(SValue pair) throws EvaluationException {
		return pair.getPair().getCdr();
	}

	@Procedure(name = "set-car!")
	public static SPair setCar(SPair pair, SValue car) throws EvaluationException {
		pair.setCar(car);
		return pair;
	}

	@Procedure(name = "set-cdr!")
	public static SPair setCdr(SPair pair, SValue cdr) throws EvaluationException {
		pair.setCdr(cdr);
		return pair;
	}

	@Procedure(name = "length")
	public static int length(SPair pair) throws EvaluationException {
		return pair.toArray().length;
	}

	@Procedure(name = "list")
	@ExampleCall(in = "(list)", out = "()")
	@ExampleCall(in = "(list 1 2 3)", out = "(1 2 3)")
	public static SValue list(SValue... contents) throws EvaluationException {
		return SPair.properListOfNullable(contents);
	}

	@Procedure(name = "append")
	@ExampleCall(in = "(append)", out = "()")
	@ExampleCall(in = "(append '(a b) '(c d))", out = "(a b c d)")
	@ExampleCall(in = "(append '(a) '(b . c))", out = "(a b . c)")
	@ExampleCall(in = "(append '() 'final)", out = "final")
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
	@ExampleCall(in = "(reverse '(a b c)", out = "(c b a)")
	public static SValue reverse(SPair list) throws EvaluationException {
		return list.reversed();
	}

	@Procedure(name = "list-tail")
	@ExampleCall(in = "(list-tail 'a 0)", out = "a")
	@ExampleCall(in = "(list-tail () 0)", out = "()")
	@ExampleCall(in = "(list-tail '(a . b) 1)", out = "b")
	@ExampleCall(in = "(list-tail '(a b c) 2)", out = "(c)")
	@ExampleCall(in = "(list-tail '(a b c) 3)", out = "()")
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
	@ExampleCall(in = "(map + '(1 2 3))", out = "((1 . 10) (2 . 20) (3 . 30))")
	@ExampleCall(in = "(map cons '(1 2 3) '(10 20 30))", out = "((1 . 10) (2 . 20) (3 . 30))")
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
	public boolean isSymbol(SValue val) {
		return val.isSymbol();
	}

	@Procedure(name = "string->symbol")
	public static SSymbol stringToSymbol(SString value) {
		return SSymbol.fromSString(value);
	}

	@Procedure(name = "symbol->string")
	public static SString symbolToString(SSymbol value) {
		return value.getStringRepresentation();
	}

	///////////////////////////////////////////////////////
	// String procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "string?")
	public boolean isString(SValue val) {
		return val.isString();
	}

	@Procedure(name = "make-string", maxVaradicArgs = 1)
	public static SString makeString(int size, char... args) throws EvaluationException {
		if (args.length == 0) {
			return new SString("\0".repeat(size));
		}

		return new SString(String.valueOf(args[0]).repeat(size));
	}

	@Procedure(name = "string")
	public static SString string(char... contents) throws EvaluationException {
		return new SString(String.valueOf(contents));
	}

	@Procedure(name = "string-length")
	public static int stringLength(SString string) throws EvaluationException {
		return string.length();
	}

	@Procedure(name = "string-ref")
	public static char stringRef(SString string, int idx) throws EvaluationException {
		return string.ref(idx);
	}

	@Procedure(name = "string-set!")
	public static void stringSet(SString str, int idx, char val) throws EvaluationException {
		str.set(idx, val);
	}


	@Procedure(name = "string=?")
	public static boolean stringEq(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) == 0;
	}

	@Procedure(name = "string>?")
	public static boolean stringGt(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) > 0;
	}

	@Procedure(name = "string>=?")
	public static boolean stringGE(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) >= 0;
	}

	@Procedure(name = "string<?")
	public static boolean stringLt(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) < 0;
	}

	@Procedure(name = "string<=?")
	public static boolean stringLE(SString lhs, SString rhs) {
		return lhs.compareTo(rhs) <= 0;
	}

	@Procedure(name = "string-ci=?")
	public static boolean stringCIEq(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) == 0;
	}

	@Procedure(name = "string-ci>?")
	public static boolean stringCIGt(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) > 0;
	}

	@Procedure(name = "string-ci>=?")
	public static boolean stringCIGE(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) >= 0;
	}

	@Procedure(name = "string-ci<?")
	public static boolean stringCILt(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) < 0;
	}

	@Procedure(name = "string-ci<=?")
	public static boolean stringCILE(SString lhs, SString rhs) {
		return lhs.compareToCaseInsensitive(rhs) <= 0;
	}

	@Procedure(name = "substring")
	public static SString substring(SString string, int start, int end) {
		return new SString(string.subSequence(start, end));
	}

	@Procedure(name = "string-append")
	public static SString stringAppend(SString... strings) {
		var sb = new StringBuilder();
		Arrays.stream(strings).forEach(sb::append);
		return new SString(sb);
	}

	@Procedure(name = "string->list")
	public static SValue stringToList(SString string) {

		var boxed = new SValue[string.length()];
		for (int i = 0; i < string.length(); i++) {
			boxed[i] = new SCharacter(string.charAt(i));
		}

		return SPair.properListOfNullable(boxed);
	}

	@Procedure(name = "list->string")
	public static SString listToString(SPair list) throws EvaluationException {
		var sb = new StringBuilder();

		for (var val : list.toArray()) {
			sb.append(val.getCharacter());
		}

		return new SString(sb);
	}

	@Procedure(name = "string-copy")
	public static SString stringCopy(SString string) {
		var sb = new StringBuilder();
		sb.append(string);
		return new SString(sb);
	}

	@Procedure(name = "string-fill!")
	public static void stringFill(SString str, char fill) throws EvaluationException {
		str.fill(fill);
	}

	///////////////////////////////////////////////////////
	// Control procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "procedure?")
	public static boolean isProcedure(SValue boxed) {
		return boxed.isProcedure();
	}

	///////////////////////////////////////////////////////
	// Char procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "char?")
	public static boolean isChar(SValue val) {
		return val.isCharacter();
	}

	@Procedure(name = "char=?")
	public static boolean charEQ(char lhs, char rhs) {
		return Character.compare(lhs, rhs) == 0;
	}

	@Procedure(name = "char<?")
	public static boolean charLT(char lhs, char rhs) {
		return Character.compare(lhs, rhs) < 0;
	}

	@Procedure(name = "char>?")
	public static boolean charGT(char lhs, char rhs) {
		return Character.compare(lhs, rhs) > 0;
	}

	@Procedure(name = "char>=?")
	public static boolean charGE(char lhs, char rhs) {
		return Character.compare(lhs, rhs) >= 0;
	}

	@Procedure(name = "char<=?")
	public static boolean charLE(char lhs, char rhs) {
		return Character.compare(lhs, rhs) <= 0;
	}

	private static int charCI(char lhs, char rhs) {
		return String.valueOf(lhs).compareToIgnoreCase(String.valueOf(rhs));
	}

	@Procedure(name = "char-ci=?")
	public static boolean charCIEQ(char lhs, char rhs) {
		return charCI(lhs, rhs) == 0;
	}

	@Procedure(name = "char-ci<?")
	public static boolean charCILT(char lhs, char rhs) {
		return charCI(lhs, rhs) < 0;
	}

	@Procedure(name = "char-ci>?")
	public static boolean charCIGT(char lhs, char rhs) {
		return charCI(lhs, rhs) > 0;
	}

	@Procedure(name = "char-ci>=?")
	public static boolean charCIGE(char lhs, char rhs) {
		return charCI(lhs, rhs) >= 0;
	}

	@Procedure(name = "char-ci<=?")
	public static boolean charCILE(char lhs, char rhs) {
		return charCI(lhs, rhs) <= 0;
	}

	@Procedure(name = "char-alphabetic?")
	public static boolean isCharAlphabetic(char c) {
		return Character.isAlphabetic(c);
	}

	@Procedure(name = "char-numeric?")
	public static boolean isCharNumeric(char c) {
		return Character.isDigit(c);
	}

	@Procedure(name = "char-whitespace?")
	public static boolean isCharWhitespace(char c) {
		return Character.isWhitespace(c);
	}

	@Procedure(name = "char-upper-case?")
	public static boolean isCharUpperCase(char c) {
		return Character.isUpperCase(c);
	}

	@Procedure(name = "char-lower-case?")
	public static boolean isCharLowerCase(char c) {
		return Character.isLowerCase(c);
	}

	@Procedure(name = "char->integer")
	public static int charToInt(char c) {
		return (int) (c);
	}

	@Procedure(name = "integer->char")
	public static char intToChar(int c) {
		return (char) c;
	}

	@Procedure(name = "char-upcase")
	public static char charUpcase(char c) {
		return Character.toUpperCase(c);
	}

	@Procedure(name = "char-downcase")
	public static char charDowncase(char c) {
		return Character.toLowerCase(c);
	}

	@Procedure(name = "force")
	public static SValue force(Environment env, SProcedure toEval) throws EvaluationException {
		return toEval.eval(env, new SValue[0]);
	}

	@Procedure(name = "gensym")
	public static SValue gensym() {
		return SSymbol.unique("gensym-");
	}

	@Procedure(name = "lookup")
	public static SValue lookup(Environment env, SSymbol sym) {
		return env.lookupSymbol(sym);
	}

	@Procedure(name = "void")
	public static SValue void_(SValue... args) {
		return SNull.VOID;
	}

	@Procedure(name = "void?")
	public static boolean isVoid(SValue arg) {
		return arg == SNull.VOID;
	}

	///////////////////////////////////////////////////////
	// Port procedures
	///////////////////////////////////////////////////////

	@Procedure(name = "port?")
	public static boolean isPort(SValue arg) {
		return arg.isPort();
	}

	@Procedure(name = "input-port?")
	public static boolean isInputPort(SValue arg) {
		return arg.isPort() && arg.getPort().isInputPort();
	}

	@Procedure(name = "output-port?")
	public static boolean isOutputPort(SValue arg) {
		return arg.isPort() && arg.getPort().isOutputPort();
	}

	@Procedure(name = "eof-object?")
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
