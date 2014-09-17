package uws.service.file;

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
 * Copyright 2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                  Astronomisches Rechen Institut (ARI)
 */

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.regex.MatchResult;

/**
 * <p>Let interpret and computing a frequency.</p>
 * 
 * <h3>Frequency syntax</h3>
 * 
 * <p>The frequency is expressed as a string at initialization of this object. This string must respect the following syntax:</p>
 * <ul>
 * 	<li>'D' hh mm : daily schedule at hh:mm</li>
 * 	<li>'W' dd hh mm : weekly schedule at the given day of the week (1:sunday, 2:monday, ..., 7:saturday) at hh:mm</li>
 * 	<li>'M' dd hh mm : monthly schedule at the given day of the month at hh:mm</li>
 * 	<li>'h' mm : hourly schedule at the given minute</li>
 * 	<li>'m' : scheduled every minute (for completness :-))</li>
 * </ul>
 * <p><i>Where: hh = integer between 0 and 23, mm = integer between 0 and 59, dd (for 'W') = integer between 1 and 7 (1:sunday, 2:monday, ..., 7:saturday),
 * dd (for 'M') = integer between 1 and 31.</i></p>
 * 
 * <p><i><b>Warning:</b>
 * 	The frequency type is case sensitive! Then you should particularly pay attention at the case
 * 	when using the frequency types 'M' (monthly) and 'm' (every minute).
 * </p>
 * 
 * <p>
 * 	Parsing errors are not thrown but "resolved" silently. The "solution" depends of the error.
 * 	2 cases of errors are considered:
 * </p>
 * <ul>
 * 	<li><b>Frequency type mismatch:</b> It happens when the first character is not one of the expected (D, W, M, h, m).
 * 	                                    That means: bad case (i.e. 'd' rather than 'D'), another character.
 * 	                                    In this case, the frequency will be: <b>daily at 00:00</b>.</li>
 * 
 * 	<li><b>Parameter(s) missing or incorrect:</b> With the "daily" frequency ('D'), at least 2 parameters must be provided ;
 * 	                                             3 for "weekly" ('W') and "monthly" ('M') ; only 1 for "hourly" ('h') ; none for "every minute" ('m').
 * 	                                             This number of parameters is a minimum: only the n first parameters will be considered while
 * 	                                             the others will be ignored.
 * 	                                             If this minimum number of parameters is not respected or if a parameter value is incorrect,
 * 	                                             <b>all parameters will be set to their default value</b>
 * 	                                             (which is 0 for all parameter except dd for which it is 1).</li>
 * </ul>
 * 
 * <p>Examples:</p>
 * <ul>
 * 	<li><i>"" or NULL</i> = every day at 00:00</li>
 * 	<li><i>"D 06 30" or "D 6 30"</i> = every day at 06:30</li>
 * 	<li><i>"D 24 30"</i> = every day at 00:00, because hh must respect the rule: 0 &le; hh &le; 23</li>
 * 	<li><i>"d 06 30" or "T 06 30"</i> = every day at 00:00, because the frequency type "d" (lower case of "D") or "T" do not exist</li>
 * 	<li><i>"W 2 6 30"</i> = every week on Tuesday at 06:30</li>
 * 	<li><i>"W 8 06 30"</i> = every week on Sunday at 00:00, because with 'W' dd must respect the rule: 1 &le; dd &le; 7</li>
 * 	<li><i>"M 2 6 30"</i> = every month on the 2nd at 06:30</li>
 * 	<li><i>"M 32 6 30"</i> = every month on the 1st at 00:00, because with 'M' dd must respect the rule: 1 &le; dd &le; 31</li>
 * 	<li><i>"M 5 6 30 12"</i> = every month on the 5th at 06:30, because at least 3 parameters are expected and so considered: "12" and eventual other parameters are ignored</li>
 * </ul>
 * 
 * <h3>Computing next event date</h3>
 * 
 * <p>
 * 	When this class is initialized with a frequency, it is able to compute the date of the event following a given date.
 * 	The functions {@link #nextEvent()} and {@link #nextEvent(Date)} will compute this next event date
 * 	from, respectively, now (current date/time) and the given date (the date of the last event). Both are computing the date of the next
 * 	event by "adding" the frequency to the given date. And finally, the computed date is stored and returned.
 * </p>
 * 
 * <p>Then, you have 2 possibilities to trigger the desired event:</p>
 * <ul>
 * 	<li>By calling {@link #isTimeElapsed()}, you can test whether at the current moment the date of the next event has been reached or not.
 * 	    In function of the value returned by this function you will be then able to process the desired action or not.</li>
 * 	<li>By creating a Timer with the next date event. Thus, the desired action will be automatically triggered at the exact moment.</li>
 * </p>
 *  
 * 
 * @author Marc Wenger (CDS)
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.1 (09/2014)
 * @since 4.1
 */
public final class EventFrequency {

	/** String format of a hour or a minute number. */
	private static final NumberFormat NN = new DecimalFormat("00");

	/** Date-Time format to use in order to identify a frequent event. */
	private static final DateFormat EVENT_ID_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmm");

	/** Ordered list of all week days (there, the first week day is Sunday). */
	private static final String[] WEEK_DAYS = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};

	/** Ordinal day number suffix (1<b>st</b>, 2<b>nd</b>, 3<b>rd</b> and <b>th</b> for the others). */
	private static final String[] DAY_SUFFIX = {"st","nd","rd","th"};

	/** Frequency type (D, W, M, h, m). Default value: 'D' */
	private char dwm = 'D';

	/** "day" (dd) parameter of the frequency. */
	private int day = 0;
	/** "hour" (hh) parameter of the frequency. */
	private int hour = 0;
	/** "minute" (mm) parameter of the frequency. */
	private int min = 0;

	/** ID of the next event. By default, it is built using the date of the last event with the format {@link #EVENT_ID_FORMAT}. */
	private String eventID = "";

	/** Date (in millisecond) of the next event. */
	private long nextEvent = -1;

	/**
	 * <p>Create a new event frequency.</p>
	 * 
	 * <p>The frequency string must respect the following syntax:</p>
	 * <ul>
	 * 	<li>'D' hh mm : daily schedule at hh:mm</li>
	 * 	<li>'W' dd hh mm : weekly schedule at the given day of the week (1:sunday, 2:monday, ..., 7:saturday) at hh:mm</li>
	 * 	<li>'M' dd hh mm : monthly schedule at the given day of the month at hh:mm</li>
	 * 	<li>'h' mm : hourly schedule at the given minute</li>
	 * 	<li>'m' : scheduled every minute (for completness :-))</li>
	 * </ul>
	 * <p><i>Where: hh = integer between 0 and 23, mm = integer between 0 and 59, dd (for 'W') = integer between 1 and 7 (1:sunday, 2:monday, ..., 7:saturday),
	 * dd (for 'M') = integer between 1 and 31.</i></p>
	 * 
	 * <p><i><b>Warning:</b>
	 * 	The frequency type is case sensitive! Then you should particularly pay attention at the case
	 * 	when using the frequency types 'M' (monthly) and 'm' (every minute).
	 * </p>
	 * 
	 * <p>
	 * 	Parsing errors are not thrown but "resolved" silently. The "solution" depends of the error.
	 * 	2 cases of errors are considered:
	 * </p>
	 * <ul>
	 * 	<li><b>Frequency type mismatch:</b> It happens when the first character is not one of the expected (D, W, M, h, m).
	 * 	                                    That means: bad case (i.e. 'd' rather than 'D'), another character.
	 * 	                                    In this case, the frequency will be: <b>daily at 00:00</b>.</li>
	 * 
	 * 	<li><b>Parameter(s) missing or incorrect:</b> With the "daily" frequency ('D'), at least 2 parameters must be provided ;
	 * 	                                             3 for "weekly" ('W') and "monthly" ('M') ; only 1 for "hourly" ('h') ; none for "every minute" ('m').
	 * 	                                             This number of parameters is a minimum: only the n first parameters will be considered while
	 * 	                                             the others will be ignored.
	 * 	                                             If this minimum number of parameters is not respected or if a parameter value is incorrect,
	 * 	                                             <b>all parameters will be set to their default value</b>
	 * 	                                             (which is 0 for all parameter except dd for which it is 1).</li>
	 * </ul>
	 * 
	 * <p>Examples:</p>
	 * <ul>
	 * 	<li><i>"" or NULL</i> = every day at 00:00</li>
	 * 	<li><i>"D 06 30" or "D 6 30"</i> = every day at 06:30</li>
	 * 	<li><i>"D 24 30"</i> = every day at 00:00, because hh must respect the rule: 0 &le; hh &le; 23</li>
	 * 	<li><i>"d 06 30" or "T 06 30"</i> = every day at 00:00, because the frequency type "d" (lower case of "D") or "T" do not exist</li>
	 * 	<li><i>"W 2 6 30"</i> = every week on Tuesday at 06:30</li>
	 * 	<li><i>"W 8 06 30"</i> = every week on Sunday at 00:00, because with 'W' dd must respect the rule: 1 &le; dd &le; 7</li>
	 * 	<li><i>"M 2 6 30"</i> = every month on the 2nd at 06:30</li>
	 * 	<li><i>"M 32 6 30"</i> = every month on the 1st at 00:00, because with 'M' dd must respect the rule: 1 &le; dd &le; 31</li>
	 * 	<li><i>"M 5 6 30 12"</i> = every month on the 5th at 06:30, because at least 3 parameters are expected and so considered: "12" and eventual other parameters are ignored</li>
	 * </ul>
	 * 
	 * @param interval	A string defining the event frequency (see above for the string format).
	 */
	public EventFrequency(String interval){
		String str;

		// Determine the separation between the frequency type character (D, W, M, h, m) and the parameters
		// and normalize the given interval:
		int p = -1;
		if (interval == null)
			interval = "";
		else{
			interval = interval.trim();
			p = interval.indexOf(' ');
		}

		// Parse the given interval ONLY IF a frequency type is provided (even if there is no parameter):
		if (p == 1 || interval.length() == 1){
			MatchResult result;
			Scanner scan = null;

			// Extract and identify the frequency type:
			dwm = interval.charAt(0);
			str = interval.substring(p + 1);
			scan = new Scanner(str);

			// Extract the parameters in function of the frequency type:
			switch(dwm){
			// CASE: DAILY
				case 'D':
					scan.findInLine("(\\d{1,2}) (\\d{1,2})");
					try{
						result = scan.match();
						hour = parseHour(result.group(1));
						min = parseMinute(result.group(2));
					}catch(IllegalStateException ise){
						day = hour = min = 0;
					}
					break;

				// CASE: WEEKLY AND MONTHLY
				case 'W':
				case 'M':
					scan.findInLine("(\\d{1,2}) (\\d{1,2}) (\\d{1,2})");
					try{
						result = scan.match();
						day = (dwm == 'W') ? parseDayOfWeek(result.group(1)) : parseDayOfMonth(result.group(1));
						hour = parseHour(result.group(2));
						min = parseMinute(result.group(3));
					}catch(IllegalStateException ise){
						day = (dwm == 'W') ? 0 : 1;
						hour = min = 0;
					}
					break;

				// CASE: HOURLY
				case 'h':
					scan.findInLine("(\\d{1,2})");
					try{
						result = scan.match();
						min = parseMinute(result.group(1));
					}catch(IllegalStateException ise){
						min = 0;
					}
					break;

				// CASE: EVERY MINUTE
				case 'm':
					// no other data needed
					break;

				// CASE: UNKNOWN FREQUENCY TYPE
				default:
					dwm = 'D';
					day = hour = min = 0;
			}
			if (scan != null)
				scan.close();
		}
	}

	/**
	 * Parse a string representing the day of the week (as a number).
	 * 
	 * @param dayNbStr	String containing an integer representing a week day.
	 * 
	 * @return	The identified week day. (integer between 0 and 6 (included))
	 * 
	 * @throws IllegalStateException	If the given string does not contain an integer or is not between 1 and 7 (included).
	 */
	private int parseDayOfWeek(final String dayNbStr) throws IllegalStateException{
		try{
			int d = Integer.parseInt(dayNbStr);
			if (d >= 1 && d <= WEEK_DAYS.length)
				return d - 1;
		}catch(Exception e){}
		throw new IllegalStateException("Incorrect day of week (" + dayNbStr + ") ; it should be between 1 and 7 (both included)!");
	}

	/**
	 * Parse a string representing the day of the month.
	 * 
	 * @param dayStr	String containing an integer representing a month day.
	 * 
	 * @return	The identified month day. (integer between 1 and 31 (included))
	 * 
	 * @throws IllegalStateException	If the given string does not contain an integer or is not between 1 and 31 (included).
	 */
	private int parseDayOfMonth(final String dayStr) throws IllegalStateException{
		try{
			int d = Integer.parseInt(dayStr);
			if (d >= 1 && d <= 31)
				return d;
		}catch(Exception e){}
		throw new IllegalStateException("Incorrect day of month (" + dayStr + ") ; it should be between 1 and 31 (both included)!");
	}

	/**
	 * Parse a string representing the hour part of a time (<b>hh</b>:mm).
	 * 
	 * @param hourStr	String containing an integer representing an hour.
	 * 
	 * @return	The identified hour. (integer between 0 and 23 (included))
	 * 
	 * @throws IllegalStateException	If the given string does not contain an integer or is not between 0 and 23 (included).
	 */
	private int parseHour(final String hourStr) throws IllegalStateException{
		try{
			int h = Integer.parseInt(hourStr);
			if (h >= 0 && h <= 23)
				return h;
		}catch(Exception e){}
		throw new IllegalStateException("Incorrect hour number(" + hourStr + ") ; it should be between 0 and 23 (both included)!");
	}

	/**
	 * Parse a string representing the minute part of a time (hh:<b>mm</b>).
	 * 
	 * @param minStr	String containing an integer representing a minute.
	 * 
	 * @return	The identified minute. (integer between 0 and 59 (included))
	 * 
	 * @throws IllegalStateException	If the given string does not contain an integer or is not between 0 and 59 (included).
	 */
	private int parseMinute(final String minStr) throws IllegalStateException{
		try{
			int m = Integer.parseInt(minStr);
			if (m >= 0 && m <= 59)
				return m;
		}catch(Exception e){}
		throw new IllegalStateException("Incorrect minute number (" + minStr + ") ; it should be between 0 and 59 (both included)!");
	}

	/**
	 * Tell whether the interval between the last event and now is greater or equals to the frequency represented by this object.
	 * 
	 * @return	<i>true</i> if the next event date has been reached, <i>false</i> otherwise.
	 */
	public boolean isTimeElapsed(){
		return (nextEvent <= 0) || (System.currentTimeMillis() >= nextEvent);
	}

	/**
	 * Get the date of the next event.
	 * 
	 * @return	Date of the next event, or NULL if no date has yet been computed.
	 */
	public Date getNextEvent(){
		return (nextEvent <= 0) ? null : new Date(nextEvent);
	}

	/**
	 * <p>Get a string which identity the period between the last event and the next one (whose the date has been computed by this object).</p>
	 * 
	 * <p>This ID is built by formatting in string the given date of the last event.</p>
	 * 
	 * @return	ID of the period before the next event.
	 */
	public String getEventID(){
		return eventID;
	}

	/**
	 * <p>Compute the date of the event, by adding the interval represented by this object to the current date/time.</p>
	 * 	
	 * <p>
	 * 	The role of this function is to compute the next event date, not to get it. After computation, you can get this date
	 * 	thanks to {@link #getNextEvent()}. Furthermore, using {@link #isTimeElapsed()} after having called this function will
	 * 	let you test whether the next event should (have) occur(red).
	 * </p>
	 * 
	 * <p><i>Note:
	 * 	This function computes the next event date by taking the current date as the date of the last event. However,
	 * 	if the last event occurred at a different date, you should use {@link #nextEvent(Date)}.
	 * </i></p>
	 * 
	 * @return	Date at which the next event should occur. (basically, it is: NOW + frequency)
	 */
	public Date nextEvent(){
		return nextEvent(new Date());
	}

	/**
	 * <p>Compute the date of the event, by adding the interval represented by this object to the given date/time.</p>
	 * 	
	 * <p>
	 * 	The role of this function is to compute the next event date, not to get it. After computation, you can get this date
	 * 	thanks to {@link #getNextEvent()}. Furthermore, using {@link #isTimeElapsed()} after having called this function will
	 * 	let you test whether the next event should (have) occur(red).
	 * </p>
	 * 
	 * @return	Date at which the next event should occur. (basically, it is lastEventDate + frequency)
	 */
	public Date nextEvent(final Date lastEventDate){
		// Set the calendar to the given date:
		GregorianCalendar date = new GregorianCalendar();
		date.setTime(lastEventDate);

		// Compute the date of the next event:
		switch(dwm){
		// CASE: DAILY
			case 'D':
				date.add(Calendar.DAY_OF_YEAR, 1);
				date.set(Calendar.HOUR_OF_DAY, hour);
				date.set(Calendar.MINUTE, min);
				date.set(Calendar.SECOND, 0);
				break;

			// CASE: WEEKLY
			case 'W':
				// find the next right day to trigger the rotation
				int weekday = date.get(Calendar.DAY_OF_WEEK);	// sunday=1, ... saturday=7
				if (weekday == day){
					date.add(Calendar.WEEK_OF_YEAR, 1);
				}else{
					// for the first scheduling which can happen any day
					int delta = day - weekday;
					if (delta <= 0)
						delta += 7;
					date.add(Calendar.DAY_OF_YEAR, delta);
				}
				date.set(Calendar.HOUR_OF_DAY, hour);
				date.set(Calendar.MINUTE, min);
				date.set(Calendar.SECOND, 0);
				break;

			// CASE: MONTHLY
			case 'M':
				date.add(Calendar.MONTH, 1);
				date.set(Calendar.DAY_OF_MONTH, day);
				date.set(Calendar.HOUR_OF_DAY, hour);
				date.set(Calendar.MINUTE, min);
				date.set(Calendar.SECOND, 0);
				break;

			// CASE: HOURLY
			case 'h':
				date.add(Calendar.HOUR_OF_DAY, 1);
				date.set(Calendar.MINUTE, min);
				date.set(Calendar.SECOND, 0);
				break;

			// CASE: EVERY MINUTE
			case 'm':
				date.add(Calendar.MINUTE, 1);
				date.set(Calendar.SECOND, 0);
				break;

		/* OTHERWISE, the next event date is the given date! */
		}

		// Save it in millisecond for afterward comparison with the current time (so that telling whether the time is elapsed or not):
		nextEvent = date.getTimeInMillis();

		// Build the ID of this waiting period (the period between the last event and the next one):
		eventID = EVENT_ID_FORMAT.format(new Date());

		// Return the date of the next event:
		return date.getTime();
	}

	/**
	 * Display in a human readable way the frequency represented by this object.
	 * 
	 * @return a string, i.e. weekly on Sunday at HH:MM
	 */
	@Override
	public String toString(){
		StringBuilder str = new StringBuilder();
		switch(dwm){
			case 'D':
				str.append("daily");
				str.append(" at ").append(NN.format(hour)).append(':').append(NN.format(min));
				break;
			case 'W':
				str.append("weekly on ").append(WEEK_DAYS[day % 7]);
				str.append(" at ").append(NN.format(hour)).append(':').append(NN.format(min));
				break;
			case 'M':
				str.append("monthly on the ").append(day).append(DAY_SUFFIX[Math.min(day - 1, 3)]);
				str.append(" at ").append(NN.format(hour)).append(':').append(NN.format(min));
				break;
			case 'h':
				str.append("hourly at ").append(NN.format(min));
				break;
			case 'm':
				str.append("every minute");
				break;
		}

		return str.toString();
	}
}