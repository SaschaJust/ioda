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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import net.ownhero.dev.ioda.FileUtils;
import net.ownhero.dev.ioda.IOUtils;
import net.ownhero.dev.ioda.sockets.Delegator.DelegatorMethodFinder;
import net.ownhero.dev.kanuni.annotations.file.WritableDirectory;
import net.ownhero.dev.kanuni.annotations.simple.NotNull;
import net.ownhero.dev.kanuni.conditions.Condition;
import net.ownhero.dev.kisa.Logger;
import net.ownhero.dev.regex.Group;
import net.ownhero.dev.regex.Match;
import net.ownhero.dev.regex.Regex;

/**
 * The Class CachingSocketImpl.
 * 
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 */
public class CachingSocketImpl extends SocketImpl {
	
	/**
	 * The Class CachedInputStream.
	 */
	private class CachedInputStream extends InputStream {
		
		/** The buffer. */
		private ByteBuffer              buffer = null;
		private final Queue<ByteBuffer> bList  = new LinkedList<>();
		
		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		@Override
		public int read() throws IOException {
			// PRECONDITIONS
			
			try {
				if (this.buffer == null) {
					this.buffer = ByteBuffer.allocate(4096);
					InputStream stream;
					
					if (doCache()) {
						final String request = CachingSocketImpl.this.outputStream.getRequest();
						if (Logger.logTrace()) {
							Logger.trace("Request is '%s'.", request);
						}
						final File cacheFile = getTargetFile(request);
						if (Logger.logTrace()) {
							Logger.trace("Target cache file is '%s'.", cacheFile);
						}
						
						if (!cacheFile.exists()) {
							if (!CachingSocketImpl.this.open && !CachingSocketImpl.this.closed) {
								_connect(CachingSocketImpl.this.address, CachingSocketImpl.this.port);
								
								for (final Integer optID : CachingSocketImpl.this.options.keySet()) {
									_setOption(optID, CachingSocketImpl.this.options.get(optID));
								}
							}
							
							final OutputStream ostream = _getOutputStream();
							ostream.write(CachingSocketImpl.this.outputStream.getCache());
							ostream.flush();
							
							final InputStream inputStream = _getInputStream();
							final OutputStream cacheStream = new FileOutputStream(cacheFile);
							IOUtils.copyInputStream(inputStream, cacheStream);
						}
						
						stream = new FileInputStream(cacheFile);
						
					} else {
						if (Logger.logTrace()) {
							Logger.trace("By-passing caching.");
						}
						
						if (!CachingSocketImpl.this.open && !CachingSocketImpl.this.closed) {
							if (Logger.logTrace()) {
								Logger.trace("Connecting internal socket.");
							}
							_connect(CachingSocketImpl.this.address, CachingSocketImpl.this.port);
							
							if (Logger.logTrace()) {
								Logger.trace("Setting socket options.");
							}
							for (final Integer optID : CachingSocketImpl.this.options.keySet()) {
								_setOption(optID, CachingSocketImpl.this.options.get(optID));
							}
						}
						
						final OutputStream ostream = _getOutputStream();
						if (Logger.logTrace()) {
							Logger.trace("Writing cached data to internal socket: '%s'",
							             new String(CachingSocketImpl.this.outputStream.getCache()));
							Logger.trace("Which is: %s",
							             Arrays.toString(CachingSocketImpl.this.outputStream.getCache()));
						}
						ostream.write(CachingSocketImpl.this.outputStream.getCache());
						ostream.flush();
						
						if (Logger.logTrace()) {
							Logger.trace("InputStream data is taking from internal stream.");
						}
						stream = _getInputStream();
					}
					
					byte b;
					if (Logger.logTrace()) {
						Logger.trace("Reading estimated '%s' bytes into cache.", stream.available());
					}
					
					while ((b = (byte) stream.read()) >= 0) {
						if (this.buffer.hasRemaining()) {
							this.buffer.put(b);
						} else {
							this.buffer.rewind();
							this.bList.add(this.buffer);
							this.buffer = ByteBuffer.allocate(4096);
						}
					}
					this.buffer.rewind();
					
					if (Logger.logTrace()) {
						final StringBuilder builder = new StringBuilder();
						
						for (final ByteBuffer theBuffer : this.bList) {
							builder.append(Arrays.toString(theBuffer.array()));
						}
						
						builder.append(this.buffer.array());
						Logger.trace("Cache now has the following content: " + builder);
					}
				}
				
				if (this.buffer.hasRemaining()) {
					return this.buffer.get();
				} else if (!this.bList.isEmpty()) {
					this.buffer = this.bList.poll();
					return this.buffer.get();
				} else {
					return -1;
				}
				
			} finally {
				// POSTCONDITIONS
			}
		}
	}
	
	/**
	 * The Class CachedOutputStream.
	 */
	private class CachedOutputStream extends OutputStream {
		
		/** The regex. */
		private final Regex                 regex  = new Regex("^GET\\s+({URL}https?://[^ ]+)$");
		
		/** The stream. */
		private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		/** The request. */
		private String                      request;
		
		/**
		 * Gets the cache.
		 * 
		 * @return the cache
		 */
		public byte[] getCache() {
			return this.stream.toByteArray();
		}
		
		/**
		 * Gets the request.
		 * 
		 * @return the request
		 */
		public String getRequest() {
			return this.request;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */
		@Override
		public void write(final int b) throws IOException {
			// PRECONDITIONS
			if (Logger.logTrace()) {
				Logger.trace("Writing to output cache: '0x%s'", (b < 16
				                                                       ? '0'
				                                                       : "") + Integer.toHexString(b));
			}
			
			try {
				if ((char) b == '\n') {
					final Match match = this.regex.find(this.stream.toString());
					if (match != null) {
						this.request = match.getGroup("URL").getMatch();
					}
				}
				this.stream.write(b);
			} finally {
				// POSTCONDITIONS
			}
		}
	}
	
	/** The directory. */
	private File                       directory;
	
	/** The open. */
	private boolean                    open         = false;
	
	/** The output stream. */
	private final CachedOutputStream   outputStream = new CachedOutputStream();
	
	/** The is server. */
	private boolean                    isServer     = false;
	
	/** The delegator. */
	private Delegator                  delegator;
	
	/** The options. */
	private final Map<Integer, Object> options      = new HashMap<>();
	
	/** The input stream. */
	private final InputStream          inputStream  = new CachedInputStream();
	
	/** The closed. */
	private boolean                    closed       = false;
	
	/** The internal socket (instance of SocksSocketImpl). */
	private Object                     internalSocket;
	
	/** The method setOption of SocksSocketImpl inherited from AbstractPlainSocketImpl. */
	private Method                     theSetOption;
	
	/**
	 * Instantiates a new caching socket impl.
	 * 
	 * @param directory
	 *            the directory
	 */
	public CachingSocketImpl(final @NotNull @WritableDirectory File directory) {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Instantiating '%s'.", getHandle());
		}
		
		try {
			this.delegator = new Delegator(this, SocketImpl.class, "java.net.SocksSocketImpl");
			this.internalSocket = this.delegator.getObject();
			
			try {
				final Class<?> forName = Class.forName("java.net.AbstractPlainSocketImpl");
				this.theSetOption = forName.getMethod("setOption", int.class, Object.class);
				this.theSetOption.setAccessible(true);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				if (Logger.logWarn()) {
					Logger.warn(e);
				}
			}
			this.directory = directory;
		} finally {
			// POSTCONDITIONS
			Condition.notNull(this.directory, "Field '%s' in '%s'.", "directory", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * _connect.
	 * 
	 * @param address
	 *            the address
	 * @param port
	 *            the port
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void _connect(final InetAddress address,
	                      final int port) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s', '%s'.",
			             getHandle(), "connect", address, port);
		}
		
		try {
			try {
				this.delegator.delegateTo("connect", InetAddress.class, int.class).invoke(address, port);
				this.open = true;
			} catch (final Exception e) {
				throw new DelegationException(e);
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * _get input stream.
	 * 
	 * @return the input stream
	 */
	private InputStream _getInputStream() {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl].",
			             getHandle(), "getInputStream");
		}
		
		try {
			try {
				return this.delegator.delegateTo("getInputStream").<InputStream> invoke();
			} catch (final Exception e) {
				throw new DelegationException(e);
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * _get output stream.
	 * 
	 * @return the output stream
	 */
	private OutputStream _getOutputStream() {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl].",
			             getHandle(), "getOutputStream");
		}
		
		try {
			try {
				return this.delegator.delegateTo("getOutputStream").<OutputStream> invoke();
			} catch (final Exception e) {
				throw new DelegationException(e);
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * _set otion.
	 * 
	 * @param optID
	 *            the opt id
	 * @param value
	 *            the value
	 */
	private void _setOption(final int optID,
	                        final Object value) {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("[INTERNAL] Delegated call to method '%s:%s' to internal socket [SocksSocketImpl] with options '%s', '%s'.",
			             getHandle(), "setOption", optID, value);
		}
		
		try {
			try {
				this.theSetOption.invoke(this.internalSocket, optID, value);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new DelegationException(e);
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#accept(java.net.SocketImpl)
	 */
	@Override
	protected void accept(final SocketImpl s) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with argument '%s'.", getHandle(), "accept", s);
		}
		
		try {
			if (this.isServer) {
				this.delegator.invoke(s);
				final DelegatorMethodFinder delegateTo = this.delegator.delegateTo("accept", SocketImpl.class);
				delegateTo.invoke(s);
			} else {
				throw new UnsupportedOperationException();
			}
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
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "available");
		}
		
		try {
			if (this.open) {
				return this.delegator.<Integer> invoke();
			} else {
				// TODO this could be wrong
				return 0;
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#bind(java.net.InetAddress, int)
	 */
	@Override
	protected void bind(final InetAddress host,
	                    final int port) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "bind", host, port);
		}
		
		try {
			this.isServer = true;
			this.delegator.invoke(host, port);
			this.open = true;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#close()
	 */
	@Override
	protected void close() throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "close");
		}
		
		try {
			if (this.open || this.isServer) {
				this.delegator.invoke();
				this.open = false;
				this.closed = true;
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#connect(java.net.InetAddress, int)
	 */
	@Override
	protected void connect(final InetAddress address,
	                       final int port) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "connect", address, port);
		}
		
		try {
			this.address = address;
			this.port = port;
		} finally {
			// POSTCONDITIONS
			Condition.notNull(this.address, "Field '%s' in '%s'.", "this.address", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#connect(java.net.SocketAddress, int)
	 */
	@Override
	protected void connect(final SocketAddress address,
	                       final int timeout) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "connect", address, timeout);
		}
		
		try {
			final InetSocketAddress isa = (InetSocketAddress) address;
			connect(isa.getAddress(), isa.getPort());
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#connect(java.lang.String, int)
	 */
	@Override
	protected void connect(final String host,
	                       final int port) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "connect", host, port);
		}
		
		try {
			this.address = InetAddress.getByName(host);
			this.port = port;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#create(boolean)
	 */
	@Override
	protected void create(final boolean stream) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s'.", getHandle(), "create", stream);
		}
		
		try {
			this.delegator.invoke(stream);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Do cache.
	 * 
	 * @return true, if successful
	 */
	private final boolean doCache() {
		return this.outputStream.getRequest() != null;
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
	 * @see java.net.SocketImpl#getInputStream()
	 */
	@Override
	protected InputStream getInputStream() throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "getInputStream");
		}
		
		try {
			if (this.isServer) {
				return this.delegator.<InputStream> invoke();
			} else {
				return this.inputStream;
			}
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
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s'.", getHandle(), "getOption", optID);
		}
		
		try {
			return this.delegator.<InputStream> invoke(optID);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#getOutputStream()
	 */
	@Override
	protected OutputStream getOutputStream() throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "getOutputStream");
		}
		
		try {
			if (this.isServer) {
				return this.delegator.<OutputStream> invoke();
			} else {
				return this.outputStream;
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Gets the target file.
	 * 
	 * @param request
	 *            the request
	 * @return the target file
	 * @throws MalformedURLException
	 */
	private File getTargetFile(final String request) throws MalformedURLException {
		final Regex regex = new Regex("https?://({hostname}[^/]+)({remainder}.*)?");
		final Match match = regex.find(request);
		final Group hostnameGroup = match.getGroup("hostname");
		final String hostname = hostnameGroup.getMatch();
		final Group remainderGroup = match.getGroup("remainder");
		String remainder = "";
		if (remainderGroup != null) {
			remainder = remainderGroup.getMatch();
		}
		
		final File targetDir = new File(this.directory.getAbsolutePath() + FileUtils.fileSeparator + hostname);
		if (targetDir.exists()) {
			if (!targetDir.isDirectory()) {
				throw new RuntimeException(targetDir.getAbsolutePath() + " is not a directory.");
			}
		} else {
			if (!targetDir.mkdir()) {
				throw new RuntimeException("Could not create directory '" + targetDir.getAbsolutePath() + "'.");
			}
		}
		
		try {
			final File target = new File(targetDir.getAbsolutePath() + FileUtils.fileSeparator + hostname
			        + (remainder.isEmpty()
			                              ? ""
			                              : "_" + URLEncoder.encode(remainder.substring(1), "ASCII")));
			
			if (Logger.logTrace()) {
				Logger.trace(String.format("Computing cache file for request '%s' to be '%s'.", request,
				                           target.getAbsolutePath()));
			}
			
			return target;
			
		} catch (final UnsupportedEncodingException e) {
			if (Logger.logError()) {
				Logger.error(e);
			}
			
			throw new MalformedURLException(e.getMessage());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#listen(int)
	 */
	@Override
	protected void listen(final int backlog) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s'.", getHandle(), "listen", backlog);
		}
		
		try {
			this.isServer = true;
			this.delegator.invoke(backlog);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#sendUrgentData(int)
	 */
	@Override
	protected void sendUrgentData(final int data) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s'.", getHandle(), "sendUrgentData", data);
		}
		
		try {
			if (this.isServer) {
				this.delegator.invoke(data);
			} else {
				throw new UnsupportedOperationException();
			}
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
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "setOption", optID, value);
		}
		
		try {
			if (this.open) {
				_setOption(optID, value);
			} else {
				this.options.put(optID, value);
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#supportsUrgentData()
	 */
	@Override
	protected boolean supportsUrgentData() {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "supportedUrgentData");
		}
		
		try {
			if (this.isServer) {
				return this.delegator.invoke();
			} else {
				throw new UnsupportedOperationException();
			}
		} finally {
			// POSTCONDITIONS
		}
	}
	
}
