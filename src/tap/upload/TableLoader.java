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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.security.InvalidParameterException;
import java.util.Enumeration;

import com.oreilly.servlet.MultipartRequest;

public class TableLoader {
	private static final String URL_REGEXP = "^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	private static final String PARAM_PREFIX = "param:";

	public final String tableName;
	private final URL url;
	private final String param;

	private final File file;

	public TableLoader(final String name, final String value){
		this(name, value, (MultipartRequest)null);
	}

	@SuppressWarnings("unchecked")
	public TableLoader(final String name, final String uri, final MultipartRequest multipart){
		if (name == null || name.trim().isEmpty())
			throw new NullPointerException("A table name can not be NULL !");
		tableName = name.trim();

		if (uri == null || uri.trim().isEmpty())
			throw new NullPointerException("The table URI can not be NULL !");
		String URI = uri.trim();
		if (URI.startsWith(PARAM_PREFIX)){
			if (multipart == null)
				throw new InvalidParameterException("The URI scheme \"param\" can be used ONLY IF the VOTable is provided inside the HTTP request (multipart/form-data) !");
			else if (URI.length() <= PARAM_PREFIX.length())
				throw new InvalidParameterException("Incomplete URI (" + URI + "): empty parameter name !");
			url = null;
			param = URI.substring(PARAM_PREFIX.length()).trim();

			Enumeration<String> enumeration = multipart.getFileNames();
			File foundFile = null;
			while(foundFile == null && enumeration.hasMoreElements()){
				String fileName = enumeration.nextElement();
				if (fileName.equals(param))
					foundFile = multipart.getFile(fileName);
			}

			if (foundFile == null)
				throw new InvalidParameterException("Incorrect file reference (" + URI + "): the parameter \"" + param + "\" does not exist !");
			else
				file = foundFile;
		}else if (URI.matches(URL_REGEXP)){
			try{
				url = new URL(URI);
				param = null;
				file = null;
			}catch(MalformedURLException mue){
				throw new InvalidParameterException(mue.getMessage());
			}
		}else
			throw new InvalidParameterException("Invalid table URI: \"" + URI + "\" !");
	}

	public InputStream openStream() throws IOException{
		if (url != null)
			return url.openStream();
		else
			return new FileInputStream(file);
	}

	public boolean deleteFile(){
		if (file != null && file.exists())
			return file.delete();
		else
			return false;
	}

}
