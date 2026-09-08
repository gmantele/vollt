package tap.config;
import tap.communication.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

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
import java.util.HashMap;
import java.util.List;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONException;

// Setup basic HTTP server
// Driver Class
public class TestAPI {
	private static HttpServer server;
	private static InetSocketAddress serverAddress;
	// Test server details
	private static int TEST_SERVER_PORT_DEFAULT = 8090;
	private static int usePort;

	@BeforeClass
	public static void setUp() throws Exception {
		usePort = TEST_SERVER_PORT_DEFAULT;
		while(!portAvailable(usePort)){
			usePort++; // Keep moving up ports until we find one we can use
		}
		serverAddress = new InetSocketAddress("127.0.0.1",usePort);

		// Create an HttpServer instance
        server = HttpServer.create(serverAddress, 0);

        // Start the server
        server.setExecutor(null); // Use the default executor
        server.start();
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
	 * Test basic get request response, no headers or body
	 */
	@Test
	public void testDefaultAPIStrGet() throws Exception {
		// For testing str
		String endpoint = "/str";
        server.createContext(endpoint, new HttpHandler(){
		   public void handle(HttpExchange exchange) throws IOException {
		       	String response = "TestResponse";
	            exchange.sendResponseHeaders(200, response.length());
	           	OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});

        String serverResponse = "";
		// define a custom HttpHandler
		try {
			// Starting client
			DefaultAPIClient client = new DefaultAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+endpoint, "GET", 5000);
			// Sending request
			serverResponse = client.sendRequest();
		} catch(Exception e){
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		} finally {
			assertEquals("TestResponse",serverResponse);
		}
	}

	/**
	 * Test sending POST request with body data. Response is just expected to mirror back the body
	 */
	@Test
	public void testDefaultAPIStrPost() throws Exception {
		// Test sending text: The text send should be mirrored back
		String endpoint = "/strMirror";
		String requestBody = "TestRequest";
        String serverResponse = "";

		// Testing str with headers
		server.createContext(endpoint, new HttpHandler(){
		   public void handle(HttpExchange exchange) throws IOException {
	            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(reader);
		        String line;
		        while ((line = br.readLine()) != null){
		            sb.append(line);
		        }
		        br.close();
		        String response = sb.toString();

	            exchange.sendResponseHeaders(200, response.length());
	           	OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});
		// Test sending text: The text send should be mirrored back
		try {
			// Starting client
			DefaultAPIClient client = new DefaultAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+endpoint, "POST", 5000);
			// Sending request

			serverResponse = client.sendRequest(requestBody);
		} catch(Exception e){
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		} finally {
			assertEquals(requestBody, serverResponse);
		}
	}

	/**
	 * Test sending POST request with header data. Response is expected to extract these headers and form a response.
	 */
	@Test
	public void testDefaultAPIStrHeaders() throws Exception {
		// Testing str with headers
		String endpoint = "/strheader";
        String serverResponse = "";
		String expectedResponse = "foo=bar,vollt=tap";
		HashMap<String, String> headers = new HashMap<>();
		headers.put("foo", "bar");
		headers.put("vollt", "tap");

		server.createContext(endpoint, new HttpHandler(){
		   public void handle(HttpExchange exchange) throws IOException {
		   		Headers headers = exchange.getRequestHeaders();
		   		exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

	            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), "UTF-8");

		   		StringBuilder sb = new StringBuilder();
		   		if (headers.containsKey("foo")){
		   			sb.append("foo="+headers.getFirst("foo"));
		   		}
		   		else {
		   			sb.append("FOO NOT IN HEADER\n");
		   		}
		   		sb.append(",");

		   		if (headers.containsKey("vollt")){
		   			sb.append("vollt="+headers.getFirst("vollt"));
		   		}
		   		else {
		   			sb.append("VOLLT NOT IN HEADER\n");
		   		}

		        String response = sb.toString();
	            exchange.sendResponseHeaders(200, response.length());
	            OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});

		try {
			// Starting client
			DefaultAPIClient client = new DefaultAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+endpoint, "POST", 5000);
			// Sending request
			serverResponse = client.sendRequest(headers);
		} catch(Exception e){
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		} finally{
			assertEquals(expectedResponse, serverResponse);
		}
	}

	/**
	 * Test sending a GET request, with both the server and client handling JSON data. APIClient should automatically format into a JSONObject
	 */
	@Test
	public void testAPIJSONGet() throws Exception {
		// For testing str
		String endpoint = "/jsonget";

        server.createContext(endpoint, new HttpHandler(){
		   public void handle(HttpExchange exchange) throws IOException {
		       	String response = new JSONObject()
         			.put("JSON", "Hello, World!").toString();

	            exchange.sendResponseHeaders(200, response.length());
	           	OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});

        JSONObject serverResponse = new JSONObject();
		// define a custom HttpHandler
		try {
			// Starting client
			JSONAPIClient client = new JSONAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+endpoint, "GET", 5000);
			// Sending request
			serverResponse = client.sendRequest();
		} catch(Exception e){
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		} finally {
			assertEquals("{\"JSON\":\"Hello, World!\"}", serverResponse.toString());
		}
	}

	/**
	 * Test sending a POST request, with both the server and client handling JSON data. APIClient should automatically format into a JSONObject.
	 *
	 * Response is used to form the test response.
	 */
	@Test
	public void testAPIJSONPost() throws Exception {
		// For testing str
		String endpoint = "/jsonpost";

        server.createContext(endpoint, new HttpHandler(){
		   public void handle(HttpExchange exchange) throws IOException {
		   		// body recieved as string, convert to JSONObject and build response string
		   		InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(reader);
		        String line;
		        while ((line = br.readLine()) != null){
		            sb.append(line);
		        }
		        br.close();
		        String requestText = sb.toString();
		   		JSONObject requestJson = new JSONObject(); // Just initialize to fix later
		   		JSONObject responseJson = new JSONObject(); // Just initialize to fix later
		   		try{
		       		requestJson = new JSONObject(requestText);
         		} catch (JSONException je){
         			responseJson.put("error", "malformed json");
         		} finally {
         			responseJson.put("answer", requestJson.getString("vollt"));
         		}

         		String response = responseJson.toString();
	            exchange.sendResponseHeaders(200, response.length());
	           	OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});
        JSONObject serverResponse = new JSONObject();
       	JSONObject requestPayload = new JSONObject().put("vollt", "tap");
		// define a custom HttpHandler
		try {
			// Starting client
			JSONAPIClient client = new JSONAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+endpoint, "POST", 5000);
			// Sending request
			serverResponse = client.sendRequest(requestPayload);
		} catch(Exception e){
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		} finally {
			assertEquals("{\"answer\":\"tap\"}", serverResponse.toString());
		}
	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}
}
