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

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uws.UWSException;

/**
 * <p>
 * 	Let controlling a duration parameter. Thus it is possible to set a default but also a minimum and a maximum value.
 * 	Moreover you can indicate whether the value of the parameter can be modified by the user or not after initialization.
 * </p>
 * 
 * <p>This controller behaves like a {@link NumericParamController} EXCEPT on two points:</p>
 * <ul>
 * 	<li>Every given value is casted into a long value.
 * 		This implies that any {@link Double} or {@link Float} values will be truncated/rounded.</li>
 * 	<li>It is possible to check a {@link String} expressing the duration in a different unit.
 * 		This string must be prefixed by a unit. See {@link #parseDuration(String)} (and its reverse operation {@link #toString(Long)})
 * 		for more details.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.2 (06/2016)
 * @since 4.2
 */
public class DurationParamController extends NumericParamController {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a parameter controller for duration value with no restriction.
	 * 
	 * <p>
	 * 	A default, minimum and/or maximum value can be set after creation using {@link #setDefault(Number)},
	 * 	{@link #setMinimum(Number)} and {@link #setMaximum(Number)}. By default this parameter can always be modified,
	 * 	but it can be forbidden using {@link #allowModification(boolean)}.
	 * </p>
	 */
	public DurationParamController(){
		super();
	}

	/**
	 * <p>Create a controller for a parameter expressing a duration.
	 * The default and the maximum value are initialized with the given parameters (expressed in milliseconds).
	 * The third parameter allows also to forbid the modification of the parameter value by the user,
	 * if set to <i>false</i>.</p>
	 * 
	 * <p>
	 * 	A default and/or maximum value can be modified after creation using {@link #setDefault(Number)}
	 * 	and {@link #setMaximum(Number)}. The flag telling whether this parameter can be modified by the user
	 * 	can be changed using {@link #allowModification(boolean)}.
	 * </p>
	 * 
	 * <p><b>Important note:</b>
	 * 	Values given in this constructor MUST be expressed in milliseconds.
	 * </p>
	 * 
	 * @param defaultValue		Value (in ms) set by default to the parameter, when none is specified.
	 * @param minValue			Minimum value (in ms) that can be set. If a smaller value is provided by the user, an exception will be thrown by {@link #check(Object)}.
	 * @param maxValue			Maximum value (in ms) that can be set. If a bigger value is provided by the user, an exception will be thrown by {@link #check(Object)}.
	 * @param allowModification	<i>true</i> to allow the user to modify this value when creating a job, <i>false</i> otherwise.
	 */
	public DurationParamController(final Long defaultValue, final Long minValue, final Long maxValue, final boolean allowModification){
		super(defaultValue, minValue, maxValue, allowModification);
	}

	/**
	 * Cast the given value as a long value and call {@link NumericParamController#setDefault(Number)}.
	 * 
	 * @see uws.job.parameters.NumericParamController#setMinimum(java.lang.Number)
	 */
	@Override
	public void setDefault(final Number newDefaultValue){
		super.setDefault((newDefaultValue == null) ? null : newDefaultValue.longValue());
	}

	/**
	 * Cast the given value as a long value and call {@link NumericParamController#setMinimum(Number)}.
	 * 
	 * @see uws.job.parameters.NumericParamController#setMinimum(java.lang.Number)
	 */
	@Override
	public void setMinimum(final Number newMinValue){
		super.setMinimum((newMinValue == null) ? null : newMinValue.longValue());
	}

	/**
	 * Cast the given value as a long value and call {@link NumericParamController#setMaximum(Number)}.
	 * 
	 * @see uws.job.parameters.NumericParamController#setMinimum(java.lang.Number)
	 */
	@Override
	public void setMaximum(final Number newMaxValue){
		super.setMaximum((newMaxValue == null) ? null : newMaxValue.longValue());
	}

	@Override
	public Object check(final Object value) throws UWSException{
		// If no value, return the default one:
		if (value == null)
			return getDefault();

		// Otherwise, parse the given numeric value:
		long numVal;
		if (value instanceof Number)
			numVal = ((Number)value).longValue();
		else if (value instanceof String){
			try{
				numVal = parseDuration((String)value);
			}catch(ParseException pe){
				throw new UWSException(UWSException.BAD_REQUEST, "Wrong format for a duration parameter: \"" + value + "\"! It should be a positive long value between " + (minValue == null ? 0 : toString((Long)minValue)) + " and " + (maxValue == null ? toString(Long.MAX_VALUE) : toString((Long)maxValue)) + " (Default value: " + (defaultValue == null ? "none" : toString((Long)defaultValue)) + "). This value may be followed by a unit among: milliseconds (ms ; the default), seconds (s), minutes (min,m), hours (h), days (D), weeks (W), months (M) or years (Y).");
			}
		}else
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, "Wrong type for a duration parameter: class \"" + value.getClass().getName() + "\"! It should be a positive long value or a string containing only a positive long value eventually followed by a unit.");

		// If the value is SMALLER than the minimum, the minimum value will be returned:
		if (minValue != null && numVal < minValue.doubleValue())
			return minValue;
		// If the value is BIGGER than the maximum, the maximum value will be returned:
		else if (maxValue != null && numVal > maxValue.doubleValue())
			return maxValue;
		// Otherwise, return the parsed number:
		else
			return numVal;
	}

	/* **************** */
	/* DURATION PARSING */
	/* **************** */

	/** Multiplication factor between milliseconds and seconds.
	 * <p>A second is here defined as 1000 milliseconds. So the value is computed as follows: 1000.</p> */
	protected final static long MULT_SEC = 1000;

	/** Multiplication factor between milliseconds and minutes.
	 * <p>A minute is here defined as 60 seconds. So the value is computed as follows: {@link #MULT_SEC}*60.</p> */
	protected final static long MULT_MIN = 60000;

	/** Multiplication factor between milliseconds and hours.
	 * <p>An hour is here defined as 60 minutes. So the value is computed as follows: {@link #MULT_MINUTES}*60.</p> */
	protected final static long MULT_HOURS = 3600000;

	/** Multiplication factor between milliseconds and days.
	 * <p>A day is here defined as 24 hours. So the value is computed as follows: {@link #MULT_HOURS}*24.</p> */
	protected final static long MULT_DAYS = 86400000;

	/** Multiplication factor between milliseconds and weeks.
	 * <p>A week is here defined as 7 days. So the value is computed as follows: {@link #MULT_DAYS}*7.</p> */
	protected final static long MULT_WEEKS = 604800000;

	/** Multiplication factor between milliseconds and months.
	 * <p>A month is here defined as 30 days. So the value is computed as follows: {@link #MULT_DAYS}*30.</p> */
	protected final static long MULT_MONTHS = 2592000000l;

	/** Multiplication factor between milliseconds and years.
	 * <p>A year is here defined as 365 days. So the value is computed as follows: {@link #MULT_DAYS}*365.</p> */
	protected final static long MULT_YEARS = 31536000000l;

	/** Regular Expression of all string allowed to mean MILLISECONDS.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_MS = "milliseconds|ms";

	/** Regular Expression of all string allowed to mean SECONDS.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_SEC = "seconds|sec|s";

	/** Regular Expression of all string allowed to mean MINUTES.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_MIN = "min|minutes|m";

	/** Regular Expression of all string allowed to mean HOURS.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_HOURS = "hours|h";

	/** Regular Expression of all string allowed to mean DAYS.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_DAYS = "days|D";

	/** Regular Expression of all string allowed to mean WEEKS.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_WEEKS = "weeks|W";

	/** Regular Expression of all string allowed to mean MONTHS.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_MONTHS = "months|M";

	/** Regular Expression of all string allowed to mean YEARS.
	 * <p><b>Important:</b> opening and closing brackets are omitted here for a better integration
	 * in the duration string's regular expression (see {@link #PATTERN_DURATION} ).</p> */
	protected static String REGEXP_YEARS = "years|Y";

	/** Pattern created with the Regular Expression of a valid duration string.
	 * <p>
	 * 	Such string MUST be a positive integer/long value eventually suffixed by a unit.
	 * 	Allowed unit strings are the following:
	 * </p>
	 * <ul>
	 * 	<li>milliseconds, ms</li>
	 * 	<li>seconds, sec, s</li>
	 * 	<li>minutes, min, m</li>
	 * 	<li>hours, h</li>
	 * 	<li>days, D</li>
	 * 	<li>weeks, W</li>
	 * 	<li>months, M</li>
	 * 	<li>years, Y</li>
	 * </ul>
	 * 
	 * <p><b>Important:</b>
	 * 	Units are case <b>sensitive</b>!
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	Space characters are ignored only if leading or trailing the whole string,
	 * 	or if between the duration and its unit.
	 * </i></p> */
	protected static Pattern PATTERN_DURATION = Pattern.compile("\\s*([0-9]+)\\s*(" + REGEXP_MS + "|" + REGEXP_SEC + "|" + REGEXP_MIN + "|" + REGEXP_HOURS + "|" + REGEXP_DAYS + "|" + REGEXP_WEEKS + "|" + REGEXP_MONTHS + "|" + REGEXP_YEARS + ")?\\s*");

	/**
	 * Parse the given duration string.
	 * 
	 * <p>
	 * 	Such string MUST be a positive integer/long value eventually suffixed by a unit.
	 * 	Allowed unit strings are the following:
	 * </p>
	 * <ul>
	 * 	<li>milliseconds, ms</li>
	 * 	<li>seconds, sec, s</li>
	 * 	<li>minutes, min, m</li>
	 * 	<li>hours, h</li>
	 * 	<li>days, D</li>
	 * 	<li>weeks, W</li>
	 * 	<li>months, M</li>
	 * 	<li>years, Y</li>
	 * </ul>
	 * 
	 * <p><b>Important:</b>
	 * 	Units are case <b>sensitive</b>!
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	Space characters are ignored only if leading or trailing the whole string,
	 * 	or if between the duration and its unit.
	 * </i></p>
	 * 
	 * @param duration	The duration string.
	 * 
	 * @return	The parsed duration converted into milliseconds,
	 *        	or <code>-1</code> if the given string is <code>null</code> or negative.
	 * 
	 * @throws ParseException	If the given string is using an unknown unit string,
	 *                       	or if the string does not start digits.
	 * 
	 * @see #toString(Long)
	 */
	public long parseDuration(final String duration) throws ParseException{
		if (duration == null || duration.matches("\\s*-.*"))
			return -1;

		Matcher matcher = PATTERN_DURATION.matcher(duration);
		if (!matcher.matches())
			throw new ParseException("Unexpected format for a duration: \"" + duration + "\"! Cause: it does not match the following Regular Expression: " + PATTERN_DURATION.pattern(), 0);

		try{
			// Extract the numerical value:
			long numDuration = Long.parseLong(matcher.group(1));

			// Apply any multiplication to this duration:
			String unit = matcher.group(2);
			if (unit == null || unit.length() == 0 || unit.matches("(" + REGEXP_MS + ")"))
				return numDuration;
			else if (unit.matches("(" + REGEXP_SEC + ")"))
				return numDuration * MULT_SEC;
			else if (unit.matches("(" + REGEXP_MIN + ")"))
				return numDuration * MULT_MIN;
			else if (unit.matches("(" + REGEXP_HOURS + ")"))
				return numDuration * MULT_HOURS;
			else if (unit.matches("(" + REGEXP_DAYS + ")"))
				return numDuration * MULT_DAYS;
			else if (unit.matches("(" + REGEXP_WEEKS + ")"))
				return numDuration * MULT_WEEKS;
			else if (unit.matches("(" + REGEXP_MONTHS + ")"))
				return numDuration * MULT_MONTHS;
			else if (unit.matches("(" + REGEXP_YEARS + ")"))
				return numDuration * MULT_YEARS;
		}catch(Exception ex){
			throw new ParseException("Unexpected format for a duration: \"" + duration + "\"! Cause: " + ex.getMessage(), matcher.regionStart());
		}

		return -1;
	}

	/**
	 * Convert a duration value (expressed in milliseconds) into the best human readable unit value.
	 * 
	 * @param duration	A duration in milliseconds.
	 * 
	 * @return	An empty string if the given duration is <code>null</code>,
	 *        	or a string expressing the given duration in the best integer value with a unit suffix.
	 * 
	 * @see #parseDuration(String)
	 */
	public String toString(Long duration){
		if (duration == null)
			return "";

		if (duration == 0)
			return "0ms";
		else if (duration % MULT_YEARS == 0)
			return duration / MULT_YEARS + "Y";
		else if (duration % MULT_MONTHS == 0)
			return duration / MULT_MONTHS + "M";
		else if (duration % MULT_WEEKS == 0)
			return duration / MULT_WEEKS + "W";
		else if (duration % MULT_DAYS == 0)
			return duration / MULT_DAYS + "D";
		else if (duration % MULT_HOURS == 0)
			return duration / MULT_HOURS + "h";
		else if (duration % MULT_MIN == 0)
			return duration / MULT_MIN + "m";
		else if (duration % MULT_SEC == 0)
			return duration / MULT_SEC + "s";
		else
			return duration + "ms";
	}

}
