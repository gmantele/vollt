package uws.job;

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

import uws.UWSException;

import uws.job.serializer.UWSSerializer;
import uws.job.user.JobOwner;

import uws.service.UWSUrl;

/**
 * This class gives a short description (mainly an ID and a URL) of a job result.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public class Result extends SerializableUWSObject {
	private static final long serialVersionUID = 1L;

	public final static String DEFAULT_RESULT_NAME = "result";

	/** <b>[Required ; Default="result"]</b> Name or ID of this result. */
	protected String id = DEFAULT_RESULT_NAME;

	/** <i>[Optional]</i> The readable URL which points toward the result file. */
	protected String href = null;

	/** <i>[Optional]</i> The XLINK URL type (simple (default), extended, locator, arc, resource, title or none ; see http://www.w3.org/TR/xlink/#linking-elements for more details). */
	protected String type = "simple";

	/** <i>[Optional]</i> The MIME type of the result. */
	protected String mimeType = null;

	/** <i>[Optional]</i> The size of the corresponding result file. */
	protected long size = -1;

	/**
	 * Tells whether a redirection toward the given URL is required to get the result content.
	 * If FALSE, the content must be read from the corresponding file managed by the {@link uws.service.file.UWSFileManager}. */
	protected final boolean redirection;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds a result with the URL toward the file which contains the result content.
	 * 
	 * @param job		Job which will own this result.
	 * @param resultUrl	Result file URL.
	 */
	public Result(final UWSJob job, java.net.URL resultUrl){
		if (resultUrl != null){
			id = resultUrl.getFile();
			href = resultUrl.toString();
		}
		redirection = isRedirectionUrl(href, id, job);
		if (!redirection)
			href = getDefaultUrl(id, job);
	}

	/**
	 * Builds a result with just a name/ID.
	 * 
	 * @param job		Job which will own this result.
	 * @param name		Name of ID of the result.
	 */
	public Result(final UWSJob job, String name){
		if (name != null)
			id = name;
		href = getDefaultUrl(name, job);
		redirection = false;
	}

	/**
	 * Builds a result with an ID/name and the URL toward the file which contains the result content.
	 * 
	 * @param job		Job which will own this result.
	 * @param name			Name or ID of the result.
	 * @param resultUrl		Result file URL.
	 * 
	 * @see #Result(UWSJob, String)
	 */
	public Result(final UWSJob job, String name, String resultUrl){
		if (name != null)
			id = name;
		redirection = isRedirectionUrl(resultUrl, id, job);
		href = redirection ? resultUrl :getDefaultUrl(name, job);
	}

	/**
	 * Builds a result with an ID/name, a result type and the URL toward the file which contains the result content.
	 * 
	 * @param job			Job which will own this result.
	 * @param name			Name or ID or the result.
	 * @param resultType	Type of result.
	 * @param resultUrl		Result file URL.
	 * 
	 * @see #Result(UWSJob, String, String)
	 */
	public Result(final UWSJob job, String name, String resultType, String resultUrl){
		this(job, name, resultUrl);
		type = resultType;
	}

	/**
	 * Builds MANUALLY a result with an ID/name, a result type and the URL toward the file which contains the result content.
	 * 
	 * @param name			Name or ID or the result.
	 * @param resultType	Type of result.
	 * @param resultUrl		Result file URL.
	 * @param redirection	<i>true</i> if a redirection toward the given URL is required to get the result content, <i>false</i> otherwise.
	 * 						<i><u>note:</u> This parameter is ignored if the given URL is NULL or empty ! In this case, redirection = FALSE.</i>
	 */
	public Result(String name, String resultType, String resultUrl, boolean redirection){
		id = name;
		href = resultUrl;
		this.redirection = (href == null || href.trim().isEmpty()) ? true : redirection;
		type = resultType;
	}

	/**
	 * Gets the HREF as {jobList}/{job}/results/ID.
	 * 
	 * @param id	ID of the concerned Result.
	 * @param job	The job which has to contain the Result instance.
	 * 
	 * @return		The HREF field of the Result or <i>null</i> if the URL of the job is unknown (which is the case when the job is not in a job list).
	 */
	public static final String getDefaultUrl(String id, UWSJob job){
		UWSUrl url = job.getUrl();
		if (url == null)
			return null;
		else{
			url.setAttributes(new String[]{UWSJob.PARAM_RESULTS, id});
			return url.toString();
		}
	}

	/**
	 * <p>Tells whether the given URL is different from the default URL of the specified result of the given job.</p>
	 * 
	 * <p>When this function returns <i>true</i> a redirection toward the given URL is required to get the result content.
	 * Otherwise, the result content must be read from a file managed by the {@link uws.service.file.UWSFileManager}.</p>
	 * 
	 * <p><i><u>note:</u> If at least one of the parameter of this function is null or an empty string, this function returns false.</i></p>
	 * 
	 * @param url			Result URL to test.
	 * @param resultId		ID of the result.
	 * @param job			Job which owns the result.
	 * 
	 * @return				<i>true</i> if a redirection is required to get the result content, <i>false</i> otherwise.
	 * 
	 * @see #getDefaultUrl(String, UWSJob)
	 */
	public static final boolean isRedirectionUrl(final String url, final String resultId, final UWSJob job){
		if (url == null || url.trim().isEmpty() || resultId == null || resultId.trim().isEmpty() || job == null || job.getUrl() == null)
			return false;
		else
			return !url.equalsIgnoreCase(getDefaultUrl(resultId, job).toString());
	}

	/* ******* */
	/* GETTERS */
	/* ******* */
	/**
	 * Gets the id/name of this result.
	 * 
	 * @return	The result id or name.
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Gets the URL of the result file.
	 * 
	 * @return	The result file URL.
	 */
	public final String getHref() {
		return href;
	}

	/**
	 * Tells whether a redirection toward the URL of this result is required to get the result content.
	 * If NOT, its content must be read from the corresponding file managed by the {@link uws.service.file.UWSFileManager}.
	 * 
	 * @return	<i>true</i> if a redirection is required to get the result content, <i>false</i> otherwise.
	 */
	public final boolean isRedirectionRequired(){
		return redirection;
	}

	/**
	 * Gets the type of this result.
	 * 
	 * @return	The result type.
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Gets the MIME type of this result.
	 * 
	 * @return The MIME Type.
	 */
	public final String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the MIME type of this result.
	 * 
	 * @param mimeType The MIME type to set.
	 */
	public final void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Gets the size of the corresponding result file.
	 * 
	 * @return Result file size (in bytes).
	 */
	public final long getSize() {
		return size;
	}

	/**
	 * Sets the size of the corresponding result file.
	 * 
	 * @return size	Result file size (in bytes).
	 */
	public final void setSize(long size) {
		this.size = size;
	}

	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	@Override
	public String serialize(UWSSerializer serializer, JobOwner owner) throws UWSException {
		return serializer.getResult(this, true);
	}

	@Override
	public String toString(){
		return "RESULT {id: "+id+"; type: \""+((type==null)?"?":type)+"\"; href: "+((href==null)?"none":href)+"; mimeType: "+((mimeType==null)?"none":mimeType)+"}";
	}
}
