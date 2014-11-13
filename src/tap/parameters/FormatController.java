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

import java.util.Iterator;

import tap.ServiceConnection;
import tap.TAPJob;
import tap.formatter.OutputFormat;
import uws.UWSException;
import uws.job.parameters.InputParamController;

/**
 * <p>Let controlling the format of all job result in a TAP service.
 * The default values are provided by the service connection.</p>
 * 
 * <p><i>Note:
 * 	By default, the format can be modified by anyone without any limitation.
 * </i></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public class FormatController implements InputParamController {

	/** Connection to the service which knows the maximum and default value of this parameter. */
	protected final ServiceConnection service;

	/** Indicates whether the output limit of jobs can be modified. */
	protected boolean allowModification = true;

	/**
	 * Build a controller for the Format parameter.
	 * 
	 * @param service	Connection to the TAP service.
	 */
	public FormatController(final ServiceConnection service){
		this.service = service;
	}

	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	/**
	 * Lets indicating whether the format parameter can be modified.
	 * 
	 * @param allowModification	<i>true</i> if the format can be modified, <i>false</i> otherwise.
	 */
	public final void allowModification(final boolean allowModif){
		this.allowModification = allowModif;
	}

	@Override
	public Object getDefault(){
		return TAPJob.FORMAT_VOTABLE;
	}

	@Override
	public Object check(Object format) throws UWSException{
		if (format == null)
			return getDefault();

		if (format instanceof String){
			String strFormat = ((String)format).trim();
			if (strFormat.isEmpty())
				return getDefault();

			if (service.getOutputFormat(strFormat) == null)
				throw new UWSException(UWSException.BAD_REQUEST, "Unknown value for the parameter \"format\": \"" + strFormat + "\". It should be " + getAllowedFormats());
			else
				return strFormat;
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for the parameter \"format\": class \"" + format.getClass().getName() + "\"! It should be a String.");
	}

	/**
	 * Get a list of all allowed output formats (for each, the main MIME type
	 * but also the short type representation are given).
	 * 
	 * @return	List of all output formats.
	 */
	protected final String getAllowedFormats(){
		Iterator<OutputFormat> itFormats = service.getOutputFormats();
		StringBuffer allowedFormats = new StringBuffer("a String value among: ");
		int i = 0;
		OutputFormat formatter;
		while(itFormats.hasNext()){
			formatter = itFormats.next();
			allowedFormats.append((i == 0) ? "" : ", ").append(formatter.getMimeType());
			if (formatter.getShortMimeType() != null && formatter.getShortMimeType().length() > 0)
				allowedFormats.append(" (or ").append(formatter.getShortMimeType()).append(')');
			i++;
		}
		if (i > 0)
			return allowedFormats.toString();
		else
			return "a String value.";
	}

}
