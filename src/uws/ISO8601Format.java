package uws;

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
 *  <h3>Date parsing</h3>
 *  
 *  <p>
 *    This class is able to parse dates - with the function {@link #parse(String)} - formatted strictly in ISO8601
 *    but is also more permissive. Particularly, separators (like '-' and ':') are optional. The date and time separator
 *    ('T') can be replaced by a space.
 *  </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (10/2014)
 * @since 4.1
 */
public class ISO8601Format {

	/** Indicate whether any date formatted with this class displays the time zone. */
	public static boolean displayTimeZone = false;
	/** Indicate whether any date formatted with this class displays the milliseconds. */
	public static boolean displayMilliseconds = false;
	/** Indicate the time zone in which the date and time should be formatted (whatever is the time zone of the given date). */
	public static String targetTimeZone = "UTC"; // for the local time zone: TimeZone.getDefault().getID();

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
	 * @param date				Date-time in milliseconds (from the 1st January 1970 ; this value is returned by java.util.Date#getTime()).
	 * @param targetTimeZone	Target time zone.
	 * @param withTimeZone		<i>true</i> to display the time zone, <i>false</i> otherwise.
	 * @param withMillisec		<i>true</i> to display the milliseconds, <i>false</i> otherwise.	
	 * 
	 * @return	Date formatted in ISO8601.
	 */
	protected static String format(final long date, final String targetTimeZone, final boolean withTimeZone, final boolean withMillisec){
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
	 * @param strDate	Date expressed as a string in ISO8601 format.
	 * 
	 * @return	Parsed date (expressed in milliseconds from the 1st January 1970 ;
	 *        	a date can be easily built with this number using {@link java.util.Date#Date(long)}).
	 * 
	 * @throws ParseException	If the given date is not expressed in ISO8601 format or is not merely parseable with this implementation.
	 */
	public static long parse(final String strDate) throws ParseException{
		Pattern p = Pattern.compile("(\\d{4})-?(\\d{2})-?(\\d{2})([T| ](\\d{2}):?(\\d{2}):?(\\d{2})(\\.(\\d+))?(Z|([\\+|\\-])(\\d{2}):?(\\d{2})(:?(\\d{2}))?)?)?");
		/*
		 * With this regular expression, we will get the following groups:
		 * 
		 * 	( 0: everything)
		 * 	  1: year  (yyyy)
		 * 	  2: month (MM)
		 * 	  3: day   (dd)
		 * 	( 4: the full time part)
		 * 	  5: hours        (hh)
		 * 	  6: minutes      (mm)
		 * 	  7: seconds      (ss)
		 * 	( 8: the full ms part)
		 * 	  9: milliseconds (sss)
		 * 	(10: the full time zone part: 'Z' or the applied time offset)
		 * 	 11: sign of the offset ('+' if an addition was applied, '-' if it was a subtraction)
		 * 	 12: applied hours offset   (hh)
		 * 	 13: applied minutes offset (mm)
		 * 	(14: the full seconds offset)
		 * 	 15: applied seconds offset (ss)
		 * 
		 * Groups in parenthesis should be ignored ; but an exception must be done for the 10th which may contain 'Z' meaning a UTC time zone.
		 * 
		 * All groups from the 4th (included) are optional. If not filled, an optional group is set to NULL.
		 * 
		 * This regular expression is more permissive than the strict definition of the ISO8601 format. Particularly, separator characters
		 * ('-', 'T' and ':') are optional and it is possible to specify seconds in the time zone offset.
		 */

		Matcher m = p.matcher(strDate);
		if (m.matches()){
			Calendar cal = new GregorianCalendar();

			// Set the time zone:
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

			// Set the date:
			cal.set(Calendar.DAY_OF_MONTH, twoDigitsFmt.parse(m.group(3)).intValue());
			cal.set(Calendar.MONTH, twoDigitsFmt.parse(m.group(2)).intValue() - 1);
			cal.set(Calendar.YEAR, Integer.parseInt(m.group(1)));

			// Set the time:
			if (m.group(4) != null){
				cal.set(Calendar.HOUR_OF_DAY, twoDigitsFmt.parse(m.group(5)).intValue());
				cal.set(Calendar.MINUTE, twoDigitsFmt.parse(m.group(6)).intValue());
				cal.set(Calendar.SECOND, twoDigitsFmt.parse(m.group(7)).intValue());
				if (m.group(9) != null)
					cal.set(Calendar.MILLISECOND, twoDigitsFmt.parse(m.group(9)).intValue());
				else
					cal.set(Calendar.MILLISECOND, 0);
			}else{
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
			}

			// Compute and apply the offset:
			if (m.group(10) != null && !m.group(10).equals("Z")){
				int sign = (m.group(11).equals("-") ? 1 : -1);
				cal.add(Calendar.HOUR_OF_DAY, sign * twoDigitsFmt.parse(m.group(12)).intValue());
				cal.add(Calendar.MINUTE, sign * twoDigitsFmt.parse(m.group(13)).intValue());
				if (m.group(15) != null)
					cal.add(Calendar.SECOND, sign * twoDigitsFmt.parse(m.group(15)).intValue());
			}

			return cal.getTimeInMillis();
		}else
			throw new ParseException("Invalid date format: \"" + strDate + "\"! An ISO8601 date was expected.", 0);
	}
}
