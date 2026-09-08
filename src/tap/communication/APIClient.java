package tap.communication;
/*
 * This file is part of TAPLibrary.
 *
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2015-2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Collections;
import java.util.List;


import tap.TAPException;

/**
 * <p>Provides an generic abstract class for acting as a client for an API. Inherit to handle
 * different payload and response data formats</p>
 *
 * <p>
 * This class serves as a base class for handling API requests and their responses.
 * This inital class defines an existing method for sending data via GET or POST request, and
 * provides default default behaviour for communicating data to and from an API (conversion to byte,
 * assumed to get String response).
 * </p>
 *
 *
 * <p>
 * When extending this class, it is expected the subclass will have a specific data structure/class
 * in mind, with it's own conversion to String and parsing method from String. Thus this class
 * provides abstract functions convertToString and convertFromString to define these behaviours.
 *
 * Existing classes are included for handling common data formats, including {@link JSONAPIClient} and {@link DefaultAPIClient}
 * </p>
 *
 *
 * @author Anthony Heng (AAO)
 * @version 04/2025
 */
public abstract class APIClient<T> {

	// OVERRIDE IN SUBCLASSES
	/**
	 * Convert the payload to a string representation to a request
	 * @param  payload Data to send as payload
	 * @return         String representation of the payload to send
	 */
	protected abstract String convertToString(T payload);

	/**
	 * Convert the data from a string representation from a response
	 * @param  data Data to be recieved from a request
	 * @return         String representation of the payload to send
	 */
	protected abstract T convertFromString(String data);


	/** URL to communicate with the API. To be set using a config file */
	protected URL url;

	/** Alternate payload encodings can be set in constructor, but utf-8 is
	 * common enough to be a good default */
	protected String stringEncoding = "UTF-8";

	/** http request type (POST or GET). To be converted from string during
	 * the Constructor*/
	protected String requestMethod;

	/** In milliseconds the amount of time this client will wait for an API before timing out **/
	protected int timeoutDuration;


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Create a APIClient for a given URL, and what kind of requests to send,
	 * and the encoding used for payloads
	 * @param  urlString     URL of the API to communicate with
	 * @param  requestMethod Type of request to send. Either "POST" or "GET"
	 * @param  stringEncoding  Encoding used for payloads
	 *
	 * @throws TAPException If the URL is malformed
	 * @throws IllegalArgumentException If <code>requestMethod</code> is not "POST" or "GET"
	 */
	public APIClient(String urlString, String requestMethod, int timeoutDuration, String stringEncoding) throws TAPException, IllegalArgumentException {
		this(urlString, requestMethod, timeoutDuration);
		this.stringEncoding = stringEncoding;
	}

	/**
	 * Create a APIClient for a given URL, what kind of requests to send
	 * @param  urlString     URL of the API to communicate with
	 * @param  requestMethod Type of request to send. Either "POST" or "GET"
	 * @param  stringEncoding  Encoding used for payloads
	 * @param  timeoutDuration In milliseconds, how long this client should wait before timing out with an API
	 *
	 * @throws TAPException If the URL is malformed
	 * @throws IllegalArgumentException If <code>requestMethod</code> is not "POST" or "GET"
	 */
	public APIClient(String urlString, String requestMethod, int timeoutDuration) throws TAPException, IllegalArgumentException {
		try{
			this.url = new URI(urlString).toURL();
		} catch (URISyntaxException e){
        	// rethrow malformed URI exceptions as TAP exceptions
        	throw new TAPException("URISyntaxException : "+e.getMessage());
		} catch (MalformedURLException e){
        	throw new TAPException("MalformedURLException : "+e.getMessage());
        }

		if (requestMethod.equals("POST") || requestMethod.equals("GET")){
			this.requestMethod = requestMethod;
		} else{
			throw new IllegalArgumentException("requestMethod must be either \"POST\" or \"GET\"");
		}
		this.timeoutDuration = timeoutDuration;
	}

	/**
	 * Send the request only with no data. Useful for GET requests.
	 * @return response as object T
	 */
	public T sendRequest() throws TAPException, IOException{
		// Attach empty headers
		Map<String, String> headers = Collections.<String, String>emptyMap();
		return convertFromString(getStringResponse(headers, ""));
	}


	/**
	 * Only send header data with request, without any payload
	 * @param headers Header fields to send to the API URL
	 * @return response as object T
	 */
	public T sendRequest(Map<String, String> headers) throws TAPException, IOException{
		return convertFromString(getStringResponse(headers, ""));
	}

	/**
	 * Send a request only with a payload. The payload will be converted to a String to send through.
	 * The payload will not have any effect if the request method is GET
	 *
	 * @param payload payload to send. Only for POST requests
	 * @return response as an object of type T
	 */
	public T sendRequest(T payload) throws TAPException, IOException{
		String payloadAsString = convertToString(payload);
		Map<String, String> headers = Collections.<String, String>emptyMap();
		return convertFromString(getStringResponse(headers, payloadAsString));
	}

	/**
	 * Send a request with a payload. The payload will be converted to a String to send through.
	 * The payload will not have any effect if the request method is GET
	 *
	 * @param headers Header fields to send to the API URL
	 * @param payload payload to send. Only for POST requests
	 * @return response as an object of type T
	 */
	public T sendRequest(Map<String, String> headers, T payload) throws TAPException, IOException{
		String payloadAsString = convertToString(payload);
		return convertFromString(getStringResponse(headers, payloadAsString));
	}

	/**
	 * Load the data from a given InputStream. Used by getStringResponse for loading the
	 * error or output streams of a given connection.
	 * @param  inputStream input stream to read data from
	 * @return Data read from the input stream
	 */
	private String readFromStream(InputStream inputStream, String streamEncoding) throws IOException{

		StringBuilder sb = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName(streamEncoding));

		BufferedReader br = new BufferedReader(reader);

        String line;
        while ((line = br.readLine()) != null){
            sb.append(line);
        }
        br.close();

		return sb.toString();

	}

	/**
	 * Send data to api. Get response back in string form.
	 * @param  headers    Header fields to send to the API URL
	 * @param  payloadStr Payload in string format. Will have been converted from format T using convertFromString()
	 * @return            Data from API based on request
	 *
	 * @throws IOException If the connection the API url fails.
	 * @throws TAPException If the HTTP response is a non-successful one (>=200 and <300)
	 */
	protected String getStringResponse(Map<String, String> headers, String payloadStr) throws TAPException, IOException{
			HttpURLConnection conn;
			// Create URL object
        	conn = (HttpURLConnection) url.openConnection();

	        // Set request method
	        conn.setRequestMethod(this.requestMethod);


			conn.setConnectTimeout(this.timeoutDuration);

			//Transform payload to encoding

	        // Add all headers to the request
	        for (Map.Entry<String, String> entry : headers.entrySet())
            	conn.setRequestProperty(entry.getKey(), entry.getValue());

	        if (this.requestMethod.equals("POST")){
	        	conn.setDoOutput(true); // Enable writing output to the request
	        	// Send payload
				OutputStream apiOut = conn.getOutputStream();
				try{
					byte[] payloadBytes = payloadStr.getBytes(this.stringEncoding);
					apiOut.write(payloadBytes);
					apiOut.flush();
					apiOut.close();
				} catch (IOException ioe){
	        		throw new TAPException("Failed to send output data through connection to "+url.toString()+" : "+ioe.getMessage());
				}
	        }
			// RESPONSE
			// Read response
	        int responseCode;
	        try{
	        	responseCode = conn.getResponseCode();
	        } catch (IOException ioe){
	        	throw new TAPException("API Communication Error at "+this.url.toString()+": No Response Code : "+ioe.getMessage());
	        }

	        String responseEncoding = conn.getContentEncoding();
	        if (responseEncoding == null){ // Encoding not specified, attempt to use same encoding as client
            	responseEncoding = this.stringEncoding;
            }

            if (responseCode >= 200 && responseCode < 300){ // If response is a success code
            	return readFromStream(conn.getInputStream(), responseEncoding);
            } else {
            	String errorMessage = readFromStream(conn.getErrorStream(), responseEncoding);
	            // Throw an exception with the error details
	            throw new TAPException("API Communication Error at "+this.url.toString()+": Response code " + responseCode + ": " + errorMessage.toString().trim(), responseCode);
            }
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	/**
	 * Get the request method used for API requests (either POST or GET)
	 *
	 * @return	The request method (either POST or GET)
	 */
	public String getRequestMethod(){
		return requestMethod;
	}

	/**
	 * Get the URL object for the API resource
	 *
	 * @return URL object for the API resource
	 */
	public URL getURL(){
		return url;
	}

	/**
	 * Get the text encoding for Strings, most often UTF-8
	 *
	 * @return name of the text encoding for both request and response bodies
	 */
	public String getStringEncoding(){
		return this.stringEncoding;
	}
}