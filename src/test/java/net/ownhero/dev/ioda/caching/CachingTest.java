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
package net.ownhero.dev.ioda.caching;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import net.ownhero.dev.ioda.FileUtils;
import net.ownhero.dev.ioda.FileUtils.FileShutdownAction;
import net.ownhero.dev.ioda.IOUtils;
import net.ownhero.dev.ioda.SocketUtils;
import net.ownhero.dev.ioda.exceptions.FetchException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 * 
 */
public class CachingTest {
	
	@BeforeClass
	public static void beforeClass() {
		final File dir = FileUtils.createRandomDir("CACHE", "TEST", FileShutdownAction.KEEP);
		SocketUtils.enableCaching(dir);
	}
	
	@Test
	public void advancedClientSocket() {
		try (Socket s = new Socket()) {
			s.connect(new InetSocketAddress("own-hero.net", 80));
			final OutputStream outputStream = s.getOutputStream();
			final PrintWriter writer = new PrintWriter(outputStream);
			writer.print("GET / HTTP/1.1\r\n");
			writer.print("Host: own-hero.net\r\n");
			writer.print("Connection: Keep-Alive\r\n");
			writer.print("User-Agent: CachingSocketClient/IODA-0.2\r\n");
			writer.print("\r\n");
			writer.flush();
			
			final InputStream inputStream = s.getInputStream();
			final InputStreamReader reader = new InputStreamReader(inputStream);
			final char[] buffer = new char[1024];
			while ((reader.read(buffer)) >= 0) {
				for (int i = 0; (i < buffer.length) && (buffer[i] != '\0'); ++i) {
					System.err.print(buffer[i]);
				}
				System.err.println();
			}
			
		} catch (final IOException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void clientSocket() {
		try (Socket s = new Socket()) {
			s.connect(new InetSocketAddress("google.de", 80));
			final OutputStream outputStream = s.getOutputStream();
			final PrintWriter writer = new PrintWriter(outputStream);
			writer.println("GET http://www.google.de/");
			writer.flush();
			
			final InputStream inputStream = s.getInputStream();
			final InputStreamReader reader = new InputStreamReader(inputStream);
			final char[] buffer = new char[1024];
			System.err.println("Result: ");
			while ((reader.read(buffer)) >= 0) {
				System.err.println(buffer);
			}
			
		} catch (final IOException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void httpClient() {
		try {
			IOUtils.fetchHttp(new URI("http://www.google.com"));
		} catch (FetchException | URISyntaxException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void httpNetCat() {
		try {
			IOUtils.fetchHttp(new URI("http://localhost"));
		} catch (FetchException | URISyntaxException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void serverSocket() {
		try (ServerSocket s = new ServerSocket()) {
			s.bind(new InetSocketAddress("localhost", 2385));
			s.close();
		} catch (final IOException e) {
			fail(e.getMessage());
		}
	}
}
