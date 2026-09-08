package tap.auth;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.UnsupportedOperationException;
import uk.ac.starlink.votable.VOSerializer;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import tap.metadata.TAPSchema;
import tap.resource.TAPResource;
import tap.metadata.TAPTable;
import tap.metadata.TAPMetadata;
import tap.auth.AuthJobOwner;
import tap.ServiceConnection;
import tap.TAPFactory;
import tap.log.TAPLog;

import uws.UWSException;
import uws.service.log.UWSLog.LogLevel;
import uws.UWSToolBox;
import uws.service.file.UWSFileManager;
/**
 * <p>
 * Authenticated replacement for {@link TAPMetadata}. Used to replace the /tables endpoint
 * </p>
 * <p>
 * The main difference is this on request, the request will be resolved to a {@link AuthJobOwner},
 * and the xml return data will only include tables allowed by the user.
 * </p>
 *
 * <p>
 * Previous write methods write and writeSchema that do not have a {@link AuthJobOwner} parameter
 * and would previously write all tables in the database, are made unsupported, and will throw an
 * error if they are called.
 * </p>
 *
 * @author Anthony Heng (AAO)
 * @version 04/2025
 *
 * @see TAPMetadata
 */
public class AuthTAPMetadata extends TAPMetadata implements TAPResource {

	/** Resource name of the TAP metadata. This name is also used - in this class - in the TAP URL
	 * to identify this resource.
	 * Here it corresponds to the following URI: ".../tables".
	 *
	 * NOTE: As a big chunk of vollt still relies on TAPMetadata, authentication will also be needed
	 * elsewhere in the code to ensure users can not query tables they do not have access to. This
	 * is just to replace the endpoint /tables.
	 *
	 * */

	/** Resource name of the TAP metadata. This name is also used - in this class - in the TAP URL to identify this resource.
	 * Here it corresponds to the following URI: ".../tables", replacing that in {@link TAPMetadata} */
	public static String RESOURCE_NAME = "tables";

	/** TAP service owning AuthTAPMetadata as a resource */
	private ServiceConnection service;

	private TAPLog logger;

	public AuthTAPMetadata(TAPMetadata meta, UWSFileManager fileManager, TAPFactory tapFactory, TAPLog logger) {
		this.logger = logger;
		this.service = tapFactory.getServiceConnection();
		for(TAPSchema s : meta) {
			this.addSchema(s);
		}
	}

	@Override
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/xml");
		response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

		PrintWriter writer = response.getWriter();
		// Get the User, copied over from TAP.java
		AuthJobOwner user = null;
		// Identify the user:
		try{
			user = (AuthJobOwner) UWSToolBox.getUser(request, this.service.getUserIdentifier());
		}catch(UWSException ue){
			this.logger.logTAP(LogLevel.ERROR, null, "IDENT_USER", "Can not identify the HTTP request user!", ue);
			throw new IOException("Failure to resolve user from request: "+ue.getMessage());
		} finally {
			write(writer, user);
		}
		return false;
	}

	// Override methods that used to return all data available and have them throw exceptions
	/**
	 * {@inheritDoc}
	 * In AuthTAPMetadata this method is not supported and throws an {@link UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException if this method is called.
	 */
	@Override
	public void write(final PrintWriter writer) throws UnsupportedOperationException{
		throw new UnsupportedOperationException("Attempted to access universal method of AuthTAPMetaData");
	}

	/**
	 * {@inheritDoc}
	 * In AuthTAPMetadata this method is not supported and throws an {@link UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException if this method is called.
	 */

	@Override
	protected void writeSchema(TAPSchema s, final PrintWriter writer) throws UnsupportedOperationException{
		throw new UnsupportedOperationException("Attempted to access universal method of AuthTAPMetaData");
	}

	// Use old method, however call writeSchema that filters out the tables the user can't access
	/**
	 * Format in XML user-specific metadata set and write it in the given writer.
	 *
	 * @param writer	Stream in which the XML representation of this metadata must be written.
	 * @param user      User in which to write the metadata for.
	 *
	 * @throws IOException	If there is any error while writing the XML in the given writer.
	 */
	public void write(final PrintWriter writer, AuthJobOwner user) throws IOException{
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		if (xsltPath != null){
			writer.print("<?xml-stylesheet type=\"text/xsl\" ");
			writer.print(VOSerializer.formatAttribute("href", xsltPath));
			writer.println("?>");
		}

		/* TODO The XSD schema for VOSITables should be fixed soon! This schema should be changed here before the library is released!
		 * Note: the XSD schema at http://www.ivoa.net/xml/VOSITables/v1.0 contains an incorrect targetNamespace ("http://www.ivoa.net/xml/VOSICapabilities/v1.0").
		 *       In order to make this XML document valid, a custom location toward a correct XSD schema is used: http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd */
		writer.println("<vosi:tableset xmlns:vosi=\"http://www.ivoa.net/xml/VOSITables/v1.0\" xmlns:vod=\"http://www.ivoa.net/xml/VODataService/v1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VOSITables/v1.0 http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd\">");

		this.logger.logTAP(LogLevel.INFO, null, "WRITE_SCHEMA", "Schema length: "+schemas.size(), null);
		for(TAPSchema s : schemas.values()) {
			this.logger.logTAP(LogLevel.INFO, null, "WRITE_SCHEMA", "Schema: "+s.getRawName(), null);
			if (user.canAccessSchema(s)){
				writeSchema(s, writer, user);
			}
		}

		writer.println("</vosi:tableset>");

		UWSToolBox.flush(writer);
	}

	/**
	 * <p>Format in XML the given schema and then write it in the given writer. 
	 * Only write the tables in the schema that the user is allowed to access</p>
	 *
	 * <p>Written lines:</p>
	 * <pre>
	 * &lt;schema&gt;
	 * 	&lt;name&gt;...&lt;/name&gt;
	 * 	&lt;title&gt;...&lt;/title&gt;
	 * 	&lt;description&gt;...&lt;/description&gt;
	 * 	&lt;utype&gt;...&lt;/utype&gt;
	 * 		// call #writeTable(TAPTable, PrintWriter) for each table
	 * &lt;/schema&gt;
	 * </pre>
	 *
	 * <p><i>Note:
	 * 	When NULL an attribute or a field is not written. Here this rule concerns: description and utype.
	 * </i></p>
	 *
	 * @param s			The schema to format and to write in XML.
	 * @param writer	Output in which the XML serialization of the given schema must be written.
	 * @param user      User in which the schema is written for
	 *
	 * @throws IOException	If the connection with the HTTP client has been either canceled or closed for another reason.
	 *
	 * @see #writeTable(TAPTable, PrintWriter)
	 */
	protected void writeSchema(TAPSchema s, PrintWriter writer, AuthJobOwner user) throws IOException{
		final String prefix = "\t\t";
		writer.println("\t<schema>");

		writeAtt(prefix, "name", s.getRawName(), false, writer);
		writeAtt(prefix, "title", s.getTitle(), true, writer);
		writeAtt(prefix, "description", s.getDescription(), true, writer);
		writeAtt(prefix, "utype", s.getUtype(), true, writer);

		int nbColumns = 0;
		for(TAPTable t : s){
			if (!user.canAccessTable(t)){
				continue; // Don't add this table if it's not in the user's list of allowed tables
			}

			// write each table:
			nbColumns += writeTable(t, writer);

			// flush the PrintWriter buffer when at least 30 tables have been read:
			/* Note: the buffer may have already been flushed before automatically,
			 *       but this manual flush is also checking whether any error has occurred while writing the previous characters.
			 *       If so, a ClientAbortException (extension of IOException) is thrown in order to interrupt the writing of the
			 *       metadata and thus, in order to spare server resources (and particularly memory if the metadata set is large). */
			if (nbColumns / 30 > 1){
				UWSToolBox.flush(writer);
				nbColumns = 0;
			}

		}

		writer.println("\t</schema>");

		if (nbColumns > 0)
			UWSToolBox.flush(writer);
	}
}


