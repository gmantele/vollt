package adql.translator;

/*
 * This file is part of ADQLLibrary.
 * 
 * ADQLLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ADQLLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * Exception thrown by an {@link ADQLTranslator}.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 */
public class TranslationException extends Exception {

	private static final long serialVersionUID = 1L;

	public TranslationException() { ; }

	public TranslationException(String msg) {
		super(msg);
	}

	public TranslationException(Throwable cause) {
		super(cause);
	}

	public TranslationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
