package tap;

import adql.db.DBColumn;

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
 * Copyright 2012-2019 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import tap.parameters.TAPParameters;

/**
 * Report the execution (including the parsing and the output writing) of an
 * ADQL query.
 *
 * <p>
 * 	It gives information on the job parameters, the job ID, whether it is a
 * 	synchronous task or not, times of each execution step (uploading, parsing,
 * 	executing and writing), the resulting columns and the success or not of the
 * 	execution.
 * </p>
 *
 * <p>
 * 	This report is completely filled by {@link ADQLExecutor}, and aims to be
 * 	used/read only at the end of the job or when it is definitely finished.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.3 (03/2019)
 */
public class TAPExecutionReport {

	/** ID of the job whose the execution is reported here. */
	public final String jobID;

	/** Indicate whether this execution is done in a synchronous or asynchronous
	 * job. */
	public final boolean synchronous;

	/** List of all parameters provided in the user request. */
	public final TAPParameters parameters;

	/** Input ADQL query after an automatic fix by TAP-Lib.
	 * <p>This field is set only if the option fix_on_fail is enabled in the TAP
	 * configuration and that a query has been fixed.</p>
	 * @since 2.3 */
	public String fixedQuery = null;

	/** List of all resulting columns. <i>Empty array, if not yet known.</i> */
	public DBColumn[] resultingColumns = new DBColumn[0];

	/** Total number of written rows.
	 * @since 2.0 */
	public long nbRows = -1;

	/** Duration of all execution steps. <i>For the moment only 4 steps (in the
	 * order): uploading, parsing, executing and writing.</i> */
	protected final long[] durations = new long[]{ -1, -1, -1, -1 };

	/** Total duration of the job execution. */
	protected long totalDuration = -1;

	/** Indicate whether this job has ended successfully or not.
	 * <i>At the beginning or while executing, this field is always FALSE.</i> */
	public boolean success = false;

	/**
	 * Build an empty execution report.
	 *
	 * @param jobID			ID of the job whose the execution must be described
	 *             			here.
	 * @param synchronous	<i>true</i> if the job is synchronous,
	 *                   	<i>false</i> otherwise.
	 * @param params		List of all parameters provided by the user for the
	 *              		execution.
	 */
	public TAPExecutionReport(final String jobID, final boolean synchronous, final TAPParameters params){
		this.jobID = jobID;
		this.synchronous = synchronous;
		parameters = params;
	}

	/**
	 * Map the execution progression with an index inside the {@link #durations}
	 * array.
	 *
	 * <p><i><b>Warning:</b>
	 * 	for the moment, only {@link ExecutionProgression#UPLOADING},
	 * 	{@link ExecutionProgression#PARSING},
	 * 	{@link ExecutionProgression#EXECUTING_ADQL} and
	 * 	{@link ExecutionProgression#WRITING_RESULT} are managed.
	 * </i></p>
	 *
	 * @param tapProgression	Execution progression.
	 *
	 * @return	Index in the array {@link #durations},
	 *        	or -1 if the given execution progression is not managed.
	 */
	protected int getIndexDuration(final ExecutionProgression tapProgression){
		switch(tapProgression){
			case UPLOADING:
				return 0;
			case PARSING:
				return 1;
			case EXECUTING_ADQL:
				return 2;
			case WRITING_RESULT:
				return 3;
			default:
				return -1;
		}
	}

	/**
	 * Get the duration corresponding to the given job execution step.
	 *
	 * @param tapStep	Job execution step.
	 *
	 * @return	The corresponding duration (in ms), or -1 if this step has not
	 *        	been (yet) processed.
	 *
	 * @see #getIndexDuration(ExecutionProgression)
	 */
	public final long getDuration(final ExecutionProgression tapStep){
		int indDuration = getIndexDuration(tapStep);
		if (indDuration < 0 || indDuration >= durations.length)
			return -1;
		else
			return durations[indDuration];
	}

	/**
	 * Set the duration corresponding to the given execution step.
	 *
	 * @param tapStep	Job execution step.
	 * @param duration	Duration (in ms) of the given execution step.
	 */
	public final void setDuration(final ExecutionProgression tapStep, final long duration){
		int indDuration = getIndexDuration(tapStep);
		if (indDuration < 0 || indDuration >= durations.length)
			return;
		else
			durations[indDuration] = duration;
	}

	/**
	 * Get the execution of the UPLOAD step.
	 * @return Duration (in ms).
	 * @see #getDuration(ExecutionProgression)
	 */
	public final long getUploadDuration(){
		return getDuration(ExecutionProgression.UPLOADING);
	}

	/**
	 * Get the execution of the PARSE step.
	 * @return Duration (in ms).
	 * @see #getDuration(ExecutionProgression)
	 */
	public final long getParsingDuration(){
		return getDuration(ExecutionProgression.PARSING);
	}

	/**
	 * Get the execution of the EXECUTION step.
	 * @return Duration (in ms).
	 * @see #getDuration(ExecutionProgression)
	 */
	public final long getExecutionDuration(){
		return getDuration(ExecutionProgression.EXECUTING_ADQL);
	}

	/**
	 * Get the execution of the FORMAT step.
	 * @return Duration (in ms).
	 * @see #getDuration(ExecutionProgression)
	 */
	public final long getFormattingDuration(){
		return getDuration(ExecutionProgression.WRITING_RESULT);
	}

	/**
	 * Get the total duration of the job execution.
	 * @return	Duration (in ms).
	 */
	public final long getTotalDuration(){
		return totalDuration;
	}

	/**
	 * Set the total duration of the job execution.
	 * @param duration	Duration (in ms) to set.
	 */
	public final void setTotalDuration(final long duration){
		totalDuration = duration;
	}

}
