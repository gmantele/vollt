package tap.resource;

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
 * Copyright 2015 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPException;
import uk.ac.starlink.votable.VOSerializer;

/**
 * <p>Additional TAP endpoint: <code>/examples</code>. Then, this {@link TAPResource} writes
 * a list of TAP examples.</p>
 * 
 * <p>
 * 	The returned document MUST be a RDFa document as requested by the IVOA documents: TAPNote 1.0 or DALI 1.0.
 * 	The syntax used in the returned document is thus expected to be one of those described by the two
 * 	IVOA documents ; for now no particular syntax has been fixed by the IVOA community. 
 * </p>
 * 
 * <p>
 * 	This TAP endpoint is also a VOSI resource. A capability is then provided thanks to the function
 * 	{@link #getCapability()}. The returned capability is the following:
 * </p>
 * <pre>&lt;capability standardID="ivo://ivoa.net/std/DALI#examples&gt;
 *     &lt;interface xsi:type="vr:WebBrowser" role="std"&gt;
 * 	       &lt;accessURL use="full"&gt;
 *             &lt;-- Full URL of the TAP service suffixed by "/examples" --&gt;
 * 	       &lt;/accessURL&gt;
 * 	   &lt;/interface&gt;
 *  &lt;/capability&gt;</pre>
 * 
 * <p>
 * 	The home page URI is expected to be either relative/absolute path (both related to the Web Application
 * 	directory, NOT the local file system), a <code>file:</code> URI (pointing toward a local file system file)
 * 	or a URL (basically any URI whose the scheme is not <code>file:</code>).
 * </p>
 * 
 * <p>
 * 	To read/write the specified file, this class extends {@link ForwardResource} in order to use its function
 * 	{@link ForwardResource#forward(String, String, HttpServletRequest, HttpServletResponse)}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.1 (11/2015)
 * @since 2.1
 */
public class Examples extends ForwardResource implements VOSIResource {

	/** Name of this TAP resource. */
	public static final String RESOURCE_NAME = "examples";
	
	/** Standard ID of this VOSI resource. */
	public static final String STANDARD_ID = "ivo://ivoa.net/std/DALI#examples";
	
	/** MIME type of this resource. */
	public static final String MIME_TYPE = "application/xhtml+xml";
	
	/** File containing the <code>/examples</code> endpoint content. */
	public final String examplesFile;

	/** <p>URL toward this TAP resource.
	 * This URL is particularly important for its declaration in the capabilities of the TAP service.</p>
	 * 
	 * <p><i>Note: By default, it is just the name of this resource. It is updated after initialization of the service
	 * when the TAP service base URL is communicated to its resources. Then, it is: baseTAPURL + "/" + RESOURCE_NAME.</i></p> */
	protected String accessURL = getName();

	/**
	 * Build an <code>/examples</code> endpoint whose the content is provided by the specified file.
	 * 
	 * @param tap	The TAP service whose the logger must be used. 
	 * @param file	The file containing the content of this endpoint.
	 *            	It must be a URI with no scheme to reference a file inside the Web Application directory,
	 *            	with the scheme <code>file:</code> to reference a local system file
	 *            	or with another scheme to make an explicit redirection toward the specified URL.
	 * 
	 * @throws NullPointerException	If at least one of the parameters is missing.
	 */
	public Examples(final TAP tap, final String file) throws NullPointerException {
		super(tap.getLogger());
		if (file == null)
			throw new NullPointerException("Examples content file missing! Impossible to build a /examples endpoint.");
		examplesFile = file;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {}

	@Override
	public void destroy() {}

	@Override
	public final void setTAPBaseURL(final String baseURL){
		accessURL = ((baseURL == null) ? "" : (baseURL + "/")) + getName();
	}

	@Override
	public String getName() {
		return RESOURCE_NAME;
	}

	@Override
	public String getCapability() {
		return "\t<capability " + VOSerializer.formatAttribute("standardID", getStandardID()) + ">\n" + "\t\t<interface xsi:type=\"vr:WebBrowser\" role=\"std\">\n" + "\t\t\t<accessURL use=\"full\"> " + ((getAccessURL() == null) ? "" : VOSerializer.formatText(getAccessURL())) + " </accessURL>\n" + "\t\t</interface>\n" + "\t</capability>";
	}

	@Override
	public String getAccessURL() {
		return accessURL;
	}

	@Override
	public final String getStandardID() {
		return STANDARD_ID;
	}

	@Override
	public boolean executeResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException, TAPException {
		return forward(examplesFile, MIME_TYPE, request, response);
	}

}
