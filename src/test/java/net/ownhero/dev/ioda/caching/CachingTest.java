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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import net.ownhero.dev.ioda.FileUtils;
import net.ownhero.dev.ioda.FileUtils.FileShutdownAction;
import net.ownhero.dev.ioda.IOUtils;
import net.ownhero.dev.ioda.SocketUtils;
import net.ownhero.dev.ioda.exceptions.FetchException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Sascha Just <sascha.just@st.cs.uni-saarland.de>
 * 
 */
public class CachingTest {
	
	private static File dir;
	
	@BeforeClass
	public static void beforeClass() {
		dir = FileUtils.createRandomDir("CACHE", "TEST", FileShutdownAction.DELETE);
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
			final ByteArrayOutputStream result = new ByteArrayOutputStream();
			IOUtils.copyInputStream(inputStream, result);
			System.err.println(result.toString());
			
			assertTrue(new File(dir.getAbsolutePath() + FileUtils.fileSeparator + "own-hero.net"
			        + FileUtils.fileSeparator + "own-hero.net").exists());
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
			final ByteArrayOutputStream result = new ByteArrayOutputStream();
			IOUtils.copyInputStream(inputStream, result);
			System.err.println(result.toString());
			assertTrue(new File(dir.getAbsolutePath() + FileUtils.fileSeparator + "www.google.de"
			        + FileUtils.fileSeparator + "www.google.de").exists());
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
	@Ignore
	public void httpNetCat() {
		try {
			IOUtils.fetchHttp(new URI("http://localhost"));
		} catch (FetchException | URISyntaxException e) {
			fail(e.getMessage());
		}
	}
	
}
