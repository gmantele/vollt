package tap.backup;

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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.Json4Uws;

import tap.ExecutionProgression;
import tap.TAPExecutionReport;
import tap.TAPJob;
import tap.parameters.DALIUpload;
import uws.UWSException;
import uws.job.UWSJob;
import uws.service.UWS;
import uws.service.backup.DefaultUWSBackupManager;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.UploadFile;

/**
 * <p>Let backup all TAP asynchronous jobs.</p>
 * 
 * <p><i>note: Basically the saved data are the same, but in addition some execution statistics are also added.</i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (12/2014)
 * 
 * @see DefaultUWSBackupManager
 */
public class DefaultTAPBackupManager extends DefaultUWSBackupManager {

	/**
	 * Build a default TAP jobs backup manager.
	 * 
	 * @param uws	The UWS containing all the jobs to backup.
	 * 
	 * @see DefaultUWSBackupManager#DefaultUWSBackupManager(UWS)
	 */
	public DefaultTAPBackupManager(UWS uws){
		super(uws);
	}

	/**
	 * Build a default TAP jobs backup manager.
	 * 
	 * @param uws		The UWS containing all the jobs to backup.
	 * @param frequency	The backup frequency (in ms ; MUST BE positive and different from 0.
	 *                  If negative or 0, the frequency will be automatically set to DEFAULT_FREQUENCY).
	 * 
	 * @see DefaultUWSBackupManager#DefaultUWSBackupManager(UWS, long)
	 */
	public DefaultTAPBackupManager(UWS uws, long frequency){
		super(uws, frequency);
	}

	/**
	 * Build a default TAP jobs backup manager.
	 * 
	 * @param uws		The UWS containing all the jobs to backup.
	 * @param byUser	Backup mode.
	 * 
	 * @see DefaultUWSBackupManager#DefaultUWSBackupManager(UWS, boolean)
	 */
	public DefaultTAPBackupManager(UWS uws, boolean byUser) throws UWSException{
		super(uws, byUser);
	}

	/**
	 * Build a default TAP jobs backup manager.
	 * 
	 * @param uws		The UWS containing all the jobs to backup.
	 * @param byUser	Backup mode.
	 * @param frequency	The backup frequency (in ms ; MUST BE positive and different from 0.
	 *                  If negative or 0, the frequency will be automatically set to DEFAULT_FREQUENCY).
	 * 
	 * @see DefaultUWSBackupManager#DefaultUWSBackupManager(UWS, boolean, long)
	 */
	public DefaultTAPBackupManager(UWS uws, boolean byUser, long frequency) throws UWSException{
		super(uws, byUser, frequency);
	}

	@Override
	protected JSONObject getJSONJob(UWSJob job, String jlName) throws UWSException, JSONException{
		JSONObject jsonJob = Json4Uws.getJson(job);

		// Re-Build the parameters map, by separating the uploads and the "normal" parameters:
		JSONArray uploads = new JSONArray();
		JSONObject params = new JSONObject();
		Object val;
		for(String name : job.getAdditionalParameters()){
			// get the raw value:
			val = job.getAdditionalParameterValue(name);
			// if no value, skip this item:
			if (val == null)
				continue;
			// if an array, build a JSON array of strings:
			else if (val.getClass().isArray()){
				JSONArray array = new JSONArray();
				for(Object o : (Object[])val){
					if (o != null && o instanceof DALIUpload)
						array.put(getDALIUploadJson((DALIUpload)o));
					else if (o != null)
						array.put(o.toString());
				}
				params.put(name, array);
			}
			// if upload file:
			else if (val instanceof UploadFile)
				uploads.put(getUploadJson((UploadFile)val));
			// if DALIUpload:
			else if (val instanceof DALIUpload)
				params.put(name, getDALIUploadJson((DALIUpload)val));
			// otherwise, just put the value:
			else
				params.put(name, val);
		}

		// Deal with the execution report of the job:
		if (job instanceof TAPJob && ((TAPJob)job).getExecReport() != null){
			TAPExecutionReport execReport = ((TAPJob)job).getExecReport();

			// Build the JSON representation of the execution report of this job:
			JSONObject jsonExecReport = new JSONObject();
			jsonExecReport.put("success", execReport.success);
			jsonExecReport.put("uploadduration", execReport.getUploadDuration());
			jsonExecReport.put("parsingduration", execReport.getParsingDuration());
			jsonExecReport.put("executionduration", execReport.getExecutionDuration());
			jsonExecReport.put("formattingduration", execReport.getFormattingDuration());
			jsonExecReport.put("totalduration", execReport.getTotalDuration());

			// Add the execution report into the parameters list:
			params.put("tapexecreport", jsonExecReport);
		}

		// Add the parameters and the uploads inside the JSON representation of the job:
		jsonJob.put(UWSJob.PARAM_PARAMETERS, params);
		jsonJob.put("uwsUploads", uploads);

		// Add the job owner:
		jsonJob.put(UWSJob.PARAM_OWNER, (job != null && job.getOwner() != null) ? job.getOwner().getID() : null);

		// Add the name of the job list owning the given job:
		jsonJob.put("jobListName", jlName);

		return jsonJob;
	}

	/**
	 * Get the JSON representation of the given {@link DALIUpload}.
	 * 
	 * @param upl	The DALI upload specification to serialize in JSON.
	 * 
	 * @return		Its JSON representation.
	 * 
	 * @throws JSONException	If there is an error while building the JSON object.
	 * 
	 * @since 2.0
	 */
	protected JSONObject getDALIUploadJson(final DALIUpload upl) throws JSONException{
		if (upl == null)
			return null;
		JSONObject o = new JSONObject();
		o.put("label", upl.label);
		o.put("uri", upl.uri);
		o.put("file", (upl.file == null ? null : upl.file.paramName));
		return o;
	}

	@Override
	protected void restoreOtherJobParams(JSONObject json, UWSJob job) throws UWSException{
		// 0. Nothing to do in this function if the job is missing OR if it is not an instance of TAPJob:
		if (job == null || !(job instanceof TAPJob))
			return;

		// 1. Build correctly the TAP UPLOAD parameter (the value of this parameter should be an array of DALIUpload):
		if (json != null && json.has(TAPJob.PARAM_PARAMETERS)){
			try{
				// Retrieve the whole list of parameters:
				JSONObject params = json.getJSONObject(TAPJob.PARAM_PARAMETERS);
				// If there is an UPLOAD parameter, convert the JSON array into a DALIUpload[] and add it to the job:
				if (params.has(TAPJob.PARAM_UPLOAD)){
					// retrieve the JSON array:
					JSONArray uploads = params.getJSONArray(TAPJob.PARAM_UPLOAD);
					// for each item of this array, build the corresponding DALIUpload and add it into an ArrayList:
					DALIUpload upl;
					ArrayList<DALIUpload> lstTAPUploads = new ArrayList<DALIUpload>();
					for(int i = 0; i < uploads.length(); i++){
						try{
							upl = getDALIUpload(uploads.getJSONObject(i), job);
							if (upl != null)
								lstTAPUploads.add(upl);
						}catch(JSONException je){
							getLogger().logUWS(LogLevel.ERROR, uploads.get(i), "RESTORATION", "Incorrect JSON format for a DALIUpload of the job \"" + job.getJobId() + "\": a JSONObject was expected!", null);
						}
					}
					// finally convert the ArrayList into a DALIUpload[] and add it inside the parameters list of the job:
					job.addOrUpdateParameter(TAPJob.PARAM_UPLOAD, lstTAPUploads.toArray(new DALIUpload[lstTAPUploads.size()]));
				}
			}catch(JSONException ex){}
		}

		// 2. Get the execution report and add it into the given job:
		TAPJob tapJob = (TAPJob)job;
		Object obj = job.getAdditionalParameterValue("tapexecreport");
		if (obj != null){
			if (obj instanceof JSONObject){
				JSONObject jsonExecReport = (JSONObject)obj;
				TAPExecutionReport execReport = new TAPExecutionReport(job.getJobId(), false, tapJob.getTapParams());
				String[] keys = JSONObject.getNames(jsonExecReport);
				for(String key : keys){
					try{
						if (key.equalsIgnoreCase("success"))
							execReport.success = jsonExecReport.getBoolean(key);
						else if (key.equalsIgnoreCase("uploadduration"))
							execReport.setDuration(ExecutionProgression.UPLOADING, jsonExecReport.getLong(key));
						else if (key.equalsIgnoreCase("parsingduration"))
							execReport.setDuration(ExecutionProgression.PARSING, jsonExecReport.getLong(key));
						else if (key.equalsIgnoreCase("executionduration"))
							execReport.setDuration(ExecutionProgression.EXECUTING_ADQL, jsonExecReport.getLong(key));
						else if (key.equalsIgnoreCase("formattingduration"))
							execReport.setDuration(ExecutionProgression.WRITING_RESULT, jsonExecReport.getLong(key));
						else if (key.equalsIgnoreCase("totalduration"))
							execReport.setTotalDuration(jsonExecReport.getLong(key));
						else
							getLogger().logUWS(LogLevel.WARNING, obj, "RESTORATION", "The execution report attribute '" + key + "' of the job \"" + job.getJobId() + "\" has been ignored because unknown!", null);
					}catch(JSONException je){
						getLogger().logUWS(LogLevel.ERROR, obj, "RESTORATION", "Incorrect JSON format for the execution report serialization of the job \"" + job.getJobId() + "\" (attribute: \"" + key + "\")!", je);
					}
				}
				tapJob.setExecReport(execReport);
			}else if (!(obj instanceof JSONObject))
				getLogger().logUWS(LogLevel.WARNING, obj, "RESTORATION", "Impossible to restore the execution report of the job \"" + job.getJobId() + "\" because the stored object is not a JSONObject!", null);
		}
	}

	/**
	 * Restore a {@link DALIUpload} from its JSON representation.
	 * 
	 * @param item	{@link JSONObject} representing the {@link DALIUpload} to restore.
	 * @param job	The job which owns this upload.
	 * 
	 * @return	The corresponding {@link DALIUpload} or NULL, if an error occurs while converting the JSON.
	 * 
	 * @since 2.0
	 */
	private DALIUpload getDALIUpload(final JSONObject item, final UWSJob job){
		try{

			// Get its label:
			String label = item.getString("label");

			// Build the DALIUpload object:
			/* If the upload spec. IS A FILE, the attribute 'file' should point toward a job parameter
			 * being an UploadFile. If so, get it and use it to build the DALIUpload: */
			if (item.has("file")){
				Object f = job.getAdditionalParameterValue(item.getString("file"));
				if (f == null || !(f instanceof UploadFile))
					getLogger().logUWS(LogLevel.ERROR, item, "RESTORATION", "Incorrect JSON format for the DALIUpload labelled \"" + label + "\" of the job \"" + job.getJobId() + "\": \"" + item.getString("file") + "\" is not pointing a job parameter representing a file!", null);
				return new DALIUpload(label, (UploadFile)f);
			}
			/* If the upload spec. IS A URI, the attribute 'uri' should contain it
			 * and should be used to build the DALIUpload: */
			else if (item.has("uri")){
				try{
					return new DALIUpload(label, new URI(item.getString("uri")), uws.getFileManager());
				}catch(URISyntaxException e){
					getLogger().logUWS(LogLevel.ERROR, item, "RESTORATION", "Incorrect URI for the DALIUpload labelled \"" + label + "\" of the job \"" + job.getJobId() + "\": \"" + item.getString("uri") + "\"!", null);
				}
			}
			/* If none of this both attribute is provided, it is an error and it is not possible to build the DALIUpload. */
			else
				getLogger().logUWS(LogLevel.ERROR, item, "RESTORATION", "Incorrect JSON format for the DALIUpload labelled \"" + label + "\" of the job \"" + job.getJobId() + "\": missing attribute 'file' or 'uri'!", null);

		}catch(JSONException je){
			getLogger().logUWS(LogLevel.ERROR, item, "RESTORATION", "Incorrect JSON format for a DALIUpload of the job \"" + job.getJobId() + "\": missing attribute 'label'!", null);
		}

		return null;
	}
}
