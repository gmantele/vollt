package uws.job.serializer.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.junit.Test;

import uws.ISO8601Format;
import uws.UWSException;
import uws.job.ErrorSummary;
import uws.job.ErrorType;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.job.serializer.filter.JobListRefiner.TopIterator;

public class TestJobListRefiner {

	@Test
	public void testJobListRefiner(){
		TestHttpServletRequest request = new TestHttpServletRequest();
		JobListRefiner filter;
		try{
			// NO PARAMS => Nothing set!
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);

			// A NON FILTER PARAMETER => Nothing set!
			request.addParams("Nothing", "Blabla");
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("No error should happen if no LAST, PHASE or AFTER parameter is provided.");
		}

		/* ************* */
		/* FILTER: AFTER */

		// INCORRECT AFTER PARAMETER => Nothing set and no error!
		try{
			request.clearParams();
			request.addParams("AFTER", "foo");
			filter = new JobListRefiner(request);
			fail("The provided parameter AFTER is NOT AT ALL an ISO-8601 date. An error should have occurred.");
		}catch(UWSException ue){
			assertEquals("Incorrect AFTER value: \"foo\"! The date must be formatted in ISO-8601.", ue.getMessage());
			assertEquals(UWSException.BAD_REQUEST, ue.getHttpErrorCode());
		}

		try{
			// CORRECT AFTER PARAMETER => an AfterFilter should be set!
			// With just a full date:
			request.clearParams();
			request.addParams("AFTER", "2015-01-01");
			filter = new JobListRefiner(request);
			assertEquals(2, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(AfterFilter.class, filter.filters.get(1).getClass());
			assertEquals("2015-01-01T00:00:00Z", ISO8601Format.format(((AfterFilter)filter.filters.get(1)).getDate()));
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);
			request.clearParams();
			// With a full date and time:
			request.addParams("AFTER", "2015-01-01T12:00:00");
			filter = new JobListRefiner(request);
			assertEquals(2, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(AfterFilter.class, filter.filters.get(1).getClass());
			assertEquals("2015-01-01T12:00:00Z", ISO8601Format.format(((AfterFilter)filter.filters.get(1)).getDate()));
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);

			// CORRECT 3 AFTER PARAMETERS => a single AfterFilter with the most recent date should be set!
			request.clearParams();
			request.addParams("AFTER", "2014-01-01T12:00:00");
			request.addParams("after", "2015-01-30T12:00:00");
			request.addParams("AFTER", "2015-01-01T12:00:00");
			filter = new JobListRefiner(request);
			assertEquals(2, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(AfterFilter.class, filter.filters.get(1).getClass());
			assertEquals("2015-01-30T12:00:00Z", ISO8601Format.format(((AfterFilter)filter.filters.get(1)).getDate()));
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("No error should happen since all provided AFTER parameters are correct.");
		}

		/* ************ */
		/* FILTER: LAST */

		// 1 INCORRECT LAST PARAMETER => nothing should be set!
		try{
			request.clearParams();
			request.addParams("LAST", "-10");
			filter = new JobListRefiner(request);
			fail("The provided parameter LAST is NOT positive. An error should have occurred.");
		}catch(UWSException ue){
			assertEquals("Incorrect LAST value: \"-10\"! A positive integer was expected.", ue.getMessage());
			assertEquals(UWSException.BAD_REQUEST, ue.getHttpErrorCode());
		}
		try{
			request.clearParams();
			request.addParams("LAST", "foo");
			filter = new JobListRefiner(request);
			fail("The provided parameter LAST is NOT an integer. An error should have occurred.");
		}catch(UWSException ue){
			assertEquals("Incorrect LAST value: \"foo\"! A positive integer was expected.", ue.getMessage());
			assertEquals(UWSException.BAD_REQUEST, ue.getHttpErrorCode());
		}

		try{
			// CORRECT 1 LAST PARAMETER => topSize, sortComp and reverseOrder should be set!
			request.clearParams();
			request.addParams("LAST", "10");
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(10, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNotNull(filter.sortComp);
			assertEquals(JobListRefiner.JobComparator.class, filter.sortComp.getClass());

			// SPECIAL CASE OF LAST=0 => same behavior as for a positive value
			request.clearParams();
			request.addParams("LAST", "0");
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(0, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNotNull(filter.sortComp);
			assertEquals(JobListRefiner.JobComparator.class, filter.sortComp.getClass());

			// CORRECT 3 LAST PARAMETERS => Only the smallest value should be kept ; a StartedFilter should be set, as well as topSize, sortComp and reverseOrder!
			request.clearParams();
			request.addParams("LAST", "10");
			request.addParams("last", "5");
			request.addParams("LAST", "7");
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(NoArchivedFilter.class, filter.filters.get(0).getClass());
			assertEquals(5, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNotNull(filter.sortComp);
			assertEquals(JobListRefiner.JobComparator.class, filter.sortComp.getClass());
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("No error should happen since all provided LAST parameters are correct.");
		}

		/* ************* */
		/* FILTER: PHASE */

		// 1 INCORRECT PHASE PARAMETER => a NoArchivedFilter should be set!
		try{
			request.clearParams();
			request.addParams("PHASE", "foo");
			filter = new JobListRefiner(request);
			fail("The provided parameter PHASE is NOT a valid execution phase. An error should have occurred.");
		}catch(UWSException ue){
			assertEquals("Incorrect PHASE value: \"foo\"! No such execution phase is known by this service.", ue.getMessage());
			assertEquals(UWSException.BAD_REQUEST, ue.getHttpErrorCode());
		}

		// CORRECT 1 CORRECT + 1 INCORRECT PHASE PARAMETER => a PhasesFilter with only the correct phase should be set!
		try{
			request.clearParams();
			request.addParams("PHASE", "foo");
			request.addParams("phase", "EXECUTING");
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(PhasesFilter.class, filter.filters.get(0).getClass());
			assertEquals(1, ((PhasesFilter)filter.filters.get(0)).phases.size());
			assertEquals(ExecutionPhase.EXECUTING, ((PhasesFilter)filter.filters.get(0)).phases.get(0));
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);
		}catch(UWSException ue){
			assertEquals("Incorrect PHASE value: \"foo\"! No such execution phase is known by this service.", ue.getMessage());
			assertEquals(UWSException.BAD_REQUEST, ue.getHttpErrorCode());
		}

		try{
			// CORRECT PHASE PARAMETER => a PhasesFilter should be set!
			request.clearParams();
			request.addParams("PHASE", "EXECUTING");
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(PhasesFilter.class, filter.filters.get(0).getClass());
			assertEquals(1, ((PhasesFilter)filter.filters.get(0)).phases.size());
			assertEquals(ExecutionPhase.EXECUTING, ((PhasesFilter)filter.filters.get(0)).phases.get(0));
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);

			// CORRECT 2 CORRECT PHASE PARAMETERS => a PhasesFilter with the two correct phases should be set!
			request.clearParams();
			request.addParams("PHASE", "QUEUED");
			request.addParams("phase", "EXECUTING");
			filter = new JobListRefiner(request);
			assertEquals(1, filter.filters.size());
			assertEquals(PhasesFilter.class, filter.filters.get(0).getClass());
			assertEquals(2, ((PhasesFilter)filter.filters.get(0)).phases.size());
			assertEquals(ExecutionPhase.QUEUED, ((PhasesFilter)filter.filters.get(0)).phases.get(0));
			assertEquals(ExecutionPhase.EXECUTING, ((PhasesFilter)filter.filters.get(0)).phases.get(1));
			assertEquals(-1, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNull(filter.sortComp);
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("No error should happen since all provided PHASE parameters are correct.");
		}

		/* *********** */
		/* FILTER: ALL */

		// ALL MIXED PARAMETERS
		try{
			request.clearParams();
			request.addParams("last", "5");
			request.addParams("phase", "EXECUTING");
			request.addParams("AFTER", "2015-02-10T12:00:00");
			request.addParams("AFTER", "2013-01-10T12:00:00");
			filter = new JobListRefiner(request);
			assertEquals(2, filter.filters.size());
			assertEquals(PhasesFilter.class, filter.filters.get(0).getClass());
			assertEquals(1, ((PhasesFilter)filter.filters.get(0)).phases.size());
			assertEquals(ExecutionPhase.EXECUTING, ((PhasesFilter)filter.filters.get(0)).phases.get(0));
			assertEquals(AfterFilter.class, filter.filters.get(1).getClass());
			assertEquals("2015-02-10T12:00:00Z", ISO8601Format.format(((AfterFilter)filter.filters.get(1)).getDate()));
			assertEquals(5, filter.topSize);
			assertFalse(filter.reverseOrder);
			assertNotNull(filter.sortComp);
		}catch(UWSException ue){
			ue.printStackTrace(System.err);
			fail("No error should happen since all provided PHASE, LAST and AFTER parameters are correct.");
		}
	}

	@Test
	public void testFilter(){
		ArrayList<UWSJob> jobs = new ArrayList<UWSJob>(10);
		try{
			// 0 -> PENDING
			UWSJob test = new UWSJob("0", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null);
			jobs.add(test);
			// 1 -> QUEUED
			test = new UWSJob("1", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null);
			test.setPhase(ExecutionPhase.QUEUED, true);
			jobs.add(test);
			// 2 -> ABORTED
			test = new UWSJob("2", (new GregorianCalendar(2010, 2, 1)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2010, 2, 1)).getTimeInMillis(), -1, null, null);
			test.setPhase(ExecutionPhase.ABORTED, true);
			jobs.add(test);
			// 3 -> ARCHIVED
			test = new UWSJob("3", (new GregorianCalendar(2010, 1, 12)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2010, 1, 12)).getTimeInMillis(), (new GregorianCalendar(2010, 1, 13)).getTimeInMillis(), null, null);
			test.setPhase(ExecutionPhase.ARCHIVED, true);
			jobs.add(test);
			// 4 -> EXECUTING
			test = new UWSJob("4", (new GregorianCalendar(2015, 2, 2)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2015, 2, 2)).getTimeInMillis(), -1, null, null);
			test.setPhase(ExecutionPhase.EXECUTING, true);
			jobs.add(test);
			// 5 -> EXECUTING
			test = new UWSJob("5", (new GregorianCalendar(2014, 8, 2)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2014, 8, 2)).getTimeInMillis(), -1, null, null);
			test.setPhase(ExecutionPhase.EXECUTING, true);
			jobs.add(test);
			// 6 -> ERROR
			test = new UWSJob("6", (new GregorianCalendar(2015, 3, 3)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2015, 3, 3)).getTimeInMillis(), (new GregorianCalendar(2015, 9, 4)).getTimeInMillis(), null, new ErrorSummary("", ErrorType.FATAL));
			test.setPhase(ExecutionPhase.ERROR, true);
			jobs.add(test);
			// 7 -> ERROR
			test = new UWSJob("7", (new GregorianCalendar(2015, 8, 3)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2015, 8, 3)).getTimeInMillis(), (new GregorianCalendar(2015, 9, 4)).getTimeInMillis(), null, new ErrorSummary("", ErrorType.FATAL));
			test.setPhase(ExecutionPhase.EXECUTING, true);
			jobs.add(test);
			// 8 -> ERROR
			test = new UWSJob("8", (new GregorianCalendar(2015, 2, 3)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2015, 2, 3)).getTimeInMillis(), (new GregorianCalendar(2015, 9, 4)).getTimeInMillis(), null, new ErrorSummary("", ErrorType.FATAL));
			test.setPhase(ExecutionPhase.EXECUTING, true);
			jobs.add(test);
			// 9 -> ERROR
			test = new UWSJob("9", (new GregorianCalendar(2015, 1, 3)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2015, 1, 3)).getTimeInMillis(), (new GregorianCalendar(2015, 9, 4)).getTimeInMillis(), null, new ErrorSummary("", ErrorType.FATAL));
			test.setPhase(ExecutionPhase.EXECUTING, true);
			jobs.add(test);
		}catch(UWSException ex){
			ex.printStackTrace();
			fail("Can not force the execution phase of the job! (see console for more details)");
		}

		/* ****************************** */
		/* No filter, no sort, no reverse */
		JobListRefiner filter = new JobListRefiner();

		Iterator<UWSJob> it = filter.refine(jobs.iterator());
		for(int i = 0; i < 10; i++){
			assertTrue(it.hasNext());
			assertEquals("" + i, it.next().getJobId());
		}
		assertFalse(it.hasNext());

		/* ************************************** */
		/* 1 PHASE filter, no sort, no reverse */
		PhasesFilter pFilter = new PhasesFilter(ExecutionPhase.EXECUTING);
		pFilter.add(ExecutionPhase.ARCHIVED);
		filter.filters.add(pFilter);

		it = filter.refine(jobs.iterator());
		assertTrue(it.hasNext());
		assertEquals("3", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("5", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("7", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("8", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("9", it.next().getJobId());
		assertFalse(it.hasNext());

		/* **************************************************** */
		/* 1 PHASE filter + 1 AFTER filter, no sort, no reverse */
		filter.filters.add(new AfterFilter((new GregorianCalendar(2015, 1, 1)).getTime()));

		it = filter.refine(jobs.iterator());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("7", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("8", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("9", it.next().getJobId());
		assertFalse(it.hasNext());

		/* ***************************************************************** */
		/* 1 PHASE filter + 1 AFTER filter, 3 first jobs, no sort, no reverse */
		filter.jobList.clear();
		filter.topSize = 3;

		it = filter.refine(jobs.iterator());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("7", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("8", it.next().getJobId());
		assertFalse(it.hasNext());

		/* ************************************************************************ */
		/* 1 PHASE filter + 1 AFTER filter, all jobs, sort by startTime, no reverse */
		filter.topSize = -1;
		filter.sortComp = new Comparator<UWSJob>(){
			@Override
			public int compare(UWSJob o1, UWSJob o2){
				return o1.getStartTime().compareTo(o2.getStartTime());
			}
		};

		it = filter.refine(jobs.iterator());
		assertTrue(it.hasNext());
		assertEquals("9", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("8", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("7", it.next().getJobId());
		assertFalse(it.hasNext());

		/* ********************************************************************************** */
		/* 1 PHASE filter + 1 AFTER filter, the 4 first jobs, sort by startTime, WITH reverse */
		filter.topSize = 4;
		filter.sortComp = new Comparator<UWSJob>(){
			@Override
			public int compare(UWSJob o1, UWSJob o2){
				return o1.getStartTime().compareTo(o2.getStartTime());
			}
		};
		filter.reverseOrder = true;

		it = filter.refine(jobs.iterator());
		assertTrue(it.hasNext());
		assertEquals("7", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("8", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("9", it.next().getJobId());
		assertFalse(it.hasNext());

		/* ************************************************************************************************************ */
		/* 1 PHASE filter + 1 AFTER filter, 3 last jobs, sort by descending creationTime => simulation of LAST "filter" */
		filter.topSize = 3;
		filter.sortComp = new Comparator<UWSJob>(){
			@Override
			public int compare(UWSJob o1, UWSJob o2){
				return -(o1.getCreationTime().compareTo(o2.getCreationTime()));
			}
		};
		filter.reverseOrder = false;

		it = filter.refine(jobs.iterator());
		assertTrue(it.hasNext());
		assertEquals("7", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("8", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertFalse(it.hasNext());

		/* ***************************************************************************************************************** */
		/* No filter (except to avoid ARCHIVED), 3 last jobs, sort by descending creationTime => simulation of LAST "filter" */
		filter.filters.clear();
		filter.filters.add(new NoArchivedFilter());
		filter.topSize = 3;
		filter.sortComp = new Comparator<UWSJob>(){
			@Override
			public int compare(UWSJob o1, UWSJob o2){
				return -(o1.getCreationTime().compareTo(o2.getCreationTime()));
			}
		};
		filter.reverseOrder = false;

		it = filter.refine(jobs.iterator());
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("0", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("7", it.next().getJobId());
		assertFalse(it.hasNext());

		/* *********************************************************************************************************************************************** */
		/* No filter (except to avoid ARCHIVED), 0 last jobs, sort by descending creationTime => simulation of LAST "filter" WITH THE SPECIAL VALUE LAST=0 */
		filter.filters.clear();
		filter.filters.add(new NoArchivedFilter());
		filter.topSize = 0;
		filter.sortComp = new Comparator<UWSJob>(){
			@Override
			public int compare(UWSJob o1, UWSJob o2){
				return -(o1.getCreationTime().compareTo(o2.getCreationTime()));
			}
		};
		filter.reverseOrder = false;

		it = filter.refine(jobs.iterator());
		assertFalse(it.hasNext());
	}

	@Test
	public void testAddJob(){
		JobListRefiner filter = new JobListRefiner();
		filter.sortComp = new Comparator<UWSJob>(){
			@Override
			public int compare(UWSJob o1, UWSJob o2){
				return o1.getCreationTime().compareTo(o2.getCreationTime());
			}
		};

		filter.addJob(new UWSJob("123456", (new GregorianCalendar(2013, 3, 10)).getTimeInMillis(), null, new UWSParameters(), -1, -1, -1, null, null));
		filter.addJob(new UWSJob("654321", (new GregorianCalendar(2010, 3, 10)).getTimeInMillis(), null, new UWSParameters(), -1, -1, -1, null, null));

		assertEquals("654321", filter.jobList.get(0).getJobId());
		assertEquals("123456", filter.jobList.get(1).getJobId());
	}

	@Test
	public void testMatch(){
		JobListRefiner filter = new JobListRefiner();

		/* *********** */
		/* No filter */

		// No job => Nope!
		assertFalse(filter.match(null));
		// A job => Yes!
		assertTrue(filter.match(new UWSJob(new UWSParameters())));

		/* ********************************************** */
		/* Only jobs in EXECUTING phase */
		filter.filters.add(new PhasesFilter(ExecutionPhase.EXECUTING));

		// No job => Nope!
		assertFalse(filter.match(null));
		// Not an EXECUTING job => Nope!
		assertFalse(filter.match(new UWSJob(new UWSParameters())));

		UWSJob testJob = new UWSJob("123456", (new Date()).getTime(), null, new UWSParameters(), -1, (new Date()).getTime(), -1, null, null);
		try{
			// EXECUTING job => OK!
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertTrue(filter.match(testJob));

			// ARCHIVED job => Nope!
			testJob.setPhase(ExecutionPhase.ARCHIVED, true);
			assertFalse(filter.match(testJob));

			// ERROR job => Nope!
			testJob.setPhase(ExecutionPhase.ERROR, true);
			assertFalse(filter.match(testJob));
		}catch(UWSException ex){
			ex.printStackTrace();
			fail("Can not force the execution phase of the job! (see console for more details)");
		}

		/* ********************************************************** */
		/* Only jobs in EXECUTING or ARCHIVED phase */
		((PhasesFilter)filter.filters.get(filter.filters.size() - 1)).add(ExecutionPhase.ARCHIVED);

		// No job => Nope!
		assertFalse(filter.match(null));
		// Not started job => Nope!
		assertFalse(filter.match(new UWSJob(new UWSParameters())));

		testJob = new UWSJob("123456", (new Date()).getTime(), null, new UWSParameters(), -1, (new Date()).getTime(), -1, null, null);
		try{
			// EXECUTING job => OK!
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertTrue(filter.match(testJob));

			// ARCHIVED job => OK!
			testJob.setPhase(ExecutionPhase.ARCHIVED, true);
			assertTrue(filter.match(testJob));

			// ERROR job => Nope!
			testJob.setPhase(ExecutionPhase.ERROR, true);
			assertFalse(filter.match(testJob));
		}catch(UWSException ex){
			ex.printStackTrace();
			fail("Can not force the execution phase of the job! (see console for more details)");
		}

		/* ***************************************************************************** */
		/* Only jobs in EXECUTING or ARCHIVED phase AND after 1/3/2010 */
		filter.filters.add(new AfterFilter((new GregorianCalendar(2010, 2, 1)).getTime()));

		// No job => Nope!
		assertFalse(filter.match(null));
		// Not started job => Nope!
		assertFalse(filter.match(new UWSJob(new UWSParameters())));

		testJob = new UWSJob("123456", (new GregorianCalendar(2010, 2, 2)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2010, 2, 2)).getTimeInMillis(), -1, null, null);
		try{
			// EXECUTING job, and after 1/3/2010 => OK!
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertTrue(filter.match(testJob));

			// ARCHIVED job, and after 1/3/2010 => OK!
			testJob.setPhase(ExecutionPhase.ARCHIVED, true);
			assertTrue(filter.match(testJob));

			// ERROR job, and after 1/3/2010 => Nope!
			testJob.setPhase(ExecutionPhase.ERROR, true);
			assertFalse(filter.match(testJob));
		}catch(UWSException ex){
			ex.printStackTrace();
			fail("Can not force the execution phase of the job! (see console for more details)");
		}

		testJob = new UWSJob("123456", (new GregorianCalendar(2010, 1, 1)).getTimeInMillis(), null, new UWSParameters(), -1, (new GregorianCalendar(2010, 1, 1)).getTimeInMillis(), -1, null, null);
		try{
			// EXECUTING job, and before 1/3/2010 => Nope!
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertFalse(filter.match(testJob));

			// ARCHIVED job, and before 1/3/2010 => Nope!
			testJob.setPhase(ExecutionPhase.ARCHIVED, true);
			assertFalse(filter.match(testJob));

			// ERROR job, and before 1/3/2010 => Nope!
			testJob.setPhase(ExecutionPhase.ERROR, true);
			assertFalse(filter.match(testJob));
		}catch(UWSException ex){
			ex.printStackTrace();
			fail("Can not force the execution phase of the job! (see console for more details)");
		}
	}

	@Test
	public void testTopIterator(){
		ArrayList<UWSJob> jobs = new ArrayList<UWSJob>(5);
		jobs.add(new UWSJob("0", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null));
		jobs.add(new UWSJob("1", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null));
		jobs.add(new UWSJob("2", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null));
		jobs.add(new UWSJob("3", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null));
		jobs.add(new UWSJob("4", (new Date()).getTime(), null, new UWSParameters(), -1, -1, -1, null, null));

		// Just the 3 first items in same order:
		TopIterator it = new TopIterator(jobs, 3, false);
		assertTrue(it.hasNext());
		assertEquals("0", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("2", it.next().getJobId());
		assertFalse(it.hasNext());

		// Just the 3 first items in reverse order:
		it = new TopIterator(jobs, 3, true);
		assertTrue(it.hasNext());
		assertEquals("2", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("0", it.next().getJobId());
		assertFalse(it.hasNext());

		// All items in same order:
		it = new TopIterator(jobs, -1, false);
		assertTrue(it.hasNext());
		assertEquals("0", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("2", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("3", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertFalse(it.hasNext());

		// All items in reverse order:
		it = new TopIterator(jobs, -1, true);
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("3", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("2", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("0", it.next().getJobId());
		assertFalse(it.hasNext());

		// More items than in the list in same order:
		it = new TopIterator(jobs, 10, false);
		assertTrue(it.hasNext());
		assertEquals("0", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("2", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("3", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertFalse(it.hasNext());

		// More items than in the list in reverse order:
		it = new TopIterator(jobs, 10, true);
		assertTrue(it.hasNext());
		assertEquals("4", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("3", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("2", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getJobId());
		assertTrue(it.hasNext());
		assertEquals("0", it.next().getJobId());
		assertFalse(it.hasNext());
	}

	protected final static class TestHttpServletRequest implements HttpServletRequest {

		private HashMap<String,String[]> parameters = new HashMap<String,String[]>();

		private static class NamesEnumeration implements Enumeration<String> {

			private final Iterator<String> it;

			public NamesEnumeration(final Set<String> names){
				this.it = names.iterator();
			}

			@Override
			public boolean hasMoreElements(){
				return it.hasNext();
			}

			@Override
			public String nextElement(){
				return it.next();
			}

		}

		public void addParams(final String name, final String value){
			if (parameters.containsKey(name)){
				String[] values = parameters.get(name);
				values = Arrays.copyOf(values, values.length + 1);
				values[values.length - 1] = value;
				parameters.put(name, values);
			}else
				parameters.put(name, new String[]{value});
		}

		public void clearParams(){
			parameters.clear();
		}

		@Override
		public Enumeration<String> getParameterNames(){
			return new NamesEnumeration(parameters.keySet());
		}

		@Override
		public String[] getParameterValues(String name){
			return parameters.get(name);
		}

		@Override
		public Map<String,String[]> getParameterMap(){
			return parameters;
		}

		@Override
		public String getParameter(String name){
			String[] values = parameters.get(name);
			if (values == null || values.length == 0)
				return null;
			else
				return values[0];
		}

		@Override
		public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1){
			return null;
		}

		@Override
		public AsyncContext startAsync(){
			return null;
		}

		@Override
		public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException{

		}

		@Override
		public void setAttribute(String arg0, Object arg1){

		}

		@Override
		public void removeAttribute(String arg0){

		}

		@Override
		public boolean isSecure(){
			return false;
		}

		@Override
		public boolean isAsyncSupported(){
			return false;
		}

		@Override
		public boolean isAsyncStarted(){
			return false;
		}

		@Override
		public ServletContext getServletContext(){
			return null;
		}

		@Override
		public int getServerPort(){
			return 0;
		}

		@Override
		public String getServerName(){
			return null;
		}

		@Override
		public String getScheme(){
			return null;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String arg0){
			return null;
		}

		@Override
		public int getRemotePort(){
			return 0;
		}

		@Override
		public String getRemoteHost(){
			return null;
		}

		@Override
		public String getRemoteAddr(){
			return null;
		}

		@Override
		public String getRealPath(String arg0){
			return null;
		}

		@Override
		public BufferedReader getReader() throws IOException{
			return null;
		}

		@Override
		public String getProtocol(){
			return null;
		}

		@Override
		public Enumeration<Locale> getLocales(){
			return null;
		}

		@Override
		public Locale getLocale(){
			return null;
		}

		@Override
		public int getLocalPort(){
			return 0;
		}

		@Override
		public String getLocalName(){
			return null;
		}

		@Override
		public String getLocalAddr(){
			return null;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException{
			return null;
		}

		@Override
		public DispatcherType getDispatcherType(){
			return null;
		}

		@Override
		public String getContentType(){
			return null;
		}

		@Override
		public int getContentLength(){
			return 0;
		}

		@Override
		public String getCharacterEncoding(){
			return null;
		}

		@Override
		public Enumeration<String> getAttributeNames(){
			return null;
		}

		@Override
		public Object getAttribute(String arg0){
			return null;
		}

		@Override
		public AsyncContext getAsyncContext(){
			return null;
		}

		@Override
		public void logout() throws ServletException{}

		@Override
		public void login(String arg0, String arg1) throws ServletException{}

		@Override
		public boolean isUserInRole(String arg0){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdValid(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromUrl(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromURL(){
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromCookie(){
			return false;
		}

		@Override
		public Principal getUserPrincipal(){
			return null;
		}

		@Override
		public HttpSession getSession(boolean arg0){
			return null;
		}

		@Override
		public HttpSession getSession(){
			return null;
		}

		@Override
		public String getServletPath(){
			return null;
		}

		@Override
		public String getRequestedSessionId(){
			return null;
		}

		@Override
		public StringBuffer getRequestURL(){
			return null;
		}

		@Override
		public String getRequestURI(){
			return null;
		}

		@Override
		public String getRemoteUser(){
			return null;
		}

		@Override
		public String getQueryString(){
			return null;
		}

		@Override
		public String getPathTranslated(){
			return null;
		}

		@Override
		public String getPathInfo(){
			return null;
		}

		@Override
		public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException{
			return null;
		}

		@Override
		public Part getPart(String arg0) throws IOException, IllegalStateException, ServletException{
			return null;
		}

		@Override
		public String getMethod(){
			return "GET";
		}

		@Override
		public int getIntHeader(String arg0){
			return 0;
		}

		@Override
		public Enumeration<String> getHeaders(String arg0){
			return null;
		}

		@Override
		public Enumeration<String> getHeaderNames(){
			return null;
		}

		@Override
		public String getHeader(String arg0){
			return null;
		}

		@Override
		public long getDateHeader(String arg0){
			return 0;
		}

		@Override
		public Cookie[] getCookies(){
			return null;
		}

		@Override
		public String getContextPath(){
			return null;
		}

		@Override
		public String getAuthType(){
			return null;
		}

		@Override
		public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException{
			return false;
		}

	}

}