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
 * Copyright 2016 - Astronomisches Rechen Institut (ARI)
 */

import java.io.Serializable;

import uws.UWSException;

/**
 * <p>
 * 	Let controlling a numeric parameter. Thus it is possible to set a default but also a minimum and a maximum value.
 * 	Moreover you can indicate whether the value of the parameter can be modified by the user or not after initialization.
 * </p>
 * 
 * <p>Here is the logic applied by this controller for a numeric parameter:</p>
 * <ul>
 * 	<li>If no value is specified by the UWS client, the default value is returned.
 * 		But if no default value is returned, <code>null</code> will then be returned.</li>
 * 	<li>If the given value is smaller than the minimum (if any is set), the minimum value is returned.</li>
 * 	<li>If the given value is bigger than the maximum (if any is set), the maximum value is returned.</li>
 * </ul>
 * <p>
 * 	This implementation aims to be generic enough to support most of the numeric parameters, but if the logic
 * 	presented above is not enough or does not fit your needs, you are free to extend it and thus benefit of the
 * 	implementation of most of the functions and attributes of this controller.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.2 (06/2016)
 * @since 4.2
 */
public class NumericParamController implements InputParamController, Serializable {
	private static final long serialVersionUID = 1L;

	/** The maximum value. */
	protected Number minValue = null;

	/** The default value. <i>MUST be between {@link #minValue} and  {@link #maxValue}</i> */
	protected Number defaultValue = null;

	/** The maximum value. */
	protected Number maxValue = null;

	/** Indicates whether the parameter can be modified. */
	protected boolean allowModification = true;

	/**
	 * Create a numeric controller with no restriction.
	 * 
	 * <p>
	 * 	A default, minimum and/or maximum value can be set after creation using {@link #setDefault(Number)},
	 * 	{@link #setMinimum(Number)} and {@link #setMaximum(Number)}. By default this parameter can always be modified,
	 * 	but it can be forbidden using {@link #allowModification(boolean)}.
	 * </p>
	 */
	public NumericParamController(){}

	/**
	 * <p>Create a controller for a numeric parameter.
	 * The default and the maximum value are initialized with the given parameters.
	 * The third parameter allows also to forbid the modification of the parameter value by the user,
	 * if set to <i>false</i>.</p>
	 * 
	 * <p>
	 * 	A default and/or maximum value can be modified after creation using {@link #setDefault(Number)}
	 * 	and {@link #setMaximum(Number)}. The flag telling whether this parameter can be modified by the user
	 * 	can be changed using {@link #allowModification(boolean)}.
	 * </p>
	 * 
	 * @param defaultValue		Value set by default to the parameter, when none is specified.
	 * @param minValue			Minimum value that can be set. If a smaller value is provided by the user, an exception will be thrown by {@link #check(Object)}.
	 * @param maxValue			Maximum value that can be set. If a bigger value is provided by the user, an exception will be thrown by {@link #check(Object)}.
	 * @param allowModification	<i>true</i> to allow the user to modify this value when creating a job, <i>false</i> otherwise.
	 */
	public NumericParamController(final Number defaultValue, final Number minValue, final Number maxValue, final boolean allowModification){
		reset(defaultValue, minValue, maxValue);
		allowModification(allowModification);
	}

	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */

	/**
	 * Gets the minimum value of this parameter.
	 * 
	 * @return	The minimum value
	 *        	or <code>null</code> if none has been specified.
	 */
	public final Number getMinimum(){
		return minValue;
	}

	/**
	 * Sets the minimum value of this parameter.
	 * 
	 * <p><b>Warning !:</b>
	 * 	If the <em>given</em> value is <em>bigger</em> than the {@link #getMaximum() maximum} value,
	 * 	the minimum will be set automatically to the {@link #getMaximum() maximum} value ;
	 * 	the given one will then be ignored.
	 * </p>
	 * 
	 * <p><b>Warning 2:</b>
	 * 	If the <em>default</em> value is <em>smaller</em> than the new {@link #getMinimum() minimum} value,
	 * 	the default will be set automatically to the new {@link #getMinimum() minimum} value.
	 * </p>
	 * 
	 * @param newMinValue The new minimum value. <i><code>null</code> is allowed ; it will remove the constraint on the minimum</i>
	 */
	public void setMinimum(final Number newMinValue){
		// If NULL, set it and return:
		if (newMinValue == null)
			minValue = null;
		// Otherwise:
		else{
			// Set the minimum to the maximum value if the given value is BIGGER than the current maximum:
			if (maxValue != null && newMinValue.doubleValue() > maxValue.doubleValue())
				minValue = maxValue;
			// Otherwise, set it exactly as provided:
			else
				minValue = newMinValue;

			/* Ensure the default value is still bigger than the minimum.
			 * If not, set the default to the minimum: */
			if (defaultValue != null && defaultValue.doubleValue() < minValue.doubleValue())
				defaultValue = minValue;
		}
	}

	/**
	 * Gets the maximum value of this parameter.
	 * 
	 * @return	The maximum value
	 *        	or <code>null</code> if none has been specified.
	 */
	public final Number getMaximum(){
		return maxValue;
	}

	/**
	 * Sets the maximum value of this parameter.
	 * 
	 * <p><b>Warning 1:</b>
	 * 	If the <em>given</em> value is <em>smaller</em> than the {@link #getMinimum() minimum} value,
	 * 	the maximum will be set automatically to the {@link #getMinimum() minimum} value ;
	 * 	the given one will then be ignored.
	 * </p>
	 * 
	 * <p><b>Warning 2:</b>
	 * 	If the <em>default</em> value is <em>bigger</em> than the new {@link #getMaximum() maximum} value,
	 * 	the default will be set automatically to the new {@link #getMaximum() maximum} value.
	 * </p>
	 * 
	 * @param newMaxValue The new maximum value. <i><code>null</code> is allowed ; it will remove the constraint on the maximum</i>
	 */
	public void setMaximum(final Number newMaxValue){
		// If NULL, set it and return:
		if (newMaxValue == null)
			maxValue = null;
		// Otherwise:
		else{
			// Set the maximum to the minimum value if the given value is SMALLER than the current minimum:
			if (minValue != null && newMaxValue.doubleValue() < minValue.doubleValue())
				maxValue = minValue;
			// Otherwise, set it exactly as provided:
			else
				maxValue = newMaxValue;

			/* Ensure the default value is still smaller than the maximum.
			 * If not, set the default to the maximum: */
			if (defaultValue != null && defaultValue.doubleValue() > maxValue.doubleValue())
				defaultValue = maxValue;
		}
	}

	@Override
	public Object getDefault(){
		return defaultValue;
	}

	/**
	 * Set the default value that the parameter must have if none is specified by the user.
	 * 
	 * <p><b>Warning:</b>
	 * 	If the given value is not between the {@link #getMinimum() minimum} and the {@link #getMaximum() maximum}
	 * 	of this controller, the default value will be automatically set to the {@link #getMinimum() minimum} value.
	 * </p>
	 * 
	 * @param newDefaultValue	The new default value for this controller.
	 *                       	<i><code>null</code> allowed ; <code>null</code> will then be returned if a given parameter is not set by the user</i>
	 */
	public void setDefault(final Number newDefaultValue){
		// If NULL, set it and return:
		if (newDefaultValue == null)
			defaultValue = null;
		// Otherwise:
		else{
			// If SMALLER than the minimum of this controller, set the default to the minimum:
			if (minValue != null && newDefaultValue.doubleValue() < minValue.doubleValue())
				defaultValue = minValue;
			// If BIGGER than the maximum of this controller, set the default to the maximum:
			else if (maxValue != null && newDefaultValue.doubleValue() > maxValue.doubleValue())
				defaultValue = maxValue;
			// Otherwise, set it exactly as provided:
			else
				defaultValue = newDefaultValue;
		}
	}

	/**
	 * Reset all fields in the same time.
	 * 
	 * @param defaultVal	Value set by default to the parameter, when none is specified.
	 * @param minVal		Minimum value that can be set. If a smaller value is provided by the user, an exception will be thrown by {@link #check(Object)}.
	 * @param maxVal		Maximum value that can be set. If a bigger value is provided by the user, an exception will be thrown by {@link #check(Object)}.
	 */
	public void reset(final Number defaultVal, final Number minVal, final Number maxVal){
		// Reset manually all concerned fields to NULL in order to avoid any conflict between the new and the old values:
		minValue = null;
		defaultValue = null;
		maxValue = null;

		// Set the given values with the appropriate set functions:
		setMinimum(minVal);
		setMaximum(maxVal);
		setDefault(defaultVal);
	}

	@Override
	public boolean allowModification(){
		return allowModification;
	}

	/**
	 * Lets indicating whether the value of the parameter can be modified after initialization.
	 * 
	 * @param allowModification <i>true</i> if the parameter value can be modified,
	 *                         	<i>false</i> otherwise.
	 */
	public void allowModification(final boolean allowModification){
		this.allowModification = allowModification;
	}

	/* ***************** */
	/* CHECKING FUNCTION */
	/* ***************** */

	@Override
	public Object check(final Object value) throws UWSException{
		// If no value, return the default one:
		if (value == null)
			return getDefault();

		// Otherwise, parse the given numeric value:
		Number numVal = null;
		if (value instanceof Number)
			numVal = (Number)value;
		else if (value instanceof String){
			String strValue = (String)value;
			try{
				numVal = Double.parseDouble(strValue);
			}catch(NumberFormatException nfe1){
				try{
					numVal = Long.parseLong(strValue);
				}catch(NumberFormatException nfe2){
					throw new UWSException(UWSException.BAD_REQUEST, "Wrong format for a numeric parameter: \"" + strValue + "\"! It should be a double or a long value between " + (minValue == null ? Double.MIN_VALUE : minValue) + " and " + (maxValue == null ? Double.MAX_VALUE : maxValue) + " (Default value: " + (defaultValue == null ? "none" : defaultValue) + ").");
				}
			}
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for a numeric parameter: class \"" + value.getClass().getName() + "\"! It should be a double, a long value or a string containing only a double or a long value.");

		// If the value is SMALLER than the minimum, the minimum value will be returned:
		if (minValue != null && numVal.doubleValue() < minValue.doubleValue())
			return minValue;
		// If the value is BIGGER than the maximum, the maximum value will be returned:
		else if (maxValue != null && numVal.doubleValue() > maxValue.doubleValue())
			return maxValue;
		// Otherwise, return the parsed number:
		else
			return numVal;
	}

}
