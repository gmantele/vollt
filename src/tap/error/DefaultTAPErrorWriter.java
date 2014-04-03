package tap.error;

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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import uws.UWSException;
import uws.job.user.JobOwner;
import uws.service.error.AbstractServiceErrorWriter;
import uws.service.error.ServiceErrorWriter;
import uws.service.log.UWSLog;

/**
 * <p>Default implementation of {@link ServiceErrorWriter} for a TAP service.</p>
 * 
 * <p>All errors are written using the function {@link #formatError(Throwable, boolean, uws.job.ErrorType, int, String, JobOwner, HttpServletResponse)}
 * of the abstract implementation of the error writer: {@link AbstractServiceErrorWriter}.</p>
 * 
 * <p>A {@link UWSException} may precise the HTTP error code to apply. That's why, {@link #writeError(Throwable, HttpServletResponse, HttpServletRequest, JobOwner, String)}
 * has been overridden: to get this error code and submit it to the {@link #formatError(Throwable, boolean, uws.job.ErrorType, int, String, JobOwner, HttpServletResponse)}
 * function. Besides, the stack trace of {@link UWSException}s is not printed (except if the message is NULL or empty).
 * And this error will be logged only if its error code is {@link UWSException#INTERNAL_SERVER_ERROR}.</p>
 * 
 * <p>2 formats are managed by this implementation: HTML (default) and JSON. That means the writer will format and
 * write a given error in the best appropriate format. This format is chosen thanks to the "Accept" header of the HTTP request.
 * If no request is provided or if there is no known format, the HTML format is chosen by default.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 * 
 * @see AbstractServiceErrorWriter
 */
public class DefaultTAPErrorWriter extends AbstractServiceErrorWriter {

	protected final ServiceConnection<?> service;

	public DefaultTAPErrorWriter(final ServiceConnection<?> service) {
		this.service = service;
	}

	@Override
	protected final UWSLog getLogger() {
		return service.getLogger();
	}

	@Override
	public void writeError(Throwable t, HttpServletResponse response, HttpServletRequest request, JobOwner user, String action) throws IOException {
		if (t instanceof UWSException){
			UWSException ue = (UWSException)t;
			formatError(ue, (ue.getMessage() == null || ue.getMessage().trim().isEmpty()), ue.getUWSErrorType(), ue.getHttpErrorCode(), action, user, response, (request != null)?request.getHeader("Accept"):null);
			if (ue.getHttpErrorCode() == UWSException.INTERNAL_SERVER_ERROR)
				getLogger().error(ue);
			getLogger().httpRequest(request, user, action, ue.getHttpErrorCode(), ue.getMessage(), ue);
		}else
			super.writeError(t, response, request, user, action);
	}

}
