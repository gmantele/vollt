package tap.parameters;

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

import tap.ServiceConnection;
import tap.TAPJob;
import uws.UWSException;
import uws.job.parameters.InputParamController;

/**
 * <p>Let controlling the execution duration of all jobs managed by a TAP service.
 * The maximum and default values are provided by the service connection.</p>
 * 
 * <p><i>Note:
 * 	By default, the execution duration can be modified by anyone without any limitation.
 * 	The default value is {@link TAPJob#UNLIMITED_DURATION}.
 * </i></p>
 * 
 * <p>The logic of the execution duration is set in this class. Here it is:</p>
 * <ul>
 * 	<li>If no value is specified by the TAP client, the default value is returned.</li>
 *  <li>If no default value is provided, the maximum duration is returned.</li>
 *  <li>If no maximum value is provided, there is no limit (={@link TAPJob#UNLIMITED_DURATION}).</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (11/2014)
 */
public class TAPExecutionDurationController implements InputParamController {

	/** Connection to the service which knows the maximum and default value of this parameter. */
	protected final ServiceConnection service;

	/** Indicate whether the execution duration of jobs can be modified. */
	protected boolean allowModification = true;

	/**
	 * Build a controller for the ExecutionDuration parameter.
	 * 
	 * @param service	Connection to the TAP service.
	 */
	public TAPExecutionDurationController(final ServiceConnection service){
		this.service = service;
	}

	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	/**
	 * Let indicate whether the execution duration of any managed job can be modified.
	 * 
	 * @param allowModification <i>true</i> if the execution duration can be modified, <i>false</i> otherwise.
	 */
	public final void allowModification(final boolean allowModif){
		allowModification = allowModif;
	}

	@Override
	public final Object getDefault(){
		// Get the default value from the service connection:
		long defaultVal = TAPJob.UNLIMITED_DURATION;
		if (service.getExecutionDuration() != null && service.getExecutionDuration().length >= 2)
			defaultVal = service.getExecutionDuration()[0];

		// The default value is also limited by the maximum value if any:
		long maxVal = getMaxDuration();
		return (defaultVal <= 0 || (maxVal > 0 && defaultVal > maxVal)) ? maxVal : defaultVal;
	}

	/**
	 * Gets the maximum execution duration.
	 * 
	 * @return The maximum execution duration <i>(0 or less mean an unlimited duration)</i>.
	 */
	public final long getMaxDuration(){
		if (service.getExecutionDuration() != null && service.getExecutionDuration().length >= 2){
			if (service.getExecutionDuration()[1] > 0)
				return service.getExecutionDuration()[1];
		}
		return TAPJob.UNLIMITED_DURATION;
	}

	@Override
	public Object check(final Object value) throws UWSException{
		// If no value, return the default one:
		if (value == null)
			return getDefault();

		// Get the default and maximum durations for comparison:
		long defaultDuration = (Long)getDefault(), maxDuration = getMaxDuration();

		// Parse the given duration:		
		Long duration;
		if (value instanceof Long)
			duration = (Long)value;
		else if (value instanceof Integer)
			duration = (long)(Integer)value;
		else if (value instanceof String){
			try{
				duration = Long.parseLong((String)value);
			}catch(NumberFormatException nfe){
				throw new UWSException(UWSException.BAD_REQUEST, "Wrong format for the parameter \"executionduration\": \"" + value.toString() + "\"! It should be a long numeric value between " + TAPJob.UNLIMITED_DURATION + " and " + maxDuration + " (Default value: " + defaultDuration + ").");
			}
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for the parameter \"executionduration\": class \"" + value.getClass().getName() + "\"! It should be long or a string containing only a long value.");

		// A negative value must be considered as an unlimited duration:
		if (duration <= 0)
			duration = TAPJob.UNLIMITED_DURATION;

		// Ensure the given value is less than the maximum duration:
		if (maxDuration > 0 && (duration > maxDuration || duration <= 0))
			duration = maxDuration;

		return duration;
	}

}
