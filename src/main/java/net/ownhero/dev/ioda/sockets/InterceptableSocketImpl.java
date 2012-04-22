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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.util.LinkedList;

import net.ownhero.dev.kanuni.annotations.simple.NotNull;
import net.ownhero.dev.kanuni.conditions.Condition;
import net.ownhero.dev.kisa.Logger;

/**
 * The Class CachingSocketImpl.
 * 
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 */
public abstract class InterceptableSocketImpl extends SocketImpl {
	
	/** The handle. */
	private String handle;
	
	/** The internal socket (instance of SocksSocketImpl). */
	private Object internalSocket;
	
	/** The accept. */
	private Method theAccept;
	
	/** The available. */
	private Method theAvailable;
	
	/** The bind. */
	private Method theBind;
	
	/** The close. */
	private Method theClose;
	
	/** The connect inet address. */
	private Method theConnectInetAddress;
	
	/** The connect socket address. */
	private Method theConnectSocketAddress;
	
	/** The connect string. */
	private Method theConnectString;
	
	/** The create. */
	private Method theCreate;
	
	/** The get input stream. */
	private Method theGetInputStream;
	
	/** The get option. */
	private Method theGetOption;
	
	/** The get output stream. */
	private Method theGetOutputStream;
	
	/** The listen. */
	private Method theListen;
	
	/** The send urgent data. */
	private Method theSendUrgentData;
	
	/** The method setOption of SocksSocketImpl inherited from AbstractPlainSocketImpl. */
	private Method theSetOption;
	
	/** The supports urgent data. */
	private Method theSupportsUrgentData;
	
	/**
	 * Instantiates a new caching socket impl.
	 * 
	 * @param directory
	 *            the directory
	 */
	public InterceptableSocketImpl() {
		// PRECONDITIONS
		
		try {
			final StringBuilder builder = new StringBuilder();
			
			final LinkedList<Class<?>> list = new LinkedList<Class<?>>();
			Class<?> me = getClass();
			list.add(me);
			
			while ((me = me.getEnclosingClass()) != null) {
				list.addFirst(me);
			}
			
			for (final Class<?> c : list) {
				if (builder.length() > 0) {
					builder.append('.');
				}
				
				builder.append(c.getSimpleName());
			}
			
			this.handle = builder.toString();
			
			if (Logger.logTrace()) {
				Logger.trace("Instantiating '%s'.", getHandle());
			}
			
			try {
				final Class<?> theAbstractPlainSocketImpl = Class.forName("java.net.AbstractPlainSocketImpl");
				final Class<?> theSocksSocketImplClass = Class.forName("java.net.SocksSocketImpl");
				
				// fetch: setOption(int, Object)
				this.theSetOption = theAbstractPlainSocketImpl.getDeclaredMethod("setOption", int.class, Object.class);
				this.theSetOption.setAccessible(true);
				
				// fetch: getOption(int)
				this.theGetOption = theAbstractPlainSocketImpl.getDeclaredMethod("getOption", int.class);
				this.theGetOption.setAccessible(true);
				
				// fetch: accept(SocketImpl)
				this.theAccept = theAbstractPlainSocketImpl.getDeclaredMethod("accept", SocketImpl.class);
				this.theAccept.setAccessible(true);
				
				// fetch: available()
				this.theAvailable = theAbstractPlainSocketImpl.getDeclaredMethod("available");
				this.theAvailable.setAccessible(true);
				
				// fetch: bind(InetAddress, int)
				this.theBind = theAbstractPlainSocketImpl.getDeclaredMethod("bind", InetAddress.class, int.class);
				this.theBind.setAccessible(true);
				
				// fetch: close()
				this.theClose = theSocksSocketImplClass.getDeclaredMethod("close");
				this.theClose.setAccessible(true);
				
				// fetch: connect(InetAddress, int)
				this.theConnectInetAddress = theAbstractPlainSocketImpl.getDeclaredMethod("connect", InetAddress.class,
				                                                                          int.class);
				this.theConnectInetAddress.setAccessible(true);
				
				// fetch: connect(SocketAddress, int)
				this.theConnectSocketAddress = theAbstractPlainSocketImpl.getDeclaredMethod("connect",
				                                                                            SocketAddress.class,
				                                                                            int.class);
				this.theConnectSocketAddress.setAccessible(true);
				
				// fetch: connect(String, int)
				this.theConnectString = theAbstractPlainSocketImpl.getDeclaredMethod("connect", String.class, int.class);
				this.theConnectString.setAccessible(true);
				
				// fetch: create(boolean)
				this.theCreate = theAbstractPlainSocketImpl.getDeclaredMethod("create", boolean.class);
				this.theCreate.setAccessible(true);
				
				// fetch: getInputStream()
				this.theGetInputStream = theAbstractPlainSocketImpl.getDeclaredMethod("getInputStream");
				this.theGetInputStream.setAccessible(true);
				
				// fetch: getOutputStream()
				this.theGetOutputStream = theAbstractPlainSocketImpl.getDeclaredMethod("getOutputStream");
				this.theGetOutputStream.setAccessible(true);
				
				// fetch: listen(int)
				this.theListen = theAbstractPlainSocketImpl.getDeclaredMethod("listen", int.class);
				this.theListen.setAccessible(true);
				
				// fetch: sendUrgendData(int)
				this.theSendUrgentData = theAbstractPlainSocketImpl.getDeclaredMethod("sendUrgentData", int.class);
				this.theSendUrgentData.setAccessible(true);
				
				// fetch: supportsUrgendData()
				this.theSupportsUrgentData = theAbstractPlainSocketImpl.getDeclaredMethod("supportsUrgentData");
				this.theSupportsUrgentData.setAccessible(true);
				
				final Class<?> socketImplClass = Class.forName("java.net.SocksSocketImpl");
				final Constructor<?> constructor = socketImplClass.getDeclaredConstructor();
				constructor.setAccessible(true);
				this.internalSocket = constructor.newInstance();
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
			        | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				if (Logger.logWarn()) {
					Logger.warn(e);
				}
			}
		} finally {
			// POSTCONDITIONS
			Condition.notNull(this.internalSocket, "Field '%s' in '%s'.", "internalSocket", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theSetOption, "Field '%s' in '%s'.", "theSetOption", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theAccept, "Field '%s' in '%s'.", "theAccept", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theAvailable, "Field '%s' in '%s'.", "theAvailable", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theBind, "Field '%s' in '%s'.", "theBind", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theClose, "Field '%s' in '%s'.", "theClose", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theConnectInetAddress, "Field '%s' in '%s'.", "theConnectInetAddress", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theConnectSocketAddress,
			                  "Field '%s' in '%s'.", "theConnectSocketAddress", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theConnectString, "Field '%s' in '%s'.", "theConnectString", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theCreate, "Field '%s' in '%s'.", "theCreate", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theGetInputStream, "Field '%s' in '%s'.", "theGetInputStream", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theGetOutputStream, "Field '%s' in '%s'.", "theGetOutputStream", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theListen, "Field '%s' in '%s'.", "theListen", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theSendUrgentData, "Field '%s' in '%s'.", "theSendUrgentData", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theSupportsUrgentData, "Field '%s' in '%s'.", "theSupportsUrgentData", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.theGetOption, "Field '%s' in '%s'.", "theGetOption", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
			Condition.notNull(this.handle, "Field '%s' in '%s'.", "handle", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#accept(java.net.SocketImpl)
	 */
	@Override
	public void accept(final SocketImpl socketImpl) throws IOException {
		// PRECONDITIONS
		
		try {
			acceptInternal(socketImpl);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Accept (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param socketImpl
	 *            the socket impl
	 */
	protected final void acceptInternal(final SocketImpl socketImpl) {
		// PRECONDITIONS
		Condition.notNull(this.theAccept, "Field '%s' in '%s'.", "theAccept", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s'.",
			             getHandle(), "accept", socketImpl);
		}
		
		try {
			invoke(this.theAccept, socketImpl);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#available()
	 */
	@Override
	protected int available() throws IOException {
		// PRECONDITIONS
		
		try {
			return availableInternal();
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Available (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @return the int
	 */
	protected final int availableInternal() {
		// PRECONDITIONS
		Condition.notNull(this.theAvailable, "Field '%s' in '%s'.", "theAvailable", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl].",
			             getHandle(), "available");
		}
		
		try {
			return invoke(this.theAvailable);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#bind(java.net.InetAddress, int)
	 */
	@Override
	public void bind(final InetAddress host,
	                 final int port) throws IOException {
		// PRECONDITIONS
		
		try {
			bindInternal(host, port);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Bind (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 */
	protected final void bindInternal(final InetAddress host,
	                                  final int port) {
		// PRECONDITIONS
		Condition.notNull(this.theBind, "Field '%s' in '%s'.", "theBind", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s', '%s'.",
			             getHandle(), "bind", host, port);
		}
		
		try {
			invoke(this.theBind, host, port);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#close()
	 */
	@Override
	public void close() throws IOException {
		// PRECONDITIONS
		
		try {
			closeInternal();
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Close (internal call to the underlying SocksSocketImpl instance).
	 */
	protected final void closeInternal() {
		// PRECONDITIONS
		Condition.notNull(this.theClose, "Field '%s' in '%s'.", "theClose", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl].",
			             getHandle(), "close");
		}
		
		try {
			invoke(this.theClose);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#connect(java.net.InetAddress, int)
	 */
	@Override
	public void connect(final InetAddress address,
	                    final int port) throws IOException {
		// PRECONDITIONS
		
		try {
			connectInternal(address, port);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#connect(java.net.SocketAddress, int)
	 */
	@Override
	public void connect(final SocketAddress address,
	                    final int timeout) throws IOException {
		// PRECONDITIONS
		
		try {
			connectInternal(address, timeout);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#connect(java.lang.String, int)
	 */
	@Override
	public void connect(final String host,
	                    final int port) throws IOException {
		// PRECONDITIONS
		
		try {
			connectInternal(host, port);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Connect (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param address
	 *            the address
	 * @param i
	 *            the i
	 */
	protected final void connectInternal(final InetAddress address,
	                                     final int i) {
		// PRECONDITIONS
		Condition.notNull(this.theConnectInetAddress, "Field '%s' in '%s'.", "theConnectInetAddress", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s', '%s'.",
			             getHandle(), "connect", address, i);
		}
		
		try {
			invoke(this.theConnectInetAddress, address, i);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Connect (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param address
	 *            the address
	 * @param timeout
	 *            the timeout
	 */
	protected final void connectInternal(final SocketAddress address,
	                                     final int timeout) {
		// PRECONDITIONS
		Condition.notNull(this.theConnectSocketAddress, "Field '%s' in '%s'.", "theConnectSocketAddress", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s', '%s'.",
			             getHandle(), "connect", address, timeout);
		}
		
		try {
			invoke(this.theConnectSocketAddress, address, timeout);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Connect (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 */
	protected final void connectInternal(final String host,
	                                     final int port) {
		// PRECONDITIONS
		Condition.notNull(this.theConnectString, "Field '%s' in '%s'.", "theConnectString", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s', '%s'.",
			             getHandle(), "connect", host, port);
		}
		
		try {
			invoke(this.theConnectString, host, port);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#create(boolean)
	 */
	@Override
	public void create(final boolean stream) throws IOException {
		// PRECONDITIONS
		
		try {
			createInternal(stream);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Creates the (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param stream
	 *            the stream
	 */
	protected final void createInternal(final boolean stream) {
		// PRECONDITIONS
		Condition.notNull(this.theCreate, "Field '%s' in '%s'.", "theCreate", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s'.",
			             getHandle(), "create", stream);
		}
		
		try {
			invoke(this.theCreate, stream);
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
		Condition.notNull(this.handle, "Field '%s' in '%s'.", "handle", getClass().getSimpleName()); //$NON-NLS-1$ //$NON-NLS-2$
		
		try {
			return this.handle;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		// PRECONDITIONS
		
		try {
			return getInputStreamInternal();
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Gets the input stream (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @return the input stream internal
	 */
	protected final InputStream getInputStreamInternal() {
		// PRECONDITIONS
		Condition.notNull(this.theGetInputStream, "Field '%s' in '%s'.", "theGetInputStream", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl].",
			             getHandle(), "getInputStream");
		}
		
		try {
			return invoke(this.theGetInputStream);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketOptions#getOption(int)
	 */
	@Override
	public Object getOption(final int optID) throws SocketException {
		// PRECONDITIONS
		
		try {
			return getOptionInternal(optID);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Gets the option (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param optID
	 *            the opt id
	 * @return the option internal
	 */
	protected final Object getOptionInternal(final int optID) {
		// PRECONDITIONS
		Condition.notNull(this.theGetOption, "Field '%s' in '%s'.", "theGetOption", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s'.",
			             getHandle(), "getOption", optID);
		}
		
		try {
			return invoke(this.theGetOption, optID);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#getOutputStream()
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		// PRECONDITIONS
		
		try {
			return getOutputStreamInternal();
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Gets the output stream (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @return the output stream internal
	 */
	protected final OutputStream getOutputStreamInternal() {
		// PRECONDITIONS
		Condition.notNull(this.theGetOutputStream, "Field '%s' in '%s'.", "theGetOutputStream", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl].",
			             getHandle(), "getOutputStream");
		}
		
		try {
			return invoke(this.theGetOutputStream);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Invoke.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param method
	 *            the method
	 * @param arguments
	 *            the arguments
	 * @return the t
	 */
	@SuppressWarnings ({ "unchecked" })
	private <T> T invoke(@NotNull final Method method,
	                     final Object... arguments) {
		try {
			return (T) method.invoke(this.internalSocket, arguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			if (Logger.logError()) {
				Logger.error(e);
			}
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#listen(int)
	 */
	@Override
	public void listen(final int backlog) throws IOException {
		// PRECONDITIONS
		
		try {
			listenInternal(backlog);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Listen (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param backlog
	 *            the backlog
	 */
	protected final void listenInternal(final int backlog) {
		// PRECONDITIONS
		Condition.notNull(this.theListen, "Field '%s' in '%s'.", "theListen", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s'.",
			             getHandle(), "bind", backlog);
		}
		
		try {
			invoke(this.theListen, backlog);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#sendUrgentData(int)
	 */
	@Override
	public void sendUrgentData(final int data) throws IOException {
		// PRECONDITIONS
		
		try {
			sendUrgentDataInternal(data);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Send urgent data (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param data
	 *            the data
	 */
	protected final void sendUrgentDataInternal(final int data) {
		// PRECONDITIONS
		Condition.notNull(this.theSendUrgentData, "Field '%s' in '%s'.", "theSendUrgentData", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s'.",
			             getHandle(), "sendUrgentData", data);
		}
		
		try {
			invoke(this.theSendUrgentData, data);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketOptions#setOption(int, java.lang.Object)
	 */
	@Override
	public void setOption(final int optID,
	                      final Object value) throws SocketException {
		// PRECONDITIONS
		
		try {
			setOptionInternal(optID, value);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Sets the option (internal call to the underlying SocksSocketImpl instance).
	 * 
	 * @param optID
	 *            the opt id
	 * @param value
	 *            the value
	 */
	protected final void setOptionInternal(final int optID,
	                                       final Object value) {
		// PRECONDITIONS
		Condition.notNull(this.theSetOption, "Field '%s' in '%s'.", "theSetOption", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s', '%s'.",
			             getHandle(), "setOption", optID, value);
		}
		
		try {
			invoke(this.theSetOption, optID, value);
		} finally {
			// POSTCONDITIONS
		}
	}
	
}
