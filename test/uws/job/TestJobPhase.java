package uws.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import uws.UWSException;

public class TestJobPhase {

	@Test
	public void testSetPhase(){
		/*
		 * Here, we just want to ensure that all Execution Phases are supported.
		 * So, we just test to test the phase with force=true.
		 * Tests with force=false, have to be done individually for each
		 * phase ; hence the other JUnit test functions of this JUnit test case.
		 */
		try{
			JobPhase jobPhase = (new UWSJob(null)).getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){
				jobPhase.setPhase(phase, true);
				assertEquals(phase, jobPhase.getPhase());
			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetPendingPhase(){
		/*
		 * The phase PENDING can be NEVER be set coming from another different
		 * (normal) phase.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into PENDING:
				jobPhase.setPhase(phase, true);
				// try to set PENDING:
				switch(phase){
					case PENDING:
					case UNKNOWN:
						try{
							jobPhase.setPhase(ExecutionPhase.PENDING, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the PENDING phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.PENDING, false);
							fail("It is not allowed to go from " + phase + " to PENDING.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to PENDING.", e.getMessage());
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into PENDING:
				jobPhase.setPhase(phase, true);
				// try to set PENDING:
				try{
					jobPhase.setPhase(ExecutionPhase.PENDING, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the PENDING phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetQueuedPhase(){
		/*
		 * The phase QUEUED can be set only if the current phase is
		 * HELD or PENDING.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into QUEUED:
				jobPhase.setPhase(phase, true);
				// try to set QUEUED:
				switch(phase){
					case QUEUED:
					case HELD:
					case PENDING:
					case UNKNOWN:
						try{
							jobPhase.setPhase(ExecutionPhase.QUEUED, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the QUEUED phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.QUEUED, false);
							fail("It is not allowed to go from " + phase + " to QUEUED.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to QUEUED.", e.getMessage());
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into QUEUED:
				jobPhase.setPhase(phase, true);
				// try to set QUEUED:
				try{
					jobPhase.setPhase(ExecutionPhase.QUEUED, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the QUEUED phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetExecutingPhase(){
		/*
		 * The phase EXECUTING can be set only if the current phase is
		 * HELD, SUSPENDED or QUEUED.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into EXECUTING:
				jobPhase.setPhase(phase, true);
				// try to set EXECUTING:
				switch(phase){
					case EXECUTING:
					case HELD:
					case SUSPENDED:
					case QUEUED:
					case UNKNOWN:
						try{
							jobPhase.setPhase(ExecutionPhase.EXECUTING, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the EXECUTING phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.EXECUTING, false);
							fail("It is not allowed to go from " + phase + " to EXECUTING.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to EXECUTING.", e.getMessage());
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into EXECUTING:
				jobPhase.setPhase(phase, true);
				// try to set EXECUTING:
				try{
					jobPhase.setPhase(ExecutionPhase.EXECUTING, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the EXECUTING phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetCompletedPhase(){
		/*
		 * The phase COMPLETED can be set only if the current phase is
		 * EXECUTING.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into COMPLETED:
				jobPhase.setPhase(phase, true);
				// try to set COMPLETED:
				switch(phase){
					case COMPLETED:
					case EXECUTING:
					case UNKNOWN:
						try{
							jobPhase.setPhase(ExecutionPhase.COMPLETED, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the COMPLETED phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.COMPLETED, false);
							fail("It is not allowed to go from " + phase + " to COMPLETED.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to COMPLETED.", e.getMessage());
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into COMPLETED:
				jobPhase.setPhase(phase, true);
				// try to set COMPLETED:
				try{
					jobPhase.setPhase(ExecutionPhase.COMPLETED, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the COMPLETED phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetAbortedPhase(){
		/*
		 * The phase ABORTED can be set if the current phase is none of the
		 * following phases: COMPLETED, ERROR and ARCHIVED.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into ABORTED:
				jobPhase.setPhase(phase, true);
				// try to set ABORTED:
				switch(phase){
					case COMPLETED:
					case ERROR:
					case ARCHIVED:
						try{
							jobPhase.setPhase(ExecutionPhase.ABORTED, false);
							fail("It is not allowed to go from " + phase + " to ABORTED.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to ABORTED.", e.getMessage());
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.ABORTED, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the ABORTED phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into ABORTED:
				jobPhase.setPhase(phase, true);
				// try to set ABORTED:
				try{
					jobPhase.setPhase(ExecutionPhase.ABORTED, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the ERROR phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetErrorPhase(){
		/*
		 * The phase ERROR can be set if the current phase is none of the
		 * following phases: COMPLETED, ABORTED and ARCHIVED.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into ERROR:
				jobPhase.setPhase(phase, true);
				// try to set ERROR:
				switch(phase){
					case COMPLETED:
					case ABORTED:
					case ARCHIVED:
						try{
							jobPhase.setPhase(ExecutionPhase.ERROR, false);
							fail("It is not allowed to go from " + phase + " to ERROR.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to ERROR.", e.getMessage());
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.ERROR, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the ERROR phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into ERROR:
				jobPhase.setPhase(phase, true);
				// try to set ERROR:
				try{
					jobPhase.setPhase(ExecutionPhase.ERROR, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the ERROR phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetHeldPhase(){
		/*
		 * The phase HELD can be set only if the current phase is
		 * PENDING or EXECUTING.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into HELD:
				jobPhase.setPhase(phase, true);
				// try to set HELD:
				switch(phase){
					case HELD:
					case PENDING:
					case EXECUTING:
					case UNKNOWN:
						try{
							jobPhase.setPhase(ExecutionPhase.HELD, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the HELD phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.HELD, false);
							fail("It is not allowed to go from " + phase + " to HELD.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to HELD.", e.getMessage());
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into HELD:
				jobPhase.setPhase(phase, true);
				// try to set HELD:
				try{
					jobPhase.setPhase(ExecutionPhase.HELD, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the HELD phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetSuspendedPhase(){
		/*
		 * The phase SUSPENDED can be set only if the current phase is
		 * EXECUTING.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into SUSPENDED:
				jobPhase.setPhase(phase, true);
				// try to set SUSPENDED:
				switch(phase){
					case SUSPENDED:
					case EXECUTING:
					case UNKNOWN:
						try{
							jobPhase.setPhase(ExecutionPhase.SUSPENDED, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the SUSPENDED phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.SUSPENDED, false);
							fail("It is not allowed to go from " + phase + " to SUSPENDED.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to SUSPENDED.", e.getMessage());
						}
						break;
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into SUSPENDED:
				jobPhase.setPhase(phase, true);
				// try to set SUSPENDED:
				try{
					jobPhase.setPhase(ExecutionPhase.SUSPENDED, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the SUSPENDED phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetArchivedPhase(){
		/*
		 * The phase ARCHIVED can be set only if the current phase is one of:
		 * ARCHIVED, COMPLETED, ABORTED and ERROR.
		 */
		try{
			UWSJob job = new UWSJob(null);
			JobPhase jobPhase = job.getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into ARCHIVED:
				jobPhase.setPhase(phase, true);
				// try to set ARCHIVED:
				switch(phase){
					case ARCHIVED:
					case COMPLETED:
					case ABORTED:
					case ERROR:
					case UNKNOWN:
						try{
							jobPhase.setPhase(ExecutionPhase.ARCHIVED, false);
						}catch(Exception e){
							e.printStackTrace();
							fail("Setting the ARCHIVED phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
						}
						break;
					default:
						try{
							jobPhase.setPhase(ExecutionPhase.ARCHIVED, false);
							fail("It is not allowed to go from " + phase + " to ARCHIVED.");
						}catch(Exception e){
							assertEquals(UWSException.class, e.getClass());
							assertEquals("Incorrect phase transition! => the job " + job.getJobId() + " is in the phase " + phase + ". It can not go to ARCHIVED.", e.getMessage());
						}
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into ARCHIVED:
				jobPhase.setPhase(phase, true);
				// try to set ARCHIVED:
				try{
					jobPhase.setPhase(ExecutionPhase.ARCHIVED, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the ARCHIVED phase with force=true from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testSetUnknownPhase(){
		/*
		 * The phase UNKNOWN is not really part of the Execution Phase state
		 * machine declared in the UWS standard. So, an exception can never be
		 * thrown when setting this phase, with force=true, or not.
		 */
		try{
			JobPhase jobPhase = (new UWSJob(null)).getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){

				/* CASE: FORCE=TRUE */
				// set the phase to turn into UNKNOWN:
				jobPhase.setPhase(phase, true);
				// try to set UNKNOWN:
				try{
					jobPhase.setPhase(ExecutionPhase.UNKNOWN, false);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the UNKNOWN phase from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

				/* CASE: FORCE=FALSE */
				// set the phase to turn into UNKNOWN:
				jobPhase.setPhase(phase, true);
				// try to set UNKNOWN:
				try{
					jobPhase.setPhase(ExecutionPhase.UNKNOWN, true);
				}catch(Exception e){
					e.printStackTrace();
					fail("Setting the UNKNOWN phase (especially with force=true) from any other phase (here: " + phase + ") MUST always be possible. See console for more details.");
				}

			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting a phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testIsJobUpdatable(){
		/*
		 * A UWS job can be updated in only one execution phase: PENDING.
		 */
		try{
			JobPhase jobPhase = (new UWSJob(null)).getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){
				// set the phase to test:
				jobPhase.setPhase(phase, true);
				// ensure isJobUpdatable(...) returns TRUE only for PENDING:
				if (phase == ExecutionPhase.PENDING)
					assertTrue(jobPhase.isJobUpdatable());
				else
					assertFalse(jobPhase.isJobUpdatable());
			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting the phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testIsFinished(){
		/*
		 * A UWS job is finished only if in the execution phase COMPLETED,
		 * ABORTED, ERROR or ARCHIVED.
		 */
		try{
			JobPhase jobPhase = (new UWSJob(null)).getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){
				// set the phase to test:
				jobPhase.setPhase(phase, true);
				// ensure isFinished(...) returns TRUE only for COMPLETED,
				// ABORTED, ERROR and ARCHIVED:
				switch(phase){
					case COMPLETED:
					case ABORTED:
					case ERROR:
					case ARCHIVED:
						assertTrue(jobPhase.isFinished());
						break;
					default:
						assertFalse(jobPhase.isFinished());
						break;
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting the phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

	@Test
	public void testIsExecuting(){
		/*
		 * A UWS job is executing if in only one execution phase: EXECUTING.
		 */
		try{
			JobPhase jobPhase = (new UWSJob(null)).getPhaseManager();
			for(ExecutionPhase phase : ExecutionPhase.values()){
				// set the phase to test:
				jobPhase.setPhase(phase, true);
				// ensure isExecuting(...) returns TRUE only for EXECUTING
				// or SUSPENDED:
				switch(phase){
					case EXECUTING:
					case SUSPENDED:
						assertTrue(jobPhase.isExecuting());
						break;
					default:
						assertFalse(jobPhase.isExecuting());
						break;
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
			fail("Setting the phase with force=true must NEVER throw an error. See console for more details.");
		}
	}

}
