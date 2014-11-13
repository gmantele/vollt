package uws.job.parameters;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.Serializable;

import uws.UWSException;
import uws.job.UWSJob;

/**
 * <p>
 * 	Let controlling the execution duration of all jobs managed by a UWS. Thus it is possible to set a default and a maximum value.
 * 	Moreover you can indicate whether the execution duration of jobs can be modified by the user or not.
 * </p>
 * 
 * <p><i><u>Note:</u> the execution duration is always expressed <b>in seconds</b>.</i></p>
 * 
 * <p><i><u>Note:</u>
 * 	By default, the execution duration can be modified by anyone without any limitation.
 * 	The default and maximum value is {@link UWSJob#UNLIMITED_DURATION}.
 * </i></p>
 * 
 * <p>The logic of the execution duration is set in this class. Here it is:</p>
 * <ul>
 * 	<li>If no value is specified by the UWS client, the default value is returned.</li>
 *  <li>If no default value is provided, the maximum duration is returned.</li>
 *  <li>If no maximum value is provided, there is no limit (={@link UWSJob#UNLIMITED_DURATION}).</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (11/2014)
 */
public class ExecutionDurationController implements InputParamController, Serializable {
	private static final long serialVersionUID = 1L;

	/** The default duration (in seconds). */
	protected long defaultDuration = UWSJob.UNLIMITED_DURATION;

	/** The maximum duration (in seconds). */
	protected long maxDuration = UWSJob.UNLIMITED_DURATION;

	/** Indicates whether the execution duration of jobs can be modified. */
	protected boolean allowModification = true;

	/**
	 * <p>Create a controller for the execution duration.
	 * By default, there is no maximum value and the default duration is {@link UWSJob#UNLIMITED_DURATION}.</p>
	 * 
	 * <p>
	 * 	A default and/or maximum value can be set after creation using {@link #setDefaultExecutionDuration(long)}
	 * 	and {@link #setMaxExecutionDuration(long)}. By default this parameter can always be modified, but it can
	 * 	be forbidden using {@link #allowModification(boolean)}.
	 * </p>
	 */
	public ExecutionDurationController(){}

	/**
	 * <p>Create a controller for the execution duration.
	 * The default and the maximum duration are initialized with the given parameters.
	 * The third parameter allows also to forbid the modification of the execution duration by the user,
	 * if set to <i>false</i>.</p>
	 * 
	 * <p>
	 * 	A default and/or maximum value can be modified after creation using {@link #setDefaultExecutionDuration(long)}
	 * 	and {@link #setMaxExecutionDuration(long)}. The flag telling whether this parameter can be modified by the user
	 * 	can be changed using {@link #allowModification(boolean)}.
	 * </p>
	 * 
	 * @param defaultDuration	Duration (in seconds) set by default to a job, when none is specified.
	 * @param maxDuration		Maximum duration (in seconds) that can be set. If a greater value is provided by the user, an exception will be thrown by {@link #check(Object)}.
	 * @param allowModification	<i>true</i> to allow the user to modify this value when creating a job, <i>false</i> otherwise.
	 */
	public ExecutionDurationController(final long defaultDuration, final long maxDuration, final boolean allowModification){
		setDefaultExecutionDuration(defaultDuration);
		setMaxExecutionDuration(maxDuration);
		allowModification(allowModification);
	}

	@Override
	public Object getDefault(){
		return (defaultDuration > 0) ? defaultDuration : getMaxExecutionDuration();
	}

	@Override
	public Object check(final Object value) throws UWSException{
		// If no value, return the default one:
		if (value == null)
			return getDefault();

		// Otherwise, parse the given duration:
		Long duration = null;
		if (value instanceof Long)
			duration = (Long)value;
		else if (value instanceof Integer)
			duration = (long)(Integer)value;
		else if (value instanceof String){
			String strValue = (String)value;
			try{
				duration = Long.parseLong(strValue);
			}catch(NumberFormatException nfe){
				throw new UWSException(UWSException.BAD_REQUEST, "Wrong format for the maximum duration parameter: \"" + strValue + "\"! It should be a long numeric value between " + UWSJob.UNLIMITED_DURATION + " and " + maxDuration + " (Default value: " + defaultDuration + ").");
			}
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for the maximum duration parameter: class \"" + value.getClass().getName() + "\"! It should be long or a string containing only a long value.");

		// If the duration is negative or zero, set it to UNLIMITED:
		if (duration <= 0)
			duration = UWSJob.UNLIMITED_DURATION;

		// Set the maximum duration if the duration is greater than the maximum value:
		if (maxDuration > 0 && (duration > maxDuration || duration <= 0))
			duration = maxDuration;

		return duration;
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	/**
	 * Gets the default execution duration.
	 * 
	 * @return The default execution duration (in seconds) <i>(0 or less mean an unlimited duration)</i>.
	 * 
	 * @deprecated This function is completely equivalent to {@link #getDefault()}.
	 */
	@Deprecated
	public final long getDefaultExecutionDuration(){
		return (Long)getDefault();
	}

	/**
	 * Sets the default execution duration.
	 * 
	 * @param defaultExecutionDuration The new default execution duration (in seconds) <i>({@link UWSJob#UNLIMITED_DURATION}, 0 or a negative value mean an unlimited duration)</i>.
	 */
	public final boolean setDefaultExecutionDuration(long defaultExecutionDuration){
		defaultExecutionDuration = (defaultExecutionDuration <= 0) ? UWSJob.UNLIMITED_DURATION : defaultExecutionDuration;

		if (defaultExecutionDuration != UWSJob.UNLIMITED_DURATION && maxDuration != UWSJob.UNLIMITED_DURATION && defaultExecutionDuration > maxDuration)
			return false;
		else
			defaultDuration = defaultExecutionDuration;

		return true;
	}

	/**
	 * Gets the maximum execution duration.
	 * 
	 * @return The maximum execution duration (in seconds) <i>(0 or less mean an unlimited duration)</i>.
	 */
	public final long getMaxExecutionDuration(){
		return maxDuration;
	}

	/**
	 * Sets the maximum execution duration.
	 * 
	 * @param maxExecutionDuration The maximum execution duration (in seconds) <i>({@link UWSJob#UNLIMITED_DURATION}, 0 or a negative value mean an unlimited duration)</i>.
	 */
	public final void setMaxExecutionDuration(long maxExecutionDuration){
		maxDuration = (maxExecutionDuration <= 0) ? UWSJob.UNLIMITED_DURATION : maxExecutionDuration;
		if (defaultDuration != UWSJob.UNLIMITED_DURATION && maxDuration != UWSJob.UNLIMITED_DURATION && defaultDuration > maxDuration)
			defaultDuration = maxDuration;
	}

	/**
	 * Tells whether the execution duration of any managed job can be modified.
	 * 
	 * @return <i>true</i> if the execution duration can be modified, <i>false</i> otherwise.
	 */
	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	/**
	 * Lets indicating whether the execution duration of any managed job can be modified.
	 * 
	 * @param allowModification <i>true</i> if the execution duration can be modified, <i>false</i> otherwise.
	 */
	public final void allowModification(boolean allowModification){
		this.allowModification = allowModification;
	}

}
