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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

import scheme.bind.ExampleCall;
import scheme.bind.Procedure;
import scheme.bind.Schemeify;
import scheme.types.SInputPort;
import scheme.types.SNull;
import scheme.types.SOutputPort;
import scheme.types.SValue;
import scheme.vm.VM;

/**
 * An instance of the Scheme execution environment.
 */
public class Scheme {

	private final InputStream in;
	private final OutputStream out;
	private final Environment env;
	
	/**
	 * Starts a new R5RS interpreter on the given input and output streams. It's the
	 * responsibility of the caller to handle closing the streams properly.
	 */
	public Scheme(final InputStream in, final OutputStream out) {
		Objects.requireNonNull(in, "in must not be null");
		Objects.requireNonNull(out, "out must not be null");

		this.in = in;
		this.out = out;
		this.env = new Environment(Optional.empty());

		Schemeify.register(new Builtins(), this);
		Schemeify.register(this, this);
		Schemeify.register(SOutputPort.of(out), this);
		Schemeify.register(SInputPort.of(in), this);

	}

	public static Scheme headless() {
		return new Scheme(new InputStream() {
			@Override
			public int read() {
				return -1;  // end of stream
			}
		}, new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				// no-op
			}
		});
	}

	public InputStream getIn() {
		return in;
	}

	public OutputStream getOut() {
		return out;
	}

	public Environment getEnv() {
		return env;
	}
	
	
	/** Evaluates a string as a Scheme expression. **/
	public Optional<SValue> loadString(String s) {
		Optional<SValue> result  = Optional.empty();
		try(var reader = new Scanner(s)){
			var p = new Parser(reader);

			while(true) {
				var expr = p.readExpression();
				if(expr == SNull.EOF) {
					break;
				}
				result = Optional.ofNullable(Scheme.eval(env, expr));
			}
		}
		return result;
	}
	
	/** Evaluates the contents of a file as a Scheme expression. **/
	@Procedure(name="load")
	public void loadFile(String s) {
		var path = Paths.get(s);
		
		try(var reader = new Scanner(path)) {
			var p = new Parser(reader);

			while(true) {
				var expr = p.readExpression();
				if(expr == SNull.EOF) {
					break;
				}
				Scheme.eval(env, expr);
			}
		} catch (IOException e) {
			throw new EvaluationException(e);
		}
	}


	/** Starts a read-evaluate-print-loop. THIS FUNCTION DOES NOT RETURN. */
	public void repl() {
		try (var reader = new Scanner(in); var writer = new PrintStream(out)) {
			
			var p = new Parser(reader);

			writer.println("Embedded Scheme v0.1");
			writer.println(String.format("Now with %d builtins!", env.size()));
			writer.println(String.format("MIT License", env.size()));
			writer.println();

			while (true) {
				// print the prompt
				writer.print("es> ");
				writer.flush();

				try {
					var expr = p.readExpression();
					var result = Scheme.eval(env, expr);
					writer.println(";Value: " + result.toString());
				} catch (final EvaluationException e) {
					writer.println(String.format(";<error> %s", e.toString()));
					e.printStackTrace(writer);
				}
			}
		}
	}

	@ExampleCall(in = "(or (= 2 2) (> 2 1))", out = "#t")
	@ExampleCall(in = "(or (= 2 2) (< 2 1))", out = "#t")
	@ExampleCall(in = "(or #f #f #f)", out = "#f")
	@ExampleCall(in = "(or (memq 'b '(a b c)) (/ 3 0))", out = "(b c)")
	@ExampleCall(in = "(and (= 2 2) (> 2 1))", out = "#t")
	@ExampleCall(in = "(and (= 2 2) (< 2 1))", out = "#f")
	@ExampleCall(in = "(and 1 2 'c '(f g))", out = "(f g)")
	@ExampleCall(in = "(and)", out = "#t")
	@ExampleCall(in = "((lambda (x) (+ x x)) 3)", out = "6")
	@ExampleCall(in = "((lambda () 42))", out = "42")
	@ExampleCall(in = "((lambda () 1 2 3))", out = "3")
	@ExampleCall(in = "((lambda v (apply + v)) 1 2 3 4 5)", out = "15")
	@ExampleCall(in = "(let ((x 2) (y 3)) (let ((x 7) (z (+ x y))) (* z x)))", out = "35")
	@ExampleCall(in = "(let ((x 2) (y 3)) (let* ((x 7) (z (+ x y))) (* z x)))", out = "70")
	@ExampleCall(in="(do ((vec (make-vector 5))\n" + 
			"           (i 0 (+ i 1)))\n" +
			"           ((= i 5) vec)\n" +
			"           (vector-set! vec i i))", out = "#(0 1 2 3 4)")
	@ExampleCall(in="(let ((x '(1 3 5 7 9)))\n" + 
			"        (do ((x x (cdr x))\n" +
			"        (sum 0 (+ sum (car x))))\n" +
			"        ((null? x) sum)))", out = "25")
	public static SValue eval(final Environment env, final SValue node) throws EvaluationException {
		Objects.requireNonNull(node, "node must not be null");
		
		return VM.eval(env, node);
	}
}
