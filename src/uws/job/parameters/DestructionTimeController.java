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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.Serializable;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import uws.UWSException;
import uws.UWSExceptionFactory;

import uws.job.UWSJob;

/**
 * <p>
 * 	Let's controlling the destruction time of all jobs managed by a UWS. Thus it is possible to set a default and a maximum value.
 * 	Moreover you can indicate whether the destruction time of jobs can be modified by the user or not.
 * </p>
 * 
 * <p>
 * 	<i><u>Notes:</u>
 * 		<ul>
 * 			<li>By default, the destruction time can be modified by anyone without any limitation.
 * 				There is no default value (that means jobs may stay forever).</li>
 * 			<li>You can specify a destruction time (default or maximum value) in two ways:
 * 				by an exact date-time or by an interval of time from the initialization (expressed in the second, minutes, hours, days, months or years).</li>
 * 		</ul>
 * 	</i>
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 05/2012
 */
public class DestructionTimeController implements InputParamController, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Represents a date/time field.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 02/2011
	 * 
	 * @see Calendar
	 */
	public static enum DateField{
		SECOND(Calendar.SECOND), MINUTE(Calendar.MINUTE), HOUR(Calendar.HOUR), DAY(Calendar.DAY_OF_MONTH), MONTH(Calendar.MONTH), YEAR(Calendar.YEAR);

		private final int index;

		private DateField(int fieldIndex){
			index = fieldIndex;
		}

		public final int getFieldIndex(){
			return index;
		}
	}

	/** Default value of an interval: a null interval. */
	public final static int NO_INTERVAL = 0;

	/** The default destruction time. */
	protected Date defaultTime = null;
	/** The date-time field on which the default interval applies. */
	protected DateField defaultIntervalField = null;
	/** The default interval from the initialization to the destruction of the concerned job. */
	protected int defaultInterval = NO_INTERVAL;

	/** The maximum destruction time. */
	protected Date maxTime = null;
	/** The date-time field on which the maximum interval applies. */
	protected DateField maxIntervalField = null;
	/** The maximum interval from the initialization to the destruction of the concerned job. */
	protected int maxInterval = NO_INTERVAL;

	/** Indicates whether the destruction time of jobs can be modified. */
	protected boolean allowModification = true;

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
				throw UWSExceptionFactory.badFormat(null, UWSJob.PARAM_DESTRUCTION_TIME, strValue, null, "A date not yet expired.");
			}
		}else
			throw UWSExceptionFactory.badFormat(null, UWSJob.PARAM_DESTRUCTION_TIME, value.toString(), value.getClass().getName(), "A date not yet expired.");

		Date maxDate = getMaxDestructionTime();
		if (maxDate != null && date.after(maxDate))
			throw new UWSException(UWSException.BAD_REQUEST, "The UWS limits " + ((defaultInterval > NO_INTERVAL) ? ("the DESTRUCTION INTERVAL (since now) to " + maxInterval + " " + maxIntervalField.name().toLowerCase() + "s") : ("the DESTRUCTION TIME to " + maxDate)) + " !");

		return date;
	}

	@Override
	public Object getDefault(){
		return getDefaultDestructionTime();
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	/**
	 * Gets the default destruction time: either computed with an interval of time or obtained directly by a default destruction time.
	 * 
	 * @return The default destruction time (<i>null</i> means that jobs may stay forever).
	 */
	public final Date getDefaultDestructionTime(){
		if (defaultInterval > NO_INTERVAL){
			Calendar date = Calendar.getInstance();
			try{
				date.add(defaultIntervalField.getFieldIndex(), defaultInterval);
				return date.getTime();
			}catch(ArrayIndexOutOfBoundsException ex){
				return null;
			}
		}else
			return defaultTime;
	}

	/**
	 * <p>Sets the default destruction time.</p>
	 * 
	 * <p>
	 * 	<i><u>Note:</u>
	 * 		If there was a default interval, it is reset and so the given destruction time will be used by {@link #getDefaultDestructionTime()}.
	 * 	</i>
	 * </p>
	 * 
	 * @param defaultDestructionTime The default destruction time to set (<i>null</i> means jobs may stay forever).
	 */
	public final void setDefaultDestructionTime(Date defaultDestructionTime){
		this.defaultTime = defaultDestructionTime;
		defaultInterval = NO_INTERVAL;
		defaultIntervalField = null;
	}

	/**
	 * <p>Gets the default interval value.</p>
	 * 
	 * <p><i><u>Note:</u> To get the corresponding unit, use {@link #getDefaultIntervalField()} and {@link DateField#name()}.</i></p>
	 * 
	 * @return The default destruction interval.
	 */
	public final int getDefaultDestructionInterval(){
		return defaultInterval;
	}

	/**
	 * Gets the date-time field of the default interval.
	 * 
	 * @return The default interval field.
	 */
	public final DateField getDefaultIntervalField(){
		return defaultIntervalField;
	}

	/**
	 * <p>Sets the default interval <b>in minutes</b> from the initialization to the destruction of the concerned job.</p>
	 * 
	 * <p>
	 * 	<i><u>Note:</u>
	 * 		If there was a default destruction time, it is reset and so the given interval will be used by {@link #getDefaultDestructionTime()}.
	 * 	</i>
	 * </p>
	 * 
	 * @param defaultDestructionInterval The default destruction interval ({@link #NO_INTERVAL}, 0 or a negative value mean the job may stay forever).
	 * 
	 * @see #setDefaultDestructionInterval(int, DateField)
	 */
	public final void setDefaultDestructionInterval(int defaultDestructionInterval){
		setDefaultDestructionInterval(defaultDestructionInterval, DateField.MINUTE);
	}

	/**
	 * <p>Sets the default interval (in the given unit) from the initialization to the destruction of the concerned job.</p>
	 * 
	 * <p>
	 * 	<i><u>Note:</u>
	 * 		If there was a default destruction time, it is reset and so the given interval will be used by {@link #getDefaultDestructionTime()}.
	 * 	</i>
	 * </p>
	 * 
	 * @param defaultDestructionInterval	The default destruction interval ({@link #NO_INTERVAL}, 0 or a negative value mean the job may stay forever).
	 * @param timeField						The unit of the interval (<i>null</i> means the job may stay forever).
	 */
	public final void setDefaultDestructionInterval(int defaultDestructionInterval, DateField timeField){
		if (defaultDestructionInterval <= 0 || timeField == null){
			defaultIntervalField = null;
			defaultInterval = NO_INTERVAL;
		}else{
			defaultIntervalField = timeField;
			defaultInterval = defaultDestructionInterval;
		}
		defaultTime = null;
	}

	/**
	 * Gets the maximum destruction time: either computed with an interval of time or obtained directly by a maximum destruction time.
	 * 
	 * @return The maximum destruction time (<i>null</i> means that jobs may stay forever).
	 */
	public final Date getMaxDestructionTime(){
		if (maxInterval > NO_INTERVAL){
			Calendar date = Calendar.getInstance();
			try{
				date.add(maxIntervalField.getFieldIndex(), maxInterval);
				return date.getTime();
			}catch(ArrayIndexOutOfBoundsException ex){
				return null;
			}
		}else
			return maxTime;
	}

	/**
	 * <p>Sets the maximum destruction time.</p>
	 * 
	 * <p>
	 * 	<i><u>Note:</u>
	 * 		If there was a maximum interval, it is reset and so the given destruction time will be used by {@link #getMaxDestructionTime()}.
	 * 	</i>
	 * </p>
	 * 
	 * @param maxDestructionTime The maximum destruction time to set (<i>null</i> means jobs may stay forever).
	 */
	public final void setMaxDestructionTime(Date maxDestructionTime){
		this.maxTime = maxDestructionTime;
		maxInterval = NO_INTERVAL;
		maxIntervalField = null;
	}

	/**
	 * <p>Gets the maximum interval value.</p>
	 * 
	 * <p><i><u>Note:</u> To get the corresponding unit, use {@link #getMaxIntervalField()} and {@link DateField#name()}.</i></p>
	 * 
	 * @return The maximum destruction interval.
	 */
	public final int getMaxDestructionInterval(){
		return maxInterval;
	}

	/**
	 * Gets the date-time field of the maximum interval.
	 * 
	 * @return The maximum interval field.
	 */
	public final DateField getMaxIntervalField(){
		return maxIntervalField;
	}

	/**
	 * <p>Sets the maximum interval <b>in minutes</b> from the initialization to the destruction of the concerned job.</p>
	 * 
	 * <p>
	 * 	<i><u>Note:</u>
	 * 		If there was a maximum destruction time, it is reset and so the given interval will be used by {@link #getMaxDestructionTime()}.
	 * 	</i>
	 * </p>
	 * 
	 * @param maxDestructionInterval The maximum destruction interval ({@link #NO_INTERVAL}, 0 or a negative value mean the job may stay forever).
	 * 
	 * @see #setMaxDestructionInterval(int, DateField)
	 */
	public final void setMaxDestructionInterval(int maxDestructionInterval){
		setMaxDestructionInterval(maxDestructionInterval, DateField.MINUTE);
	}

	/**
	 * <p>Sets the maximum interval (in the given unit) from the initialization to the destruction of the concerned job.</p>
	 * 
	 * <p>
	 * 	<i><u>Note:</u>
	 * 		If there was a maximum destruction time, it is reset and so the given interval will be used by {@link #getMaxDestructionTime()}.
	 * 	</i>
	 * </p>
	 * 
	 * @param maxDestructionInterval		The maximum destruction interval ({@link #NO_INTERVAL}, 0 or a negative value mean the job may stay forever).
	 * @param timeField						The unit of the interval (<i>null</i> means the job may stay forever).
	 */
	public final void setMaxDestructionInterval(int maxDestructionInterval, DateField timeField){
		this.maxInterval = maxDestructionInterval;
		maxIntervalField = timeField;
		maxTime = null;
	}

	/**
	 * Tells whether the destruction time of any managed job can be modified.
	 * 
	 * @return <i>true</i> if the destruction time can be modified, <i>false</i> otherwise.
	 */
	public final boolean allowModification(){
		return allowModification;
	}

	/**
	 * Lets indicating whether the destruction time of any managed job can be modified.
	 * 
	 * @param allowModification <i>true</i> if the destruction time can be modified, <i>false</i> otherwise.
	 */
	public final void allowModification(boolean allowModification){
		this.allowModification = allowModification;
	}

}
