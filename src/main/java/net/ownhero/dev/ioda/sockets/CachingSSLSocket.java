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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import net.ownhero.dev.kanuni.conditions.Condition;

/**
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 * 
 */
public class CachingSSLSocket extends SSLSocket {
	
	public CachingSSLSocket() {
		super();
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated constructor stub
			System.err.println(getHandle());
		} finally {
			// POSTCONDITIONS
		}
	}
	
	public CachingSSLSocket(final InetAddress address, final int port) throws IOException {
		super(address, port);
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated constructor stub
			System.err.println(getHandle());
		} finally {
			// POSTCONDITIONS
		}
	}
	
	public CachingSSLSocket(final InetAddress address, final int port, final InetAddress clientAddress,
	        final int clientPort) throws IOException {
		super(address, port, clientAddress, clientPort);
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated constructor stub
			System.err.println(getHandle());
		} finally {
			// POSTCONDITIONS
		}
	}
	
	public CachingSSLSocket(final String host, final int port) throws IOException, UnknownHostException {
		super(host, port);
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated constructor stub
			System.err.println(getHandle());
		} finally {
			// POSTCONDITIONS
		}
	}
	
	public CachingSSLSocket(final String host, final int port, final InetAddress clientAddress, final int clientPort)
	        throws IOException, UnknownHostException {
		super(host, port, clientAddress, clientPort);
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated constructor stub
			System.err.println(getHandle());
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#addHandshakeCompletedListener(javax.net.ssl.HandshakeCompletedListener)
	 */
	@Override
	public void addHandshakeCompletedListener(final HandshakeCompletedListener arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getEnabledCipherSuites()
	 */
	@Override
	public String[] getEnabledCipherSuites() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return null;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getEnabledProtocols()
	 */
	@Override
	public String[] getEnabledProtocols() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return null;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getEnableSessionCreation()
	 */
	@Override
	public boolean getEnableSessionCreation() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return false;
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
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getNeedClientAuth()
	 */
	@Override
	public boolean getNeedClientAuth() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return false;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getSession()
	 */
	@Override
	public SSLSession getSession() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return null;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getSupportedCipherSuites()
	 */
	@Override
	public String[] getSupportedCipherSuites() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return null;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getSupportedProtocols()
	 */
	@Override
	public String[] getSupportedProtocols() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return null;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getUseClientMode()
	 */
	@Override
	public boolean getUseClientMode() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return false;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#getWantClientAuth()
	 */
	@Override
	public boolean getWantClientAuth() {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
			return false;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#removeHandshakeCompletedListener(javax.net.ssl.HandshakeCompletedListener)
	 */
	@Override
	public void removeHandshakeCompletedListener(final HandshakeCompletedListener arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#setEnabledCipherSuites(java.lang.String[])
	 */
	@Override
	public void setEnabledCipherSuites(final String[] arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#setEnabledProtocols(java.lang.String[])
	 */
	@Override
	public void setEnabledProtocols(final String[] arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#setEnableSessionCreation(boolean)
	 */
	@Override
	public void setEnableSessionCreation(final boolean arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#setNeedClientAuth(boolean)
	 */
	@Override
	public void setNeedClientAuth(final boolean arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#setUseClientMode(boolean)
	 */
	@Override
	public void setUseClientMode(final boolean arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#setWantClientAuth(boolean)
	 */
	@Override
	public void setWantClientAuth(final boolean arg0) {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.net.ssl.SSLSocket#startHandshake()
	 */
	@Override
	public void startHandshake() throws IOException {
		// PRECONDITIONS
		
		try {
			// TODO Auto-generated method stub
		} finally {
			// POSTCONDITIONS
		}
	}
}
