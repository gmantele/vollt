package uws.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.junit.Before;
import org.junit.Test;

public class TestUWSUrl {

	public static final class TestHttpServletRequest implements HttpServletRequest {

		private final String scheme;
		private final String serverName;
		private final int serverPort;
		private final String contextPath;
		private final String pathInfo;
		private final String requestURI;
		private final StringBuffer requestURL;
		private final String servletPath;

		public TestHttpServletRequest(StringBuffer requestURL, String requestURI, String scheme, String serverName, int serverPort, String contextPath, String servletPath, String pathInfo){
			this.scheme = scheme;
			this.serverName = serverName;
			this.serverPort = serverPort;
			this.contextPath = contextPath;
			this.pathInfo = pathInfo;
			this.requestURI = requestURI;
			this.requestURL = requestURL;
			this.servletPath = servletPath;
		}

		@Override
		public String getScheme(){
			return scheme;
		}

		@Override
		public String getServerName(){
			return serverName;
		}

		@Override
		public int getServerPort(){
			return serverPort;
		}

		@Override
		public String getPathInfo(){
			return pathInfo;
		}

		@Override
		public String getRequestURI(){
			return requestURI;
		}

		@Override
		public StringBuffer getRequestURL(){
			return requestURL;
		}

		@Override
		public String getContextPath(){
			return contextPath;
		}

		@Override
		public String getServletPath(){
			return servletPath;
		}

		@Override
		public AsyncContext getAsyncContext(){
			return null;
		}

		@Override
		public Object getAttribute(String arg0){
			return null;
		}

		@Override
		public Enumeration<String> getAttributeNames(){
			return null;
		}

		@Override
		public String getCharacterEncoding(){
			return null;
		}

		@Override
		public int getContentLength(){
			return 0;
		}

		@Override
		public String getContentType(){
			return null;
		}

		@Override
		public DispatcherType getDispatcherType(){
			return null;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException{
			return null;
		}

		@Override
		public String getLocalAddr(){
			return null;
		}

		@Override
		public String getLocalName(){
			return null;
		}

		@Override
		public int getLocalPort(){
			return 0;
		}

		@Override
		public Locale getLocale(){
			return null;
		}

		@Override
		public Enumeration<Locale> getLocales(){
			return null;
		}

		@Override
		public String getParameter(String arg0){
			return null;
		}

		@Override
		public Map<String,String[]> getParameterMap(){
			return null;
		}

		@Override
		public Enumeration<String> getParameterNames(){
			return null;
		}

		@Override
		public String[] getParameterValues(String arg0){
			return null;
		}

		@Override
		public String getProtocol(){
			return null;
		}

		@Override
		public BufferedReader getReader() throws IOException{
			return null;
		}

		@Override
		public String getRealPath(String arg0){
			return null;
		}

		@Override
		public String getRemoteAddr(){
			return null;
		}

		@Override
		public String getRemoteHost(){
			return null;
		}

		@Override
		public int getRemotePort(){
			return 0;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String arg0){
			return null;
		}

		@Override
		public ServletContext getServletContext(){
			return null;
		}

		@Override
		public boolean isAsyncStarted(){
			return false;
		}

		@Override
		public boolean isAsyncSupported(){
			return false;
		}

		@Override
		public boolean isSecure(){
			return false;
		}

		@Override
		public void removeAttribute(String arg0){

		}

		@Override
		public void setAttribute(String arg0, Object arg1){

		}

		@Override
		public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException{

		}

		@Override
		public AsyncContext startAsync(){
			return null;
		}

		@Override
		public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1){
			return null;
		}

		@Override
		public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException{
			return false;
		}

		@Override
		public String getAuthType(){
			return null;
		}

		@Override
		public Cookie[] getCookies(){
			return null;
		}

		@Override
		public long getDateHeader(String arg0){
			return 0;
		}

		@Override
		public String getHeader(String arg0){
			return null;
		}

		@Override
		public Enumeration<String> getHeaderNames(){
			return null;
		}

		@Override
		public Enumeration<String> getHeaders(String arg0){
			return null;
		}

		@Override
		public int getIntHeader(String arg0){
			return 0;
		}

		@Override
		public String getMethod(){
			return null;
		}

		@Override
		public Part getPart(String arg0) throws IOException, IllegalStateException, ServletException{
			return null;
		}

		@Override
		public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException{
			return null;
		}

		@Override
		public String getPathTranslated(){
			return null;
		}

		@Override
		public String getQueryString(){
			return null;
		}

		@Override
		public String getRemoteUser(){
			return null;
		}

		@Override
		public String getRequestedSessionId(){
			return null;
		}

		@Override
		public HttpSession getSession(){
			return null;
		}

		@Override
		public HttpSession getSession(boolean arg0){
			return null;
		}

		@Override
		public Principal getUserPrincipal(){
			return null;
		}

		@Override
		public boolean isRequestedSessionIdFromCookie(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromURL(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromUrl(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdValid(){
			return false;
		}

		@Override
		public boolean isUserInRole(String arg0){
			return false;
		}

		@Override
		public void login(String arg0, String arg1) throws ServletException{}

		@Override
		public void logout() throws ServletException{}

	}

	private HttpServletRequest requestFromRoot2root;
	private HttpServletRequest requestFromRoot2async;

	private HttpServletRequest requestFromPath2root;
	private HttpServletRequest requestFromPath2async;

	private HttpServletRequest requestWithServletPathNull;

	@Before
	public void setUp() throws Exception{
		requestFromRoot2root = new TestHttpServletRequest(new StringBuffer("http://localhost:8080/tapTest/"), "/tapTest/", "http", "localhost", 8080, "/tapTest", "", "/");
		requestFromRoot2async = new TestHttpServletRequest(new StringBuffer("http://localhost:8080/tapTest/async"), "/tapTest/async", "http", "localhost", 8080, "/tapTest", "", "/async");

		requestFromPath2root = new TestHttpServletRequest(new StringBuffer("http://localhost:8080/tapTest/path/"), "/tapTest/path/", "http", "localhost", 8080, "/tapTest", "/path", "/");
		requestFromPath2async = new TestHttpServletRequest(new StringBuffer("http://localhost:8080/tapTest/path/async"), "/tapTest/path/async", "http", "localhost", 8080, "/tapTest", "/path", "/async");

		requestWithServletPathNull = new TestHttpServletRequest(new StringBuffer("http://localhost:8080/tapTest/"), "/tapTest/", "http", "localhost", 8080, "/tapTest", null, "/");
	}

	@Test
	public void testExtractBaseURI(){
		// CASE 1: http://localhost:8080/tapTest/path with url-pattern = /path/*
		try{
			UWSUrl uu = new UWSUrl(requestFromPath2root);
			assertEquals("/path", uu.getBaseURI());
			assertEquals("", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/path/", uu.toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromPath2root.getRequestURL());
		}

		// CASE 2: http://localhost:8080/tapTest/path/async with url-pattern = /path/*
		try{
			UWSUrl uu = new UWSUrl(requestFromPath2async);
			assertEquals("/path", uu.getBaseURI());
			assertEquals("/async", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/path/async", uu.toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromPath2async.getRequestURL());
		}

		// CASE 3: http://localhost:8080/tapTest with url-pattern = /*
		try{
			UWSUrl uu = new UWSUrl(requestFromRoot2root);
			assertEquals("", uu.getBaseURI());
			assertEquals("", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/", uu.toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromRoot2root.getRequestURL());
		}

		// CASE 4: http://localhost:8080/tapTest/async with url-pattern = /*
		try{
			UWSUrl uu = new UWSUrl(requestFromRoot2async);
			assertEquals("", uu.getBaseURI());
			assertEquals("/async", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/async", uu.toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromRoot2async.getRequestURL());
		}

		// CASE 5: http://localhost:8080/tapTest/path/async with url-pattern = /path/*
		try{
			new UWSUrl(requestWithServletPathNull);
			fail("RequestURL with no servlet path: this test should have failed!");
		}catch(Exception e){
			assertTrue(e instanceof NullPointerException);
			assertEquals(e.getMessage(), "The extracted base UWS URI is NULL!");
		}
	}

	@Test
	public void testLoadHttpServletRequest(){
		// CASE 1a: http://localhost:8080/tapTest/path with url-pattern = /path/*
		try{
			UWSUrl uu = new UWSUrl(requestFromPath2root);
			uu.load(requestFromPath2root);
			assertEquals("", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/path/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromPath2root.getRequestURL());
		}
		// CASE 1b: Idem while loading http://localhost:8080/tapTest/path/async
		try{
			UWSUrl uu = new UWSUrl(requestFromPath2root);
			uu.load(requestFromPath2async);
			assertEquals("/async", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/path/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromPath2async.getRequestURL());
		}

		// CASE 2a: http://localhost:8080/tapTest/path/async with url-pattern = /path/*
		try{
			UWSUrl uu = new UWSUrl(requestFromPath2async);
			uu.load(requestFromPath2async);
			assertEquals("/async", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/path/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromPath2async.getRequestURL());
		}

		// CASE 2b: Idem while loading http://localhost:8080/tapTest/path
		try{
			UWSUrl uu = new UWSUrl(requestFromPath2async);
			uu.load(requestFromPath2root);
			assertEquals("", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/path/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromPath2root.getRequestURL());
		}

		// CASE 3a: http://localhost:8080/tapTest with url-pattern = /*
		try{
			UWSUrl uu = new UWSUrl(requestFromRoot2root);
			uu.load(requestFromRoot2root);
			assertEquals("", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(NullPointerException e){
			fail("This HTTP request is perfectly correct: " + requestFromRoot2root.getRequestURL());
		}

		// CASE 3b: Idem while loading http://localhost:8080/tapTest/async
		try{
			UWSUrl uu = new UWSUrl(requestFromRoot2root);
			uu.load(requestFromRoot2async);
			assertEquals("/async", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromRoot2async.getRequestURL());
		}

		// CASE 4a: http://localhost:8080/tapTest/async with url-pattern = /*
		try{
			UWSUrl uu = new UWSUrl(requestFromRoot2async);
			uu.load(requestFromRoot2async);
			assertEquals("/async", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromRoot2async.getRequestURL());
		}

		// CASE 4b: Idem while loading http://localhost:8080/tapTest
		try{
			UWSUrl uu = new UWSUrl(requestFromRoot2async);
			uu.load(requestFromRoot2root);
			assertEquals("", uu.getUwsURI());
			assertEquals("http://localhost:8080/tapTest/async/123456A", uu.jobSummary("async", "123456A").toString());
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromRoot2root.getRequestURL());
		}

		// SPECIAL CASE 1: Creation with http://localhost:8080/tapTest[/async] (/*) but loading with http://localhost:8080/tapTest/path[/async] (/path/*):
		try{
			UWSUrl uu = new UWSUrl(requestFromRoot2async);
			uu.load(requestFromPath2async);
			assertFalse(uu.getUwsURI().equals(""));
		}catch(Exception e){
			e.printStackTrace(System.err);
			fail("This HTTP request is perfectly correct: " + requestFromRoot2root.getRequestURL());
		}
	}

}
