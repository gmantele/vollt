package tap.upload;

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
 * Copyright 2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.IOException;

/**
 * Thrown to indicate an upload exceeded the maximum size.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 4.4 (08/2018)
 * @since 4.4
 */
public class ExceededSizeException extends IOException {
	private static final long serialVersionUID = 1L;

	public ExceededSizeException(){
		super();
	}

	public ExceededSizeException(final String message){
		super(message);
	}
}
