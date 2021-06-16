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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Function;

import main.java.scheme.Environment;
import main.java.scheme.EvaluationException;
import main.java.scheme.types.SProcedure;
import main.java.scheme.types.SValue;

/**
 * BindingOperator adapts Java methods to Scheme procedures.
 */
class BindingOperator extends SProcedure {
	private final Procedure annotation;
	private final Object parent;
	private final Method method;

	private boolean injectEnv;
	private Function<SValue, Object>[] argUnboxers;
	private Function<SValue, Object> varadicUnboxer;
	private Class<?> varadicComponentType;
	private Function<Object, SValue> resultBoxer;
	
	@SuppressWarnings("unchecked")
	public BindingOperator(Procedure annotation, Object parent, Method method) {
		this.annotation = annotation;
		this.parent = parent;
		this.method = method;
		
		// Sanity check
		if(!isVaradic() && annotation.maxVaradicArgs() >= 0) {
			throw new BindException("can't specify maxVaradicArgs on a non-varadic method");
		}
		
		// Initialize to empty values
		this.injectEnv = false;
		this.varadicUnboxer = val -> {throw new BindException("unexpected varadic expansion");}; 
		
		
		// Set up the boxers and unboxers.
		resultBoxer = Caster.toValue(method.getReturnType());
		
		var unboxers = new LinkedList<Function<SValue, Object>>();
		var params = method.getParameterTypes();
		for(int i = 0; i < params.length; i++) {
			var param = params[i];
			// env can optionally be requested as the first
			// argument and won't have any effect on the formal
			// parameters
			if(i == 0 && param.isAssignableFrom(Environment.class)){
				injectEnv = true;
				continue;
			}
			
			if(i == params.length - 1 && isVaradic()) {
				// If the method is varadic, the final param
				// should be an array. Fetch the base type and
				// convert for that.
				var componentType = param.getComponentType();
				// sanity check
				Objects.requireNonNull(componentType, "method claims to be varadic, but last param isn't array");
				varadicUnboxer = Caster.fromValue(componentType);
				varadicComponentType = componentType;
				continue;
			}
			
			unboxers.add(Caster.fromValue(param));
		}
		
		argUnboxers = unboxers.toArray(Function[]::new);
	}
	
	private boolean isVaradic() {
		return method.isVarArgs();
	}
	
	private int minArgs() {
		return argUnboxers.length;
	}
	
	private int maxArgs() {
		if(!isVaradic()) {
			return minArgs();
		}
		
		if(annotation.maxVaradicArgs() < 0) {
			return Integer.MAX_VALUE;
		}
		
		return minArgs() + annotation.maxVaradicArgs();
	}
	
	private void assertArgsLengthMatch(int argCount) throws EvaluationException {
		var min = minArgs();
		var max = maxArgs();
		
		if(argCount < min || argCount > max) {
			var name = annotation.name();
			
			if(min == max) {
				throw EvaluationException.format("%s expects %d arg(s)", name, min);
			}
			
			if(max == Integer.MAX_VALUE) {
				throw EvaluationException.format("%s expects at least %d args", name, min);
			}
			
			throw EvaluationException.format("%s expects between %d and %d args", name, min, max);
		}
	}

	@Override
	public SValue eval(Environment env, SValue[] args) throws EvaluationException {
		assertArgsLengthMatch(args.length);
				
		try {
			var boundArgs = new LinkedList<Object>();
			if(injectEnv) {
				boundArgs.add(env);
			}
			
			for(int i = 0; i < argUnboxers.length; i++) {
				var unboxer = argUnboxers[i];
				boundArgs.add(unboxer.apply(args[i]));
			}
			
			if(isVaradic()) {
				var size = args.length - argUnboxers.length;
				var varadicParam = Array.newInstance(varadicComponentType, size);
				for(int i = 0; i < size; i++) {
					Array.set(varadicParam, i, varadicUnboxer.apply(args[argUnboxers.length + i]));
				}
				boundArgs.add(varadicParam);
			}
						
			var returnValue = method.invoke(parent, boundArgs.toArray());
			return resultBoxer.apply(returnValue);
			
		} catch(InvocationTargetException e) {
			// unwrap caught exceptions
			if(e.getCause() instanceof EvaluationException) {
				throw (EvaluationException)e.getCause();
			}
			
	        throw new EvaluationException(String.format("couldn't invoke %s: %s", toString(), e.getCause().toString()), e.getCause());
		} catch (IllegalAccessException e) {
	        throw EvaluationException.format("couldn't invoke %s: %s", toString(), e.toString());
	    }
	}
	
	public String toScheme() {
		return String.format("#[bound procedure: %s]", method.toGenericString());
	}
}
