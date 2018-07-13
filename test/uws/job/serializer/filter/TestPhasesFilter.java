package uws.job.serializer.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import uws.UWSException;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;

public class TestPhasesFilter {

	@Test
	public void testMatch(){
		PhasesFilter filter = new PhasesFilter(ExecutionPhase.EXECUTING);

		/* FILTER WITH ONLY ONE PHASE: EXECUTING */

		// No job => Nope!
		assertFalse(filter.match(null));

		// Job PENDING => Nope!
		UWSJob testJob = new UWSJob(new UWSParameters());
		assertFalse(filter.match(testJob));

		// Job EXECUTING => OK!
		try{
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertTrue(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		/* FILTER WITH TWO PHASES: QUEUED & EXECUTING */
		filter.add(ExecutionPhase.QUEUED);

		// Job PENDING => Nope!
		testJob = new UWSJob(new UWSParameters());
		assertFalse(filter.match(testJob));

		// Job QUEUED => OK!
		try{
			testJob.setPhase(ExecutionPhase.QUEUED, true);
			assertTrue(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		// Job EXECUTING => OK!
		try{
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertTrue(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		// Job ARCHIVED => Nope!
		try{
			testJob.setPhase(ExecutionPhase.ARCHIVED, true);
			assertFalse(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		// Job ABORTED => Nope!
		try{
			testJob.setPhase(ExecutionPhase.ABORTED, true);
			assertFalse(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		/* FILTER WITH THREE PHASES: QUEUED & EXECUTING & ARCHIVED */
		filter.add(ExecutionPhase.ARCHIVED);

		// Job PENDING => Nope!
		testJob = new UWSJob(new UWSParameters());
		assertFalse(filter.match(testJob));

		// Job QUEUED => OK!
		try{
			testJob.setPhase(ExecutionPhase.QUEUED, true);
			assertTrue(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		// Job EXECUTING => OK!
		try{
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertTrue(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		// Job ARCHIVED => OK!
		try{
			testJob.setPhase(ExecutionPhase.ARCHIVED, true);
			assertTrue(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}

		// Job ABORTED => Nope!
		try{
			testJob.setPhase(ExecutionPhase.ABORTED, true);
			assertFalse(filter.match(testJob));
		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}
	}

}