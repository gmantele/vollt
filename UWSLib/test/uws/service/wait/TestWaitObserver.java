package uws.service.wait;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import uws.UWSException;
import uws.job.ExecutionPhase;

public class TestWaitObserver {

	@Before
	public void setUp() throws Exception{}

	protected final void waitALittle(){
		synchronized(this){
			try{
				Thread.sleep(10);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testUpdate(){
		Thread thread = new TestThread();
		WaitObserver obs = new WaitObserver(thread);

		thread.start();

		try{
			// No phase => Thread still blocked!
			obs.update(null, null, null);
			waitALittle();
			assertTrue(thread.isAlive());
			obs.update(null, ExecutionPhase.PENDING, null);
			waitALittle();
			assertTrue(thread.isAlive());
			obs.update(null, null, ExecutionPhase.PENDING);
			waitALittle();
			assertTrue(thread.isAlive());

			// Same phase => Thread still blocked!
			obs.update(null, ExecutionPhase.PENDING, ExecutionPhase.PENDING);
			waitALittle();
			assertTrue(thread.isAlive());

			// Different phase => Thread UNblocked!
			obs.update(null, ExecutionPhase.PENDING, ExecutionPhase.EXECUTING);
			waitALittle();
			assertFalse(thread.isAlive());
		}catch(UWSException e){
			e.printStackTrace();
			fail("No error should have happened while calling WaitObserver.update()!");
		}

	}

	protected final static class TestThread extends Thread {
		@Override
		public void run(){
			synchronized(this){
				try{
					wait();
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}

	}

}