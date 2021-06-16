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

package main.java.scheme.types;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import main.java.scheme.Environment;
import main.java.scheme.EvaluationException;
import main.java.scheme.Scheme;
import main.java.scheme.bind.Init;
import main.java.scheme.bind.Procedure;

public class SOutputPort extends SPort {

	private static final SSymbol CURRENT_OUTPUT_PORT = SSymbol.unique("out");

	private final OutputStream out;

	private SOutputPort(final OutputStream out) {
		Objects.requireNonNull(out);
		this.out = out;
	}

	public static final SOutputPort systemOut() {
		return new SOutputPort(System.out);
	}

	public boolean isOutputPort() {
		return true;
	}

	public SOutputPort getOutputPort() {
		return this;
	}

	@Init
	public static void init(Scheme scm) {
		setCurrentOutputPort(scm.getEnv(), systemOut());
	}

	@Procedure(name = "current-output-port")
	public static SOutputPort getCurrentOutputPort(Environment env) {
		return env.lookupSymbol(CURRENT_OUTPUT_PORT).getPort().getOutputPort();
	}

	public static void setCurrentOutputPort(Environment env, SOutputPort port) {
		env.define(CURRENT_OUTPUT_PORT, port);
	}

	public String toScheme() {
		return "#[output-port]";
	}

	@Override
	public void close() {
		try {
			out.close();
		} catch (IOException e) {
			throw new EvaluationException(e);
		}
	}

	/**
	 * Writes an OS specific end-of-line character to the port.
	 */
	public void newline() {
		writeString(System.lineSeparator());
	}

	@Procedure(name = "newline", maxVaradicArgs = 1)
	public static void newline(Environment env, SOutputPort... ports) {
		portOrCurrent(env, ports).newline();
	}

	/**
	 * Writes a character to the output.
	 * 
	 * @param c the character.
	 */
	public void writeChar(char c) {
		writeString(String.valueOf(c));
	}

	@Procedure(name = "write-char", maxVaradicArgs = 1)
	public static void writeChar(Environment env, SCharacter c, SOutputPort... ports) {
		portOrCurrent(env, ports).writeChar(c.toChar());
	}

	public void write(SValue value) {
		writeString(value.toScheme());
	}

	@Procedure(name = "write", maxVaradicArgs = 1)
	public static void write(Environment env, SValue value, SOutputPort... ports) {
		portOrCurrent(env, ports).write(value);
	}


	public void display(SValue value) {
		writeString(value.toString());
	}

	@Procedure(name = "display", maxVaradicArgs = 1)
	public static void display(Environment env, SValue value, SOutputPort... ports) {
		portOrCurrent(env, ports).display(value);
	}

	private void writeString(String s) {
		try {
			out.write(s.toString().getBytes());
		} catch (IOException e) {
			throw new EvaluationException(e);
		}
	}

	private static SOutputPort portOrCurrent(Environment env, SOutputPort[] ports) {
		if (ports.length == 0) {
			return getCurrentOutputPort(env);
		}
		return ports[0];
	}
}
