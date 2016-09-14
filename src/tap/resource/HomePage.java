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
 * Copyright 2015-2016 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.TAPException;
import tap.formatter.OutputFormat;
import uws.UWSToolBox;

/**
 * <p>Write the content of the TAP service's home page.</p>
 * 
 * <p><i>Note:
 * 	This class is using the two following {@link TAP} attributes in order to display the home page:
 * 	{@link TAP#homePageURI} and {@link TAP#homePageMimeType}. The MIME type is used only for the third case below (local file).
 * </i></p>
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
 * @version 2.1 (09/2016)
 * @since 2.0
 */
public class HomePage extends ForwardResource {

	/** Name of this TAP resource. */
	public static final String RESOURCE_NAME = "HOME PAGE";

	/** TAP service owning this resource. */
	protected final TAP tap;

	public HomePage(final TAP tap){
		super(tap.getLogger());
		this.tap = tap;
	}

	@Override
	public void init(final ServletConfig config) throws ServletException{}

	@Override
	public void destroy(){}

	@Override
	public void setTAPBaseURL(String baseURL){}

	@Override
	public final String getName(){
		return RESOURCE_NAME;
	}

	@Override
	public boolean executeResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException, TAPException{
		// Try by default a forward toward the specified file:
		boolean written = forward(tap.homePageURI, tap.homePageMimeType, request, response);

		// DEFAULT: list all available resources:
		if (!written){
			// Set the content type: HTML document
			response.setContentType("text/html");

			// Set the character encoding:
			response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

			// Get the output stream:
			PrintWriter writer = response.getWriter();

			// HTML header + CSS + Javascript:
			writer.print("<!DOCTYPE html>\n<html>\n\t<head>\n\t\t<meta charset=\"UTF-8\">\n\t\t<title>TAP HOME PAGE</title>\n\t\t<style type=\"text/css\">\n\t\t\th1 { text-align: center; }\n\t\t\t.subtitle { font-size: .8em; }\n\t\t\th2 { border-bottom: 1px solid black; }\n\t\t\t.formField textarea { padding: .5em; display: block; margin: 0; }\n\t\t\t.formField { margin-bottom: .5em; }\n\t\t\t#submit { font-weight: bold; font-size: 1.1em; padding: .4em; color: green; }\n\t\t\t#footer { font-style: .8em; font-style: italic; }\n\t\t</style>\n\t\t<script type=\"text/javascript\">\n\t\t\tfunction toggleTextInput(id){\n\t\t\t\tdocument.getElementById(id).disabled = !document.getElementById(id).disabled;\n\t\t\t\tif (document.getElementById(id).disabled){\n\t\t\t\t\tdocument.getElementById(id).value = \"-1\";\n\t\t\t\t\tdocument.getElementById(id).removeAttribute('name');\n\t\t\t\t}else\n\t\t\t\t\tdocument.getElementById(id).name = id;\n\t\t\t}\n\t\t\tfunction toggleUploadFeature(){\n\t\t\t\tdocument.getElementById('uplTable').disabled = !document.getElementById('uplTable').disabled;\n\t\t\t\tif (document.getElementById('uplTable').disabled){\n\t\t\t\t\tdocument.getElementById('uplTable').removeAttribute('name');\n\t\t\t\t\tdocument.getElementById('queryForm').enctype = \"application/x-www-form-urlencoded\";\n\t\t\t\t\tdocument.getElementById('queryForm').removeChild(document.getElementById('uploadParam'));\n\t\t\t\t}else{\n\t\t\t\t\tdocument.getElementById('queryForm').enctype = \"multipart/form-data\";\n\t\t\t\t\tdocument.getElementById('uplTable').name = 'uplTable';\n\t\t\t\t\tvar newInput = document.createElement('input');\n\t\t\t\t\tnewInput.id = \"uploadParam\";\n\t\t\t\t\tnewInput.type = \"hidden\";\n\t\t\t\t\tnewInput.name = \"UPLOAD\";\n\t\t\t\t\tnewInput.value = \"upload,param:uplTable\";\n\t\t\t\t\tdocument.getElementById('queryForm').appendChild(newInput);\n\t\t\t\t}\n\t\t\t}\n\t\t</script>\n\t</head>\n\t<body>");

			// Page title:
			writer.print("\n\t\t<h1 class=\"centered\">TAP HOME PAGE<br/>");
			if (tap.getServiceConnection().getProviderName() != null)
				writer.print("<span class=\"subtitle\">- " + tap.getServiceConnection().getProviderName() + " -</span>");
			writer.print("</h1>");

			// Service description:
			if (tap.getServiceConnection().getProviderDescription() != null)
				writer.print("\n\n\t\t<h2>Service description</h2>\n\t\t<p>" + tap.getServiceConnection().getProviderDescription() + "</p>");

			// List of all available resources:
			writer.print("\n\n\t\t<h2>Available resources</h2>\n\t\t<ul>");
			for(TAPResource res : tap.resources.values())
				writer.println("\n\t\t\t<li><a href=\"" + tap.tapBaseURL + "/" + res.getName() + "\">" + res.getName() + "</a></li>");
			writer.print("\n\t\t</ul>");

			// ADQL query form:
			writer.print("\n\t\t\n\t\t<h2>ADQL query</h2>\n\t\t<noscript>\n\t\t\t<p><strong><u>WARNING</u>! Javascript is disabled in your browser. Consequently, you can submit queries ONLY in synchronous mode and no limit on result nor execution duration can be specified. Besides, no table can be uploaded.</strong></p>\n\t\t</noscript>");
			writer.print("\n\t\t<form id=\"queryForm\" action=\"" + tap.getAccessURL() + "/sync\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" target=\"_blank\">\n\t\t\t<input type=\"hidden\" name=\"REQUEST\" value=\"doQuery\" />\n\t\t\t<input type=\"hidden\" name=\"VERSION\" value=\"1.0\" />\n\t\t\t<input type=\"hidden\" name=\"LANG\" value=\"ADQL\" />\n\t\t\t<input type=\"hidden\" name=\"PHASE\" value=\"RUN\" />\n\t\t\t<div class=\"formField\">\n\t\t\t\t<strong>Query:</strong>\n\t\t\t\t<textarea name=\"QUERY\" cols=\"80\" rows=\"5\">\nSELECT *\nFROM TAP_SCHEMA.tables;</textarea>\n\t\t\t</div>");
			writer.print("\n\n\t\t\t<div class=\"formField\">\n\t\t\t\t<strong>Execution mode:</strong> <input id=\"asyncOption\" name=\"REQUEST\" value=\"doQuery\" type=\"radio\" onclick=\"document.getElementById('queryForm').action='" + tap.getAccessURL() + "/async';\" /><label for=\"asyncOption\"> Asynchronous/Batch</label>\n\t\t\t\t<input id=\"syncOption\" name=\"REQUEST\" value=\"doQuery\" type=\"radio\" onclick=\"document.getElementById('queryForm').action='" + tap.getAccessURL() + "/sync';\" checked=\"checked\" /><label for=\"syncOption\"> Synchronous</label>\n\t\t\t</div>");
			writer.print("\n\t\t\t<div class=\"formField\"><strong>Format:</strong>\n\t\t\t\t<select name=\"FORMAT\">");
			Iterator<OutputFormat> itFormats = tap.getServiceConnection().getOutputFormats();
			OutputFormat fmt;
			while(itFormats.hasNext()){
				fmt = itFormats.next();
				writer.println("\n\t\t\t<option value=\"" + fmt.getMimeType() + "\"" + (fmt.getShortMimeType() != null && fmt.getShortMimeType().equalsIgnoreCase("votable") ? " selected=\"selected\"" : "") + ">" + fmt.getShortMimeType() + "</option>");
			}
			writer.print("\n\t\t\t\t</select>\n\t\t\t</div>");

			// Result limit:
			writer.print("\n\t\t\t<div class=\"formField\">\n\t\t\t\t<input id=\"toggleMaxRec\" type=\"checkbox\" onclick=\"toggleTextInput('MAXREC');\" /><label for=\"toggleMaxRec\"><strong>Result limit:</strong></label> <input id=\"MAXREC\" type=\"text\" value=\"-1\" list=\"maxrecList\" disabled=\"disabled\" /> rows <em>(0 to get only metadata ; a value &lt; 0 means 'default value')</em>\n\t\t\t\t<datalist id=\"maxrecList\">");
			if (tap.getServiceConnection().getOutputLimit() != null && tap.getServiceConnection().getOutputLimit().length >= 2){
				writer.print("\n\t\t\t\t\t<option value=\"" + tap.getServiceConnection().getOutputLimit()[0] + "\">Default</option>");
				writer.print("\n\t\t\t\t\t<option value=\"" + tap.getServiceConnection().getOutputLimit()[1] + "\">Maximum</option>");
			}
			writer.print("\n\t\t\t\t</datalist>\n\t\t\t</div>");

			// Execution duration limit:
			writer.print("\n\t\t\t<div class=\"formField\">\n\t\t\t\t<input id=\"toggleDuration\" type=\"checkbox\" onclick=\"toggleTextInput('EXECUTIONDURATION');\" /><label for=\"toggleDuration\"><strong>Duration limit:</strong></label> <input id=\"EXECUTIONDURATION\" type=\"text\" value=\"-1\" list=\"durationList\" disabled=\"disabled\" /> seconds <em>(a value &le; 0 means 'default value')</em>\n\t\t\t\t<datalist id=\"durationList\">");
			if (tap.getServiceConnection().getExecutionDuration() != null && tap.getServiceConnection().getExecutionDuration().length >= 2){
				writer.print("\n\t\t\t\t\t<option value=\"" + (tap.getServiceConnection().getExecutionDuration()[0] / 1000) + "\">Default</option>");
				writer.print("\n\t\t\t\t\t<option value=\"" + (tap.getServiceConnection().getExecutionDuration()[1] / 1000) + "\">Maximum</option>");
			}
			writer.print("\n\t\t\t\t</datalist>\n\t\t\t</div>");

			// Upload feature:
			if (tap.getServiceConnection().uploadEnabled())
				writer.print("\n\t\t\t<div class=\"formField\">\n\t\t\t\t<input id=\"toggleUpload\" type=\"checkbox\" onclick=\"toggleUploadFeature();\" /><label for=\"toggleUpload\"><strong>Upload a VOTable:</strong></label> <input id=\"uplTable\" type=\"file\" disabled=\"disabled\" /> <em>(the uploaded table must be referenced in the ADQL query with the following full name: TAP_UPLOAD.upload)</em>\n\t\t\t</div>");

			// Footer:
			writer.print("\n\t\t\t<input id=\"submit\" type=\"submit\" value=\"Execute!\" />\n\t\t</form>\n\t\t<br/>\n\t\t<hr/>\n\t\t<div id=\"footer\">\n\t\t\t<p>Page generated by <a href=\"http://cdsportal.u-strasbg.fr/taptuto/\" target=\"_blank\">TAPLibrary</a> v2.0</p>\n\t\t</div>\n\t</body>\n</html>");

			writer.flush();

			written = true;
		}

		return written;
	}

}
