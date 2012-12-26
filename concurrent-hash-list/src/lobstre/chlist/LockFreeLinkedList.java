package lobstre.chlist;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import lobstre.chlist.util.Pair;

public class LockFreeLinkedList {
	private final Node head;
	private final Comparator<Object> comparator;

	public LockFreeLinkedList () {
		this.comparator = new Comparator<Object> () {
			@Override
			public int compare (Object o1, Object o2) {
				if (o1 == MINUS_INFINITE_KEY || o2 == PLUS_INFINITE_KEY) {
					return -1;
				}
				if (o1 == PLUS_INFINITE_KEY || o2 == MINUS_INFINITE_KEY) {
					return +1;
				}
				
				final Integer i1 = (Integer) o1;
				final Integer i2 = (Integer) o2;
				return i1.compareTo (i2);
			}
		};
		final Node tail = new Node (PLUS_INFINITE_KEY, PLUS_INFINITE_KEY, null);
		this.head = new Node (MINUS_INFINITE_KEY, MINUS_INFINITE_KEY, tail);
	}

	/**
	 * Searches for a node with the supplied key
	 * 
	 * @param k
	 *            the key instance
	 * @return a {@link Node} instance
	 */
	 Node search (final int k) {
		final Pair<Node, Node> curAndNext =
				searchCurrentAndNextFrom (Integer.valueOf (k), head);
		final Node current = curAndNext.getFirst ();
		if (0 == this.comparator.compare (current.key, k)) {
			return current;
		} else {
			return null;
		}
	}

	/**
	 * Deletes a Node from the list
	 * @param k the node's key
	 * @return the deleted Node if found, null otherwise
	 */
	Node delete (final int k) {
		final Pair<Node, Node> search = searchPrevAndCurrentFrom (
				Integer.valueOf (k),
				this.head);
		Node prevNode = search.getFirst ();
		final Node delNode = search.getSecond ();
	
		if (0 != this.comparator.compare (delNode.key, k)) {
			// k is not found in the list.
			return null;
		}
	
		final Pair<Node, Boolean> tryFlag = tryFlag (prevNode, delNode);
		prevNode = tryFlag.getFirst ();
		if (null != prevNode) {
			helpFlagged (prevNode, delNode);
		}
		return tryFlag.getSecond ().booleanValue () ? delNode : null;
	}

	/**
	 * Inserts a node into the list
	 * 
	 * @param k
	 *            the node's key
	 * @param v
	 *            the node's value
	 * @return the insert Node if inserted, null if a previous node already
	 *         exists
	 */
	Node insert (final int k, final Object v) {
		Pair<Node, Node> search = 
				searchCurrentAndNextFrom (Integer.valueOf (k), head);
		Node prevNode = search.getFirst ();
		Node nextNode = search.getSecond ();
		
		if (prevNode.key.equals (k)) {
			return null;
		}
		
		for (;;) {
			final Node newNode = new Node (k, v, nextNode);
			final Link prevLink = prevNode.link ();
			if (prevLink.flagged () == true) {
				// If predecessor is flagged : help deletion to complete
				helpFlagged (prevNode, prevLink.next);
			} else {
				final Link failed = prevNode.compareAndSetLink (
					nextNode, false, false, prevLink.value, 
					newNode, false, false, prevLink.value);
				if (failed == null) {
					// Success
					return newNode;
				} else {
					// Failure
					if (failed.marked () == false && failed.flagged () == true) {
						// Failure due to flagging : 
						// Help complete deletion
						helpFlagged (prevNode, failed.next);
					}
					// Possibly a failure due to marking 
					// Help complete deletion: traverse the backlink chain
					while (prevNode.link ().marked () == true) {
						prevNode = prevNode.backlink;
					}
				}
			}
			
			// Search again from prevNode
			search = searchCurrentAndNextFrom (k, prevNode);
			prevNode = search.getFirst ();
			nextNode = search.getSecond ();
			if (prevNode.key.equals (k)) {
				return null;
			}
		}
	}
	
	Node getHead () {
		return this.head;
	}

	/**
	 * Finds two consecutive nodes n1 and n2 such that n1.key <= k < n2.key.
	 * 
	 * @param k
	 *            the key instance
	 * @param currNode
	 *            the current node to start searching for
	 * @return a {@link Pair} of two {@link Node} instances
	 */
	private Pair<Node, Node> searchCurrentAndNextFrom (
			final Object k,
			final Node currNode) {
		return searchFromInternal (k, currNode, LOWER_OR_EQUAL);
	}

	/**
	 * Finds two consecutive nodes n1 and n2 such that n1.key < k <= n2.key.
	 * 
	 * @param k
	 *            the key instance
	 * @param currNode
	 *            the previous node to start searching for
	 * @return a {@link Pair} of two {@link Node} instances
	 */
	private Pair<Node, Node> searchPrevAndCurrentFrom (
			final Object k,
			final Node currNode) {
		return searchFromInternal (k, currNode, STRICTLY_LOWER);
	}

	/**
	 * Attempts to mark and physically delete node delNode, which is the
	 * successor of the flagged node prevNode.
	 * 
	 * @param prevNode
	 *            the previous node
	 * @param delNode
	 *            the candidate node for deletion
	 */
	private void helpFlagged (
			final Node prevNode,
			final Node delNode) {
		delNode.backlink = prevNode;
		if (!delNode.link ().marked ()) {
			tryMark (delNode);
		}
		helpMarked (prevNode, delNode);
	}

	private void tryMark (final Node delNode) {
		do {
		    Link delLink = delNode.link ();
			final Node nextNode = delLink.next;
			final Link failedLink = delNode.compareAndSetLink (
					nextNode, false, false, delLink.value, 
					nextNode, true, false, delLink.value);
			if (null != failedLink && 
					failedLink.marked () == false && 
					failedLink.flagged () == true) {
				helpFlagged (delNode, failedLink.next);
			}
		} while (delNode.link ().marked () != true);
	}

	private Pair<Node, Boolean> tryFlag (Node prevNode,
			final Node targetNode) {
		for (;;) {
			final Link prevLink = prevNode.link ();
			if (prevLink.next == targetNode &&
					!prevLink.marked () &&
					prevLink.flagged ()) {
				// Predecessor is already flagged
				return new Pair<Node, Boolean> (
						prevNode, Boolean.TRUE);
			}
			// Flagging attempt
			final Link failed = prevNode.compareAndSetLink (
					targetNode, false, false, prevLink.value, 
					targetNode, false, true, prevLink.value);
			if (null == failed) {
				// Successful flaging : report the success
				return new Pair<Node, Boolean> (
						prevNode, Boolean.TRUE);
			}
			if (failed.next == targetNode &&
					failed.marked () == false &&
					failed.flagged () == true) {
				// Failure due to a concurrent operation
				// Report the failure, return a pointer to prev node.
				return new Pair<Node, Boolean> (
						prevNode, Boolean.FALSE);
			}
			while (prevNode.link ().marked ()) {
				// Possibly a failure due to marking,
				// Traverse a chain of backlinks to reach an unmarked node.
				prevNode = prevNode.backlink;
			}
			// Search again
			final Pair<Node, Node> search = searchPrevAndCurrentFrom (
					targetNode.key,
					prevNode);
			prevNode = search.getFirst ();
			final Node delNode = search.getSecond ();
			if (delNode != targetNode) {
				// targetNode got deleted
				// report the failure, return no pointer.
				return new Pair<Node, Boolean> (
						null, Boolean.FALSE);
			}
		}
	}

	private Pair<Node, Node> searchFromInternal (
			final Object k,
			Node currNode,
			final Comparison c) {
		Node nextNode = currNode.link ().next;
		while (c.apply (this.comparator.compare (nextNode.key, k), 0)) {
			// Ensure that either nextNode is unmarked,
			// or both currNode and nextNode are mark
			// and currNode was mark earlier.
			while (nextNode.link ().marked ()
					&& (!currNode.link ().marked () || currNode.link ().next != nextNode)) {
				if (currNode == nextNode) {
					helpMarked (currNode, nextNode);
				}
				nextNode = currNode.link ().next;
			}
			if (c.apply (this.comparator.compare (nextNode.key, k), 0)) {
				currNode = nextNode;
				nextNode = currNode.link ().next;
			}
		}
		return new Pair<Node, Node> (currNode, nextNode);
	}

	private void helpMarked (final Node prevNode, final Node delNode) {
		// Attempts to physically delete the marked
		// node delNode and unflag prevNode.
		final Node nextNode = delNode.link ().next;
		final Link prevLink = prevNode.link ();
        prevNode.compareAndSetLink (
				delNode, false, true, prevLink.value, 
				nextNode, false, false, prevLink.value);
	}

	static class Node {
		public Node (final Object key, final Object value, Node nextNode) {
			this.key = key;
			NEXT_UPDATER.set (this, new Link (nextNode, value, false, false));
			this.backlink = null;
		}

		public Link link () {
			return NEXT_UPDATER.get (this);
		}

		/**
		 * CAS the link pointer
		 *
		 * @param expectedNextNode
		 *            the expected node
		 * @param expectedMark
		 *            the expected mark
		 * @param expectedFlag
		 *            the expected flag
		 * @param expectedValue 
	 *                the expected value
		 * @param replacementNextNode
		 *            the replacement node
		 * @param replacementMark
		 *            the replacement mark
		 * @param replacementFlag
		 *            the replacement flag
         * @param replacementFlag
         *            the replacement value            
		 * @return null if CAS worked, or the previous value if CAS failed.
		 */
		public Link compareAndSetLink (final Node expectedNextNode,
				final boolean expectedMark, 
				final boolean expectedFlag,
				final Object expectedValue,
				final Node replacementNextNode, 
				final boolean replacementMark, 
				final boolean replacementFlag, final Object replacementValue) {
			final Link currentLink = link ();
			if (currentLink.next == expectedNextNode
					&& currentLink.marked () == expectedMark
					&& currentLink.flagged () == expectedFlag
					&& currentLink.value == expectedValue) {
				final Link update = new Link (
						replacementNextNode, replacementValue, replacementMark, replacementFlag);
				final boolean set = NEXT_UPDATER.compareAndSet (
						this, currentLink, update);
				return set ? null : currentLink;
			} else {
				return currentLink;
			}
		}

		final Object key;
		volatile Link link;
		volatile Node backlink;

		static final AtomicReferenceFieldUpdater<Node, Link> NEXT_UPDATER =
				AtomicReferenceFieldUpdater
						.newUpdater (Node.class, Link.class, "link");
	}

	static class Link {
		public Link (
				final Node next,
				final Object value,
				final boolean mark, 
				final boolean flag) {
			this.next = next;
			this.value = value;
			this.mark = mark;
			this.flag = flag;
		}
		
		final Node next;
		final Object value;
		final boolean mark;
        final boolean flag;
        
        public boolean marked () {
            return mark;
        }

        public boolean flagged () {
            return flag;
        }
	}

	public interface Comparison {
		public boolean apply (int a, int b);
	}

	static Comparison LOWER_OR_EQUAL = new Comparison () {
		@Override
		public boolean apply (final int a, final int b) {
			return a <= b;
		}
	};
	
	static Comparison STRICTLY_LOWER = new Comparison () {
		@Override
		public boolean apply (final int a, final int b) {
			return a < b;
		}
	};
	
	static final Object MINUS_INFINITE_KEY = new Object () {
		public String toString() {
			return "MINUS_INFINITE_KEY";
		};
	};
	
	static final Object PLUS_INFINITE_KEY = new Object () {
		public String toString() {
			return "PLUS_INFINITE_KEY";
		};
	};
}
