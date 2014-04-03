package uws.service.log;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * Different types of log messages.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 * 
 * @see UWSLog
 * @see DefaultUWSLog
 */
public enum UWSLogType{
	HTTP_ACTIVITY, DEBUG, INFO, WARNING, ERROR, CUSTOM;

	protected String customType = this.name();

	public final String getCustomType(){
		return customType;
	}

	public static final UWSLogType createCustomLogType(final String customType){
		UWSLogType type = UWSLogType.CUSTOM;
		type.customType = customType;
		return type;
	}

	@Override
	public String toString(){
		if (this == CUSTOM)
			return customType;
		else
			return name();
	}
}
