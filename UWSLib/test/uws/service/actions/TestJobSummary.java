package uws.service.actions;

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

import uws.UWSException;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.user.JobOwner;
import uws.service.wait.BlockingPolicy;
import uws.service.wait.LimitedBlockingPolicy;

public class TestJobSummary {

	@Before
	public void setUp() throws Exception{}

	@Test
	public void testBlock(){
		long waitingDuration = 1;

		/* **************************************** */
		/* NO BLOCKING POLICY (=> default behavior) */

		UWSJob job = new UWSJob(new UWSParameters());
		TestHttpServletRequest req = new TestHttpServletRequest();
		req.addParams("WAIT", "" + waitingDuration);

		// Without request and/or job => No blocking should occur:
		TestThread t = new TestThread(null, null, null, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertTrue(t.getTime() >= 0 && t.getTime() < 1);
		t = new TestThread(null, req, null, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertTrue(t.getTime() >= 0 && t.getTime() < 1);
		t = new TestThread(null, null, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertTrue(t.getTime() >= 0 && t.getTime() < 1);

		// No WAIT value or a not legal value (not integer) => no blocking:
		req.clearParams();
		t = new TestThread(null, req, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertTrue(t.getTime() >= 0 && t.getTime() < 1);
		req.addParams("WAIT", "");
		t = new TestThread(null, req, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertTrue(t.getTime() >= 0 && t.getTime() < 1);
		req.clearParams();
		req.addParams("WAIT", "foo");
		t = new TestThread(null, req, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertTrue(t.getTime() >= 0 && t.getTime() < 1);

		// With a job not in an "active" phase => No blocking ; immediate return:
		req.clearParams();
		req.addParams("WAIT", "" + waitingDuration);
		ExecutionPhase[] nonActivePhases = new ExecutionPhase[]{ExecutionPhase.COMPLETED,ExecutionPhase.ABORTED,ExecutionPhase.ERROR,ExecutionPhase.ARCHIVED,ExecutionPhase.HELD,ExecutionPhase.SUSPENDED,ExecutionPhase.UNKNOWN};
		for(ExecutionPhase p : nonActivePhases){
			try{
				job.setPhase(p, true);
			}catch(UWSException ue){
				fail("No error should occur when forcing a phase modification!");
			}
			t = new TestThread(null, req, job, null);
			t.start();
			waitFor(t);
			assertFalse(t.isAlive());
			assertTrue(t.getTime() >= 0 && t.getTime() < 1);
		}

		// With a job in one of the "active" phases:
		ExecutionPhase[] activePhases = new ExecutionPhase[]{ExecutionPhase.PENDING,ExecutionPhase.QUEUED,ExecutionPhase.EXECUTING};
		for(ExecutionPhase p : activePhases){
			try{
				job.setPhase(p, true);
			}catch(UWSException ue){
				fail("No error should occur when forcing a phase modification!");
			}
			t = new TestThread(null, req, job, null);
			t.start();
			waitFor(t);
			assertFalse(t.isAlive());
			assertEquals(waitingDuration, t.getTime());
		}

		// With a PHASE parameter:
		req.addParams("PHASE", "EXECUTING");
		activePhases = new ExecutionPhase[]{ExecutionPhase.PENDING,ExecutionPhase.QUEUED};
		for(ExecutionPhase p : activePhases){
			try{
				job.setPhase(p, true);
			}catch(UWSException ue){
				fail("No error should occur when forcing a phase modification!");
			}
			t = new TestThread(null, req, job, null);
			t.start();
			waitFor(t);
			assertFalse(t.isAlive());
			assertTrue(t.getTime() >= 0 && t.getTime() < 1);
		}
		try{
			job.setPhase(ExecutionPhase.EXECUTING, true);
		}catch(UWSException ue){
			fail("No error should occur when forcing a phase modification!");
		}
		t = new TestThread(null, req, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertEquals(waitingDuration, t.getTime());

		// With several WAIT and PHASE parameters:
		waitingDuration = 2;
		req.addParams("wait", "" + waitingDuration);
		req.addParams("PHASE", "PENDING");
		activePhases = new ExecutionPhase[]{ExecutionPhase.EXECUTING,ExecutionPhase.QUEUED};
		for(ExecutionPhase p : activePhases){
			try{
				job.setPhase(p, true);
			}catch(UWSException ue){
				fail("No error should occur when forcing a phase modification!");
			}
			t = new TestThread(null, req, job, null);
			t.start();
			waitFor(t);
			assertFalse(t.isAlive());
			assertTrue(t.getTime() >= 0 && t.getTime() < 1);
		}
		try{
			job.setPhase(ExecutionPhase.PENDING, true);
		}catch(UWSException ue){
			fail("No error should occur when forcing a phase modification!");
		}
		t = new TestThread(null, req, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertEquals(1, t.getTime());

		// With several WAIT parameters, including a negative one:
		req.clearParams();
		req.addParams("Wait", "-10");
		req.addParams("wait", "1");
		req.addParams("WAIT", "5");
		try{
			job.setPhase(ExecutionPhase.PENDING, true);
		}catch(UWSException ue){
			fail("No error should occur when forcing a phase modification!");
		}
		t = new TestThread(null, req, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertEquals(1, t.getTime());

		/* ********************** */
		/* WITH A BLOCKING POLICY */
		req.clearParams();
		req.addParams("wait", "" + waitingDuration);
		long policyDuration = 1;
		try{
			job.setPhase(ExecutionPhase.EXECUTING, true);
		}catch(UWSException ue){
			fail("No error should occur when forcing a phase modification!");
		}
		t = new TestThread(new LimitedBlockingPolicy(policyDuration), req, job, null);
		t.start();
		waitFor(t);
		assertFalse(t.isAlive());
		assertEquals(policyDuration, t.getTime());
	}

	protected final void waitALittle(){
		synchronized(this){
			try{
				Thread.sleep(10);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}

	protected final void waitFor(final Thread t){
		synchronized(this){
			try{
				t.join();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}

	protected static final class TestThread extends Thread {

		private final BlockingPolicy policy;
		private final HttpServletRequest req;
		private final UWSJob job;
		private final JobOwner user;

		public long start = -1;
		public long end = -1;

		public TestThread(final BlockingPolicy policy, final HttpServletRequest req, final UWSJob job, final JobOwner user){
			this.policy = policy;
			this.req = req;
			this.job = job;
			this.user = user;
		}

		@Override
		public void run(){
			start = System.currentTimeMillis();
			JobSummary.block(policy, req, job, user);
			end = System.currentTimeMillis();
		}

		/**
		 * Get execution duration in seconds.
		 *
		 * @return	Execution duration of this thread in seconds
		 *        	or -1 if still alive.
		 */
		public final long getTime(){
			return (isAlive() || end > -1) ? (end - start) / 1000 : -1;
		}

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
		public String getRemoteAddr(){
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