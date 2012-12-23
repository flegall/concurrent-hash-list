package lobstre.chlist;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TestMultiThreadLockFreeLinkedList {

	private static final int LOOPS = 50;
	private static final int SIZE = 1000;

	@Test
	public void testIndependentUpdates2 () {
		testIndependentUpdates (2);
	}
	
	@Test
	public void testIndependentUpdates8 () {
		testIndependentUpdates (8);
	}
	
	@Test
	public void testIndependentUpdates37 () {
		testIndependentUpdates (37);
	}

	private void testIndependentUpdates (final int nThreads) {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		final ExecutorService es = Executors.newFixedThreadPool (nThreads);
		for (int i = 0; i < nThreads; i++) {
			final int id = i;
			es.submit (new Runnable () {
				@Override
				public void run () {
					for (int i = 0; i < LOOPS; i++) {
						for (int j = 0; j < SIZE; j++) {
							if (j % nThreads == id) {
								assertNotNull (ll.insert (
										j, Integer.valueOf (j)));
							}
						}
						for (int j = 0; j < SIZE; j++) {
							if (j % nThreads == id) {
								assertNotNull (ll.delete (j));
							}
						}
					}
				}
			});
		}
		es.shutdown ();
		try {
			es.awaitTermination (3600L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.interrupted ();
		}
		
		for (int j = 0; j < SIZE; j++) {
			assertNull (ll.search (j));
		}
	}
}
