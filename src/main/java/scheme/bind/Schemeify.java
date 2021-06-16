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

package main.java.scheme.bind;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

import main.java.scheme.Scheme;
import main.java.scheme.types.SSymbol;

/** Utilities to bind Java methods as Scheme functions via annotation. */
public class Schemeify {

	private Schemeify() {
		// No-op, utility class constructor.
	}

	/**
	 * Registers all methods on input with {@link Procedure} and {@link Init}
	 * annotations in the Scheme environment.
	 */
	public static void register(final Object input, final Scheme scm) {
		Objects.requireNonNull(input);
		Objects.requireNonNull(scm);

		Arrays.stream(input.getClass().getMethods())
				.filter(m -> m.isAnnotationPresent(Procedure.class))
				.forEach(method -> {
					var procedure = method.getAnnotation(Procedure.class);

					try {
						var binding = new BindingOperator(procedure, input, method);
						scm.getEnv().define(SSymbol.of(procedure.name()), binding);
					} catch (RuntimeException e) {
								var className = method.getDeclaringClass().getCanonicalName();
								var methodName = method.getName();
								var errorMessage = String.format("error registering %s from %s::%s", procedure.name(),
										className, methodName);

								throw new RuntimeException(errorMessage, e);
					}
				});

		Arrays.stream(input.getClass().getMethods())
				.filter(m -> m.isAnnotationPresent(Init.class))
				.forEach(method -> {
					try {
						method.invoke(input, scm);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								var className = method.getDeclaringClass().getCanonicalName();
								var methodName = method.getName();
								var errorMessage = String.format("error running init %s::%s", className, methodName);

								throw new RuntimeException(errorMessage, e);
					}
				});
	}
}
