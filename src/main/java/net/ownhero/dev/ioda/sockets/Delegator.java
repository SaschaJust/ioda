/*******************************************************************************
 * Copyright 2012 Kim Herzig, Sascha Just
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package net.ownhero.dev.ioda.sockets;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import net.ownhero.dev.kanuni.conditions.Condition;

/**
 * The Class Delegator.
 * 
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 */
public class Delegator {
	
	/**
	 * The Class DelegatorMethodFinder.
	 */
	public class DelegatorMethodFinder {
		
		/** The method. */
		private final Method method;
		
		/**
		 * Instantiates a new delegator method finder.
		 * 
		 * @param methodName
		 *            the method name
		 * @param parameterTypes
		 *            the parameter types
		 */
		public DelegatorMethodFinder(final String methodName, final Class<?>... parameterTypes) {
			try {
				this.method = Delegator.this.superclass.getDeclaredMethod(methodName, parameterTypes);
			} catch (final RuntimeException e) {
				throw e;
			} catch (final Exception e) {
				throw new DelegationException(e);
			}
		}
		
		/**
		 * Invoke.
		 * 
		 * @param <T>
		 *            the generic type
		 * @param parameters
		 *            the parameters
		 * @return the t
		 */
		public <T> T invoke(final Object... parameters) {
			@SuppressWarnings ("unchecked")
			final T t = (T) invoke0(this.method, parameters);
			return t;
		}
	}
	
	/** The source. */
	private final Object source;
	
	/** The delegate. */
	private final Object delegate;
	
	/** The superclass. */
	private final Class  superclass;
	
	/**
	 * Instantiates a new delegator.
	 * 
	 * @param source
	 *            the source
	 * @param superclass
	 *            the superclass
	 * @param delegate
	 *            the delegate
	 */
	public Delegator(final Object source, final Class superclass, final Object delegate) {
		this.source = source;
		this.superclass = superclass;
		this.delegate = delegate;
	}
	
	/**
	 * Instantiates a new delegator.
	 * 
	 * @param source
	 *            the source
	 * @param superclass
	 *            the superclass
	 * @param delegateClassName
	 *            the delegate class name
	 */
	public Delegator(final Object source, final Class superclass, final String delegateClassName) {
		try {
			this.source = source;
			this.superclass = superclass;
			final Class implCl = Class.forName(delegateClassName);
			final Constructor delegateConstructor = implCl.getDeclaredConstructor();
			delegateConstructor.setAccessible(true);
			this.delegate = delegateConstructor.newInstance();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new DelegationException("Could not make delegate object", e);
		}
	}
	
	/**
	 * Convert primitive class.
	 * 
	 * @param primitive
	 *            the primitive
	 * @return the class
	 */
	private Class<?> convertPrimitiveClass(final Class<?> primitive) {
		if (primitive.isPrimitive()) {
			if (primitive == int.class) {
				return Integer.class;
			}
			if (primitive == boolean.class) {
				return Boolean.class;
			}
			if (primitive == float.class) {
				return Float.class;
			}
			if (primitive == long.class) {
				return Long.class;
			}
			if (primitive == double.class) {
				return Double.class;
			}
			if (primitive == short.class) {
				return Short.class;
			}
			if (primitive == byte.class) {
				return Byte.class;
			}
			if (primitive == char.class) {
				return Character.class;
			}
		}
		return primitive;
	}
	
	/**
	 * Delegate to.
	 * 
	 * @param methodName
	 *            the method name
	 * @param parameters
	 *            the parameters
	 * @return the delegator method finder
	 */
	public DelegatorMethodFinder delegateTo(final String methodName,
	                                        final Class<?>... parameters) {
		return new DelegatorMethodFinder(methodName, parameters);
	}
	
	/**
	 * Extract method name.
	 * 
	 * @return the string
	 */
	private String extractMethodName() {
		final Throwable t = new Throwable();
		final String methodName = t.getStackTrace()[2].getMethodName();
		return methodName;
	}
	
	/**
	 * Find method.
	 * 
	 * @param methodName
	 *            the method name
	 * @param args
	 *            the args
	 * @return the method
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 */
	private Method findMethod(final String methodName,
	                          final Object[] args) throws NoSuchMethodException {
		final Class<?> clazz = this.superclass;
		if (args.length == 0) {
			return clazz.getDeclaredMethod(methodName);
		}
		Method match = null;
		next: for (final Method method : clazz.getDeclaredMethods()) {
			if (method.getName().equals(methodName)) {
				final Class<?>[] classes = method.getParameterTypes();
				if (classes.length == args.length) {
					for (int i = 0; i < classes.length; i++) {
						Class<?> argType = classes[i];
						argType = convertPrimitiveClass(argType);
						if (!argType.isInstance(args[i])) {
							continue next;
						}
					}
					if (match == null) {
						match = method;
					} else {
						throw new DelegationException("Duplicate matches");
					}
				}
			}
		}
		if (match != null) {
			return match;
		}
		throw new DelegationException("Could not find method: " + methodName);
	}
	
	/**
	 * Gets the simple name of the class.
	 * 
	 * @return the simple name of the class.
	 */
	public final String getHandle() {
		// PRECONDITIONS
		
		final StringBuilder builder = new StringBuilder();
		
		try {
			final LinkedList<Class<?>> list = new LinkedList<Class<?>>();
			Class<?> clazz = getClass();
			list.add(clazz);
			
			while ((clazz = clazz.getEnclosingClass()) != null) {
				list.addFirst(clazz);
			}
			
			for (final Class<?> c : list) {
				if (builder.length() > 0) {
					builder.append('.');
				}
				
				builder.append(c.getSimpleName());
			}
			
			return builder.toString();
		} finally {
			// POSTCONDITIONS
			Condition.notNull(builder,
			                  "Local variable '%s' in '%s:%s'.", "builder", getClass().getSimpleName(), "getHandle"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	/**
	 * Gets the object.
	 * 
	 * @return the object
	 */
	public Object getObject() {
		// PRECONDITIONS
		
		try {
			return this.delegate;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Invoke.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param args
	 *            the args
	 * @return the t
	 */
	public final <T> T invoke(final Object... args) {
		try {
			final String methodName = extractMethodName();
			final Method method = findMethod(methodName, args);
			@SuppressWarnings ("unchecked")
			final T t = (T) invoke0(method, args);
			return t;
		} catch (final NoSuchMethodException e) {
			throw new DelegationException(e);
		}
	}
	
	/**
	 * Invoke0.
	 * 
	 * @param method
	 *            the method
	 * @param args
	 *            the args
	 * @return the object
	 */
	private Object invoke0(final Method method,
	                       final Object[] args) {
		try {
			writeFields(this.superclass, this.source, this.delegate);
			method.setAccessible(true);
			final Object result = method.invoke(this.delegate, args);
			writeFields(this.superclass, this.delegate, this.source);
			return result;
		} catch (final RuntimeException e) {
			throw e;
		} catch (final InvocationTargetException e) {
			throw new DelegationException(e.getCause());
		} catch (final Exception e) {
			throw new DelegationException(e);
		}
	}
	
	/**
	 * Write fields.
	 * 
	 * @param clazz
	 *            the clazz
	 * @param from
	 *            the from
	 * @param to
	 *            the to
	 * @throws Exception
	 *             the exception
	 */
	private void writeFields(final Class clazz,
	                         final Object from,
	                         final Object to) throws Exception {
		for (final Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			field.set(to, field.get(from));
		}
	}
}
