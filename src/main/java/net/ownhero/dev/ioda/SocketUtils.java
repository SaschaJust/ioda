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
package net.ownhero.dev.ioda;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.util.LinkedList;

import javax.net.ssl.SSLSocketFactory;

import net.ownhero.dev.ioda.sockets.CachingSSLSocketFactory;
import net.ownhero.dev.ioda.sockets.CachingSocketImplFactory;
import net.ownhero.dev.kanuni.annotations.file.WritableDirectory;
import net.ownhero.dev.kanuni.annotations.simple.NotNull;
import net.ownhero.dev.kanuni.conditions.Condition;
import net.ownhero.dev.kisa.Logger;

/**
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 * 
 */
public class SocketUtils {
	
	public static final void disableSSLSecurity() {
		// stub
	}
	
	public static final void enableCaching(@NotNull @WritableDirectory final File directory) {
		try {
			Security.setProperty("ssl.SocketFactory.provider", CachingSSLSocketFactory.class.getCanonicalName());
			ServerSocket.setSocketFactory(new CachingSocketImplFactory(directory));
			Socket.setSocketImplFactory(new CachingSocketImplFactory(directory));
		} catch (final IOException e) {
			if (Logger.logError()) {
				Logger.error(e);
			}
			
		};
	}
	
	public static void main(final String[] args) {
		disableSSLSecurity();
		enableCaching(new File("/tmp"));
		Socket s;
		try {
			s = SSLSocketFactory.getDefault().createSocket("own-hero.net", 443);
			System.err.println(s.getPort());
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			if (Logger.logError()) {
				Logger.error(e);
			}
			
		}
		
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
}
