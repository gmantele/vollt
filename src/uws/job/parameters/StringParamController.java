package uws.job.parameters;

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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import uws.UWSException;

/**
 * Let controlling a String parameter.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (09/2014)
 */
public class StringParamController implements InputParamController {

	/** Name of the controlled parameter. */
	private final String paramName;

	/** Default value of this parameter. <i>By default: NULL</i> */
	private String defaultValue = null;

	/** List of all allowed values. If NULL, any value is allowed. <i>By default: NULL</i> */
	private String[] possibleValues = null;

	/** Tells whether the parameter can be modified after its initialization. */
	private boolean allowModification = true;

	/**
	 * Builds a controller of the specified String parameter.
	 * 
	 * @param paramName	Name of the controlled parameter.
	 * 
	 * @see #StringParamController(String, String, String[], boolean)
	 */
	public StringParamController(final String paramName){
		this(paramName, null, null, true);
	}

	/**
	 * Builds a controller of the specified String parameter and configures it.
	 * 
	 * @param paramName			Name of the controlled parameter.
	 * @param defaultValue		Default value of this parameter. <i><u>note:</u> It may be NULL. If the next parameter is not NULL or empty, the default value must be one of its values.</i>
	 * @param possibleValues	List of all allowed values. <i><u>note:</u> It may be NULL or empty. In this case, any value is allowed.</i>
	 * @param allowModif		<i>true</i> to allow the modification of the specified parameter after its initialization, <i>false</i> otherwise.
	 */
	public StringParamController(final String paramName, final String defaultValue, final String[] possibleValues, final boolean allowModif){
		this.paramName = paramName;
		setDefaultValue(defaultValue);
		setPossibleValues(possibleValues);
		allowModification(allowModif);
	}

	/**
	 * Gets the default value of the parameter.
	 * 
	 * @return The default value. (MAY BE NULL)
	 */
	public final String getDefaultValue(){
		return defaultValue;
	}

	/**
	 * Sets the default value of the parameter.
	 * 
	 * @param defaultValue The new default value. (MAY BE NULL)
	 */
	public final void setDefaultValue(String defaultValue){
		this.defaultValue = defaultValue;
	}

	/**
	 * Gets the list of all allowed values.
	 * 
	 * @return The allowed values. <i><u>note:</u> If NULL or empty, any value is allowed.</i>
	 */
	public final String[] getPossibleValues(){
		return possibleValues;
	}

	/**
	 * Sets the list of all allowed values.
	 * 
	 * @param possibleValues The new allowed values. <i><u>note:</u> If NULL or empty, any value is allowed.</i>
	 */
	public final void setPossibleValues(String[] possibleValues){
		if (possibleValues == null || possibleValues.length == 0)
			this.possibleValues = null;
		else
			this.possibleValues = possibleValues;
	}

	@Override
	public final boolean allowModification(){
		return allowModification;
	}

	/**
	 * Lets telling if the parameter can be modified after it initialization.
	 * 
	 * @param allowModif	<i>true</i> to allow its modification after initialization, <i>false</i> otherwise.
	 */
	public final void allowModification(final boolean allowModif){
		allowModification = allowModif;
	}

	@Override
	public Object check(Object value) throws UWSException{
		if (value == null)
			return null;

		if (value instanceof String){
			String strValue = (String)value;
			if (possibleValues != null && possibleValues.length > 0){
				for(String v : possibleValues){
					if (strValue.equalsIgnoreCase(v))
						return v;
				}
				throw new UWSException(UWSException.BAD_REQUEST, "Unknown value for the job parameter " + paramName + ": \"" + strValue + "\". It should be " + getExpectedFormat());
			}else
				return strValue;
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for the parameter " + paramName + ": \"" + value.getClass().getName() + "\"! It should be a String.");
	}

	/**
	 * Gets a string which lists all the allowed values.
	 * 
	 * @return	A string which describes the format expected by this controller.
	 */
	protected final String getExpectedFormat(){
		if (possibleValues == null || possibleValues.length == 0){
			StringBuffer buffer = new StringBuffer("a String value among: ");
			for(int i = 0; i < possibleValues.length; i++)
				buffer.append((i == 0) ? "" : ", ").append(possibleValues[i]);
			return buffer.toString();
		}else
			return "a String value.";
	}

	@Override
	public Object getDefault(){
		return defaultValue;
	}

}
