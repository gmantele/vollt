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
import uws.job.parameters.InputParamController;

/**
 * <p>Let controlling the maximum number of rows that can be output by a TAP service.
 * The maximum and default values are provided by the service connection.</p>
 * 
 * <p><i>Note:
 * 	By default, this parameter can be modified by anyone without any limitation.
 * </i></p>
 * 
 * <p>The logic of the output limit is set in this class. Here it is:</p>
 * <ul>
 * 	<li>If no value is specified by the TAP client, none is returned.</li>
 *  <li>If no default value is provided, no default limitation is set (={@link TAPJob#UNLIMITED_MAX_REC}).</li>
 *  <li>If no maximum value is provided, there is no output limit (={@link TAPJob#UNLIMITED_MAX_REC}).</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public class MaxRecController implements InputParamController {

	/** Connection to the service which knows the maximum and default value of this parameter. */
	protected final ServiceConnection service;

	/** Indicates whether the output limit of jobs can be modified. */
	protected boolean allowModification = true;

	/**
	 * Build a controller for the MaxRec parameter.
	 * 
	 * @param service	Connection to the TAP service.
	 */
	public MaxRecController(final ServiceConnection service){
		this.service = service;
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

	/**
	 * Get the maximum number of rows that can be output.
	 * 
	 * @return	Maximum output limit.
	 */
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
				throw new UWSException(UWSException.BAD_REQUEST, "Wrong format for the MaxRec parameter: \"" + strValue + "\"! It should be a integer value between " + TAPJob.UNLIMITED_MAX_REC + " and " + maxOutputLimit + " (Default value: " + defaultOutputLimit + ").");
			}
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for the MaxRec parameter: class \"" + value.getClass().getName() + "\"! It should be an integer or a string containing only an integer value.");

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
