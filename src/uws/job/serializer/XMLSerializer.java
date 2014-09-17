package uws.job.serializer;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

import uws.job.ErrorSummary;
import uws.job.JobList;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSUrl;

/**
 * Lets serializing any UWS resource in XML.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public class XMLSerializer extends UWSSerializer {
	private static final long serialVersionUID = 1L;

	/** Tab to add just before each next XML node. */
	protected String tabPrefix = "";

	/** The path of the XSLT style-sheet. */
	protected String xsltPath = null;

	/**
	 * Builds a XML serializer.
	 */
	public XMLSerializer(){
		;
	}

	/**
	 * Builds a XML serializer with a XSLT link.
	 * 
	 * @param xsltPath	Path of a XSLT style-sheet.
	 */
	public XMLSerializer(final String xsltPath){
		this.xsltPath = xsltPath;
	}

	/**
	 * Gets the path/URL of the XSLT style-sheet to use.
	 * 
	 * @return	XSLT path/url.
	 */
	public final String getXSLTPath(){
		return xsltPath;
	}

	/**
	 * Sets the path/URL of the XSLT style-sheet to use.
	 * 
	 * @param path	The new XSLT path/URL.
	 */
	public final void setXSLTPath(final String path){
		if (path == null)
			xsltPath = null;
		else{
			xsltPath = path.trim();
			if (xsltPath.isEmpty())
				xsltPath = null;
		}
	}

	/**
	 * <p>Gets the XML file header (xml version, encoding and the xslt style-sheet link if any).</p>
	 * <p>It is always called by the implementation of the UWSSerializer functions
	 * if their boolean parameter (<i>root</i>) is <i>true</i>.</p>
	 * 
	 * @return	The XML file header.
	 */
	public String getHeader(){
		StringBuffer xmlHeader = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		if (xsltPath != null)
			xmlHeader.append("<?xml-stylesheet type=\"text/xsl\" href=\"").append(escapeXMLAttribute(xsltPath)).append("\"?>\n");
		return xmlHeader.toString();
	}

	/**
	 * Gets all UWS namespaces declarations needed for an XML representation of a UWS object.
	 * 
	 * @return	The UWS namespaces: <br /> (i.e. <i>= "xmlns:uws=[...] xmlns:xlink=[...] xmlns:xs=[...] xmlns:xsi=[...]"</i>).
	 */
	public String getUWSNamespace(){
		return "xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
	}

	/**
	 * Gets the node attributes which declare the UWS namespace.
	 * 
	 * @param root	<i>false</i> if the attribute to serialize will be included
	 * 				in a top level serialization (for a job attribute: job), <i>true</i> otherwise.
	 * 
	 * @return		"" if <i>root</i> is <i>false</i>, " "+UWSNamespace otherwise.
	 * 
	 * @see #getUWSNamespace()
	 */
	protected final String getUWSNamespace(boolean root){
		if (root)
			return " " + getUWSNamespace();
		else
			return "";
	}

	@Override
	public final String getMimeType(){
		return MIME_TYPE_XML;
	}

	@Override
	public String getUWS(final UWS uws, final JobOwner user){
		String name = uws.getName(), description = uws.getDescription();
		StringBuffer xml = new StringBuffer(getHeader());

		xml.append("<uws").append(getUWSNamespace(true));
		if (name != null)
			xml.append(" name=\"").append(escapeXMLAttribute(name)).append("\"");
		xml.append(">\n");

		if (description != null)
			xml.append("\t<description>\n").append(escapeXMLData(description)).append("\n\t</description>\n");

		xml.append("\t<jobLists>\n");
		for(JobList jobList : uws){
			UWSUrl jlUrl = jobList.getUrl();
			xml.append("\t\t<jobListRef name=\"").append(escapeXMLAttribute(jobList.getName())).append("\" href=\"");
			if (jlUrl != null && jlUrl.getRequestURL() != null)
				xml.append(escapeURL(jlUrl.getRequestURL()));
			xml.append("\" />\n");
		}
		xml.append("\t</jobLists>\n");

		xml.append("</uws>\n");

		return xml.toString();
	}

	@Override
	public String getJobList(final JobList jobsList, final JobOwner owner, final boolean root){
		String name = jobsList.getName();
		StringBuffer xml = new StringBuffer(getHeader());

		xml.append("<uws:jobList").append(getUWSNamespace(true));
		if (name != null)
			xml.append(" name=\"").append(escapeXMLAttribute(name)).append("\"");
		xml.append(">");

		UWSUrl jobsListUrl = jobsList.getUrl();
		Iterator<UWSJob> it = jobsList.getJobs(owner);
		while(it.hasNext())
			xml.append("\n\t").append(getJobRef(it.next(), jobsListUrl));

		xml.append("\n</uws:jobList>");

		return xml.toString();
	}

	@Override
	public String getJob(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		String newLine = "\n\t";

		// general information:
		xml.append("<uws:job").append(getUWSNamespace(root)).append(">");
		xml.append(newLine).append(getJobID(job, false));
		xml.append(newLine).append(getRunID(job, false));
		xml.append(newLine).append(getOwnerID(job, false));
		xml.append(newLine).append(getPhase(job, false));
		xml.append(newLine).append(getQuote(job, false));
		xml.append(newLine).append(getStartTime(job, false));
		xml.append(newLine).append(getEndTime(job, false));
		xml.append(newLine).append(getExecutionDuration(job, false));
		xml.append(newLine).append(getDestructionTime(job, false));

		tabPrefix = "\t";
		newLine = "\n";

		// parameters:
		xml.append(newLine).append(getAdditionalParameters(job, false));

		// results:
		xml.append(newLine).append(getResults(job, false));

		// errorSummary:
		xml.append(newLine).append(getErrorSummary(job.getErrorSummary(), false));

		tabPrefix = "";
		return xml.append("\n</uws:job>").toString();
	}

	@Override
	public String getJobRef(final UWSJob job, final UWSUrl jobsListUrl){
		String url = null;
		if (jobsListUrl != null){
			jobsListUrl.setJobId(job.getJobId());
			url = jobsListUrl.getRequestURL();
		}

		StringBuffer xml = new StringBuffer("<uws:jobRef id=\"");
		xml.append(escapeXMLAttribute(job.getJobId()));
		if (job.getRunId() != null && job.getRunId().length() > 0)
			xml.append("\" runId=\"").append(escapeXMLAttribute(job.getRunId()));
		xml.append("\" xlink:href=\"");
		if (url != null)
			xml.append(escapeURL(url));
		xml.append("\">").append(getPhase(job, false)).append("</uws:jobRef>");

		return xml.toString();
	}

	@Override
	public String getJobID(final UWSJob job, final boolean root){
		return (new StringBuffer(root ? getHeader() : "")).append("<uws:jobId").append(getUWSNamespace(root)).append(">").append(escapeXMLData(job.getJobId())).append("</uws:jobId>").toString();
	}

	@Override
	public String getRunID(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<uws:runId").append(getUWSNamespace(root));
		if (job.getRunId() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append(">").append(escapeXMLData(job.getRunId())).append("</uws:runId>");
		return xml.toString();
	}

	@Override
	public String getOwnerID(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<uws:ownerId").append(getUWSNamespace(root));
		if (job.getOwner() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append(">").append(escapeXMLData(job.getOwner().getPseudo())).append("</uws:ownerId>");
		return xml.toString();
	}

	@Override
	public String getPhase(final UWSJob job, final boolean root){
		return (new StringBuffer(root ? getHeader() : "")).append("<uws:phase").append(getUWSNamespace(root)).append(">").append(job.getPhase()).append("</uws:phase>").toString();
	}

	@Override
	public String getQuote(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<uws:quote").append(getUWSNamespace(root));
		if (job.getQuote() <= 0)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append(">").append(job.getQuote()).append("</uws:quote>");
		return xml.toString();
	}

	@Override
	public String getStartTime(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<uws:startTime").append(getUWSNamespace(root));
		if (job.getStartTime() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append(">").append(UWSJob.dateFormat.format(job.getStartTime())).append("</uws:startTime>");
		return xml.toString();
	}

	@Override
	public String getEndTime(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<uws:endTime").append(getUWSNamespace(root));
		if (job.getEndTime() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append(">").append(UWSJob.dateFormat.format(job.getEndTime())).append("</uws:endTime>");
		return xml.toString();
	}

	@Override
	public String getDestructionTime(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<uws:destruction").append(getUWSNamespace(root));
		if (job.getDestructionTime() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append(">").append(UWSJob.dateFormat.format(job.getDestructionTime())).append("</uws:destruction>");
		return xml.toString();
	}

	@Override
	public String getExecutionDuration(final UWSJob job, final boolean root){
		return (new StringBuffer(root ? getHeader() : "")).append("<uws:executionDuration").append(getUWSNamespace(root)).append(">").append(job.getExecutionDuration()).append("</uws:executionDuration>").toString();
	}

	@Override
	public String getErrorSummary(final ErrorSummary error, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append(tabPrefix).append("<uws:errorSummary").append(getUWSNamespace(root));
		if (error != null){
			xml.append(" type=\"").append(error.getType()).append("\"").append(" hasDetail=\"").append(error.hasDetail()).append("\">");
			xml.append("\n\t").append(tabPrefix).append("<uws:message>").append(escapeXMLData(error.getMessage())).append("</uws:message>");
			xml.append("\n").append(tabPrefix).append("</uws:errorSummary>");
		}else
			xml.append(" xsi:nil=\"true\" />");
		return xml.toString();
	}

	@Override
	public String getAdditionalParameters(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append(tabPrefix).append("<uws:parameters").append(getUWSNamespace(root)).append(">");
		String newLine = "\n\t" + tabPrefix;
		for(String paramName : job.getAdditionalParameters())
			xml.append(newLine).append(getAdditionalParameter(paramName, job.getAdditionalParameterValue(paramName), false));
		xml.append("\n").append(tabPrefix).append("</uws:parameters>");
		return xml.toString();
	}

	@Override
	public String getAdditionalParameter(final String paramName, final Object paramValue, final boolean root){
		if (paramName != null && paramValue != null){
			if (root)
				return paramValue.toString();
			else
				return (new StringBuffer("<uws:parameter")).append(getUWSNamespace(root)).append(" id=\"").append(escapeXMLAttribute(paramName)).append("\">").append(escapeXMLData(paramValue.toString())).append("</uws:parameter>").toString();
		}else
			return "";
	}

	@Override
	public String getResults(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append(tabPrefix).append("<uws:results").append(getUWSNamespace(root)).append(">");

		Iterator<Result> it = job.getResults();
		String newLine = "\n\t" + tabPrefix;
		while(it.hasNext())
			xml.append(newLine).append(getResult(it.next(), false));
		xml.append("\n").append(tabPrefix).append("</uws:results>");
		return xml.toString();
	}

	@Override
	public String getResult(final Result result, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<uws:result").append(getUWSNamespace(root)).append(" id=\"").append(escapeXMLAttribute(result.getId())).append("\"");
		if (result.getHref() != null){
			if (result.getType() != null)
				xml.append(" xlink:type=\"").append(escapeXMLAttribute(result.getType())).append("\"");
			xml.append(" xlink:href=\"").append(escapeURL(result.getHref())).append("\"");
		}
		if (result.getMimeType() != null)
			xml.append(" mime=\"").append(escapeXMLAttribute(result.getMimeType())).append("\"");
		if (result.getSize() >= 0)
			xml.append(" size=\"").append(result.getSize()).append("\"");
		return xml.append(" />").toString();
	}

	/* ************** */
	/* ESCAPE METHODS */
	/* ************** */
	/**
	 * <p>Escapes the content of a node (data between the open and the close tags).</p>
	 * 
	 * <p><i>By default: surrounds the given data by "&lt;![CDATA[" and "]]&gt;".</i></p>
	 * 
	 * @param data	Data to escape.
	 * 
	 * @return		Escaped data.
	 */
	public static String escapeXMLData(final String data){
		return "<![CDATA[" + data + "]]>";
	}

	/**
	 * Escapes the given value of an XML attribute.
	 * 
	 * @param value	Value of an XML attribute.
	 * 
	 * @return		The escaped value.
	 */
	public static String escapeXMLAttribute(final String value){
		StringBuffer encoded = new StringBuffer();
		for(int i = 0; i < value.length(); i++){
			char c = value.charAt(i);
			switch(c){
				case '&':
					encoded.append("&amp;");
					break;
				case '<':
					encoded.append("&lt;");
					break;
				case '>':
					encoded.append("&gt;");
					break;
				case '"':
					encoded.append("&quot;");
					break;
				case '\'':
					encoded.append("&#039;");
					break;
				default:
					encoded.append(c);
			}
		}
		return encoded.toString();
	}

	/**
	 * Escapes the given URL.
	 * 
	 * @param url	URL to escape.
	 * 
	 * @return		The escaped URL.
	 * 
	 * @see URLEncoder
	 * @see #escapeXMLAttribute(String)
	 */
	public static String escapeURL(final String url){
		try{
			return URLEncoder.encode(url, "UTF-8");
		}catch(UnsupportedEncodingException e){
			return escapeXMLAttribute(url);
		}
	}

}
