package tap.file;

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

import java.io.File;

import uws.service.file.UWSFileManager;

/**
 * Minimal API of the object which will be used by the TAP service (but more particularly by its UWS resource)
 * to create, delete, write and read files needed to the service (i.e. results, errors, logs, backups, upload files).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 * 
 * @see UWSFileManager
 */
public interface TAPFileManager extends UWSFileManager {

	/**
	 * Local directory in which all uploaded files will be kept until they are read or ignored (in this case, they will be deleted).
	 * 
	 * @return	Path of the directory in which uploaded files must be written.
	 */
	public File getUploadDirectory();

}
