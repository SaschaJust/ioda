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

import java.io.File;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.LinkedList;

import net.ownhero.dev.kanuni.annotations.file.WritableDirectory;
import net.ownhero.dev.kanuni.annotations.simple.NotNull;
import net.ownhero.dev.kanuni.conditions.Condition;
import net.ownhero.dev.kisa.Logger;

/**
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 * 
 */
public class CachingSocketImplFactory implements SocketImplFactory {
	
	private final File   directory;
	private final String handle;
	
	/**
	 * 
	 */
	public CachingSocketImplFactory(final @NotNull @WritableDirectory File directory) {
		// PRECONDITIONS
		
		try {
			final StringBuilder builder = new StringBuilder();
			
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
			
			this.handle = builder.toString();
			
			if (Logger.logTrace()) {
				Logger.trace("Initialized '%s'.", getHandle());
			}
			
			this.directory = directory;
		} finally {
			// POSTCONDITIONS
			Condition.notNull(this.handle, "Field '%s' in '%s'.", "handle", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.directory, "Field '%s' in '%s'.", "directory", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImplFactory#createSocketImpl()
	 */
	@Override
	public SocketImpl createSocketImpl() {
		// PRECONDITIONS
		
		try {
			return new CachingSocketImpl(this.directory);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Gets the simple name of the class.
	 * 
	 * @return the simple name of the class.
	 */
	public final String getHandle() {
		// PRECONDITIONS
		
		try {
			return this.handle;
		} finally {
			// POSTCONDITIONS
		}
	}
}
