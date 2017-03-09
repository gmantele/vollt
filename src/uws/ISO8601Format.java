package uws;

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
 * Copyright 2014-2017 - Astronomisches Rechen Institut (ARI)
 */

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Let formatting and parsing date expressed in ISO8601 format.</p>
 * 
 * <h3>Date formatting</h3>
 * 
 * <p>
 * 	Dates are formatted using the following format: "yyyy-MM-dd'T'hh:mm:ss'Z'" if in UTC or "yyyy-MM-dd'T'hh:mm:ss[+|-]hh:mm" otherwise.
 * 	On the contrary to the time zone, by default the number of milliseconds is not displayed. However, when displayed, the format is:
 * 	"yyyy-MM-dd'T'hh:mm:ss.sss'Z'" if in UTC or "yyyy-MM-dd'T'hh:mm:ss.sss[+|-]hh:mm" otherwise.
 * </b>
 * 
 * <p>
 * 	As said previously, it is possible to display or to hide the time zone and the milliseconds. This can be easily done by changing
 * 	the value of the static attributes {@link #displayTimeZone} and {@link #displayMilliseconds}. By default {@link #displayTimeZone} is <i>true</i>
 * 	and {@link #displayMilliseconds} is <i>false</i>.
 * </i>
 * 
 * <p>
 * 	By default the date will be formatted in the local time zone. But this could be specified either in the format function {@link #format(long, String, boolean, boolean)}
 * 	or by changing the static attribute {@link #targetTimeZone}. The time zone must be specified with its ID. The list of all available time zone IDs is given by
 * 	{@link TimeZone#getAvailableIDs()}.
 * </p>
 * 
 * <h3>Date parsing</h3>
 * 
 * <p>
 * 	This class is able to parse dates - with the function {@link #parse(String)} - formatted in ISO-8601.
 * 	This parser allows the following general syntaxes:
 * </p>
 * <ul>
 * 	<li>YYYY (e.g. 2015)</li>
 * 	<li>YYYY-MM (e.g. 2015-12)</li>
 * 	<li>YYYY-MM-DD (e.g. 2015-12-11)</li>
 * 	<li>YYYY-MM-DD'T'hh:mmTZD (e.g. 2015-12-11T20:28+01:00 or 2015-12-11T19:28Z)</li>
 * 	<li>YYYY-MM-DD'T'hh:mm:ssTZD (e.g. 2015-12-11T20:28:30+01:00 or 2015-12-11T19:28:30Z)</li>
 * 	<li>YYYY-MM-DD'T'hh:mm:ss.sTZD (e.g. 2015-12-11T20:28:30.45+01:00 or 2015-12-11T19:28:30.45Z)</li>
 * </ul>
 * <p>Where:</p>
 * <ul>
 * 	<li>YYYY = four-digit year</li>
 * 	<li>MM   = two-digit month (01=January, etc.)</li>
 * 	<li>DD   = two-digit day of month (01 through 31)</li>
 * 	<li>hh   = two digits of hour (00 through 23) (am/pm NOT allowed)</li>
 * 	<li>mm   = two digits of minute (00 through 59)</li>
 * 	<li>ss   = two digits of second (00 through 59)</li>
 * 	<li>s    = one or more digits representing a decimal fraction of a second (i.e. milliseconds)</li>
 * 	<li>TZD  = time zone designator (Z or +hh:mm or -hh:mm)</li>
 * </ul>
 * 
 * <p>
 * 	It is also possible to express the date in weeks with the following syntax: YYYY-'W'ww-D
 * 	(e.g. 2015-W50, 2015-W50-5, 2015-W50-5T20:28:30.45+01:00). <code>ww</code> must a 2 digits number between
 * 	1 and the number of weeks available in the chosen year. <code>D</code> corresponds to the day
 * 	of the week: Monday = 1, Tuesday = 2, ..., Sunday = 7.
 * </p>
 * 
 * <p>
 * 	A last representation of the date is possible: in days of year: YYYY-DDD
 * 	(e.g. 2015-345, 2015-345T20:28:30.45+01:00). <code>DDD</code> must be a value between 1 and the number of
 * 	days there is in the chosen year.
 * </p>
 * 
 * <p>
 * 	Separators (like '-', ':' and '.') are optional. The date and time separator ('T') may be replaced by a space.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.2 (03/2017)
 * @since 4.1
 */
public class ISO8601Format {

	/** Indicate whether any date formatted with this class displays the time zone. */
	public static boolean displayTimeZone = true;
	/** Indicate whether any date formatted with this class displays the milliseconds. */
	public static boolean displayMilliseconds = false;
	/** Indicate the time zone in which the date and time should be formatted (whatever is the time zone of the given date).
	 * Note: for the local time zone, this attribute could be set to <code>TimeZone.getDefault().getID()</code>. */
	public static String targetTimeZone = "UTC"; // for the local time zone: TimeZone.getDefault().getID();

	/** Object to use to format numbers with one digit (ie. 1, 2, 0).
	 * @since 4.2 */
	protected final static DecimalFormat oneDigitFmt = new DecimalFormat("0");
	/** Object to use to format numbers with two digits (ie. 12, 02, 00). */
	protected final static DecimalFormat twoDigitsFmt = new DecimalFormat("00");
	/** Object to use to format numbers with three digits (ie. 001, 000, 123). */
	protected final static DecimalFormat threeDigitsFmt = new DecimalFormat("000");

	/**
	 * <p>Format the given date-time in ISO8601 format.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is equivalent to {@link #format(long, String, boolean, boolean)} with the following parameters:
	 * 	d, ISO8601Format.targetTimeZone, ISO8601Format.displayTimeZone, ISO8601Format.displayMilliseconds.
	 * </i></p>
	 * 
	 * @param date	Date-time.
	 * 
	 * @return	Date formatted in ISO8601.
	 */
	public static String format(final Date date){
		return format(date.getTime(), targetTimeZone, displayTimeZone, displayMilliseconds);
	}

	/**
	 * <p>Format the given date-time in ISO8601 format.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is equivalent to {@link #format(long, String, boolean, boolean)} with the following parameters:
	 * 	d, ISO8601Format.targetTimeZone, ISO8601Format.displayTimeZone, ISO8601Format.displayMilliseconds.
	 * </i></p>
	 * 
	 * @param date	Date-time in milliseconds (from the 1st January 1970 ; this value is returned by java.util.Date#getTime()).
	 * 
	 * @return	Date formatted in ISO8601.
	 */
	public static String format(final long date){
		return format(date, targetTimeZone, displayTimeZone, displayMilliseconds);
	}

	/**
	 * <p>Convert the given date-time in the given time zone and format it in ISO8601 format.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is equivalent to {@link #format(long, String, boolean, boolean)} with the following parameters:
	 * 	d, ISO8601Format.targetTimeZone, withTimeZone, ISO8601Format.displayMilliseconds.
	 * </i></p>
	 * 
	 * @param date				Date-time in milliseconds (from the 1st January 1970 ; this value is returned by java.util.Date#getTime()).
	 * @param withTimeZone	Target time zone.
	 * 
	 * @return	Date formatted in ISO8601.
	 */
	public static String format(final long date, final boolean withTimeZone){
		return format(date, targetTimeZone, withTimeZone, displayMilliseconds);
	}

	/**
	 * <p>Convert the given date-time in UTC and format it in ISO8601 format.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is equivalent to {@link #format(long, String, boolean, boolean)} with the following parameters:
	 * 	d, "UTC", ISO8601Format.displayTimeZone, ISO8601Format.displayMilliseconds.
	 * </i></p>
	 * 
	 * @param date		Date-time in milliseconds (from the 1st January 1970 ; this value is returned by java.util.Date#getTime()).
	 * 
	 * @return	Date formatted in ISO8601.
	 */
	public static String formatInUTC(final long date){
		return format(date, "UTC", displayTimeZone, displayMilliseconds);
	}

	/**
	 * <p>Convert the given date-time in UTC and format it in ISO8601 format.</p>
	 * 
	 * <p><i>Note:
	 * 	This function is equivalent to {@link #format(long, String, boolean, boolean)} with the following parameters:
	 * 	d, "UTC", withTimeZone, ISO8601Format.displayMilliseconds.
	 * </i></p>
	 * 
	 * @param date			Date-time in milliseconds (from the 1st January 1970 ; this value is returned by java.util.Date#getTime()).
	 * @param withTimeZone	Target time zone.
	 * 
	 * @return	Date formatted in ISO8601.
	 */
	public static String formatInUTC(final long date, final boolean withTimeZone){
		return format(date, "UTC", withTimeZone, displayMilliseconds);
	}

	/**
	 * Convert the given date in the given time zone and format it in ISO8601 format, with or without displaying the time zone
	 * and/or the milliseconds field.
	 * 
	 * <p><i><b>Important Note:</b>
	 * 	This function is synchronized because it is using (directly or in other static functions) static {@link DecimalFormat} instances.
	 * 	A {@link DecimalFormat} is a Java class which can be used only by one thread at a time. So {@link #format(long, String, boolean, boolean)}
	 * 	and {@link #parse(String)} (main public functions of {@link ISO8601Format}) must be synchronized in order to avoid concurrent access
	 * 	to the {@link DecimalFormat} instances and so to avoid unpredictable errors/results.
	 * </i></p>
	 * 
	 * @param date				Date-time in milliseconds (from the 1st January 1970 ; this value is returned by java.util.Date#getTime()).
	 * @param targetTimeZone	Target time zone.
	 * @param withTimeZone		<i>true</i> to display the time zone, <i>false</i> otherwise.
	 * @param withMillisec		<i>true</i> to display the milliseconds, <i>false</i> otherwise.
	 * 
	 * @return	Date formatted in ISO8601.
	 */
	protected static synchronized String format(final long date, final String targetTimeZone, final boolean withTimeZone, final boolean withMillisec){
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis(date);

		// Convert the given date in the target Time Zone:
		if (targetTimeZone != null && targetTimeZone.length() > 0)
			cal.setTimeZone(TimeZone.getTimeZone(targetTimeZone));
		else
			cal.setTimeZone(TimeZone.getTimeZone(ISO8601Format.targetTimeZone));

		StringBuffer buf = new StringBuffer();

		// Date with format yyyy-MM-dd :
		buf.append(cal.get(Calendar.YEAR)).append('-');
		buf.append(twoDigitsFmt.format(cal.get(Calendar.MONTH) + 1)).append('-');
		buf.append(twoDigitsFmt.format(cal.get(Calendar.DAY_OF_MONTH)));

		// Time with format 'T'HH:mm:ss :
		buf.append('T').append(twoDigitsFmt.format(cal.get(Calendar.HOUR_OF_DAY))).append(':');
		buf.append(twoDigitsFmt.format(cal.get(Calendar.MINUTE))).append(':');
		buf.append(twoDigitsFmt.format(cal.get(Calendar.SECOND)));
		if (withMillisec){
			buf.append('.').append(threeDigitsFmt.format(cal.get(Calendar.MILLISECOND)));
		}

		// Time zone with format (+|-)HH:mm :
		if (withTimeZone){
			int tzOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000); // offset in minutes
			boolean negative = (tzOffset < 0);
			if (negative)
				tzOffset *= -1;
			int hours = tzOffset / 60, minutes = tzOffset - (hours * 60);
			if (hours == 0 && minutes == 0)
				buf.append('Z');
			else{
				buf.append(negative ? '-' : '+');
				buf.append(twoDigitsFmt.format(hours)).append(':');
				buf.append(twoDigitsFmt.format(minutes));
			}
		}

		return buf.toString();
	}

	/**
	 * <p>Parse the given date expressed using the ISO8601 format ("yyyy-MM-dd'T'hh:mm:ss.sssZ"
	 * or "yyyy-MM-dd'T'hh:mm:ss.sssZ[+|-]hh:mm:ss").</p>
	 * 
	 * <p>
	 * 	The syntax of the given date may be more or less strict. Particularly, separators like '-' and ':' are optional.
	 * 	Besides the date and time separator ('T') may be replaced by a space.
	 * </p>
	 * 
	 * <p>
	 * 	The minimum allowed string is the date: "yyyy-MM-dd". All other date-time fields are optional,
	 * 	BUT, the time zone can be given without the time.
	 * </p>
	 * 
	 * <p>
	 * 	If no time zone is specified (by a 'Z' or a time offset), the time zone in which the date is expressed
	 * 	is supposed to be the local one.
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	This function is equivalent to {@link #parse(String)}, but whose the returned value is used to create a Date object, like this:
	 * 	return new Date(parse(strDate)).
	 * </i></p>
	 * 
	 * @param strDate	Date expressed as a string in ISO8601 format.
	 * 
	 * @return	Parsed date (expressed in milliseconds from the 1st January 1970 ;
	 *        	a date can be easily built with this number using {@link java.util.Date#Date(long)}).
	 * 
	 * @throws ParseException	If the given date is not expressed in ISO8601 format or is not merely parseable with this implementation.
	 */
	public final static Date parseToDate(final String strDate) throws ParseException{
		return new Date(parse(strDate));
	}

	public final static void main(final String[] args) throws Throwable{
		System.out.println("Date in millis: " + ISO8601Format.parse("2015-12-11"));
	}

	/**
	 * <p>Regular expression of the Time part of the ISO-8601 representation.</p>
	 * 
	 * <p>Indexes of the identified groups in this regular expression.</p><pre>
	 * ( 0: everything)
	 * ( 1: T or space)
	 *   2: hours (hh)
	 * ( 3: minutes + seconds + milliseconds)
	 *   4: minutes (mm)
	 * ( 5: seconds + milliseconds)
	 *   6: seconds (ss)
	 * ( 7: '.' + milliseconds)
	 *   8: milliseconds (s...)
	 * ( 9: full time zone: 'Z' or hours:minutes)
	 *  10: sign (+ or -)
	 *  11: hours offset (hh)
	 * (12: ':' + minutes offset)
	 *  13: minutes offset (mm)</pre>
	 * 
	 * @since 4.2
	 */
	private final static String ISO8601_TIME_REGEX = "((T| )(\\d{2})(:?(\\d{2})(:?(\\d{2})(\\.?(\\d{1,}))?)?)?(Z|(\\+|-)(\\d{2})(:?(\\d{2}))?)?)?";

	/**
	 * <p>{@link Pattern} object implementing the ISO-8601 representation.</p>
	 * 
	 * <p>The regular expression used in this {@link Pattern} identifies the following groups:</p><pre>
	 * (    0: everything)
	 *      1: year (yyyy)
	 * (    2: '-' and the rest of the date (may include the time))
	 * (    3: the rest of the date (may include the time))
	 *      4: month (MM)
	 * (    5: '-' and the day of the month)
	 *      6: day of the month (dd)
	 *   7-20: TIME
	 *     21: day of the year (ddd)
	 *  22-35: TIME
	 *     36: week of the year (ww)
	 * (   37: '-' and the day of the week)
	 *     38: the day of the week (d)
	 *  39-52: TIME</pre>
	 * 
	 * <p>All groups named <code>TIME</code> refer to {@link #ISO8601_TIME_REGEX}.</p>
	 * 
	 * <p>Groups in parenthesis should be ignored ; but an exception must be done for the 9th of {@link #ISO8601_TIME_REGEX} which may contain 'Z' meaning a UTC time zone.</p>
	 * 
	 * <p>Separator characters ('-', '.' and ':') are optional. The separator 'T' may be replaced by a ' '.</p>
	 * 
	 * @since 4.2
	 */
	private final static Pattern ISO8601_PATTERN = Pattern.compile("(\\d{4})(-?((\\d{2})(-?(\\d{2})" + ISO8601_TIME_REGEX + ")?|(\\d{3})" + ISO8601_TIME_REGEX + "|W(\\d{2})(-?(\\d)" + ISO8601_TIME_REGEX + ")?))?");

	/**
	 * <p>Parse the given date expressed using the ISO8601 format ("yyyy-MM-dd'T'hh:mm:ss.sssZ"
	 * or "yyyy-MM-dd'T'hh:mm:ss.sssZ[+|-]hh:mm:ss").</p>
	 * 
	 * <p>
	 * 	This parser allows the following general syntaxes:
	 * </p>
	 * <ul>
	 * 	<li>YYYY (e.g. 2015)</li>
	 * 	<li>YYYY-MM (e.g. 2015-12)</li>
	 * 	<li>YYYY-MM-DD (e.g. 2015-12-11)</li>
	 * 	<li>YYYY-MM-DD'T'hh:mmTZD (e.g. 2015-12-11T20:28+01:00 or 2015-12-11T19:28Z)</li>
	 * 	<li>YYYY-MM-DD'T'hh:mm:ssTZD (e.g. 2015-12-11T20:28:30+01:00 or 2015-12-11T19:28:30Z)</li>
	 * 	<li>YYYY-MM-DD'T'hh:mm:ss.sTZD (e.g. 2015-12-11T20:28:30.45+01:00 or 2015-12-11T19:28:30.45Z)</li>
	 * </ul>
	 * <p>Where:</p>
	 * <ul>
	 * 	<li>YYYY = four-digit year</li>
	 * 	<li>MM   = two-digit month (01=January, etc.)</li>
	 * 	<li>DD   = two-digit day of month (01 through 31)</li>
	 * 	<li>hh   = two digits of hour (00 through 23) (am/pm NOT allowed)</li>
	 * 	<li>mm   = two digits of minute (00 through 59)</li>
	 * 	<li>ss   = two digits of second (00 through 59)</li>
	 * 	<li>s    = one or more digits representing a decimal fraction of a second (i.e. milliseconds)</li>
	 * 	<li>TZD  = time zone designator (Z or +hh:mm or -hh:mm)</li>
	 * </ul>
	 * 
	 * <p>
	 * 	It is also possible to express the date in weeks with the following syntax: YYYY-'W'ww-D
	 * 	(e.g. 2015-W50, 2015-W50-5, 2015-W50-5T20:28:30.45+01:00). <code>ww</code> must a 2 digits number between
	 * 	1 and the number of weeks available in the chosen year. <code>D</code> corresponds to the day
	 * 	of the week: Monday = 1, Tuesday = 2, ..., Sunday = 7.
	 * </p>
	 * 
	 * <p>
	 * 	A last representation of the date is possible: in days of year: YYYY-DDD
	 * 	(e.g. 2015-345, 2015-345T20:28:30.45+01:00). <code>DDD</code> must be a value between 1 and the number of
	 * 	days there is in the chosen year.
	 * </p>
	 * 
	 * <p>
	 * 	If no time zone is specified (by a 'Z' or a time offset), the time zone in which the date is expressed
	 * 	is supposed to be the local one.
	 * </p>
	 * 
	 * <p>
	 * 	Separators (like '-', ':' and '.') are optional. The date and time separator ('T') may be replaced by a space.
	 * </p>
	 * 
	 * <p><i><b>Important Note:</b>
	 * 	This function is synchronized because it is using (directly or in other static functions) static {@link DecimalFormat} instances.
	 * 	A {@link DecimalFormat} is a Java class which can be used only by one thread at a time. So {@link #format(long, String, boolean, boolean)}
	 * 	and {@link #parse(String)} (main public functions of {@link ISO8601Format}) must be synchronized in order to avoid concurrent access
	 * 	to the {@link DecimalFormat} instances and so to avoid unpredictable errors/results.
	 * </i></p>
	 * 
	 * @param strDate	Date expressed as a string in ISO8601 format.
	 * 
	 * @return	Parsed date (expressed in milliseconds from the 1st January 1970 ;
	 *        	a date can be easily built with this number using {@link java.util.Date#Date(long)}).
	 * 
	 * @throws ParseException	If the given date is not expressed in ISO8601 format or is not merely parseable with this implementation.
	 */
	public static synchronized long parse(final String strDate) throws ParseException{
		Matcher m = ISO8601_PATTERN.matcher(strDate);
		if (m.matches()){
			GregorianCalendar cal = new GregorianCalendar();
			int timeGroupInd = -1;

			// SET THE TIME ZONE:
			/*
			 * Note: In this library, we suppose that any date provided without specified time zone, is in UTC.
			 * 
			 * It is more a TAP specification than a UWS one ; see the REC-TAP 1.0 at section 2.3.4 (page 15):
			 *  "Within the ADQL query, the service must support the use of timestamp values in
			 *  ISO8601 format, specifically yyyy-MM-dd['T'HH:mm:ss[.SSS]], where square
			 *  brackets denote optional parts and the 'T' denotes a single character separator
			 *  (T) between the date and time parts."
			 * 
			 * ...and 2.5 (page 20):
			 *  "TIMESTAMP values are specified using ISO8601 format without a timezone (as in 2.3.4 ) and are assumed to be in UTC."
			 */
			cal.setTimeZone(TimeZone.getTimeZone("UTC"));

			// SET THE DATE:
			cal.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
			// ...month based:
			if (m.group(4) != null){
				cal.set(Calendar.MONTH, getMonth(m.group(4)));
				if (m.group(5) != null)
					cal.set(Calendar.DAY_OF_MONTH, getDayOfMonth(m.group(6), cal));
				else
					cal.set(Calendar.DAY_OF_MONTH, 1);
				timeGroupInd = 7;
			}
			// ...day based:
			else if (m.group(21) != null){
				cal.set(Calendar.DAY_OF_YEAR, getDayOfYear(m.group(21), cal));
				timeGroupInd = 22;
			}
			// ...week based:
			else if (m.group(36) != null){
				cal.set(Calendar.WEEK_OF_YEAR, getWeekOfYear(m.group(36), cal));
				if (m.group(37) != null)
					cal.set(Calendar.DAY_OF_WEEK, getDayOfWeek(m.group(38)));
				// set the index of the time group:
				timeGroupInd = 39;
			}
			// ...no month & day specified:
			else{
				cal.set(Calendar.MONTH, 0);
				cal.set(Calendar.DAY_OF_MONTH, 1);
			}

			// SET THE TIME:
			if (timeGroupInd > 0 && m.group(timeGroupInd) != null){
				cal.set(Calendar.HOUR_OF_DAY, getHours(m.group(timeGroupInd + 2)));
				if (m.group(timeGroupInd + 3) != null){
					cal.set(Calendar.MINUTE, getMinutes(m.group(timeGroupInd + 4)));
					if (m.group(timeGroupInd + 5) != null){
						cal.set(Calendar.SECOND, getSeconds(m.group(timeGroupInd + 6)));
						if (m.group(timeGroupInd + 7) != null)
							cal.set(Calendar.MILLISECOND, twoDigitsFmt.parse(m.group(timeGroupInd + 8)).intValue());
						else
							cal.set(Calendar.MILLISECOND, 0);
					}else{
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
					}
				}else{
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MILLISECOND, 0);
				}
			}else{
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}

			// COMPUTE AND APPLY THE OFFSET (if any is specified):
			if (timeGroupInd > 0 && m.group(timeGroupInd + 9) != null && !m.group(timeGroupInd + 9).equals("Z")){
				int sign = (m.group(timeGroupInd + 10).equals("-") ? 1 : -1);
				cal.add(Calendar.HOUR_OF_DAY, sign * getHours(m.group(timeGroupInd + 11)));
				if (m.group(timeGroupInd + 12) != null)
					cal.add(Calendar.MINUTE, sign * getMinutes(m.group(timeGroupInd + 13)));
			}

			return cal.getTimeInMillis();
		}else
			throw new ParseException("Invalid date format: \"" + strDate + "\"! An ISO8601 date was expected.", 0);
	}

	/**
	 * <p>Convert the given ISO-8601 day of year value into a Java day of year value.</p>
	 * 
	 * <i>Note: Same representation in ISO-8601 and Java.</i>
	 * 
	 * @param str	Textual representation of the day of year in ISO-8601.
	 * @param cal	The calendar in which the year has already been set.
	 *           	<i>Note: This parameter is used to know the maximum number of days there are for the set year.
	 *           	(see {@link GregorianCalendar#getActualMaximum(int)})</i>
	 * 
	 * @return	The corresponding Java day of year.
	 * 
	 * @throws ParseException	If the given day of year is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getDayOfYear(final String str, final GregorianCalendar cal) throws ParseException{
		/* A day of year can only be between 1 and 365 (or 366 in leap year). */
		int dayOfYear = threeDigitsFmt.parse(str).intValue();
		if (dayOfYear < 1 || dayOfYear > cal.getActualMaximum(Calendar.DAY_OF_YEAR))
			throw new ParseException("Incorrect day of year: " + dayOfYear + "! An integer between 1 and " + cal.getActualMaximum(Calendar.DAY_OF_YEAR) + " was expected.", -1);
		return dayOfYear;
	}

	/**
	 * <p>Convert the given ISO-8601 day of month value into a Java day of month value.</p>
	 * 
	 * <i>Note: Same representation in ISO-8601 and Java.</i>
	 * 
	 * @param str	Textual representation of the day of month in ISO-8601.
	 * @param cal	The calendar in which the year and the month has already been set.
	 *           	<i>Note: This parameter is used to know the maximum number of days there are for the set month and year.
	 *           	(see {@link GregorianCalendar#getActualMaximum(int)})</i>
	 * 
	 * @return	The corresponding Java day of month.
	 * 
	 * @throws ParseException	If the given day of month is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getDayOfMonth(final String str, final GregorianCalendar cal) throws ParseException{
		int dayOfMonth = twoDigitsFmt.parse(str).intValue();
		if (dayOfMonth < 1 || dayOfMonth > cal.getActualMaximum(Calendar.DAY_OF_MONTH))
			throw new ParseException("Incorrect day of month: " + dayOfMonth + "! An integer between 1 and " + cal.getActualMaximum(Calendar.DAY_OF_MONTH) + " was expected.", -1);
		return dayOfMonth;
	}

	/**
	 * <p>Convert the given ISO-8601 day of week value into a Java day of week value.</p>
	 * 
	 * <ul>
	 * 	<li><u>In ISO-8601</u>: Monday = 1, Tuesday = 2, ..., Saturday = 6, Sunday = 7.</li>
	 * 	<li><u>In Java</u>    : Monday = 2, Tuesday = 3, ..., Saturday = 7, Sunday = 1.</li>
	 * </ul>
	 * 
	 * @param str	Textual representation of the day of week in ISO-8601.
	 * 
	 * @return	The corresponding Java day of week.
	 * 
	 * @throws ParseException	If the given day of week is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getDayOfWeek(final String str) throws ParseException{
		int dayOfWeek = oneDigitFmt.parse(str).intValue();
		if (dayOfWeek < 1 || dayOfWeek > 7)
			throw new ParseException("Incorrect day of week: " + dayOfWeek + "! An integer between 1 (for Monday) and 7 (for Sunday) was expected.", -1);
		else if (dayOfWeek == 7)
			dayOfWeek = 1;
		else
			dayOfWeek++;
		return dayOfWeek;
	}

	/**
	 * <p>Convert the given ISO-8601 week of year value into a Java week of year value.</p>
	 * 
	 * <i>Note: Same representation in ISO-8601 and Java.</i>
	 * 
	 * @param str	Textual representation of the week of year value in ISO-8601.
	 * @param cal	The calendar in which the year has already been set.
	 *           	<i>Note: This parameter is used to know the maximum number of weeks there are for the set year.
	 *           	(see {@link GregorianCalendar#getActualMaximum(int)})</i>
	 * 
	 * @return	The corresponding Java week of year value.
	 * 
	 * @throws ParseException	If the given week of year value is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getWeekOfYear(final String str, final GregorianCalendar cal) throws ParseException{
		int weekOfYear = twoDigitsFmt.parse(str).intValue();
		if (weekOfYear < 1 || weekOfYear > cal.getActualMaximum(Calendar.WEEK_OF_YEAR))
			throw new ParseException("Incorrect week of year value: " + weekOfYear + "! An integer between 1 and " + cal.getActualMaximum(Calendar.WEEK_OF_YEAR) + " was expected.", -1);
		return weekOfYear;
	}

	/**
	 * <p>Convert the given ISO-8601 month index into a Java index.</p>
	 * 
	 * <ul>
	 * 	<li><u>In ISO-8601</u>: January = 1, February = 2, ..., December = 12.</li>
	 * 	<li><u>In Java</u>    : January = 0, February = 1, ..., December = 11.</li>
	 * </ul>
	 * 
	 * @param str	Textual representation of the month index in ISO-8601.
	 * 
	 * @return	The corresponding Java month index.
	 * 
	 * @throws ParseException	If the given month index is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getMonth(final String str) throws ParseException{
		int month = twoDigitsFmt.parse(str).intValue();
		if (month < 1 || month > 12)
			throw new ParseException("Incorrect month value: " + month + "! An integer between 1 and 12 was expected.", -1);
		return month - 1;
	}

	/**
	 * <p>Convert the given ISO-8601 hours value into a Java hours value.</p>
	 * 
	 * <ul>
	 * 	<li><u>In ISO-8601</u>: 0 -&gt; 24.</li>
	 * 	<li><u>In Java</u>    : Calendar.HOUR_OF_DAY for 0 -&gt; 24.</li>
	 * </ul>
	 * 
	 * @param str	Textual representation of the hours value in ISO-8601.
	 * 
	 * @return	The corresponding Java hours value.
	 * 
	 * @throws ParseException	If the given hours value is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getHours(final String str) throws ParseException{
		int hours = twoDigitsFmt.parse(str).intValue();
		if (hours < 0 || hours > 24)
			throw new ParseException("Incorrect hour value: " + hours + "! An integer between 0 and 24 was expected.", -1);
		return hours;
	}

	/**
	 * <p>Convert the given ISO-8601 minutes value into a Java minutes value.</p>
	 * 
	 * <ul>
	 * 	<li><u>In ISO-8601</u>: 0 -&gt; 60.</li>
	 * 	<li><u>In Java</u>    : 0 -&gt; 60.</li>
	 * </ul>
	 * 
	 * @param str	Textual representation of the minutes value in ISO-8601.
	 * 
	 * @return	The corresponding Java minutes value.
	 * 
	 * @throws ParseException	If the given minutes value is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getMinutes(final String str) throws ParseException{
		int minutes = twoDigitsFmt.parse(str).intValue();
		if (minutes < 0 || minutes > 60)
			throw new ParseException("Incorrect minute value: " + minutes + "! An integer between 0 and 60 was expected.", -1);
		return minutes;
	}

	/**
	 * <p>Convert the given ISO-8601 seconds value into a Java seconds value.</p>
	 * 
	 * <ul>
	 * 	<li><u>In ISO-8601</u>: 0 -&gt; 60.</li>
	 * 	<li><u>In Java</u>    : 0 -&gt; 60.</li>
	 * </ul>
	 * 
	 * @param str	Textual representation of the seconds value in ISO-8601.
	 * 
	 * @return	The corresponding Java seconds value.
	 * 
	 * @throws ParseException	If the given seconds value is incorrect according to ISO-8601.
	 * 
	 * @since 4.2
	 */
	private static final int getSeconds(final String str) throws ParseException{
		int seconds = twoDigitsFmt.parse(str).intValue();
		if (seconds < 0 || seconds > 60)
			throw new ParseException("Incorrect second value: " + seconds + "! An integer between 0 and 60 was expected.", -1);
		return seconds;
	}
}
