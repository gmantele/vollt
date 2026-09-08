package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import adql.db.FunctionDef;
import tap.auth.*;
import tap.communication.JSONAPIClient;
import tap.formatter.OutputFormat;
import tap.metadata.TAPTable;
import tap.metadata.TAPSchema;
import tap.metadata.TAPMetadata;
import tap.parameters.TAPParameters;
import tap.ServiceConnection;
import tap.TAPJob;
import tap.TAPException;
import tap.log.DefaultTAPLog;
import tap.log.TAPLog;
import tap.TAPFactory;
import uws.service.file.UWSFileManager;
import uws.service.UserIdentifier;
import uws.job.user.JobOwner;
import uws.UWSException;
import javax.servlet.http.HttpServletRequest;	

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

// Java Program to Set up a Basic HTTP Server
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Collection;
import org.json.JSONObject;
import org.json.JSONException;


// Current method to mock httpservletrequest without having to include a whole implementation. Also helps for ServiceConnection
import org.mockito.Mockito;

public class TestAuth {

	// Basic HTTP server to test API authentication
	private static HttpServer server;
	private static JSONAPIClient client;
	private static InetSocketAddress serverAddress;
	// Mock server details
	private static int TEST_SERVER_PORT_DEFAULT = 8090;
	private static String LOCAL_TEST_AUTH_SERVER_IP = "127.0.0.1"; // localhost a example server
	private static int usePort;

	// Test schemas
	private static TAPSchema schema1 = new TAPSchema("SCHEMA1");
	private static TAPSchema schema2 = new TAPSchema("SCHEMA2");
	private static TAPSchema schema2_fuller = new TAPSchema("SCHEMA2");
	private static TAPSchema schema3 = new TAPSchema("SCHEMA3");

	private final static Properties getConfigurableAuthTestProperties(String endpoint){
		Properties validProp = new Properties();
		validProp.setProperty("auth_header_field", "Authorization");
		validProp.setProperty("session_authentication_url", "http://"+LOCAL_TEST_AUTH_SERVER_IP+":"+Integer.toString(8090)+endpoint);
		validProp.setProperty("response_id_field", "userid");
		validProp.setProperty("response_id_datatype", "string");
		validProp.setProperty("response_pseudo_field", "username");
		validProp.setProperty("response_allowed_access_field", "allowed_access");
		validProp.setProperty("auth.schemes", "Basic, Bearer");
		return validProp;
	}


	@BeforeClass
	public static void setUp() throws Exception {

		usePort = TEST_SERVER_PORT_DEFAULT;
		while(!portAvailable(usePort)){
			usePort++; // Keep moving up ports until we find one we can use
		}
		serverAddress = new InetSocketAddress(LOCAL_TEST_AUTH_SERVER_IP,usePort);
		// Create an HttpServer instance
        server = HttpServer.create(serverAddress, 0);
        // Start the server
        server.setExecutor(null); // Use the default executor
        server.start();


		// Create auth contexts
		server.createContext("/auth", new AuthHttpHandler());
		server.createContext("/badauth", new AuthErroringHandler());

        // Load up test schemas
        schema1.addTable("t1");
		schema1.addTable("t2");
		schema1.addTable("t3");
		schema2.addTable("table1");
		schema2_fuller.addTable("table1");
		schema2_fuller.addTable("table2");
		schema3.addTable("t1");
    }

    // Check if port available to host test server
    private static boolean portAvailable(int port) throws IllegalStateException {
	    try (Socket ignored = new Socket("localhost", port)){
	        return false;
	    } catch (ConnectException e){
	        return true;
	    } catch (IOException e){
	        throw new IllegalStateException("Error while trying to check open port", e);
	    }
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop(5);
	}

	/**
	 * Basic test first to ensure the user identifier and the AuthJobOwner classes work correctly without a remote connection,
	 * and all checks work.
	 */
	@Test
	public void testUserIdentifierWWWAuthenticates() throws Exception {
		Properties testProps = getConfigurableAuthTestProperties("/auth");


		testProps.setProperty("auth.schemes", "Basic, Bearer");
		testProps.setProperty("auth.realm", "\"vollt_auth\"");
		testProps.setProperty("auth.Bearer.testprop", "confirmed");

		ConfigurableAuthUserIdentifier authUserIdentifier = new ConfigurableAuthUserIdentifier(testProps);
		List<String> allWWWAuths = authUserIdentifier.getWWWAuthenticates();
		assertEquals(allWWWAuths.size(), 2);

		for (String headerStr : allWWWAuths){
		    // TODO: Actually check the header value
		    String [] parts = headerStr.split(" ");
		    String challenge = parts[0];
		    assertTrue("Challenge recieved: "+challenge,challenge.equals("Basic") || challenge.equals("Bearer"));
			HashMap<String, String> challengeProps = new HashMap<>();
			for (int i = 1; i<parts.length; i++){
				String[] keyValue = parts[i].split("=");
				System.out.println(parts[i]);
				assertEquals(keyValue.length, 2);
				challengeProps.put(keyValue[0], keyValue[1]);
			}

			if (challenge.equals("Basic")){
				assertTrue(challengeProps.containsKey("realm"));
				assertEquals(challengeProps.get("realm"), "\"vollt_auth\"");
				assertFalse(challengeProps.containsKey("testprop"));
			} else if (challenge.equals("Bearer")){
				assertTrue(challengeProps.containsKey("realm"));
				assertEquals(challengeProps.get("realm"), "\"vollt_auth\"");
				assertTrue(challengeProps.containsKey("testprop"));
				assertEquals(challengeProps.get("testprop"), "confirmed");
			}
		}
	}

	/**
	 * Basic test first to ensure the user identifier and the AuthJobOwner classes work correctly without a remote connection,
	 * and all checks work.
	 */
	@Test
	public void testUserIdentifierAuthJobOwner() throws Exception {

		ConfigurableAuthUserIdentifier authUserIdentifier = new ConfigurableAuthUserIdentifier(getConfigurableAuthTestProperties("/auth"));
		
		HashMap<String, Object> userInfo = new HashMap<>();
		// Setup schemas and tables
		List<TAPSchema> schemasAllowed = Arrays.asList(schema1, schema2);
		userInfo.put("allowedData", schemasAllowed);

		AuthJobOwner jobOwner = (AuthJobOwner) authUserIdentifier.restoreUser("001", "tapuser001", userInfo);

		// Now time to run some checks
		// Test schema access
		assertTrue(jobOwner.canAccessSchema(schema1));
		assertTrue(jobOwner.canAccessSchema(schema2));
		assertFalse(jobOwner.canAccessSchema(schema3));

		// A jobowner can access the table if theres a match in the list. The fullname used in the ADQL queries
		assertTrue(jobOwner.canAccessTable(schema1.getTable("t1")));
		assertTrue(jobOwner.canAccessTable(schema1.getTable("t2")));
		assertTrue(jobOwner.canAccessTable(schema1.getTable("t3")));
		assertTrue(jobOwner.canAccessTable(schema2.getTable("table1")));
		assertFalse(jobOwner.canAccessTable(schema2_fuller.getTable("table2")));
		assertFalse(jobOwner.canAccessTable(schema3.getTable("t1")));

		TAPParameters testTapParams = null;
		try{
			testTapParams = buildTAPParameters("SELECT * FROM TAP_SCHEMA.schemas;");
		} catch (TAPException te){
			fail("Failed to build test tap parameters: "+ te.getMessage());
		}
		// Check allowed tap jobs
		TAPJob testJob = new TAPJob((JobOwner) jobOwner, testTapParams);// todo create ADQL query example

		assertFalse(jobOwner.hasExecutePermission(testJob));

		try{
			testTapParams = buildTAPParameters("SELECT * FROM SCHEMA1.t1, SCHEMA2.table1;");
		} catch (TAPException te){
			fail("Failed to build test tap parameters: "+ te.getMessage());
		}
		// Check allowed tap jobs
		testJob = new TAPJob((JobOwner) jobOwner, testTapParams);// todo create ADQL query example
		assertTrue(jobOwner.hasExecutePermission(testJob));

		// Check allowed tap jobs
		try{
			testTapParams = buildTAPParameters("SELECT * FROM SCHEMA1.t1, SCHEMA2.table2;");
		} catch (TAPException te){
			fail("Failed to build test tap parameters: "+ te.getMessage());
		}
		testJob = new TAPJob((JobOwner) jobOwner, testTapParams);// todo create ADQL query example
		assertFalse(jobOwner.hasExecutePermission(testJob));
	}

	@Test
	/**
 	 * Test the ability for ConfigurableAuthUserIdentifier to correctly load properties, and communicate with
 	 * a external server to obtain user information with a given authorisation key.
 	 *
 	 * Ensure all user information including allowed table access permissions is correct.
	 */
	public void testUserIdentifierAPI() throws Exception {
        Properties authProps = getConfigurableAuthTestProperties("/auth");

		ConfigurableAuthUserIdentifier useridentifier = new ConfigurableAuthUserIdentifier(authProps);
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		// Check credentials for User1
		Mockito.when(mockRequest.getHeader(authProps.getProperty("auth_header_field"))).
			thenReturn("Bearer IZ1J08K5Vwzu9J3StE33R6zELhswmyZkE2MMb0pLtec3dwl0IjPPdx189Z1IV7DK");
		AuthJobOwner jobOwner = useridentifier.extractUserId(null, mockRequest);
		assertEquals("id1", jobOwner.getID());
		assertEquals("User1", jobOwner.getPseudo());
		// Check schemas
		assertTrue(jobOwner.canAccessSchema(schema1));
		assertTrue(jobOwner.canAccessSchema(schema2_fuller));
		assertFalse(jobOwner.canAccessSchema(schema3));
		// Check tables
		assertTrue(jobOwner.canAccessTable(schema1.getTable("t1")));
		assertTrue(jobOwner.canAccessTable(schema1.getTable("t2")));
		assertTrue(jobOwner.canAccessTable(schema1.getTable("t3")));
		assertTrue(jobOwner.canAccessTable(schema2.getTable("table1")));
		assertTrue(jobOwner.canAccessTable(schema2_fuller.getTable("table2")));
		assertFalse(jobOwner.canAccessTable(schema3.getTable("t1")));

		// Check credentials for User2
		Mockito.when(mockRequest.getHeader(authProps.getProperty("auth_header_field"))).
			thenReturn("Bearer kp1bSyTa2KqPRIdOV1m4MWea884KeMBeDBl6QDssXK8adj4Y3bo6YrRXzuk93HRm");
		jobOwner = useridentifier.extractUserId(null, mockRequest);
		assertEquals("id2", jobOwner.getID());
		assertEquals("User2", jobOwner.getPseudo());
		// Check schemas
		assertFalse(jobOwner.canAccessSchema(schema1));
		assertTrue(jobOwner.canAccessSchema(schema2_fuller)); // has same name as schema2
		assertTrue(jobOwner.canAccessSchema(schema3));
		// Check tables
		assertTrue(jobOwner.canAccessTable(schema2.getTable("table1")));
		assertFalse(jobOwner.canAccessTable(schema2_fuller.getTable("table2"))); // However schema2 does not contain table2
		assertTrue(jobOwner.canAccessTable(schema3.getTable("t1")));
	}

	/**
	 * Basic test first to ensure the user identifier and the AuthJobOwner classes work correctly without a remote connection,
	 * and all checks work.
	 */
	@Test
	public void testAllowedAnonymous() throws Exception {
		Properties authProps = getConfigurableAuthTestProperties("/auth");
		authProps.setProperty("anonymous_user_support", "true");
		ConfigurableAuthUserIdentifier userIdentifier = new ConfigurableAuthUserIdentifier(authProps);

		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(mockRequest.getHeader(authProps.getProperty("auth_header_field"))).
			thenReturn(null);

		// Should return null without issue
		AuthJobOwner anonJobOwner = userIdentifier.extractUserId(null, mockRequest);

		assertEquals("id0", anonJobOwner.getID());
		assertEquals("Anonymous", anonJobOwner.getPseudo());
		// Check schemas
		assertTrue(anonJobOwner.canAccessSchema(schema2));
		// Check tables
		assertTrue(anonJobOwner.canAccessTable(schema2.getTable("table1")));
	}
	/**
	 * Ensure a UWSException is raised when attempting to create a user without the auth header
	 */
	@Test(expected = UWSException.class)
	public void testNotAllowedAnonymous() throws Exception {
		Properties authProps = getConfigurableAuthTestProperties("/auth");
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(mockRequest.getHeader(authProps.getProperty("auth_header_field"))).
			thenReturn(null);
		authProps.setProperty("anonymous_user_support", "false"); // Now test when its false
		ConfigurableAuthUserIdentifier userIdentifier = new ConfigurableAuthUserIdentifier(authProps);
		try{
			// Should throw a UWSException fulfilling the condition
			userIdentifier.extractUserId(null, mockRequest); 
		} catch(UWSException ue) {
			// Assertion exceptions will fail the test
			assertEquals(ue.getHttpErrorCode(), 401); // Check the Unauthorized code was thrown
			throw ue; // Rethrow to fulfil expected exception requirements
		}

	}

	/**
	 * Ensure a UWSException is raised when attempting to create a user without the auth header
	 */
	@Test(expected = UWSException.class)
	public void testPropagatedError() throws Exception {
		Properties authProps = getConfigurableAuthTestProperties("/badauth");
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(mockRequest.getHeader(authProps.getProperty("auth_header_field"))).
			thenReturn("Bearer notGoingtoWorkAnyway");
		authProps.setProperty("anonymous_user_support", "true");
		ConfigurableAuthUserIdentifier userIdentifier = new ConfigurableAuthUserIdentifier(authProps);
		try{
			// Should throw a UWSException fulfilling the condition
			userIdentifier.extractUserId(null, mockRequest);
		} catch(UWSException ue) {
			// Assertion exceptions will fail the test
			assertEquals(ue.getHttpErrorCode(), 403); // Check the error code given by the http hander is propagated back up here
			throw ue; // Rethrow to fulfil expected exception requirements
		}

	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}

	public static final TAPParameters buildTAPParameters(String query) throws TAPException {
		// Build request expected for T
		// Build dummy service connection
		ServiceConnection dummyServiceConn = Mockito.mock(ServiceConnection.class);

		//setup the behaviour here (or do it in setup method or something)
		HashMap<String,Object> tapParams = new HashMap<>();
		tapParams.put(TAPJob.PARAM_QUERY, query);
		tapParams.put(TAPJob.PARAM_REQUEST, TAPJob.REQUEST_DO_QUERY);
		return new TAPParameters(dummyServiceConn, tapParams);
	}

	public static class AuthHttpHandler implements HttpHandler {
			HashMap<String, UserDetailsContainer> sessContainer = new HashMap<>();
        	public AuthHttpHandler(){
        		sessContainer.put("IZ1J08K5Vwzu9J3StE33R6zELhswmyZkE2MMb0pLtec3dwl0IjPPdx189Z1IV7DK",
        			new UserDetailsContainer("id1", "User1", Arrays.asList(schema1, schema2_fuller))
        		);
        		sessContainer.put("kp1bSyTa2KqPRIdOV1m4MWea884KeMBeDBl6QDssXK8adj4Y3bo6YrRXzuk93HRm",
        			new UserDetailsContainer("id2", "User2", Arrays.asList(schema2, schema3))
        		);
        	}

        	private class UserDetailsContainer {
			    private String userId;
			    private String username;
			    private List<TAPSchema> allowedData;

			    public UserDetailsContainer(String userId, String username, List<TAPSchema> allowedData){
			        this.userId = userId;
			        this.username = username;
			        this.allowedData = allowedData;
			    }
			    // Getters
			    public String getUserId(){
			        return userId;
			    }
			    public String getUsername(){
			        return username;
			    }
			    public Map<String,List<String>> allowedDataAsMap(){
			    	Map<String,List<String>> allowedDataMap = new HashMap<>();
			    	for (TAPSchema userSchema : this.allowedData){
			    		ArrayList<String> allowedTablesList = new ArrayList<String>();
			    		for (TAPTable t : userSchema){
			    			allowedTablesList.add(t.getADQLName());
			    		}
			    		allowedDataMap.put(userSchema.getADQLName(), allowedTablesList);
			    	}
			    	return allowedDataMap;
			    }
			}

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				try{
					// body recieved as string, convert to JSONObject and build response string
					Headers headers = exchange.getRequestHeaders();
					String authStr = headers.getFirst("Authorization");
					exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
					// Set as anonymous user until a token is extracted
					UserDetailsContainer userDetails = new UserDetailsContainer("id0", "Anonymous", Arrays.asList(schema2));
					boolean isAnonymous;
					if (authStr != null){
						if (authStr.startsWith("Bearer ")){
							String token = authStr.replace("Bearer ", "");
							userDetails = sessContainer.get(token);
						}
					}
					JSONObject responseJson = new JSONObject();
					responseJson.put("userid", userDetails.getUserId());
					responseJson.put("username", userDetails.getUsername());
					responseJson.put("allowed_access", userDetails.allowedDataAsMap());
					String response = responseJson.toString();

				    exchange.sendResponseHeaders(200, response.length());
				   	OutputStream os = exchange.getResponseBody();
				    DataOutputStream outStream = new DataOutputStream(os);
					try{
						outStream.writeBytes(response);
						outStream.flush();
					} catch (Exception e){
						System.err.println("Failed to send response "+response+" : "+e.toString());
					}finally {
						outStream.close();
					}
				} catch (Exception e){
					System.out.println("Server error: "+e.toString());
				}
			}
		}

		public static class AuthErroringHandler implements HttpHandler {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				try{
					// body recieved as string, convert to JSONObject and build response string
					String response = "Bad Access!";
				    exchange.sendResponseHeaders(403, response.length());
				   	OutputStream os = exchange.getResponseBody();
				    DataOutputStream outStream = new DataOutputStream(os);
					try{
						outStream.writeBytes(response);
						outStream.flush();
					} catch (Exception e){
						System.err.println("Failed to send response "+response+" : "+e.toString());
					}finally {
						outStream.close();
					}
				} catch (Exception e){
					System.out.println("Server error: "+e.toString());
				}
			}
		}


}
