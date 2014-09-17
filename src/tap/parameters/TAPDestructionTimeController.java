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
import uws.UWSException;
import uws.job.UWSJob;
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
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
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
	 * @param allowModification <i>true</i> if the destruction time can be modified, <i>false</i> otherwise.
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
		int defaultPeriod = getDefaultRetentionPeriod();
		if (defaultPeriod > 0){
			Calendar date = Calendar.getInstance();
			try{
				date.add(DateField.SECOND.getFieldIndex(), defaultPeriod);
				return date.getTime();
			}catch(ArrayIndexOutOfBoundsException ex){
				return null;
			}
		}else
			return null;
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
		int maxPeriod = getMaxRetentionPeriod();
		if (maxPeriod > 0){
			Calendar date = Calendar.getInstance();
			try{
				date.add(DateField.SECOND.getFieldIndex(), maxPeriod);
				return date.getTime();
			}catch(ArrayIndexOutOfBoundsException ex){
				return null;
			}
		}else
			return null;
	}

	@Override
	public Object check(Object value) throws UWSException{
		if (value == null)
			return null;

		Date date = null;
		if (value instanceof Date)
			date = (Date)value;
		else if (value instanceof String){
			String strValue = (String)value;
			try{
				date = UWSJob.dateFormat.parse(strValue);
			}catch(ParseException pe){
				throw new UWSException(UWSException.BAD_REQUEST, pe, "Wrong date format for the destruction time parameter: \"" + strValue + "\"! The format to respect is: " + UWSJob.DEFAULT_DATE_FORMAT);
			}
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for the destruction time parameter: class \"" + value.getClass().getName() + "\"! It should be a Date or a string containing a date with the format \"" + UWSJob.DEFAULT_DATE_FORMAT + "\".");

		Date maxDate = getMaxDestructionTime();
		if (maxDate != null && date.after(maxDate))
			throw new UWSException(UWSException.BAD_REQUEST, "The TAP service limits the DESTRUCTION INTERVAL (since now) to " + getMaxRetentionPeriod() + " s !");

		return date;
	}

}
