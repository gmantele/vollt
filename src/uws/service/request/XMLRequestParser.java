package uws.service.request;

/*
 * This file is part of UWSLibrary.
 *
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2017-2024 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import uws.UWSException;
import uws.job.jobInfo.XMLJobInfo;
import uws.service.UWS;
import uws.service.file.UWSFileManager;

/**
 * This parser aims to copy the full content of an HTTP request if it is
 * identified as an XML document.
 *
 * <p><b>UWS's Job Description</b></p>
 *
 * <p>
 * 	Actually, this parser implements the possibility defined in the UWS 1.0
 * 	standard to provide an XML document describing the parameters of a UWS job.
 * 	This XML document is then called "Job Description".
 * </p>
 *
 * <p><b>Validation</b></p>
 *
 * <p>
 * 	In the UWS 1.0 standard, it is said that this Job Description has to follow
 * 	a Job Description Language (JDL ; that's to say a known pattern describing
 * 	the expected job parameters) dependent of the UWS service implementation.
 * </p>
 *
 * <p>
 * 	By default, this parser copies the request content and checks it is an XML
 * 	document. Nothing else is done, and particularly not the validation of
 * 	its content. To do so, a particular service implementation can extend this
 * 	class and overwrite its function {@link #validate(InputStream)}. By default
 * 	this function just ensures the request content is a valid XML document.
 * </p>
 *
 * <p><b>Document access</b></p>
 *
 * <p>
 * 	Once parsed, the request content will be made accessible through an
 * 	{@link HttpServletRequest} attribute under the name
 * 	<b>{@value uws.service.UWS#REQ_ATTRIBUTE_JOB_DESCRIPTION}</b>.
 * 	The associated object <b>is typed as an {@link XMLJobInfo}</b>.
 * </p>
 *
 * <p><i>Note:
 * 	Afterwards, it is intended to be attached to a {@link uws.job.UWSJob} and
 * 	then made accessible through its function
 * 	{@link uws.job.UWSJob#getJobInfo()}.
 * </i></p>
 *
 * <p><b>Document storage</b></p>
 *
 * <p>{@link XMLJobInfo} gives two storage possibility:</p>
 * <ol>
 * 	<li><i>in memory</i> with the constructor
 * 		{@link XMLJobInfo#XMLJobInfo(String) XMLJobInfo(String)}</li>
 * 	<li><i>in a file</i> with the constructor
 * 		{@link XMLJobInfo#XMLJobInfo(UploadFile) XMLJobInfo(UploadFile)}</li>
 * </ol>
 *
 * <p>
 * 	The storage chosen by this parser depends on the size of the input document.
 * 	If it exceeds {@link #SMALL_XML_THRESHOLD} (expressed in bytes), then
 * 	the document will be stored inside a file. Otherwise it will be kept in
 * 	memory. To change this threshold, it is just needed to set the static
 * 	field {@link #SMALL_XML_THRESHOLD} to the desired value (in bytes).
 * 	By default, it is set to {@value #DEFAULT_SMALL_XML_THRESHOLD} bytes.
 * </p>
 *
 * <p><b>Important:</b>
 * 	It is possible to prevent the unwanted storage of a very large document
 * 	by setting the limit {@link #SIZE_LIMIT} to a different value (in bytes).
 * 	If the input document exceeds this size, the request will be rejected with
 * 	an 413 (REQUEST ENTITY TOO LARGE) error.
 * 	By default this limit is set to {@value #DEFAULT_SIZE_LIMIT} bytes.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.5 (08/2024)
 * @since 4.2
 */
public class XMLRequestParser implements RequestParser {

	/** Default maximum allowed size for an HTTP request content: 200 kiB. */
	public static final int DEFAULT_SIZE_LIMIT = 200 * 1024;

	/** <p>Maximum allowed size for an HTTP request content. Over this limit, an exception is thrown and the request is aborted.</p>
	 * <p><i>Note:
	 * 	The default value is {@link #DEFAULT_SIZE_LIMIT} (= {@value #DEFAULT_SIZE_LIMIT} Bytes).
	 * </i></p>
	 * <p><i>Note:
	 * 	This limit is expressed in bytes and can not be negative.
	 *  Its smallest possible value is 0. If the set value is though negative,
	 *  it will be ignored and {@link #DEFAULT_SIZE_LIMIT} will be used instead.
	 * </i></p> */
	public static int SIZE_LIMIT = DEFAULT_SIZE_LIMIT;

	/** Default threshold for XML document that can be kept entirely in memory: 2 kiB. */
	public static final int DEFAULT_SMALL_XML_THRESHOLD = 2 * 1024;

	/** This threshold determines whether an XML request content should be
	 * stored in memory or inside a file.
	 * <p><i>In short: between 0 and this value, the
	 * XML document will be stored in memory ; above this value, it will be
	 * stored in a file.</i></p> */
	public static int SMALL_XML_THRESHOLD = DEFAULT_SMALL_XML_THRESHOLD;

	/** File manager to use to create {@link UploadFile} instances.
	 * It is required by this new object to execute open, move and delete operations whenever it could be asked. */
	protected final UWSFileManager fileManager;

	/**
	 * Build the request parser.
	 *
	 * @param fileManager	A file manager. <i>Must NOT be NULL.</i>
	 */
	public XMLRequestParser(final UWSFileManager fileManager){
		if (fileManager == null)
			throw new NullPointerException("Missing file manager => can not create an XMLRequestParser!");
		this.fileManager = fileManager;
	}

	@Override
	public Map<String, Object> parse(final HttpServletRequest request) throws UWSException{
		// Result of the request parsing => a JobInfo containing or pointing toward the sent request content:
		XMLJobInfo jobDesc = null;

		// Prepare to write a file if the XML is too large to fit in memory:
		// (note: this file has to be deleted if not used or in case or error)
		Object reqID = request.getAttribute(UWS.REQ_ATTRIBUTE_ID);
		if (reqID == null || !(reqID instanceof String))
			reqID = (new Date()).getTime();
		File xmlFile = new File(fileManager.getTmpDirectory(), "JOB_DESCRIPTION_" + reqID);

		OutputStream output = null;
		InputStream input = null;
		long totalLength = 0;
		try{
			// prepare the reading of the HTTP request body:
			input = new BufferedInputStream(request.getInputStream());

			// open in WRITE access the output file:
			output = new BufferedOutputStream(new FileOutputStream(xmlFile));

			// compute the maximum limit and the memory size threshold:
			final int maxSize = (SIZE_LIMIT < 0 ? DEFAULT_SIZE_LIMIT : SIZE_LIMIT);
			final int memoryThreshold = (SMALL_XML_THRESHOLD < 0 ? DEFAULT_SMALL_XML_THRESHOLD : SMALL_XML_THRESHOLD);
			final String tooLargeErrorMsg = "XML document too large (>" + maxSize + " bytes) => Request rejected! You should see with the service administrator to extend this limit.";

			// Start reading the HTTP request body:
			byte[] buffer = new byte[memoryThreshold + 1];
			int len = input.read(buffer);

			// If nothing, no body and no parameter => stop here immediately:
			if (len <= 0){
				output.close();
				output = null;
				xmlFile.delete();
			}
			// If the HTTP request body is already finished => small document => memory storage:
			else if (len <= memoryThreshold){
				output.close();
				output = null;
				xmlFile.delete();
				if (len > maxSize)
					throw new UWSException(UWSException.REQUEST_ENTITY_TOO_LARGE, tooLargeErrorMsg);
				else{
					// Build the corresponding String:
					String smallXML = new String(buffer, 0, len, (request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8"));

					// Check it is really an XML document:
					validate(smallXML);

					// Finally build the corresponding Job-Description:
					jobDesc = new XMLJobInfo(smallXML);
				}
			}
			// Otherwise....
			else{
				// ...store the full content inside the temporary file
				// until the EOF or a length exceed:
				do{
					output.write(buffer, 0, len);
					totalLength += len;
					// if content too large => stop here with an error:
					if (totalLength > maxSize){
						output.close();
						output = null;
						xmlFile.delete();
						throw new UWSException(UWSException.REQUEST_ENTITY_TOO_LARGE, tooLargeErrorMsg);
					}
				}while((len = input.read(buffer)) > 0);
				output.flush();
				output.close();
				output = null;

				// Check the file is really an XML document:
				validate(xmlFile);

				// Create a UWS wrapping for this uploaded file:
				UploadFile xmlUpload = new UploadFile(UWS.REQ_ATTRIBUTE_JOB_DESCRIPTION, xmlFile.toURI().toString(), fileManager);
				xmlUpload.setMimeType(request.getContentType());
				xmlUpload.setLength(totalLength);

				// And create the corresponding jobInfo:
				jobDesc = new XMLJobInfo(xmlUpload);
			}
		}catch(IOException ioe){
			xmlFile.delete();
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Internal error => Impossible to get the XML document from the HTTP request!");
		}catch(UWSException ue){
			xmlFile.delete();
			throw ue;
		}finally{
			if (output != null){
				try{
					output.close();
				}catch(IOException ioe2){
				}
			}
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe2){
				}
			}
		}

		// Put the job description in a HttpServletRequest attribute:
		if (jobDesc != null)
			request.setAttribute(UWS.REQ_ATTRIBUTE_JOB_DESCRIPTION, jobDesc);

		// Return an empty map => no parameter has been directly provided:
		return new HashMap<String, Object>(0);
	}

	/**
	 * Validate the given XML document.
	 *
	 * <p>
	 * 	By default, it is only ensured this document is an XML one.
	 * </p>
	 *
	 * @param smallXML		The document to check.
	 *
	 * @throws UWSException	If the given document is not valid.
	 *
	 * @see #validate(InputStream)
	 */
	protected void validate(final String smallXML) throws UWSException{
		validate(new ByteArrayInputStream(smallXML.getBytes()));
	}

	/**
	 * Validate the specified XML document.
	 *
	 * <p>
	 * 	By default, it is only ensured this document is an XML one.
	 * </p>
	 *
	 * @param xmlFile		The file containing the document to check.
	 *
	 * @throws UWSException	If the specified document is not valid.
	 *
	 * @see #validate(InputStream)
	 */
	protected void validate(final File xmlFile) throws UWSException{
		InputStream input = null;
		try{
			input = new FileInputStream(xmlFile);
			validate(input);
		}catch(IOException ioe){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe);
		}finally{
			if (input != null){
				try{
					input.close();
				}catch(IOException ioe){
				}
			}
		}
	}

	/**
	 * Validate the given XML document.
	 *
	 * <p>
	 * 	By default, it is only ensured this document is an XML one.
	 * </p>
	 *
	 * @param input			Stream toward the document to check.
	 *
	 * @throws UWSException	If the given document is not valid.
	 */
	protected void validate(final InputStream input) throws UWSException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true); // why not :)
		spf.setValidating(false);    // no need to check the DTD or XSDs
		SAXParser saxParser = null;
		try{
			saxParser = spf.newSAXParser();
			saxParser.parse(input, new DefaultHandler());
		}catch(SAXParseException spe){
			throw new UWSException(UWSException.BAD_REQUEST, "Incorrect XML input! ERROR at [l." + spe.getLineNumber() + ", c." + spe.getColumnNumber() + "]: " + spe.getMessage() + ".");
		}catch(Exception se){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, se);
		}
	}

	/**
	 * Utility method that determines whether the content of the given
	 * request is an XML (or XML-derived) document.
	 *
	 * <p><b>Important:</b>
	 * 	This function just tests the content-type of the request.
	 * 	Neither the HTTP method (e.g. GET, POST, ...) nor the content is tested.
	 * </p>
	 *
	 * @param request	The servlet request to be evaluated.
	 *               	<i>Must NOT be NULL.</i>
	 *
	 * @return	<i>true</i> if the request is an XML document,
	 *        	<i>false</i> otherwise.
	 */
	public final static boolean isXMLRequest(final HttpServletRequest request){
		// Extract the content type and determine if it is an XML request:
		String contentType = request.getContentType();
		if (contentType == null)
			return false;
		else if (contentType.toLowerCase().matches("(text|application)/(.+\\+)?xml"))
			return true;
		else
			return false;
	}

}
