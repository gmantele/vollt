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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import tap.ServiceConnection;
import uws.ISO8601Format;
import uws.UWSException;
import uws.job.parameters.DestructionTimeController.DateField;
import uws.job.parameters.InputParamController;

/**
 * <p>Let controlling the destruction time of all jobs managed by a TAP service.
 * The maximum and default values are provided by the service connection.</p>
 * 
 * <p><i>Note:
 * 	By default, the destruction time can be modified by anyone without any limitation.
 * 	There is no default value (that means jobs may stay forever).
 * </i></p>
 * 
 * <p>The logic of the destruction time is set in this class. Here it is:</p>
 * <ul>
 * 	<li>If no value is specified by the UWS client, the default value is returned.</li>
 *  <li>If no default value is provided, the maximum destruction date is returned.</li>
 *  <li>If no maximum value is provided, there is no destruction.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (11/2014)
 */
public class TAPDestructionTimeController implements InputParamController {

	/** Connection to the service which knows the maximum and default value of this parameter. */
	protected final ServiceConnection service;

	/** Indicates whether the execution duration of jobs can be modified. */
	protected boolean allowModification = true;

	/**
	 * Build a controller for the Destruction parameter.
	 * 
	 * @param service	Connection to the TAP service.
	 */
	public TAPDestructionTimeController(final ServiceConnection service){
		this.service = service;
	}

	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	/**
	 * Let indicate whether the destruction time of any managed job can be modified.
	 * 
	 * @param allowModif <i>true</i> if the destruction time can be modified, <i>false</i> otherwise.
	 */
	public final void allowModification(final boolean allowModif){
		allowModification = allowModif;
	}

	/**
	 * Get the default period during which a job is kept.
	 * After this period, the job should be destroyed.
	 * 
	 * @return	The default retention period, -1 if none is provided.
	 */
	public final int getDefaultRetentionPeriod(){
		if (service.getRetentionPeriod() != null && service.getRetentionPeriod().length >= 2){
			if (service.getRetentionPeriod()[0] > 0)
				return service.getRetentionPeriod()[0];
		}
		return -1;
	}

	@Override
	public final Object getDefault(){
		// Get the default period and ensure it is always less or equal the maximum period, if any:
		int defaultPeriod = getDefaultRetentionPeriod();
		int maxPeriod = getMaxRetentionPeriod();
		if (defaultPeriod <= 0 || (maxPeriod > 0 && defaultPeriod > maxPeriod))
			defaultPeriod = maxPeriod;

		// Build and return the date:
		if (defaultPeriod > 0){
			Calendar date = Calendar.getInstance();
			try{
				date.add(DateField.SECOND.getFieldIndex(), defaultPeriod);
				return date.getTime();
			}catch(ArrayIndexOutOfBoundsException ex){}
		}

		// If no default period is specified or if an exception occurs, the maximum destruction time must be returned:
		return getMaxDestructionTime();
	}

	/**
	 * Get the maximum period during which a job is kept.
	 * After this period, the job should be destroyed.
	 * 
	 * @return	The maximum retention period, -1 if none is provided.
	 */
	public final int getMaxRetentionPeriod(){
		if (service.getRetentionPeriod() != null && service.getRetentionPeriod().length >= 2){
			if (service.getRetentionPeriod()[1] > 0)
				return service.getRetentionPeriod()[1];
		}
		return -1;
	}

	/**
	 * Gets the maximum destruction time: either computed with an interval of time or obtained directly by a maximum destruction time.
	 * 
	 * @return The maximum destruction time (<i>null</i> means that jobs may stay forever).
	 */
	public final Date getMaxDestructionTime(){
		// Get the maximum period:
		int maxPeriod = getMaxRetentionPeriod();

		// Build and return the maximum destruction date:
		if (maxPeriod > 0){
			Calendar date = Calendar.getInstance();
			try{
				date.add(DateField.SECOND.getFieldIndex(), maxPeriod);
				return date.getTime();
			}catch(ArrayIndexOutOfBoundsException ex){}
		}

		// If no maximum period is specified or if an exception occurs, NULL must be returned:
		return null;
	}

	@Override
	public Object check(Object value) throws UWSException{
		// If NULL value, return the default value:
		if (value == null)
			return getDefault();

		// Parse the given date:
		Date date = null;
		if (value instanceof Date)
			date = (Date)value;
		else if (value instanceof String){
			String strValue = (String)value;
			try{
				date = ISO8601Format.parseToDate(strValue);
			}catch(ParseException pe){
				throw new UWSException(UWSException.BAD_REQUEST, pe, "Wrong date format for the parameter \"destruction\": \"" + strValue + "\"! A date must be formatted in the ISO8601 format (\"yyyy-MM-dd'T'hh:mm:ss[.sss]['Z'|[+|-]hh:mm]\", fields inside brackets are optional).");
			}
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for the parameter \"destruction\": class \"" + value.getClass().getName() + "\"! It should be a Date or a string containing a date formatted in ISO8601 (\"yyyy-MM-dd'T'hh:mm:ss[.sss]['Z'|[+|-]hh:mm]\", fields inside brackets are optional).");

		// Ensure the date is before the maximum destruction time (from now):
		Date maxDate = getMaxDestructionTime();
		if (maxDate != null && date.after(maxDate))
			date = maxDate;

		// Return the parsed date
		return date;
	}

}
