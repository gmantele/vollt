package uws.service.error;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.UWSToolBox;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.log.UWSLog;

/**
 * <p>Default implementation of {@link ServiceErrorWriter} for a UWS service.</p>
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
public class DefaultUWSErrorWriter extends AbstractServiceErrorWriter {

	protected final UWS uws;

	public DefaultUWSErrorWriter(final UWS uws){
		this.uws = uws;
	}

	@Override
	protected final UWSLog getLogger(){
		return (uws != null && uws.getLogger() != null) ? uws.getLogger() : UWSToolBox.getDefaultLogger();
	}

	@Override
	public void writeError(Throwable t, HttpServletResponse response, HttpServletRequest request, JobOwner user, String action) throws IOException{
		if (t instanceof UWSException){
			UWSException ue = (UWSException)t;
			formatError(ue, (ue.getMessage() == null || ue.getMessage().trim().isEmpty()), ue.getUWSErrorType(), ue.getHttpErrorCode(), action, user, response, request.getHeader("Accept"));
			if (ue.getHttpErrorCode() == UWSException.INTERNAL_SERVER_ERROR)
				getLogger().error(ue);
			getLogger().httpRequest(request, user, action, ue.getHttpErrorCode(), ue.getMessage(), ue);
		}else
			super.writeError(t, response, request, user, action);
	}

}
