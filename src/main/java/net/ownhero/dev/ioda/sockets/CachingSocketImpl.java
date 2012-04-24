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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.ownhero.dev.ioda.FileUtils;
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
public final class CachingSocketImpl extends InterceptableSocketImpl {
	
	private class CachedFileInputStream extends InputStream {
		
		private InputStream  input;
		private OutputStream output;
		
		/**
		 * @param file
		 * @throws FileNotFoundException
		 */
		public CachedFileInputStream(final InputStream input, final OutputStream output) throws FileNotFoundException {
			super();
			// PRECONDITIONS
			
			try {
				this.input = input;
				this.output = output;
			} finally {
				// POSTCONDITIONS
			}
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.io.FileInputStream#read()
		 */
		@Override
		public int read() throws IOException {
			// PRECONDITIONS
			
			try {
				final int i = this.input.read();
				this.output.write(i);
				return i;
			} finally {
				// POSTCONDITIONS
			}
		}
	}
	
	/**
	 * The Class CachedInputStream.
	 */
	private class CachedInputStream extends InputStream {
		
		/** The b list. */
		// private final Queue<ByteBuffer> bufferQueue = new LinkedList<>();
		
		/** The buffer. */
		// private ByteBuffer currentBuffer = null;
		
		private InputStream stream;
		
		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#available()
		 */
		@Override
		public int available() throws IOException {
			// PRECONDITIONS
			
			try {
				// return this.currentBuffer.position() + (this.bufferQueue.size() * BUFFER_SIZE);
				return this.stream.available();
			} finally {
				// POSTCONDITIONS
			}
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		@Override
		public int read() throws IOException {
			// PRECONDITIONS
			
			try {
				if (this.stream == null) {
					// this.currentBuffer = ByteBuffer.allocate(BUFFER_SIZE);
					// InputStream stream;
					final byte[] outputCache = CachingSocketImpl.this.outputStream.getCache();
					
					if (doCache()) {
						final String request = CachingSocketImpl.this.outputStream.getRequest();
						if (Logger.logTrace()) {
							Logger.trace("Request is '%s'.", request);
						}
						final File cacheFile = getTargetFile(request);
						if (Logger.logTrace()) {
							Logger.trace("Target cache file is '%s'.", cacheFile);
						}
						
						// check if cache file already exists
						if (!cacheFile.exists()) {
							// if not, fetch data with internal socket into the file
							if (!CachingSocketImpl.this.opened && !CachingSocketImpl.this.closed) {
								if (Logger.logTrace()) {
									Logger.trace("Connecting internal socket.");
								}
								connectInternal(CachingSocketImpl.this.address, CachingSocketImpl.this.port);
								CachingSocketImpl.this.opened = true;
								
								if (Logger.logTrace()) {
									if (CachingSocketImpl.this.options.isEmpty()) {
										Logger.trace("No options set for socket.");
									} else {
										Logger.trace("Setting options for socket:");
									}
								}
								for (final Integer optID : CachingSocketImpl.this.options.keySet()) {
									if (Logger.logTrace()) {
										Logger.trace("- Option: '%s' -> '%s'", optID,
										             CachingSocketImpl.this.options.get(optID));
									}
									setOptionInternal(optID, CachingSocketImpl.this.options.get(optID));
								}
							}
							
							if (Logger.logTrace()) {
								Logger.trace("Sending cached output data: " + byteArrayToHexString(outputCache));
							}
							final OutputStream ostream = getOutputStreamInternal();
							ostream.write(outputCache);
							ostream.flush();
							
							final InputStream inputStream = getInputStreamInternal();
							
							if (Logger.logTrace()) {
								Logger.trace("Creating FileOutputStream from cache-file: '%s'",
								             cacheFile.getAbsolutePath());
							}
							final OutputStream cacheStream = new FileOutputStream(cacheFile);
							this.stream = new CachedFileInputStream(inputStream, cacheStream);
						} else {
							// serve data from the cache file
							this.stream = new FileInputStream(cacheFile);
						}
						
					} else {
						if (Logger.logTrace()) {
							Logger.trace("By-passing caching.");
						}
						
						if (!CachingSocketImpl.this.opened && !CachingSocketImpl.this.closed) {
							if (Logger.logTrace()) {
								Logger.trace("Connecting internal socket.");
							}
							connectInternal(CachingSocketImpl.this.address, CachingSocketImpl.this.port);
							CachingSocketImpl.this.opened = true;
							
							if (Logger.logTrace()) {
								if (CachingSocketImpl.this.options.isEmpty()) {
									Logger.trace("No options set for socket.");
								} else {
									Logger.trace("Setting options for socket:");
								}
							}
							for (final Integer optID : CachingSocketImpl.this.options.keySet()) {
								if (Logger.logTrace()) {
									Logger.trace("- Option: '%s' -> '%s'", optID,
									             CachingSocketImpl.this.options.get(optID));
								}
								setOptionInternal(optID, CachingSocketImpl.this.options.get(optID));
							}
						}
						
						final OutputStream ostream = getOutputStreamInternal();
						
						if (Logger.logTrace()) {
							Logger.trace("Writing buffered data to internal socket: '%s'", outputCache);
							Logger.trace("Byte representation of buffered data: %s",
							             byteArrayToHexString(CachingSocketImpl.this.outputStream.getCache()));
						}
						ostream.write(outputCache);
						ostream.flush();
						
						if (Logger.logTrace()) {
							Logger.trace("InputStream data is taking from internal stream.");
						}
						
						this.stream = getInputStreamInternal();
					}
					
					// byte currentByte;
					// if (Logger.logTrace()) {
					// Logger.trace("Reading estimated '%s' bytes into buffer.", stream.available());
					// }
					//
					// while ((currentByte = (byte) stream.read()) >= 0) {
					// if (!this.currentBuffer.hasRemaining()) {
					// this.currentBuffer.rewind();
					// this.bufferQueue.add(this.currentBuffer);
					// this.currentBuffer = ByteBuffer.allocate(BUFFER_SIZE);
					// }
					//
					// this.currentBuffer.put(currentByte);
					// }
					//
					// this.currentBuffer.rewind();
					//
					// if (Logger.logTrace()) {
					// final StringBuilder builder = new StringBuilder();
					//
					// for (final ByteBuffer theBuffer : this.bufferQueue) {
					// builder.append(byteArrayToHexString(theBuffer.array()));
					// }
					//
					// builder.append(byteArrayToHexString(this.currentBuffer.array()));
					// Logger.trace("Buffer now has the following content: " + builder);
					// }
				}
				
				if (Logger.logTrace()) {
					Logger.trace("Reading one byte.");
				}
				return this.stream.read();
				
			} finally {
				// POSTCONDITIONS
			}
		}
	}
	
	/**
	 * The Class CachedOutputStream.
	 */
	private class CachedOutputStream extends OutputStream {
		
		/** The get request regex. */
		private final Regex                 getRequestRegex = new Regex("^GET ({remainder}.*) HTTP",
		                                                                Pattern.CASE_INSENSITIVE);
		
		/** The host regex. */
		private final Regex                 hostRegex       = new Regex("^Host: ({hostname}.*)$",
		                                                                Pattern.CASE_INSENSITIVE);
		
		private final Regex                 addressRegex    = new Regex(
		                                                                "^GET https?://({hostname}[^/ ]+)({remainder}/[^ ]*)",
		                                                                Pattern.CASE_INSENSITIVE);
		
		/** The stream. */
		private final ByteArrayOutputStream stream          = new ByteArrayOutputStream();
		
		/** The line. */
		private final ByteArrayOutputStream line            = new ByteArrayOutputStream();
		
		/** The remainder. */
		private String                      remainder       = null;
		
		/** The hostname. */
		private String                      hostname        = null;
		
		/**
		 * Gets the cache.
		 * 
		 * @return the cache
		 */
		public byte[] getCache() {
			final byte[] ret = this.stream.toByteArray();
			this.stream.reset();
			return ret;
		}
		
		/**
		 * Gets the request.
		 * 
		 * @return the request
		 */
		public String getRequest() {
			// return the request if we found some HTTP/GET
			if ((this.remainder != null) && (this.hostname != null)) {
				return String.format("http://%1$s%2$s", this.hostname, this.remainder);
			} else {
				return null;
			}
			
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */
		@Override
		public void write(final int b) throws IOException {
			// PRECONDITIONS
			if (Logger.logTrace()) {
				Logger.trace("Writing to output buffer: '0x%s'", (b < 16
				                                                        ? '0'
				                                                        : "") + Integer.toHexString(b));
			}
			
			try {
				// TODO we can further improve socket behavior if we do some heuristics on the requests and directly
				// by-pass caching at all and relay all calls directly to the underlying socket implementation.
				if ((char) b == '\n') {
					if (this.remainder == null) {
						final Match match = this.getRequestRegex.find(this.line.toString());
						if (match != null) {
							this.remainder = match.getGroup("remainder").getMatch();
						} else {
							final Match match2 = this.addressRegex.find(this.line.toString());
							if (match2 != null) {
								this.hostname = match2.getGroup("hostname").getMatch();
								if (match2.hasNamedGroup("remainder")) {
									this.remainder = match2.getGroup("remainder").getMatch();
								} else {
									this.remainder = "";
								}
							}
						}
					} else {
						final Match match = this.hostRegex.find(this.line.toString());
						if (match != null) {
							this.hostname = match.getGroup("hostname").getMatch();
						}
					}
					
					this.line.reset();
				}
				
				this.stream.write(b);
				this.line.write(b);
			} finally {
				// POSTCONDITIONS
			}
		}
	}
	
	/** The closed. */
	private boolean                    closed       = false;
	
	/** The directory. */
	private File                       directory;
	
	/** The input stream. */
	private final CachedInputStream    inputStream  = new CachedInputStream();
	
	/** The open. */
	private boolean                    opened       = false;
	
	/** The options. */
	private final Map<Integer, Object> options      = new HashMap<>();
	
	/** The output stream. */
	private final CachedOutputStream   outputStream = new CachedOutputStream();
	
	/**
	 * Instantiates a new caching socket impl.
	 * 
	 * @param directory
	 *            the directory
	 */
	public CachingSocketImpl(final @NotNull @WritableDirectory File directory) {
		// PRECONDITIONS
		
		try {
			this.directory = directory;
		} finally {
			// POSTCONDITIONS
			Condition.notNull(this.directory, "Field '%s' in '%s'.", "directory", getHandle()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#accept(java.net.SocketImpl)
	 */
	@Override
	public void accept(final SocketImpl s) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with argument '%s'.", getHandle(), "accept", s);
		}
		
		try {
			acceptInternal(s);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#available()
	 */
	@Override
	public int available() throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "available");
		}
		
		try {
			if (this.opened) {
				return availableInternal();
			} else {
				return this.inputStream.available();
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
	public void bind(final InetAddress host,
	                 final int port) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "bind", host, port);
		}
		
		try {
			bindInternal(host, port);
			this.opened = true;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Byte array to hex string.
	 * 
	 * @param array
	 *            the array
	 * @return the string
	 */
	private String byteArrayToHexString(final byte[] array) {
		final StringBuilder builder = new StringBuilder();
		
		for (final byte b : array) {
			builder.append("0x").append((b < 16
			                                   ? '0'
			                                   : "")).append(Integer.toHexString(b)).append(' ');
		}
		
		return builder.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#close()
	 */
	@Override
	public void close() throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "close");
		}
		
		try {
			if (this.opened) {
				closeInternal();
				this.opened = false;
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
	public void connect(final InetAddress address,
	                    final int port) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "connect", address, port);
		}
		
		try {
			// only save connect options, but DO NOT CONNECT here
			// we have to decide if we want/have to connect later on as soon as people try to read from the input
			// stream.
			
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
	public void connect(final SocketAddress address,
	                    final int timeout) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "connect", address, timeout);
		}
		
		try {
			// only save connect options, but DO NOT CONNECT here
			// we have to decide if we want/have to connect later on as soon as people try to read from the input
			// stream.
			
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
	public void connect(final String host,
	                    final int port) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s', '%s'.", getHandle(), "connect", host, port);
		}
		
		try {
			// only save connect options, but DO NOT CONNECT here
			// we have to decide if we want/have to connect later on as soon as people try to read from the input
			// stream.
			
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
	public void create(final boolean stream) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s'.", getHandle(), "create", stream);
		}
		
		try {
			// just delegate to the internal socket
			createInternal(stream);
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Determines if we should use caching or by-pass the caching mechanism. This solely depends on the fact that we saw
	 * some HTTP GET request beforehand.
	 * 
	 * @return true, if successful
	 */
	private final boolean doCache() {
		return this.outputStream.getRequest() != null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.net.SocketImpl#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "getInputStream");
		}
		
		try {
			// hand out our own CachingInputStream
			return this.inputStream;
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
			// check if the internal socket implementation is active
			if (this.opened) {
				// return the options used in the active connection
				return getOptionInternal(optID);
			} else {
				// return the options saved to be used after connecting the socket
				return this.options.get(optID);
			}
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
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "getOutputStream");
		}
		
		try {
			// hand out our own CachingOutputStream
			return this.outputStream;
		} finally {
			// POSTCONDITIONS
		}
	}
	
	/**
	 * Computes the target cache file from a given URL.
	 * 
	 * @param request
	 *            the request
	 * @return the target file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private File getTargetFile(final String request) throws IOException {
		final Regex regex = new Regex("https?://({hostname}[^/]+)({remainder}.*)?");
		final Match match = regex.find(request);
		final Group hostnameGroup = match.getGroup("hostname");
		final String hostname = hostnameGroup.getMatch();
		final Group remainderGroup = match.getGroup("remainder");
		
		String remainder = "";
		if (remainderGroup != null) {
			remainder = remainderGroup.getMatch();
		}
		
		// base dir for the cache. Solely depends on the hostname under suspect
		final File targetDir = new File(this.directory.getAbsolutePath() + FileUtils.fileSeparator + hostname);
		
		// try to make target directory ready for writing
		if (targetDir.exists()) {
			if (!targetDir.isDirectory()) {
				throw new IOException(targetDir.getAbsolutePath() + " is not a directory.");
			}
		} else {
			if (!targetDir.mkdir()) {
				throw new IOException("Could not create directory '" + targetDir.getAbsolutePath() + "'.");
			}
		}
		
		try {
			// compose target file with abolsute path of the base dir + file separator + hostname (+ '_' + escaped
			// remainder of the url)
			final File target = new File(targetDir.getAbsolutePath() + FileUtils.fileSeparator + hostname
			        + (remainder.length() < 2
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
	public void listen(final int backlog) throws IOException {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s'.", getHandle(), "listen", backlog);
		}
		
		try {
			// relay to the internal socket implementation
			listenInternal(backlog);
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
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s' with options '%s'.", getHandle(), "sendUrgentData", data);
		}
		
		try {
			// Caching sockets do not support sending of urgent data (which should be checked with 'supportsUrgentData'
			// beforehand).
			// We encapsulate an UnsupportedOperationException within an IOException to be conform with the declared
			// exceptions of the interface.
			throw new IOException(
			                      new UnsupportedOperationException(
			                                                        "Sending urgent data is not supported on caching sockets."));
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
			// set options directly on the internal socket implementation if the socket is already active.
			// otherwise save the options to be applied later on
			if (this.opened) {
				setOptionInternal(optID, value);
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
	public boolean supportsUrgentData() {
		// PRECONDITIONS
		if (Logger.logTrace()) {
			Logger.trace("Called method '%s:%s'.", getHandle(), "supportedUrgentData");
		}
		
		try {
			// caching sockets do not support urgent data.
			return false;
		} finally {
			// POSTCONDITIONS
		}
	}
	
}
