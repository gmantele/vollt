package uws.service.file.io;

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
 * Copyright 2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.IOException;
import java.io.OutputStream;

/**
 * This {@link OutputStream} wraps another {@link OutputStream}. It forwards all
 * write requests to this inner {@link OutputStream}. The only difference lies
 * in its {@link #close()} function which runs a given {@link CloseAction} just
 * after having called {@link OutputStream#close() close()} on the inner
 * {@link OutputStream} successfully.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 4.4 (07/2018)
 * @since 4.4
 *
 * @see CloseAction
 */
public class OutputStreamWithCloseAction extends OutputStream {

	/** Wrapped {@link OutputStream}. */
	private final OutputStream output;

	/** Action(s) to run after the wrapped {@link OutputStream} has been
	 * successfully closed.
	 *
	 * <p><i>
	 * 	It can be <code>null</code>. In such case, the wrapped {@link OutputStream}
	 * 	will be closed and nothing else will be done.
	 * </i></p> **/
	private final CloseAction closeAction;

	/**
	 * Create an {@link OutputStreamWithCloseAction} instance.
	 *
	 * @param output	The {@link OutputStream} to wrap.
	 *              	<i>MANDATORY</i>
	 * @param action	The action(s) to run after the given {@link OutputStream}
	 *              	has been successfully closed.
	 *              	<i>OPTIONAL</i>
	 *
	 * @throws NullPointerException	If the given {@link OutputStream} is missing.
	 */
	public OutputStreamWithCloseAction(final OutputStream output, final CloseAction action) throws NullPointerException{
		if (output == null)
			throw new NullPointerException("Missing OutputStream to wrap!");
		else
			this.output = output;

		this.closeAction = action;
	}

	@Override
	public void write(final byte[] b) throws IOException{
		output.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException{
		output.write(b, off, len);
	}

	@Override
	public void write(final int b) throws IOException{
		output.write(b);
	}

	@Override
	public void flush() throws IOException{
		output.flush();
	}

	@Override
	public void close() throws IOException{
		// Close the output stream:
		output.close();

		// Once close, run the close action:
		if (closeAction != null)
			closeAction.run();
	}

}