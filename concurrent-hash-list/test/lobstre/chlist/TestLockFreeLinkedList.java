package lobstre.chlist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lobstre.chlist.LockFreeLinkedList.NextLink;
import lobstre.chlist.LockFreeLinkedList.Node;

import org.junit.Test;

public class TestLockFreeLinkedList {

	@Test
	public void testInsert () {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
		
		testInsert (ll, -1, "-1");
		testInsert (ll, 0, "0");
		testInsert (ll, 1, "1");

		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
		
		for (int i = 5; i < 25; i++) {
			testInsert (ll, i, Integer.toString (i));
		}
		
		assertNull (ll.insert (-1, "VOID"));
		
		for (int i = 5; i < 25; i++) {
			assertNull (ll.insert (i, "VOID"));
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
	}
	
	@Test
	public void testInsertReverse () {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		
		for (int i = 24; i >= 0; i--) {
			testInsert (ll, i, Integer.toString (i));
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
		
		assertNull (ll.insert (0, "VOID"));
		
		for (int i = 5; i < 25; i++) {
			assertNull (ll.insert (i, "VOID"));
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
	}
	
	@Test
	public void testDelete () {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		for (int i = 0; i < 25; i++) {
			testInsert (ll, i, Integer.toString (i));
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
		
		for (int i = 0; i < 25; i++) {
			testDelete (ll, i);
		}
		
		for (int i = 0; i < 25; i++) {
			assertNull (ll.delete (i));
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
	}
	
	@Test
	public void testDeleteReverse () {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		for (int i = 0; i < 25; i++) {
			testInsert (ll, i, Integer.toString (i));
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
		
		for (int i = 24; i >= 0; i--) {
			testDelete (ll, i);
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
	}
	
	@Test
	public void testDeleteRandom () {
		final LockFreeLinkedList ll = new LockFreeLinkedList ();
		final List<Integer> indices = new ArrayList<Integer> ();
		for (int i = 0; i < 25; i++) {
			testInsert (ll, i, Integer.toString (i));
			indices.add (i);
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
		
		Collections.shuffle (indices);
		
		for (final int i : indices) {
			testDelete (ll, i);
		}
		
		TestMultiThreadLockFreeLinkedList.checkSanity (ll);
	}

	private void testDelete (final LockFreeLinkedList ll, int i) {
		Node search = ll.search (i);
		checkNode (search, i, Integer.toString (i));
		
		final Node deleted = ll.delete (i);
		assertNotNull (deleted);
		
		search = ll.search (i);
		assertNull (search);
	}

	private void testInsert (
			final LockFreeLinkedList ll, 
			final int expectedKey, 
			final String expectedValue) {
		Node search = ll.search (expectedKey);
		assertNull (search);
		
		final Node inserted = ll.insert (expectedKey, expectedValue);
		checkNode (inserted, expectedKey, expectedValue);
		
		search = ll.search (expectedKey);
		checkNode (search, expectedKey, expectedValue);
	}

	private void checkNode (final Node node, final int expectedKey, final String expectedValue) {
		assertNotNull (node);
		assertNull (node.backlink);
		assertEquals (expectedKey, node.key);
		assertEquals (expectedValue, node.value);
		final NextLink next = node.next ();
		assertFalse (next.flag);
		assertFalse (next.marked ());
		assertNotNull (next.node);
	}
}
