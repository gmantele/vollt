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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import tap.ServiceConnection;
import tap.ServiceConnection.LimitUnit;
import tap.TAPJob;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.job.parameters.InputParamController;

/**
 * The logic of the output limit is set in this class. Here it is:
 * 
 *  - If no value is specified by the TAP client, none is returned.
 *  - If no default value is provided, no default limitation is set (={@link TAPJob#UNLIMITED_MAX_REC}).
 *  - If no maximum value is provided, there is no output limit (={@link TAPJob#UNLIMITED_MAX_REC}).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI) - gmantele@ari.uni-heidelberg.de
 * @version 1.1 (03/2014)
 */
public class MaxRecController implements InputParamController {

	protected final ServiceConnection service;

	/** Indicates whether the output limit of jobs can be modified. */
	protected boolean allowModification = true;

	public MaxRecController(final ServiceConnection service){
		this.service = service;
		allowModification(allowModification);
	}

	@Override
	public final Object getDefault(){
		// If a default output limit is set by the TAP service connection, return it:
		if (service.getOutputLimit() != null && service.getOutputLimit().length >= 2 && service.getOutputLimitType() != null && service.getOutputLimitType().length == service.getOutputLimit().length){
			if (service.getOutputLimit()[0] > 0 && service.getOutputLimitType()[0] == LimitUnit.rows)
				return service.getOutputLimit()[0];
		}
		// Otherwise, return no limitation:
		return TAPJob.UNLIMITED_MAX_REC;
	}

	public final int getMaxOutputLimit(){
		// If a maximum output limit is set by the TAP service connection, return it:
		if (service.getOutputLimit() != null && service.getOutputLimit().length >= 2 && service.getOutputLimitType() != null && service.getOutputLimitType().length == service.getOutputLimit().length){
			if (service.getOutputLimit()[1] > 0 && service.getOutputLimitType()[1] == LimitUnit.rows)
				return service.getOutputLimit()[1];
		}
		// Otherwise, there is no limit:
		return TAPJob.UNLIMITED_MAX_REC;
	}

	@Override
	public Object check(Object value) throws UWSException{
		// If no limit is provided by the TAP client, none is returned:
		if (value == null)
			return null;

		// Parse the provided limit:
		int maxOutputLimit = getMaxOutputLimit();
		Integer defaultOutputLimit = (Integer)getDefault(), maxRec = null;
		if (value instanceof Integer)
			maxRec = (Integer)value;
		else if (value instanceof String){
			String strValue = (String)value;
			try{
				maxRec = Integer.parseInt(strValue);
			}catch(NumberFormatException nfe){
				throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_MAX_REC, strValue, null, "An integer value between " + TAPJob.UNLIMITED_MAX_REC + " and " + maxOutputLimit + " (Default value: " + defaultOutputLimit + ").");
			}
		}else
			throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_MAX_REC, null, value.getClass().getName(), "An integer value between " + TAPJob.UNLIMITED_MAX_REC + " and " + maxOutputLimit + " (Default value: " + defaultOutputLimit + ").");

		// A negative output limit is considered as an unlimited output limit:
		if (maxRec < TAPJob.UNLIMITED_MAX_REC)
			maxRec = TAPJob.UNLIMITED_MAX_REC;

		// If the limit is greater than the maximum one, an exception is thrown:
		if (maxRec == TAPJob.UNLIMITED_MAX_REC || maxRec > maxOutputLimit)
			maxRec = maxOutputLimit;

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
	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	/**
	 * Lets indicating whether the output limit of any managed job can be modified.
	 * 
	 * @param allowModification <i>true</i> if the output limit can be modified, <i>false</i> otherwise.
	 */
	public final void allowModification(boolean allowModification){
		this.allowModification = allowModification;
	}

}
