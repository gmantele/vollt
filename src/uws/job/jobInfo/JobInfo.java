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
 * Copyright 2017 - Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.job.UWSJob;
import uws.job.serializer.XMLSerializer;

/**
 * API wrapping an object which provides more information about a job.
 * 
 * <p>
 * 	It can be a simple information (e.g. job progress ;
 * 	see {@link SingleValueJobInfo}) or a whole job description instead of
 * 	parameters as described in REC-UWS-1.0,
 * 	"1.3. Job description language, service contracts and universality"
 * 	(e.g. an XML document ; see {@link XMLJobInfo}).
 * </p>
 * 
 * <p><b>Representation</b></p>
 * <p>
 * 	As requested by REC-UWS-1.0, the function {@link #getXML(String)}
 * 	must return an XML representation of this jobInfo, but that does not
 * 	mean that the additional job information have to be in XML ; they can be
 * 	an XML, a .txt, an image, ... The function {@link #getXML(String)} just
 * 	needs to return a representation of this jobInfo:
 * 	either the jobInfo content itself or a link to access it into details.
 * </p>
 * <p>
 * 	The function {@link #write(HttpServletResponse)} is only used when
 * 	ONLY the content of a job's jobInfo is requested:
 * 	with the URL <code>{uws-root}/{job-list}/{job-id}/jobInfo</code>.
 * 	It allows to return the real content of this jobInfo (if not already
 * 	the XML returned by {@link #getXML(String)}).
 * </p>
 * 
 * <p><b>Resource management</b></p>
 * <p>
 * 	In case the jobInfo is associated with other resources (e.g. memory, file,
 * 	...), the function {@link #destroy()} must be able to discard them.
 * 	This function is always called at job destruction.
 * </p>
 * 
 * <p><b>Backup</b></p>
 * <p>
 * 	The implementation of a {@link JobInfo} being free, the only viable way to
 * 	backup a such object is by Java Class Serialization (see
 * 	{@link Serializable}, {@link ObjectOutputStream} and
 * 	{@link ObjectInputStream}). A default serialization is already implemented,
 * 	but it can be customized by overriding the following functions:
 * </p>
 * <ul>
 * 	<li><code>private void writeObject(java.io.ObjectOutputStream out)
 *              throws IOException</code></li>
 * 	<li><code>private void readObject(java.io.ObjectInputStream in)
 *              throws IOException, ClassNotFoundException;</code></li>
 * 	<li><code>private void readObjectNoData()
 *              throws ObjectStreamException;</code></li>
 * </ul>
 * 
 * <p><i>See the Javadoc of {@link Serializable} for more details.</i></p>
 * 
 * <p><b>Link with {@link UWSJob}</b></p>
 * 
 * <p>
 * 	Once a {@link JobInfo} is attached to a job (thanks to
 * 	{@link UWSJob#setJobInfo(JobInfo)}), the function {@link #setJob(UWSJob)} is
 * 	called. In some implementation, no action is needed (see
 * 	{@link SingleValueJobInfo}), but in some others it may be required to either
 * 	keep a link with the parent job or to execute some special action.
 * </p>
 * 
 * <p><b>Warning:</b>
 * 	Since a {@link JobInfo} must be {@link Serializable} it is recommended to
 * 	flag complex objects like {@link UWSJob} as <i>transient</i> as much as
 * 	possible in order to make the backup and restore processes lighter.
 * 	Some objects like the parent job are naturally restored when
 * 	{@link #setJob(UWSJob)} is called. See {@link XMLJobInfo} for a concrete
 * 	example.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.2 (06/2017)
 * @since 4.2
 */
public interface JobInfo extends Serializable {

	/**
	 * Get the XML representation of this {@link JobInfo}.
	 * 
	 * <p><i>Note 1:
	 * 	This function does not force the jobInfo to be in
	 * 	XML but asks for a piece of XML document to append
	 * 	to the XML representation of a job and representing
	 * 	this jobInfo. It may be a full serialization of it or
	 * 	merely a link (see <a href="https://www.w3.org/TR/xlink11/">Xlink</a>)
	 * 	toward a complete document (XML or not).
	 * </i></p>
	 * 
	 * <p><i>Note 2:
	 * 	The returned piece of XML can refer to the following
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
	 * 	at the root of the XML returned by this function (if possible
	 * 	with a valid <code>xsi:schemaLocation</code>). An alternative
	 * 	would be to extend {@link XMLSerializer} in order to append
	 * 	the needed namespaces to the root XML node of any formatted XML
	 * 	documents.
	 * </i></p>
	 * 
	 * @param newLinePrefix	Characters (generally white-spaces) that should
	 *                     	prefix all new line of the returned piece of
	 *                     	XML. New line characters should also be
	 *                     	included in this string ; if not, the
	 *                     	returned XML should be on a single line.
	 *                     	<i>This parameter may be NULL.</i>
	 * 
	 * @return	XML representation of this jobInfo.
	 * 
	 * @throws UWSException	If any error occurs while building the XML
	 *                     	representation of this jobInfo.
	 */
	public String getXML(final String newLinePrefix) throws UWSException;

	/**
	 * Write the content of this jobInfo as a complete HTTP response
	 * when the URL <code>{uws-root}/{job-list}/{job-id}/jobInfo</code> is
	 * requested.
	 * 
	 * <p><b>Important:</b>
	 * 	At least the Content-Type, the Content-Length and Character-Encoding
	 * 	should be set in addition of the response content.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	If formatted into XML, the root node of the returned document
	 * 	may be the UWS node "jobInfo" or not, depending on your
	 * 	desired implementation. Since the UWS standard does not specify
	 * 	any way to retrieve individually a jobInfo, this part is left
	 * 	here totally free to the developer will.
	 * </i></p>
	 * 
	 * @param response	HTTP response in which the jobInfo content must be
	 *                	written.
	 * 
	 * @throws IOException	If there is any error while writing the jobInfo
	 *                    	content.
	 * @throws UWSException	If there is any error while formating the jobInfo
	 *                     	content.
	 */
	public void write(final HttpServletResponse response) throws IOException, UWSException;

	/**
	 * Notify this {@link JobInfo} that it is now owned by the given job.
	 * 
	 * @param myJob	The new owner of this {@link JobInfo}.
	 *             	<i>This parameter may be NULL.</i>
	 */
	public void setJob(final UWSJob myJob);

	/**
	 * Free/Discard any resource associated with this {@link JobInfo}.
	 * 
	 * <p><i>Note:</i>
	 * 	This function should be called only at job destruction.
	 * 	It particularly aims to delete any file containing the full
	 * 	content of this JobInfo, but it should also be used for any
	 * 	other kind of associated resource.
	 * </p>
	 * 
	 * @throws UWSException	If all associated resources can not be freed.
	 */
	public void destroy() throws UWSException;

}
