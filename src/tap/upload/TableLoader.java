package tap.upload;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Enumeration;

import tap.TAPException;
import tap.parameters.TAPParameters;

import com.oreilly.servlet.MultipartRequest;

/**
 * <p>Represent an uploaded table in a {@link TAPParameters} object.</p>
 * 
 * <p>
 * 	This class is very useful to interpret the "upload" parameter and to get the ADQL name of the table
 * 	and particularly to get easily an input stream on its data. Thus, it is able to open a stream on table data
 * 	provided as a URL or inline (inside a multipart HTTP request). 
 * </p>
 * 
 * <p>The syntax for the "upload" parameter is the following:</p>
 * <ul>
 * 	<li><b>Case tables provided as URL:</b> table_a,http://host_a/path;table_b,http://host_b/path;...</li>
 * 	<li><b>Case tables provided inline:</b> table_c,param:table1;...
 * 		and "table1" is the name of the parameter (a multipart item = a file) containing the table data.
 * 	</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (08/2014)
 */
public class TableLoader {
	/** Regular expression of any acceptable URL for a table data source. */
	private static final String URL_REGEXP = "^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	/** Prefix of a multipart item name (when tables are included inline in a multipart HTTP request). */
	private static final String PARAM_PREFIX = "param:";

	/** Name of the uploaded table. This name is the one used in the ADQL query. */
	public final String tableName;

	/** URL at which the table data are.
	 * <i>Note: This attribute is NULL if the table is provided inline.</i> */
	private final URL url;

	/** Name of the multipart HTTP request parameter (a multipart item = a file) containing the table data.
	 * <i>Note: This attribute is NULL if the table is provided as a URL.</i> */
	private final String param;
	/** File containing the table data. It points toward the multipart item/parameter whose the name matches the attribute {@link #param}.
	 * <i>Note: This attribute is NULL if the table is provided as a URL.</i> */
	private final File file;

	/**
	 * <p>Build the object representation of an item of the UPLOAD parameter: a table.</p>
	 * 
	 * <p>
	 * 	<b>This table MUST be provided as a URL!</b> Otherwise, a multipart request MUST be provided and in this case
	 * 	the other constructor ({@link #TableLoader(String, String, MultipartRequest)}) MUST be used.
	 * </p>
	 * 
	 * @param name	ADQL name of the table. <i>It is the key of an item inside the UPLOAD parameter value.</i>
	 * @param url	URL at which the table data can be found.
	 * 
	 * @throws TAPException	If the given URI is malformed,
	 *                     	or if no multipart request is provided whereas the table is provided as a request parameter,
	 *                     	or if the name or value is missing.
	 * 
	 * @see #TableLoader(String, String, MultipartRequest)
	 */
	public TableLoader(final String name, final String url) throws TAPException{
		this(name, url, (MultipartRequest)null);
	}

	/**
	 * <p>Build the object representation of an item of the UPLOAD parameter: a table.</p>
	 * 
	 * <p>This table can be provided either as a URL or inline ; the generated instance of {@link TableLoader} is able to deal with both.</p>
	 * 
	 * <p><i>Note:
	 * 	The search of a parameter inside the multipart request is done case sensitively.
	 * </i></p>
	 * 
	 * @param name		ADQL name of the table. <i>It is the key of an item inside the UPLOAD parameter value.</i>
	 * @param value		URL or "param:"+paramName (where paramName is the name of the multipart request parameter containing the table data).
	 *             		<i>It is the value of an item inside the UPLAOD parameter value.</i>
	 * @param multipart	Request containing all parameters provided in multipart. <i>It MAY be NULL if the given "value" is an URL. Otherwise, it MUST NOT be NULL.</i>
	 * 
	 * @throws TAPException	If the given URI is malformed,
	 *                     	or if no multipart request is provided whereas the table is provided as a request parameter,
	 *                     	or if the name or value is missing.
	 */
	@SuppressWarnings("unchecked")
	public TableLoader(final String name, final String value, final MultipartRequest multipart) throws TAPException{
		// Get the ADQL table name:
		if (name == null || name.trim().isEmpty())
			throw new TAPException("UPLOAD parameter incorrect: missing table name!");
		else
			tableName = name.trim();

		// Get the item value (either URL or parameter name):
		if (value == null || value.trim().isEmpty())
			throw new NullPointerException("UPLOAD parameter incorrect: missing table URI!");
		String tableId = value.trim();

		// CASE MULTIPART PARAMETER:
		if (tableId.startsWith(PARAM_PREFIX)){
			// Ensure the multipart request is provided and the parameter name is correct:
			if (multipart == null)
				throw new TAPException("UPLOAD parameter incorrect: incorrect table URI: \"" + tableId + "\"! The URI scheme \"" + PARAM_PREFIX + "\" can be used ONLY IF the VOTable is provided inside the HTTP request (multipart/form-data)!");
			else if (tableId.length() <= PARAM_PREFIX.length())
				throw new TAPException("UPLOAD parameter incorrect: missing parameter name in \"" + tableId + "\"!");

			// Set the parameter name:
			url = null;
			param = tableId.substring(PARAM_PREFIX.length()).trim();

			// Get the corresponding file in the multipart request (search case sensitive):
			Enumeration<String> enumeration = multipart.getFileNames();
			File foundFile = null;
			while(foundFile == null && enumeration.hasMoreElements()){
				String fileName = enumeration.nextElement();
				if (fileName.equals(param))
					foundFile = multipart.getFile(fileName);
			}

			// Set the file:
			if (foundFile == null)
				throw new TAPException("UPLOAD parameter incorrect: parameter not found: \"" + tableId + "\"!");
			else
				file = foundFile;
		}
		// CASE URL:
		else if (tableId.matches(URL_REGEXP)){
			try{
				url = new URL(tableId);
				param = null;
				file = null;
			}catch(MalformedURLException mue){
				throw new InvalidParameterException(mue.getMessage());
			}
		}
		// OTHER:
		else
			throw new TAPException("UPLOAD parameter incorrect: invalid table URI: \"" + tableId + "\"!");
	}

	/**
	 * Open a stream toward the table data (whatever is their source, a URL or a file).
	 * 
	 * @return	Input over the table data.
	 * 
	 * @throws IOException	If any error occurs while open the stream.
	 */
	public InputStream openStream() throws IOException{
		if (url != null)
			return url.openStream();
		else
			return new FileInputStream(file);
	}

	/**
	 * <p>Delete the table data stored in the cache.</p>
	 * 
	 * <p>
	 * 	This function will just delete the file in case the table data are coming from a multipart request.
	 * 	If the table data are provided as a URL, nothing is done (so we can consider that the cache does not contain any more the associated table data).
	 * </p>
	 * 
	 * @return	<i>true</i> if the file does not exist any more in the cache,
	 *        	<i>false</i> otherwise.
	 */
	public boolean deleteFile(){
		if (file != null && file.exists())
			return file.delete();
		else
			return true;
	}

}
