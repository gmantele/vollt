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
 * Action(s) executed when the function {@link OutputStreamWithCloseAction#close() close()}
 * is called on an {@link OutputStreamWithCloseAction} instance, after the
 * wrapped {@link OutputStream} has been successfully closed.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 4.4 (07/2018)
 * @since 4.4
 */
public interface CloseAction {

	/**
	 * This function is executed after the wrapped {@link OutputStream} of
	 * an {@link OutputStreamWithCloseAction} has been successfully closed.
	 *
	 * @throws IOException	If any error prevents this {@link CloseAction} to
	 *                    	run.
	 */
	public void run() throws IOException;
}