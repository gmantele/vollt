package uws.job.jobInfo;

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
 * Copyright 2017-2020 - Astronomisches Rechen Institut (ARI),
 *                       UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.UWSJob;
import uws.job.serializer.XMLSerializer;
import uws.service.UWS;
import uws.service.file.UWSFileManager;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.UploadFile;

/**
 * A full XML document attached to a {@link UWSJob job}.
 *
 * <p><b>XML representation</b></p>
 *
 * <p>
 * 	The document stored inside this {@link JobInfo} is considered formatted
 * 	in XML. So the functions {@link #getXML(String)} and
 * 	{@link #write(HttpServletResponse)} will return them as such.
 * </p>
 *
 * <p><i>Note 1:
 * 	The represented document is supposed to be XML, but absolutely no
 * 	verification is performed by {@link XMLJobInfo}.
 * </i></p>
 *
 * <p><i>Note 2:
 * 	{@link #getXML(String)} will skip the XML declaration
 * 	(e.g. <code>&lt;?xml version="1.0" encoding="utf-8"?&gt;</code>)
 * 	if any is provided. On the contrary, {@link #write(HttpServletResponse)}
 * 	will write an exact copy of the stored XML document.
 * 	Both functions can be overwritten if a different behavior is needed.
 * </i></p>
 *
 * <p><i>Note 3:
 * 	The stored XML document can refer to the following
 * 	XML schemas:
 * </i></p>
 * <ul><i>
 * 	<li>"http://www.ivoa.net/xml/UWS/v1.0" (no prefix),</li>
 * 	<li>"http://www.w3.org/1999/xlink" (prefix: xlink),</li>
 * 	<li>"http://www.w3.org/2001/XMLSchema" (prefix: xs),</li>
 * 	<li>"http://www.w3.org/2001/XMLSchema-instance" (prefix: xsi).</li>
 * </i></ul>
 * <p><i>
 * 	If more namespaces are needed they should be specified directly
 * 	at the root of the stored XML document (if possible with a valid
 * 	<code>xsi:schemaLocation</code>). An alternative would be to extend
 * 	{@link XMLSerializer} in order to append the needed namespaces to the root
 * 	XML node of any formatted XML documents.
 * </i></p>
 *
 * <p><b>Internal representation and Creation</b></p>
 *
 * <p>
 * 	This class proposes the two following constructors, each for a different
 * 	internal representation:
 * </p>
 * <ul>
 * 	<li><i>{@link #XMLJobInfo(String)} for an in-memory string.</i> The given
 * 		string is supposed to contained the full XML document and will be
 * 		stored as such in this class. This constructor should be used <b>only
 * 		for small XML document</b>.</li>
 * 	<li><i>{@link #XMLJobInfo(UploadFile)} for an XML file storage.</i>	The
 * 		given {@link UploadFile} is supposed to give access to the complete
 * 		XML document.This constructor should be used <b>for large XML
 * 		document.</b></li>
 * </ul>
 *
 * <p><b>Modification</b></p>
 *
 * <p>
 * 	By default, this implementation of {@link JobInfo} does not allow the
 * 	modification of its XML document. If needed, this class should be
 * 	extended with the adequate functions.
 * </p>
 *
 * <p><b>Backup/Restoration</b></p>
 *
 * <p>
 * 	An {@link UploadFile} can not be serialized using the Java Class
 * 	Serialization mechanism because it does not implement the
 * 	{@link Serializable} interface. Consequently, the given {@link UploadFile}
 * 	will be marked as <i>transient</i> and will have to be rebuilt when needed
 * 	after a restoration process.
 * </p>
 *
 * <p>However, it can be rebuilt only if:</p>
 * <ol>
 * 	<li>an access to a {@link UWSFileManager} is
 * 	possible.</li>
 * 	<li>the location of the file is known.</li>
 * </ol>
 *
 * <p>
 *  The first point (1) is fortunately possible through a {@link UWSJob} object.
 * 	This object is known after attachment to a job thanks to the function
 * 	{@link #setJob(UWSJob)}. So, a link toward the parent job should be kept ;
 * 	also marked as <i>transient</i>: see the attribute {@link #job}.
 * </p>
 *
 * <p>For the second point (2), the location of the file must be kept as a
 * 	non-transient attribute (see {@link #location}) so that being backuped with the
 * 	other non-transient attributes of this class. In order to backup this
 * 	location up-to-date, the function
 * 	{@link #writeObject(java.io.ObjectOutputStream)} updates {@link #location}
 * 	before the normal Java Class Serialization.
 *
 * <p>
 * 	So finally, the restoration of the {@link UploadFile} will be done by
 * 	{@link #getXML(String)} and {@link #write(HttpServletResponse)} with the
 * 	function {@link #restoreFile()}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 4.5 (05/2020)
 * @since 4.2
 */
public class XMLJobInfo implements JobInfo {
	private static final long serialVersionUID = 1L;

	/** XML file location.
	 *
	 * <p><b>Warning:</b>
	 * 	This field <b>ONLY</b> aims to contain the updated result of
	 * 	{@link UploadFile#getLocation() file.getLocation()}.
	 * </p> */
	protected String location = null;

	/** Link toward the XML file represented by this {@link JobInfo}.
	 *
	 * <p><b>Important:</b>
	 * 	This field must be used when a large XML document has to be represented
	 * 	by this {@link JobInfo}.
	 * </p>
	 *
	 * <p><i>Note:
	 *  It can be set only by {@link #XMLJobInfo(UploadFile)}.
	 *  If set, {@link #content} must be NULL.
	 * </i></p>*/
	protected transient UploadFile file = null;

	/** XML document represented by this {@link JobInfo}.
	 *
	 * <p><b>Important:</b>
	 * 	This field MUST be used <b>ONLY</b> for small XML document in order to
	 * 	keep enough memory free for the normal UWS service operations.
	 * </p>
	 *
	 * <p><i>Note:
	 * 	It can be set only by {@link #XMLJobInfo(String)}.
	 * 	If set, {@link #file} must be NULL.
	 * </i></p> */
	protected String content = null;

	/** Precise length (in bytes) of the represented XML document. */
	protected int length = -1;

	/** The job owning this {@link JobInfo}.
	 *
	 * <p><i>Note:
	 * 	This field is set only by {@link #setJob(UWSJob)} and is used
	 * 	only by {@link #restoreFile()} in order to rebuild {@link #file}
	 * 	after a UWS service restoration.
	 * </i></p> */
	protected transient UWSJob job = null;

	/**
	 * Build a {@link JobInfo} representing a <b>small</b> XML document.
	 *
	 * <p><b>Important:</b>
	 * 	This constructor should be used only for <b>small</b> XML document
	 * 	because the given string will be kept as such in memory. If the given
	 * 	string is too large, not enough memory will be available for normal
	 * 	UWS service operations.<br/><br/>
	 * 	<i>If you estimate the XML document is too big to stay in memory, you
	 * 	should save it in a file and use the constructor
	 * 	{@link #XMLJobInfo(UploadFile)}.</i>
	 * </p>
	 *
	 * @param smallXML	The small XML document to represent.
	 *
	 * @throws NullPointerException	If the given string is NULL or empty.
	 */
	public XMLJobInfo(final String smallXML) throws NullPointerException {
		if (smallXML == null || smallXML.trim().length() == 0)
			throw new NullPointerException("Missing XML content!");

		content = smallXML;
		length = smallXML.getBytes().length;
		file = null;
		location = null;
	}

	/**
	 * Build a {@link JobInfo} representing a <b>large</b> XML document stored
	 * inside a file.
	 *
	 * @param xmlFile	Link toward the large XML document to represent.
	 *
	 * @throws NullPointerException	If the given file is NULL or empty.
	 */
	public XMLJobInfo(final UploadFile xmlFile) throws NullPointerException {
		if (xmlFile == null || xmlFile.length <= 0)
			throw new NullPointerException("Missing XML file!");

		file = xmlFile;
		location = file.getLocation();
		length = (int)file.length;
		content = null;
	}

	@Override
	public String getXML(final String newLinePrefix) throws UWSException {
		// CASE: SMALL XML DOCUMENT:
		if (content != null) {
			if (content.trim().startsWith("<?"))
				return content.substring(content.indexOf("?>") + 2);
			else
				return content;

		}// CASE: XML FILE
		else {
			restoreFile();

			StringBuffer xml = new StringBuffer();
			BufferedReader input = null;
			try {
				// Open the XML file:
				input = new BufferedReader(new InputStreamReader(file.open()));
				String line;
				// Read it line by line:
				while((line = input.readLine()) != null) {
					// Ignore the XML declarative lines:
					if (line.trim().startsWith("<?")) {
						line = line.substring(line.indexOf("?>") + 2);
						if (line.trim().length() == 0)
							continue;
					}
					// Append the line prefix (if any):
					if (newLinePrefix != null && xml.length() > 0)
						xml.append(newLinePrefix);
					// Append the fetched line:
					xml.append(line);
				}
			} catch(IOException ioe) {
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Impossible to get the XML representation of the JobInfo!");
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch(IOException ioe) {
					}
				}
			}
			return xml.toString();
		}
	}

	@Override
	public void write(final HttpServletResponse response) throws IOException, UWSException {
		// CASE: SMALL XML DOCUMENT:
		if (content != null) {
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/xml");
			UWSToolBox.setContentLength(response, content.getBytes("UTF-8").length);

			PrintWriter writer = response.getWriter();
			writer.println(content);
			writer.flush();
		}

		// CASE: XML FILE:
		else {
			restoreFile();
			UWSToolBox.write(file.open(), "text/xml", file.length, response);
		}
	}

	@Override
	public void setJob(final UWSJob myJob) {
		job = myJob;

		if (job != null && file != null) {
			try {
				file.move(job);
				location = file.getLocation();
			} catch(IOException ioe) {
				if (job.getLogger() != null)
					job.getLogger().logUWS(LogLevel.ERROR, job, "SET_JOB_INFO", "Error when moving the XML JobInfo file closer to the job " + job.getJobId() + "! Current file location: " + file.getLocation(), ioe);
			}
		}
	}

	@Override
	public void destroy() throws UWSException {
		if (file != null) {
			try {
				file.deleteFile();
			} catch(IOException ioe) {
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ioe, "Error when deleting a JobInfo file!");
			}
		}
	}

	/**
	 * Serialize this {@link XMLJobInfo}.
	 *
	 * <p><i>Note:</i>
	 * 	This function will be called by the Java Class Serialization mechanism.
	 * 	See the Javadoc of {@link Serializable} for more details.
	 * </i></p>
	 *
	 * <p>
	 * 	This function just updates the XML file (if any) location before the
	 * 	normal Java Class Serialization of this object.
	 * </p>
	 *
	 * @param out	The stream used to contained the serialization of this
	 *           	{@link XMLJobInfo}.
	 *
	 * @throws IOException	If any error occurs while serializing this
	 *                    	{@link XMLJobInfo}
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		// Ensure the location is up-to-date before performing the backup of this jobInfo:
		if (file != null)
			location = file.getLocation();

		// Apply the default Java serialization method:
		out.defaultWriteObject();
	}

	/**
	 * Restore the link toward the XML file represented by this {@link JobInfo}.
	 *
	 * <p>
	 * 	This function has an effect only if {@link #file} is NULL but not
	 * 	{@link #location} ; indeed, such configuration can be encountered only
	 * 	if this {@link XMLJobInfo} has been de-serialized.
	 * </p>
	 *
	 * <p>
	 * 	Nothing can be done if the parent job is unknown. In other words,
	 * 	this {@link JobInfo} has to be attached to a job first
	 * 	(i.e. {@link #setJob(UWSJob)} has to be called first with a non-NULL
	 * 	parameter). If not, an exception will be thrown.
	 * </p>
	 *
	 * @throws UWSException	If this {@link JobInfo} is not attached to a job.
	 */
	protected void restoreFile() throws UWSException {
		/* If the file is NULL, it means a UWS restore has just occurred.
		 * Because UploadFile is not Serializable, it was impossible to restore the file.
		 * To solve this problem, the location has been saved.
		 * So, the file can be and has to be restored. */
		if (file == null && location != null) {
			if (job != null)
				file = new UploadFile(UWS.REQ_ATTRIBUTE_JOB_DESCRIPTION, location, job.getFileManager());
			else
				throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Missing jobInfo's file: impossible to display its content! Cause: missing UWSJob parent.");
		}
	}

}
