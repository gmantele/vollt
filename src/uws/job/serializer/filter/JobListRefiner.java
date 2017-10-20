package uws.job.serializer.filter;

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
 * Copyright 2017 - Astronomisches Rechen Institut (ARI)
 */

import java.text.ParseException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import uws.ISO8601Format;
import uws.UWSException;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;

/**
 * Let filter (and optionally order) a list of jobs according to the filter
 * parameters given in an {@link HttpServletRequest}.
 *
 * <p>Only the following HTTP-GET parameters will generate a job filter:</p>
 * <ul>
 * 	<li><b>PHASE</b>: a single legal phase is expected. Only jobs having this
 * 	                  phase will pass through this filter.
 *
 * 	                  <p><i><b>Note:</b>Several <code>PHASE</code> parameters
 * 	                  with different execution phases may be provided. Their
 * 	                  effect will be joint with a logical OR (so, all jobs of
 * 	                  one of the given phases will pass the filter).</i></p></li>
 *
 * 	<li><b>AFTER</b>: an ISO-8601 date is expected. Only jobs started
 * 	                  after this date will pass through the filter.
 *
 * 	                  <p><i><b>Note:</b> If several <code>AFTER</code>
 * 	                  parameters are provided, only the one with the most recent
 * 	                  date will be taken into account. For instance:
 * 	                  <code>?AFTER=2015-01-01T12:00:00Z&AFTER=2014-01-01T12:00:00Z</code>
 * 	                  will be interpreted as just 1 AFTER filter with the date
 * 	                  <code>2015-01-01T12:00:00Z</code>.</i></p></li>
 *
 * 	<li><b>LAST</b>: a positive integer number is expected. Only the LAST most
 * 	                 recently created jobs will pass through the filter.
 * 	                 The jobs will be sorted by descending creationTime.
 *
 * 	                 <p><i><b>Note:</b> If several <code>LAST</code>
 * 	                 parameters are provided, only the smallest positive (and
 * 	                 not null) value will be taken into account.</i></p>
 * </ul>
 *
 * <p><i><b>IMPORTANT Note:</b>
 * 	If no PHASE filter is specified, a default one rejecting ARCHIVED filter is
 * 	set by default. This is specified in UWS 1.1 standard so that being backward
 * 	compatible with the version 1.0 of the standard in which no ARCHIVED phase
 * 	existed.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 4.3 (10/2017)
 * @since 4.3
 */
public class JobListRefiner {

	/** List of all job filters to apply. All of these filters must match in
	 * order to keep a job. */
	protected final List<JobFilter> filters = new ArrayList<JobFilter>();

	/** List of retained jobs after filtering. */
	protected List<UWSJob> jobList = new ArrayList<UWSJob>();

	/** Indicate how jobs must be sorted in the {@link #jobList} list.
	 *
	 * <p><i>Note:
	 * 	If NULL, jobs are merely added at the end of the list: no order.
	 * </i></p> */
	protected Comparator<UWSJob> sortComp = null;

	/** Indicate how many of the first jobs of the list {@link #jobList} must be
	 * really retained.
	 *
	 * <p><i>Note:
	 * 	If negative or null, all jobs are of the filtered list are returned.
	 * </i></p> */
	protected int topSize = -1;

	/** Indicate if the jobs of the {@link #jobList} list must be returned from
	 * the beginning to the end (<code>false</code>) or from the end to the
	 * beginning (<code>true</code>).
	 *
	 * <p><i>Note:
	 * 	If {@link #topSize} have a positive and not null value, this attribute
	 * 	will then apply on the {@link #topSize} first jobs: if <code>false</code>
	 * 	jobs from 0 to {@link #topSize} are returned, otherwise they are
	 * 	returned from {@link #topSize} to 0.
	 * </i></p> */
	protected boolean reverseOrder = false;

	/**
	 * Empty constructor. No filter and no sorting is done here.
	 * All jobs given to the {@link #filter(Iterator)} function will then be
	 * allowed.
	 */
	protected JobListRefiner(){}

	/**
	 * Build a filter for a whole list of jobs.
	 *
	 * <p>
	 * 	This filter will actually be composed of several Job filters,
	 * 	in function of the HTTP-GET parameters specified in the given HTTP
	 * 	request.
	 * </p>
	 *
	 * <p>Only the following HTTP-GET parameters will generate a job filter:</p>
	 * <ul>
	 * 	<li><b>PHASE</b>: a single legal phase is expected. Only jobs having
	 * 	                  this phase will pass through this filter.
	 *
	 * 	                  <p><i><b>Note:</b>Several <code>PHASE</code>
	 * 	                  parameters with different execution phases may be
	 * 	                  provided. Their effect will be joint with a logical OR
	 * 	                  (so, all jobs of one of the given phases will pass the
	 * 	                  filter).</i></p></li>
	 *
	 * 	<li><b>AFTER</b>: an ISO-8601 date is expected. Only jobs started
	 * 	                  after this date will pass through the filter.
	 *
	 * 	                  <p><i><b>Note:</b> If several <code>AFTER</code>
	 * 	                  parameters are provided, only the one with the most
	 * 	                  recent date will be taken into account. For instance:
	 * 	                  <code>?AFTER=2015-01-01T12:00:00Z&AFTER=2014-01-01T12:00:00Z</code>
	 * 	                  will be interpreted as just 1 AFTER filter with the
	 * 	                  date <code>2015-01-01T12:00:00Z</code>.</i></p></li>
	 *
	 * 	<li><b>LAST</b>: a positive integer number is expected. Only the LAST
	 * 	                 most recently created jobs will pass through the
	 * 	                 filter. The jobs will be sorted by descending
	 * 	                 creationTime.
	 *
	 * 	                 <p><i><b>Note:</b> If several <code>LAST</code>
	 * 	                 parameters are provided, only the smallest positive
	 * 	                 (and not null) value will be taken into account.</i></p>
	 * </ul>
	 *
	 * <p><i><b>IMPORTANT Note:</b>
	 * 	If no PHASE filter is specified, a default one rejecting ARCHIVED filter
	 * 	is set by default. This is specified in UWS 1.1 standard so that being
	 * 	backward compatible with the version 1.0 of the standard in which no
	 * 	ARCHIVED phase existed.
	 * </i></p>
	 *
	 * @param request	An HTTP request in which HTTP-GET parameters correspond
	 *               	to Job filters to create.
	 *
	 * @throws UWSException	If the value of at least one AFTER, PHASE or LAST
	 *                     	parameter is incorrect.
	 */
	public JobListRefiner(final HttpServletRequest request) throws UWSException{
		String pName;
		String[] values;

		ExecutionPhase phase = null;
		Date afterDate = null;
		int last = -1;

		AfterFilter afterFilter = null;
		PhasesFilter phasesFilter = null;

		/* *************************************************** */
		/* Identify all filters inside the HTTP-GET parameters */

		Enumeration<String> paramNames = request.getParameterNames();
		while(paramNames.hasMoreElements()){
			pName = paramNames.nextElement();
			values = request.getParameterValues(pName);
			// Case: PHASE (case INsensitively):
			if (pName.toUpperCase().equals("PHASE")){
				for(String p : values){
					if (p != null){
						try{
							// resolve the execution phase:
							phase = ExecutionPhase.valueOf(p.toUpperCase());
							// add the phase into the PhasesFilter:
							if (phasesFilter == null)
								phasesFilter = new PhasesFilter(phase);
							else
								phasesFilter.add(phase);
						}catch(IllegalArgumentException iae){
							throw new UWSException(UWSException.BAD_REQUEST, "Incorrect PHASE value: \"" + p + "\"! No such execution phase is known by this service.");
						}
					}
				}
			}else if (pName.toUpperCase().equals("AFTER")){
				for(String p : values){
					if (p != null){
						try{
							// resolve date:
							afterDate = ISO8601Format.parseToDate(p);
							/* create/replace the AfterFilter if the date is
							 * more recent: */
							if (afterFilter == null || afterDate.after(afterFilter.getDate()))
								afterFilter = new AfterFilter(afterDate);
						}catch(ParseException pe){
							throw new UWSException(UWSException.BAD_REQUEST, "Incorrect AFTER value: \"" + p + "\"! The date must be formatted in ISO-8601.");
						}
					}
				}
			}else if (pName.toUpperCase().equals("LAST")){
				for(String p : values){
					if (p != null){
						try{
							// resolve the number of jobs to fetch:
							last = Integer.parseInt(p);
							/* update the last counter (the value is updated
							 * only if the new value is positive and smaller): */
							if (last >= 0 && (topSize < 0 || last < topSize))
								topSize = last;
							else if (last < 0)
								throw new UWSException(UWSException.BAD_REQUEST, "Incorrect LAST value: \"" + p + "\"! A positive integer was expected.");
						}catch(NumberFormatException nfe){
							throw new UWSException(UWSException.BAD_REQUEST, "Incorrect LAST value: \"" + p + "\"! A positive integer was expected.");
						}
					}
				}
			}
		}

		/* ************************ */
		/* Append all found filters */

		/* Set the PHASES filter (if no filter is specified, a default one
		 * forbidding ARCHIVED jobs is set): */
		if (phasesFilter != null)
			filters.add(phasesFilter);
		else
			filters.add(new NoArchivedFilter());

		// Set the AFTER filter:
		if (afterFilter != null)
			filters.add(afterFilter);

		// Set the LAST filter:
		if (topSize >= 0){
			/* jobs are sorted by descending creation-time (so that only the
			 * topSize first can be easily read): */
			sortComp = new JobComparator();
			/* the topSize most recently jobs must be returned in descending creation-time,
			 * so the order of jobs set by the sortComp comparator must NOT be reversed: */
			reverseOrder = false;
		}

	}

	/**
	 * Add the given job in the temporary internal list of filtered jobs by
	 * preserving the specified sorting.
	 *
	 * <p>
	 * 	This function added the job at the end of the list IF no sort is
	 * 	required. Otherwise, a binary search is performed and the job is added
	 * 	at the right place in the list so that keeping the list sorted.
	 * </p>
	 *
	 * @param job	The job to keep in the display-able job list.
	 */
	protected final void addJob(final UWSJob job){
		if (job == null)
			return;

		if (sortComp == null)
			jobList.add(job);
		else{
			int index = Collections.binarySearch(jobList, job, sortComp);
			if (index < 0)
				index = -(index + 1);
			jobList.add(index, job);
		}
	}

	/**
	 * Filter (and eventually sort and/or limit in size) the given list of jobs.
	 *
	 * @param jobList	Job list to filter.
	 *
	 * @return	The filtered (and eventually sorted/limited) job list.
	 */
	@SuppressWarnings("rawtypes")
	public Iterator<UWSJob> refine(final Iterator<UWSJob> jobList){
		// Remove all items of the last filtering result:
		if (this.jobList instanceof AbstractList)
			((AbstractList)this.jobList).clear();
		else{
			while(!this.jobList.isEmpty())
				this.jobList.remove(0);
		}

		// Filters the given jobs with the simple job filters:
		UWSJob job;
		while(jobList.hasNext()){
			job = jobList.next();

			// Apply the job filters on this job and retain it if it passes them:
			if (match(job))
				addJob(job);	// if a sort must be done, it is performed here by #addJob(UWSJob)
		}

		// Return an iterator on this whole filtered job list:
		if (topSize < 0)
			return this.jobList.iterator();

		// OR Return an iterator on the topSize first jobs (in the current order or reverse):
		else
			return new TopIterator(this.jobList, topSize, reverseOrder);
	}

	/**
	 * Tell whether the given job matches all the job filters.
	 *
	 * <p>
	 * 	In other words, this function operates a logical AND between all listed
	 * 	filters.
	 * </p>
	 *
	 * <p><i>Note:
	 * 	If the given job is NULL, <code>false</code> will be returned.
	 * 	In case of error while evaluating one of the filters on the given job,
	 * 	<code>false</code> will be returned as well.
	 * </i></p>
	 *
	 * @param job	A job to filter.
	 *
	 * @return	<code>true</code> if the job matches all the filters,
	 *        	<code>false</code> otherwise.
	 */
	protected final boolean match(final UWSJob job){
		if (job == null)
			return false;

		for(JobFilter filter : filters){
			if (!filter.match(job))
				return false;
		}

		return true;
	}

	/**
	 * Compare the 2 given {@link UWSJob} instances by using only their creation
	 * date/time. The most recently created job is considered as inferior. So,
	 * this comparator aims to sort jobs by descending creation date/time.
	 *
	 * <p><i><b>WARNING!</b>
	 * 	It must be ensured that all compared jobs have always a NOT-NULL
	 * 	creationTime attribute. Otherwise this comparator may fail or return an
	 * 	incorrect value.
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 4.3 (10/2017)
	 * @since 4.3
	 */
	public final static class JobComparator implements Comparator<UWSJob> {
		@Override
		public int compare(UWSJob o1, UWSJob o2){
			return -(o1.getCreationTime().compareTo(o2.getCreationTime()));
		}
	}

	/**
	 * This iterator is designed to return just the N first items of the given
	 * list.
	 *
	 * <p>
	 * 	It is also possible to inverse the order of these N first items by setting
	 * 	the last constructor parameter to <code>true</code>.
	 * </p>
	 *
	 * <p><i>Note:
	 * 	This iterator does not support the remove operation ;
	 * 	the function {@link #remove()} will then return an
	 * 	{@link UnsupportedOperationException}.
	 * </i></p>
	 *
	 * @author Gr&eacute;gory Mantelet (ARI)
	 * @version 4.3 (10/2017)
	 * @since 4.3
	 */
	protected final static class TopIterator implements Iterator<UWSJob> {

		/** Jobs list on which this iterator must iterate. */
		private final List<UWSJob> list;
		/** The number of items that must be read by this iterator. */
		private final int topSize;
		/** Indicate whether the topSize items must be read in the given order
		 * or in the reverse one. */
		private final boolean reverseOrder;

		/** Index of the last read item. */
		private int currentIndex = -1;
		/** Number of read items. */
		private int count = 0;

		/**
		 * Create an iterator which will read the <code>topSize</code> first
		 * item of the given list.
		 *
		 * @param joblist	List of jobs to return.
		 * @param topSize	Number of items to read from the beginning of the
		 *               	list.
		 * @param reverse	<code>true</code> if the <code>topSize</code> first
		 *               	items must be read in the given order,
		 *               	<code>false</code> if they must be read in the
		 *               	reverse order.
		 */
		public TopIterator(final List<UWSJob> joblist, final int topSize, final boolean reverse){
			this.list = joblist;
			this.topSize = topSize;
			this.reverseOrder = reverse;

			if (reverseOrder && topSize >= 0)
				currentIndex = (list.size() <= topSize) ? list.size() : topSize;
			else
				currentIndex = reverseOrder ? list.size() : -1;

			count = 0;
		}

		@Override
		public boolean hasNext(){
			return (topSize < 0 || count + 1 <= topSize) && (reverseOrder ? currentIndex - 1 >= 0 : currentIndex + 1 < list.size());
		}

		@Override
		public UWSJob next(){
			if (!hasNext())
				throw new NoSuchElementException("No more jobs in this filtered job list!");

			count++;
			currentIndex = reverseOrder ? currentIndex - 1 : currentIndex + 1;

			return list.get(currentIndex);
		}

		@Override
		public void remove(){
			throw new UnsupportedOperationException("No remove operation possible on this iterator of filtered job list!");
		}

	}

}