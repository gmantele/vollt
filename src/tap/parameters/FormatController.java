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

import java.util.Iterator;

import tap.ServiceConnection;
import tap.TAPJob;
import tap.formatter.OutputFormat;
import uws.UWSException;
import uws.UWSExceptionFactory;
import uws.job.parameters.InputParamController;

public class FormatController implements InputParamController {

	protected final ServiceConnection service;
	protected boolean allowModification = true;

	public FormatController(final ServiceConnection service){
		this.service = service;
	}

	@Override
	public final boolean allowModification(){
		return allowModification;
	}

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
			return null;

		if (format instanceof String){
			String strFormat = ((String)format).trim();
			if (strFormat.isEmpty())
				return getDefault();

			if (service.getOutputFormat(strFormat) == null)
				throw new UWSException(UWSException.BAD_REQUEST, "Unknown output format (=" + strFormat + ") ! This TAP service can format query results ONLY in the following formats:" + getAllowedFormats() + ".");
			else
				return strFormat;
		}else
			throw UWSExceptionFactory.badFormat(null, TAPJob.PARAM_FORMAT, format.toString(), format.getClass().getName(), "A String equals to one of the following values: " + getAllowedFormats() + ".");
	}

	public final String getAllowedFormats(){
		Iterator<OutputFormat> itFormats = service.getOutputFormats();
		StringBuffer allowedFormats = new StringBuffer();
		int i = 0;
		OutputFormat formatter;
		while(itFormats.hasNext()){
			formatter = itFormats.next();
			allowedFormats.append((i == 0) ? "" : ", ").append(formatter.getMimeType());
			if (formatter.getShortMimeType() != null && formatter.getShortMimeType().length() > 0)
				allowedFormats.append(" (or ").append(formatter.getShortMimeType()).append(')');
			i++;
		}
		return allowedFormats.toString();
	}

}
