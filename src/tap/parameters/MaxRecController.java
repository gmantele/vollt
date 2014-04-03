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
import tap.ServiceConnection.LimitUnit;

import uws.UWSException;
import uws.UWSExceptionFactory;

import uws.job.parameters.InputParamController;

public class MaxRecController implements InputParamController {

	protected final ServiceConnection<?> service;

	/** Indicates whether the output limit of jobs can be modified. */
	protected boolean allowModification = true;


	public MaxRecController(final ServiceConnection<?> service){
		this.service = service;
		allowModification(allowModification);
	}

	@Override
	public final Object getDefault() {
		if (service.getOutputLimit() != null && service.getOutputLimit().length >= 2 && service.getOutputLimitType() != null && service.getOutputLimitType().length == service.getOutputLimit().length){
			if (service.getOutputLimit()[0] > 0 && service.getOutputLimitType()[0] == LimitUnit.rows)
				return service.getOutputLimit()[0];
		}
		return TAPJob.UNLIMITED_MAX_REC;
	}

	public final int getMaxOutputLimit(){
		if (service.getOutputLimit() != null && service.getOutputLimit().length >= 2 && service.getOutputLimitType() != null && service.getOutputLimitType().length == service.getOutputLimit().length){
			if (service.getOutputLimit()[1] > 0 && service.getOutputLimitType()[1] == LimitUnit.rows)
				return service.getOutputLimit()[1];
		}
		return TAPJob.UNLIMITED_MAX_REC;
	}

	@Override
	public Object check(Object value) throws UWSException {
		if (value == null)
			return null;

		int maxOutputLimit = getMaxOutputLimit();
		Integer defaultOutputLimit = (Integer)getDefault(), maxRec = null;
		if (value instanceof Integer)
			maxRec = (Integer)value;
		else if (value instanceof String){
			String strValue = (String)value;
			try{
				maxRec = Integer.parseInt(strValue);
			}catch(NumberFormatException nfe){
				throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_MAX_REC, strValue, null, "An integer value between "+TAPJob.UNLIMITED_MAX_REC+" and "+maxOutputLimit+" (Default value: "+defaultOutputLimit+").");
			}
		}else
			throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_MAX_REC, null, value.getClass().getName(), "An integer value between "+TAPJob.UNLIMITED_MAX_REC+" and "+maxOutputLimit+" (Default value: "+defaultOutputLimit+").");

		if (maxRec < TAPJob.UNLIMITED_MAX_REC)
			maxRec = TAPJob.UNLIMITED_MAX_REC;
		else if (maxOutputLimit > TAPJob.UNLIMITED_MAX_REC && maxRec > maxOutputLimit)
			throw new UWSException(UWSException.BAD_REQUEST, "The TAP limits the maxRec parameter (=output limit) to maximum "+maxOutputLimit+" rows !");

		return maxRec;
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	/**
	 * Tells whether the output limit of any managed job can be modified.
	 * 
	 * @return <i>true</i> if the output limit can be modified, <i>false</i> otherwise.
	 */
	public final boolean allowModification() {
		return allowModification;
	}

	/**
	 * Lets indicating whether the output limit of any managed job can be modified.
	 * 
	 * @param allowModification <i>true</i> if the output limit can be modified, <i>false</i> otherwise.
	 */
	public final void allowModification(boolean allowModification) {
		this.allowModification = allowModification;
	}

}
