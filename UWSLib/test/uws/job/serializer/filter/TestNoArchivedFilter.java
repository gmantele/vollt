package uws.job.serializer.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import uws.UWSException;
import uws.job.ExecutionPhase;
import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;

public class TestNoArchivedFilter {

	@Test
	public void testMatch(){
		NoArchivedFilter filter = new NoArchivedFilter();

		// No job => Nope!
		assertFalse(filter.match(null));

		UWSJob testJob = new UWSJob(new UWSParameters());

		// Job PENDING => OK!
		assertTrue(filter.match(testJob));

		try{
			// Job QUEUED => OK!
			testJob.setPhase(ExecutionPhase.QUEUED, true);
			assertTrue(filter.match(testJob));

			// Job HELD => OK!
			testJob.setPhase(ExecutionPhase.HELD, true);
			assertTrue(filter.match(testJob));

			// Job SUSPENDED => OK!
			testJob.setPhase(ExecutionPhase.SUSPENDED, true);
			assertTrue(filter.match(testJob));

			// Job EXECUTING => OK!
			testJob.setPhase(ExecutionPhase.EXECUTING, true);
			assertTrue(filter.match(testJob));

			// Job ERROR => OK!
			testJob.setPhase(ExecutionPhase.ERROR, true);
			assertTrue(filter.match(testJob));

			// Job ABORTED => OK!
			testJob.setPhase(ExecutionPhase.ABORTED, true);
			assertTrue(filter.match(testJob));

			// Job COMPLETED => OK!
			testJob.setPhase(ExecutionPhase.COMPLETED, true);
			assertTrue(filter.match(testJob));

			// Job UNKNOWN => OK!
			testJob.setPhase(ExecutionPhase.UNKNOWN, true);
			assertTrue(filter.match(testJob));

			// Job ARCHIVED => Nope!
			testJob.setPhase(ExecutionPhase.ARCHIVED, true);
			assertFalse(filter.match(testJob));

		}catch(UWSException e){
			e.printStackTrace();
			fail("Impossible to change the phase! (see console for more details)");
		}
	}

}