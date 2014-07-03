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

import tap.ServiceConnection;
import tap.TAPJob;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.job.parameters.InputParamController;

public class TAPExecutionDurationController implements InputParamController {

	protected final ServiceConnection service;
	protected boolean allowModification = true;

	public TAPExecutionDurationController(final ServiceConnection service){
		this.service = service;
	}

	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	public final void allowModification(final boolean allowModif){
		allowModification = allowModif;
	}

	@Override
	public final Object getDefault(){
		if (service.getExecutionDuration() != null && service.getExecutionDuration().length >= 2){
			if (service.getExecutionDuration()[0] > 0)
				return service.getExecutionDuration()[0];
		}
		return TAPJob.UNLIMITED_DURATION;
	}

	public final long getMaxDuration(){
		if (service.getExecutionDuration() != null && service.getExecutionDuration().length >= 2){
			if (service.getExecutionDuration()[1] > 0)
				return service.getExecutionDuration()[1];
		}
		return TAPJob.UNLIMITED_DURATION;
	}

	@Override
	public Object check(Object value) throws UWSException{
		if (value == null)
			return null;

		long defaultDuration = ((Long)getDefault()).longValue(), maxDuration = getMaxDuration();
		Long duration;

		if (value instanceof Long)
			duration = (Long)value;
		else if (value instanceof String){
			try{
				duration = Long.parseLong((String)value);
			}catch(NumberFormatException nfe){
				throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_EXECUTION_DURATION, value.toString(), null, "A long value between " + TAPJob.UNLIMITED_DURATION + " and " + maxDuration + " (Default value: " + defaultDuration + ").");
			}
		}else
			throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_EXECUTION_DURATION, null, value.getClass().getName(), "A long value between " + TAPJob.UNLIMITED_DURATION + " and " + maxDuration + " (Default value: " + defaultDuration + ").");

		if (duration < TAPJob.UNLIMITED_DURATION)
			duration = TAPJob.UNLIMITED_DURATION;
		else if (maxDuration > TAPJob.UNLIMITED_DURATION && duration > maxDuration)
			throw new UWSException(UWSException.BAD_REQUEST, "The TAP service limits the execution duration to maximum " + maxDuration + " seconds !");

		return duration;
	}

}
