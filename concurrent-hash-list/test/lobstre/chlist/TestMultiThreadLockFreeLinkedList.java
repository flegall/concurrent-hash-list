package lobstre.chlist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lobstre.chlist.LockFreeLinkedList.Node;

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
	
	@Test
	public void testConcurrentUpdates2 () {
		testConcurrentUpdates (2);
	}

	@Test
	public void testConcurrentUpdates8 () {
		testConcurrentUpdates (8);
	}
	
	@Test
	public void testConcurrentUpdates37 () {
		testConcurrentUpdates (37);
	}
	
	private void testConcurrentUpdates (final int nThreads) {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		final ExecutorService es = Executors.newFixedThreadPool (nThreads);
		final AtomicInteger counter = new AtomicInteger ();
		for (int i = 0; i < nThreads; i++) {
			es.submit (new Runnable () {
				@Override
				public void run () {
					for (int i = 0 ; i < SIZE * LOOPS; i++) {
						final int index = i % 57;
						if (i % 2 == 0) {
							ll.insert (index, Integer.toString (index));
						} else {
							ll.delete (index);
						}
						ll.search (index);
					}
					counter.incrementAndGet ();
				}
			});
		}
		
		es.shutdown ();
		try {
			es.awaitTermination (3600L, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			Thread.interrupted ();
		}
		
		assertEquals (nThreads, counter.get ());
		
		checkSanity (ll);
	}

	private void testIndependentUpdates (final int nThreads) {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		final ExecutorService es = Executors.newFixedThreadPool (nThreads);
		final AtomicInteger counter = new AtomicInteger ();
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
					counter.incrementAndGet ();
				}
			});
		}
		es.shutdown ();
		try {
			es.awaitTermination (3600L, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			Thread.interrupted ();
		}
		
		assertEquals (nThreads, counter.get ());
		
		for (int j = 0; j < SIZE; j++) {
			assertNull (ll.search (j));
		}
		
		checkSanity (ll);
	}

	static void checkSanity (final LockFreeLinkedList ll) {
		final Node head = ll.getHead ();
		assertEquals (LockFreeLinkedList.MINUS_INFINITE_KEY, head.key);
		assertEquals (LockFreeLinkedList.MINUS_INFINITE_KEY, head.value);
		assertNull (head.backlink);
		
		Node node = head;
		while (node.next.node != null) {
			assertNotNull (node.key);
			assertNotNull (node.value);
			assertFalse (node.next.mark);
			assertFalse (node.next.flag);
			node = node.next.node;
		}
		
		final Node tail = node;
		assertEquals (LockFreeLinkedList.PLUS_INFINITE_KEY, tail.key);
		assertEquals (LockFreeLinkedList.PLUS_INFINITE_KEY, tail.value);
		assertFalse (tail.next.mark);
		assertFalse (tail.next.flag);
		assertNull (tail.next.node);
	}
}
