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
 * Copyright 2017-2019 - Astronomisches Rechen Institut (ARI),
 *                       UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.UWSJob;
import uws.job.serializer.XMLSerializer;

/**
 * Very simple implementation of {@link JobInfo}. It aims to represent a
 * key-value pair.
 *
 * <p>
 * 	Both functions {@link #getXML(String)} and
 * 	{@link #write(HttpServletResponse)} will return the following XML document:
 * </p>
 *
 * <pre>&lt;KEY&gt;VALUE&lt;/KEY&gt;</pre>
 *
 * <p>, where:</p>
 * <ul>
 * 	<li><i><code>KEY</code></i> can be get with {@link #getName()} and can be
 * 		set <b>only</b> at creation</li>
 * 	<li><i><code>VALUE</code></i> can be get with {@link #getValue()} and set
 * 		with {@link #setValue(String)}.</li>
 * </ul>
 *
 * @author Gr&eacute;gory Mantelet (ARI;CDS)
 * @version 4.4 (03/2019)
 * @since 4.2
 */
public class SingleValueJobInfo implements JobInfo {
	private static final long serialVersionUID = 1L;

	/** Name of the value stored inside this {@link JobInfo}.
	 *
	 * <p><i><b>Warning:</b> By default, this name is not supposed to be
	 * 	changed after initialization of this class. That's why only a public
	 * 	getter function is provided.</i></p> */
	protected String name = null;

	/** Value stored inside this {@link JobInfo}. */
	protected String value = null;

	/** XML representation of this {@link JobInfo}, returned by
	 * {@link #getXML(String)} and {@link #write(HttpServletResponse)}.
	 *
	 * <p><i>Note:
	 * 	It has to be updated each time the {@link #value} is changed. So by
	 * 	default, it is rebuilt by {@link #setValue(String)}.
	 * </i></p> */
	protected String xmlRepresentation = null;

	/**
	 * Build a {@link JobInfo} representing a single value having the given
	 * name.
	 *
	 * <p><i>Note 1:
	 * 	The name can not be changed after creation.
	 * </i></p>
	 *
	 * <p><i>Note 2:
	 * 	With this constructor, the represented value is NULL. To set a value,
	 * 	you have to use the function {@link #setValue(String)}. An alternative
	 * 	would be to use the constructor
	 * 	{@link #SingleValueJobInfo(String, String)} so that setting immediately
	 * 	the name and value.
	 * </i></p>
	 *
	 * @param name	Name of the value to represent.
	 *
	 * @throws NullPointerException		If the given name is NULL or an empty
	 *                             		string.
	 * @throws IllegalArgumentException	If the given name is not a valid XML
	 *                                 	node name according to the W3C (see
	 *                                 	{@link XMLSerializer#isValidXMLNodeName(String)}
	 *                                 	for more details).
	 */
	public SingleValueJobInfo(final String name) throws NullPointerException, IllegalArgumentException{
		this(name, null);
	}

	/**
	 * Build a {@link JobInfo} representing a single value having the given
	 * name and initial value.
	 *
	 * <p><i>Note 1:
	 * 	The name can not be changed after creation.
	 * </i></p>
	 *
	 * <p><i>Note 2:
	 * 	The value can change after object creation with the function
	 * 	{@link #setValue(String)}.
	 * </i></p>
	 *
	 * @param name	Name of the value to represent. <i>Can not be NULL or an
	 *            	empty string, and must be a valid XML node name.</i>
	 * @param value	Value to represent. <i>May be NULL.</i>
	 *
	 * @throws NullPointerException		If the given name is NULL or an empty
	 *                             		string.
	 * @throws IllegalArgumentException	If the given name is not a valid XML
	 *                                 	node name according to the W3C (see
	 *                                 	{@link XMLSerializer#isValidXMLNodeName(String)}
	 *                                 	for more details).
	 */
	public SingleValueJobInfo(final String name, final String value) throws NullPointerException, IllegalArgumentException{
		if (name == null || name.trim().length() == 0)
			throw new NullPointerException("Missing SingleValueJobInfo name!");
		else if (!XMLSerializer.isValidXMLNodeName(name))
			throw new IllegalArgumentException("Invalid XML node name: \"" + name + "\"! You should choose a different name for your SingleValueJobInfo.");

		this.name = name;
		setValue(value);
	}

	/**
	 * Get the name of the represented value.
	 *
	 * @return	Value name. <i>Can NEVER be NULL.</i>
	 */
	public String getName(){
		return name;
	}

	/**
	 * Get the represented value.
	 *
	 * @return	The represented value. <i>Can be NULL.</i>
	 */
	public String getValue(){
		return value;
	}

	/**
	 * Set the value represented by this {@link JobInfo}.
	 *
	 * @param value	The new value to represent. <i>Can be NULL.</i>
	 */
	public void setValue(final String value){
		this.value = value;

		xmlRepresentation = "<" + name + ">" + XMLSerializer.escapeXMLData(this.value) + "</" + name + ">";
	}

	@Override
	public String getXML(final String newLinePrefix){
		return xmlRepresentation;
	}

	@Override
	public void write(HttpServletResponse response) throws IOException, UWSException{
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/xml");
		UWSToolBox.setContentLength(response, xmlRepresentation.getBytes("UTF-8").length);

		PrintWriter writer = response.getWriter();
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println(xmlRepresentation);
		writer.flush();
	}

	@Override
	public void setJob(final UWSJob myJob){
		// Nothing to do!
	}

	@Override
	public void destroy() throws UWSException{
		// Nothing to do!
	}

}
