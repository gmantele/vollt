package uws.service.wait;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;

public class TestLimitedBlockingPolicy {

	@Before
	public void setUp() throws Exception{}

	@Test
	public void testLimitedBlockingPolicy(){
		LimitedBlockingPolicy policy = new LimitedBlockingPolicy();
		assertEquals(LimitedBlockingPolicy.DEFAULT_TIMEOUT, policy.timeout);
	}

	@Test
	public void testLimitedBlockingPolicyLong(){
		// Negative time => DEFAULT_TIMEOUT
		LimitedBlockingPolicy policy = new LimitedBlockingPolicy(-1);
		assertEquals(LimitedBlockingPolicy.DEFAULT_TIMEOUT, policy.timeout);

		// Null time => 0 (meaning no blocking)
		policy = new LimitedBlockingPolicy(0);
		assertEquals(0, policy.timeout);

		// A time LESS THAN the default one => the given time
		policy = new LimitedBlockingPolicy(10);
		assertEquals(10, policy.timeout);

		// A time GREATER THAN the default one => the given time
		policy = new LimitedBlockingPolicy(100);
		assertEquals(100, policy.timeout);
	}

	@Test
	public void testBlock(){
		LimitedBlockingPolicy policy = new LimitedBlockingPolicy();
		Thread thread = new Thread("1");
		UWSJob testJob = new UWSJob(new UWSParameters());

		// Nothing should happen if no job and/or thread:
		assertEquals(0, policy.block(null, 10, null, null, null));
		assertEquals(0, policy.block(thread, 10, null, null, null));
		assertEquals(0, policy.block(null, 10, testJob, null, null));

		// If no time is specified by the user (i.e. 0), 0 is set (meaning no blocking):
		assertEquals(0, policy.block(thread, 0, testJob, null, null));

		// If a negative time is specified by the user (meaning an unlimited waiting time), the default time is set:
		assertEquals(policy.timeout, policy.block(thread, -1, testJob, null, null));

		// If a positive time is specified by the user BUT LESS THAN the time set in the policy, the user time is set:
		long userTime = policy.timeout - 10;
		assertEquals(userTime, policy.block(thread, userTime, testJob, null, null));

		// If a positive time is specified by the user BUT LESS THAN the time set in the policy, the user time is set:
		userTime = policy.timeout + 10;
		assertEquals(policy.timeout, policy.block(thread, userTime, testJob, null, null));
	}

}