package uws.service.wait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

import uws.job.JobList;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;

public class TestUserLimitedBlockingPolicy {

	@Before
	public void setUp() throws Exception{}

	@Test
	public void testUserLimitedBlockingPolicyLongInt(){
		/* ****************************** */
		/* NB MAX BLOCKED THREADS BY USER */

		// Negative number of threads:
		UserLimitedBlockingPolicy policy = new UserLimitedBlockingPolicy(10000, -1);
		assertEquals(UserLimitedBlockingPolicy.DEFAULT_NB_MAX_BLOCKED, policy.maxBlockedThreadsByUser);

		// Null number of threads:
		policy = new UserLimitedBlockingPolicy(10000, 0);
		assertEquals(UserLimitedBlockingPolicy.DEFAULT_NB_MAX_BLOCKED, policy.maxBlockedThreadsByUser);

		// A positive number of threads LESS THAN DEFAULT_NB_MAX_BLOCKED:
		policy = new UserLimitedBlockingPolicy(10000, 1);
		assertEquals(1, policy.maxBlockedThreadsByUser);

		// A positive number of threads GREATER THAN DEFAULT_NB_MAX_BLOCKED:
		policy = new UserLimitedBlockingPolicy(10000, 10);
		assertEquals(10, policy.maxBlockedThreadsByUser);
	}

	@Test
	public void testBuildKey(){
		UserLimitedBlockingPolicy policy = new UserLimitedBlockingPolicy();
		UWSJob testJob = new UWSJob("123456", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null);

		// With no job => ERROR!
		try{
			policy.buildKey(null, null, null);
			fail("Impossible to generate a key without a job!");
		}catch(NullPointerException npe){}

		// With only a job => jobId + ";???"
		assertEquals("123456;???", policy.buildKey(testJob, null, null));

		// With a job and a user whose the ID is null => jobId + ";???"
		assertEquals("123456;???", policy.buildKey(testJob, new TestUser(null), null));

		// With a job and a user whose the ID is NOT null => jobId + ";" + UserID
		assertEquals("123456;myID", policy.buildKey(testJob, new TestUser("myID"), null));

		// With a job and a request => jobId + ";" + IPAddress
		assertEquals("123456;1.2.3.4", policy.buildKey(testJob, null, new TestHttpServletRequest()));

		// With a job, a request and a user whose the ID is null => jobId + ";" + IPAddress
		assertEquals("123456;1.2.3.4", policy.buildKey(testJob, new TestUser(null), new TestHttpServletRequest()));
	}

	@Test
	public void testBlock(){
		/* ************************************* */
		/* No Problem If No Job And/Or No Thread */

		try{
			UserLimitedBlockingPolicy policy = new UserLimitedBlockingPolicy();
			assertEquals(0, policy.block(null, 10, null, null, null));
			assertEquals(0, policy.block(new Thread("0"), 10, null, null, null));
			assertEquals(0, policy.block(null, 10, new UWSJob(new UWSParameters()), null, null));
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Nothing should happen if no job and/or thread is asked to be blocked!");
		}

		JobOwner user = new TestUser("myId");
		UWSJob testJob1 = new UWSJob("123456", (new Date()).getTime(), user, new UWSParameters(), -1, -1, -1, null, null);
		UWSJob testJob2 = new UWSJob("654321", (new Date()).getTime(), user, new UWSParameters(), -1, -1, -1, null, null);
		final String key1 = "123456;myId", key2 = "654321;myId";

		/* *************************** */
		/* NO OLD BLOCKING REPLACEMENT */

		// Test with just one job and only one allowed access => OK!
		UserLimitedBlockingPolicy policy = new UserLimitedBlockingPolicy(10, 1, false);
		assertEquals(5, policy.block(new Thread("1"), 5, testJob1, user, null));
		assertEquals(key1, policy.buildKey(testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(1, policy.blockedThreads.get(key1).size());
		assertEquals("1", policy.blockedThreads.get(key1).peek().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());

		// Test with the same job and same user => Rejected!
		assertEquals(0, policy.block(new Thread("2"), 5, testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(1, policy.blockedThreads.get(key1).size());
		assertEquals("1", policy.blockedThreads.get(key1).peek().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());

		// Test with a second job => OK!
		assertEquals(5, policy.block(new Thread("3"), 5, testJob2, user, null));
		assertEquals(key2, policy.buildKey(testJob2, user, null));
		assertTrue(policy.blockedThreads.containsKey(key2));
		assertEquals(1, policy.blockedThreads.get(key2).size());
		assertEquals("3", policy.blockedThreads.get(key2).peek().getName());
		assertEquals(0, policy.blockedThreads.get(key2).remainingCapacity());

		/* ************************ */
		/* OLD BLOCKING REPLACEMENT */

		// 1st test with just one job and only one allowed access => OK!
		policy = new UserLimitedBlockingPolicy(10, 1, true);
		assertEquals(5, policy.block(new Thread("1"), 5, testJob1, user, null));
		assertEquals(key1, policy.buildKey(testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(1, policy.blockedThreads.get(key1).size());
		assertEquals("1", policy.blockedThreads.get(key1).peek().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());

		// 2nd test with the same job and same user => OK!
		assertEquals(5, policy.block(new Thread("2"), 5, testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(1, policy.blockedThreads.get(key1).size());
		assertEquals("2", policy.blockedThreads.get(key1).peek().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());

		// Test with a second job => OK!
		assertEquals(5, policy.block(new Thread("3"), 5, testJob2, user, null));
		assertEquals(key2, policy.buildKey(testJob2, user, null));
		assertTrue(policy.blockedThreads.containsKey(key2));
		assertEquals(1, policy.blockedThreads.get(key2).size());
		assertEquals("3", policy.blockedThreads.get(key2).peek().getName());
		assertEquals(0, policy.blockedThreads.get(key2).remainingCapacity());

		/* ************************************************************************* */
		/* MORE THAN 1 OF CAPACITY (i.e. nb access for a given job and a given user) */

		/* WITH NO old blocking replacement */

		// 1st test with just one job and 2 allowed accesses => OK!
		policy = new UserLimitedBlockingPolicy(10, 2, false);
		assertEquals(5, policy.block(new Thread("1"), 5, testJob1, user, null));
		assertEquals(key1, policy.buildKey(testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(1, policy.blockedThreads.get(key1).size());
		assertEquals("1", policy.blockedThreads.get(key1).peek().getName());
		assertEquals(1, policy.blockedThreads.get(key1).remainingCapacity());

		// 2nd test with the same job and same user => OK!
		assertEquals(5, policy.block(new Thread("2"), 5, testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(2, policy.blockedThreads.get(key1).size());
		Iterator<Thread> it = policy.blockedThreads.get(key1).iterator();
		assertEquals("1", it.next().getName());
		assertEquals("2", it.next().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());

		// 3rd test with the same job and same user => Rejected!
		assertEquals(0, policy.block(new Thread("3"), 5, testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(2, policy.blockedThreads.get(key1).size());
		it = policy.blockedThreads.get(key1).iterator();
		assertEquals("1", it.next().getName());
		assertEquals("2", it.next().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());

		/* WITH old blocking replacement */

		// 1st test with just one job and 2 allowed accesses => OK!
		policy = new UserLimitedBlockingPolicy(10, 2, true);
		assertEquals(5, policy.block(new Thread("1"), 5, testJob1, user, null));
		assertEquals(key1, policy.buildKey(testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(1, policy.blockedThreads.get(key1).size());
		assertEquals("1", policy.blockedThreads.get(key1).peek().getName());
		assertEquals(1, policy.blockedThreads.get(key1).remainingCapacity());

		// 2nd test with the same job and same user => OK!
		assertEquals(5, policy.block(new Thread("2"), 5, testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(2, policy.blockedThreads.get(key1).size());
		it = policy.blockedThreads.get(key1).iterator();
		assertEquals("1", it.next().getName());
		assertEquals("2", it.next().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());

		// 3rd test with the same job and same user => OK!
		assertEquals(5, policy.block(new Thread("3"), 5, testJob1, user, null));
		assertTrue(policy.blockedThreads.containsKey(key1));
		assertEquals(2, policy.blockedThreads.get(key1).size());
		it = policy.blockedThreads.get(key1).iterator();
		assertEquals("2", it.next().getName());
		assertEquals("3", it.next().getName());
		assertEquals(0, policy.blockedThreads.get(key1).remainingCapacity());
	}

	@Test
	public void testUnblocked(){
		JobOwner user = new TestUser("myId");
		Thread thread1 = new Thread("1"), thread2 = new Thread("2");
		UWSJob testJob = new UWSJob("123456", (new Date()).getTime(), user, new UWSParameters(), -1, -1, -1, null, null);
		final String key = "123456;myId";

		// Block 2 jobs:
		UserLimitedBlockingPolicy policy = new UserLimitedBlockingPolicy(10, 2, false);
		assertEquals(5, policy.block(thread1, 5, testJob, user, null));
		assertEquals(5, policy.block(thread2, 5, testJob, user, null));
		assertEquals(key, policy.buildKey(testJob, user, null));
		assertTrue(policy.blockedThreads.containsKey(key));
		assertEquals(2, policy.blockedThreads.get(key).size());
		Iterator<Thread> it = policy.blockedThreads.get(key).iterator();
		assertEquals("1", it.next().getName());
		assertEquals("2", it.next().getName());
		assertEquals(0, policy.blockedThreads.get(key).remainingCapacity());

		// Unblock one:
		policy.unblocked(thread1, testJob, user, null);
		assertTrue(policy.blockedThreads.containsKey(key));
		assertEquals(1, policy.blockedThreads.get(key).size());
		assertEquals("2", policy.blockedThreads.get(key).peek().getName());
		assertEquals(1, policy.blockedThreads.get(key).remainingCapacity());

		// Unblock the second one:
		policy.unblocked(thread2, testJob, user, null);
		assertFalse(policy.blockedThreads.containsKey(key));

		// Try unblocking a not-blocked thread:
		try{
			policy.unblocked(new Thread("3"), testJob, user, null);
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Nothing should happen if the given thread is not blocked!");
		}

		// Nothing should happen if no job and/or thread:
		try{
			policy.unblocked(null, null, null, null);
			policy.unblocked(new Thread("0"), null, null, null);
			policy.unblocked(null, new UWSJob(new UWSParameters()), null, null);
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Nothing should happen if no job and/or thread is asked to be unblocked!");
		}
	}

	protected final static class TestUser implements JobOwner {

		private final String id;

		public TestUser(final String ID){
			id = ID;
		}

		@Override
		public String getID(){
			return id;
		}

		@Override
		public String getPseudo(){
			return null;
		}

		@Override
		public boolean hasReadPermission(JobList jl){
			return false;
		}

		@Override
		public boolean hasWritePermission(JobList jl){
			return false;
		}

		@Override
		public boolean hasReadPermission(UWSJob job){
			return false;
		}

		@Override
		public boolean hasWritePermission(UWSJob job){
			return false;
		}

		@Override
		public boolean hasExecutePermission(UWSJob job){
			return false;
		}

		@Override
		public Map<String,Object> getDataToSave(){
			return null;
		}

		@Override
		public void restoreData(Map<String,Object> data){}

	}

	protected final static class TestHttpServletRequest implements HttpServletRequest {

		private HashMap<String,String[]> parameters = new HashMap<String,String[]>();

		private static class NamesEnumeration implements Enumeration<String> {

			private final Iterator<String> it;

			public NamesEnumeration(final Set<String> names){
				this.it = names.iterator();
			}

			@Override
			public boolean hasMoreElements(){
				return it.hasNext();
			}

			@Override
			public String nextElement(){
				return it.next();
			}

		}

		@Override
		public String getRemoteAddr(){
			return "1.2.3.4";
		}

		public void addParams(final String name, final String value){
			if (parameters.containsKey(name)){
				String[] values = parameters.get(name);
				values = Arrays.copyOf(values, values.length + 1);
				values[values.length - 1] = value;
				parameters.put(name, values);
			}else
				parameters.put(name, new String[]{value});
		}

		public void clearParams(){
			parameters.clear();
		}

		@Override
		public Enumeration<String> getParameterNames(){
			return new NamesEnumeration(parameters.keySet());
		}

		@Override
		public String[] getParameterValues(String name){
			return parameters.get(name);
		}

		@Override
		public Map<String,String[]> getParameterMap(){
			return parameters;
		}

		@Override
		public String getParameter(String name){
			String[] values = parameters.get(name);
			if (values == null || values.length == 0)
				return null;
			else
				return values[0];
		}

		@Override
		public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1){
			return null;
		}

		@Override
		public AsyncContext startAsync(){
			return null;
		}

		@Override
		public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException{

		}

		@Override
		public void setAttribute(String arg0, Object arg1){

		}

		@Override
		public void removeAttribute(String arg0){

		}

		@Override
		public boolean isSecure(){
			return false;
		}

		@Override
		public boolean isAsyncSupported(){
			return false;
		}

		@Override
		public boolean isAsyncStarted(){
			return false;
		}

		@Override
		public ServletContext getServletContext(){
			return null;
		}

		@Override
		public int getServerPort(){
			return 0;
		}

		@Override
		public String getServerName(){
			return null;
		}

		@Override
		public String getScheme(){
			return null;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String arg0){
			return null;
		}

		@Override
		public int getRemotePort(){
			return 0;
		}

		@Override
		public String getRemoteHost(){
			return null;
		}

		@Override
		public String getRealPath(String arg0){
			return null;
		}

		@Override
		public BufferedReader getReader() throws IOException{
			return null;
		}

		@Override
		public String getProtocol(){
			return null;
		}

		@Override
		public Enumeration<Locale> getLocales(){
			return null;
		}

		@Override
		public Locale getLocale(){
			return null;
		}

		@Override
		public int getLocalPort(){
			return 0;
		}

		@Override
		public String getLocalName(){
			return null;
		}

		@Override
		public String getLocalAddr(){
			return null;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException{
			return null;
		}

		@Override
		public DispatcherType getDispatcherType(){
			return null;
		}

		@Override
		public String getContentType(){
			return null;
		}

		@Override
		public int getContentLength(){
			return 0;
		}

		@Override
		public String getCharacterEncoding(){
			return null;
		}

		@Override
		public Enumeration<String> getAttributeNames(){
			return null;
		}

		@Override
		public Object getAttribute(String arg0){
			return null;
		}

		@Override
		public AsyncContext getAsyncContext(){
			return null;
		}

		@Override
		public void logout() throws ServletException{}

		@Override
		public void login(String arg0, String arg1) throws ServletException{}

		@Override
		public boolean isUserInRole(String arg0){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdValid(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromUrl(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromURL(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromCookie(){
			return false;
		}

		@Override
		public Principal getUserPrincipal(){
			return null;
		}

		@Override
		public HttpSession getSession(boolean arg0){
			return null;
		}

		@Override
		public HttpSession getSession(){
			return null;
		}

		@Override
		public String getServletPath(){
			return null;
		}

		@Override
		public String getRequestedSessionId(){
			return null;
		}

		@Override
		public StringBuffer getRequestURL(){
			return null;
		}

		@Override
		public String getRequestURI(){
			return null;
		}

		@Override
		public String getRemoteUser(){
			return null;
		}

		@Override
		public String getQueryString(){
			return null;
		}

		@Override
		public String getPathTranslated(){
			return null;
		}

		@Override
		public String getPathInfo(){
			return null;
		}

		@Override
		public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException{
			return null;
		}

		@Override
		public Part getPart(String arg0) throws IOException, IllegalStateException, ServletException{
			return null;
		}

		@Override
		public String getMethod(){
			return "GET";
		}

		@Override
		public int getIntHeader(String arg0){
			return 0;
		}

		@Override
		public Enumeration<String> getHeaders(String arg0){
			return null;
		}

		@Override
		public Enumeration<String> getHeaderNames(){
			return null;
		}

		@Override
		public String getHeader(String arg0){
			return null;
		}

		@Override
		public long getDateHeader(String arg0){
			return 0;
		}

		@Override
		public Cookie[] getCookies(){
			return null;
		}

		@Override
		public String getContextPath(){
			return null;
		}

		@Override
		public String getAuthType(){
			return null;
		}

		@Override
		public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException{
			return false;
		}

	}

}