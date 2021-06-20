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

import scheme.Environment;
import scheme.EvaluationException;
import scheme.Scheme;
import scheme.bind.Init;
import scheme.bind.Procedure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class SInputPort extends SPort{

	private static final SSymbol CURRENT_INPUT_PORT = SSymbol.unique("in");

	private final InputStream in;

	private SInputPort(final InputStream out) {
		this.in = Objects.requireNonNull(out);
	}

	public static SInputPort of(final InputStream in) {
		return new SInputPort(in);
	}

	@Init
	public static void init(Scheme scm) {
		setCurrentInputPort(scm.getEnv(), of(scm.getIn()));
	}

	@Procedure(name = "current-input-port")
	public static SInputPort getCurrentInputPort(Environment env) {
		return env.lookupSymbol(CURRENT_INPUT_PORT).getPort().getInputPort();
	}

	public static void setCurrentInputPort(Environment env, SInputPort port) {
		env.define(CURRENT_INPUT_PORT, port);
	}

	public boolean isInputPort() {
		return true;
	}
	
	public SInputPort getInputPort() {
		return this;
	}

	@Override
	public void close() {
		try {
			in.close();
		} catch (IOException e) {
			throw new EvaluationException(e);
		}
	}

	@Override
	public String toScheme() {
		return "#[input-port]";
	}
}
