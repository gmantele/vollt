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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import tap.ServiceConnection;
import tap.TAPJob;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.job.UWSJob;
import uws.job.parameters.DestructionTimeController.DateField;
import uws.job.parameters.InputParamController;

public class TAPDestructionTimeController implements InputParamController {

	protected final ServiceConnection service;
	protected boolean allowModification = true;

	public TAPDestructionTimeController(final ServiceConnection service){
		this.service = service;
	}

	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	public final void allowModification(final boolean allowModif){
		allowModification = allowModif;
	}

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

	public final int getMaxRetentionPeriod(){
		if (service.getRetentionPeriod() != null && service.getRetentionPeriod().length >= 2){
			if (service.getRetentionPeriod()[1] > 0)
				return service.getRetentionPeriod()[1];
		}
		return -1;
	}

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
				throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_DESTRUCTION_TIME, strValue, null, "A date not yet expired.");
			}
		}else
			throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_DESTRUCTION_TIME, value.toString(), value.getClass().getName(), "A date not yet expired.");

		Date maxDate = getMaxDestructionTime();
		if (maxDate != null && date.after(maxDate))
			throw new UWSException(UWSException.BAD_REQUEST, "The TAP service limits the DESTRUCTION INTERVAL (since now) to " + getMaxRetentionPeriod() + " s !");

		return date;
	}

}
