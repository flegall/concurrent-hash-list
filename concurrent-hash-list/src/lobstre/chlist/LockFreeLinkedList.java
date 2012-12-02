package lobstre.chlist;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import lobstre.chlist.util.Pair;

public class LockFreeLinkedList<K, V> {
	private final Node<K, V> head;
	private final Comparator<K> comparator;

	public LockFreeLinkedList (
			final Comparator<K> comparator, 
			final K headKey, 
			final K tailKey) {
		this.comparator = comparator;
		this.head = new Node<K, V> (headKey, null);
		final Node<K, V> tail = new Node<K, V> (tailKey, null);
	}
	
	/**
	 * Searches for a node with the supplied key
	 * 
	 * @param k
	 *            the key instance
	 * @return a {@link Node} instance
	 */
	public Node<K, V> search (final K k) {
		final Pair<Node<K, V>, Node<K, V>> curAndNext = searchFrom (k, head);
		Node<K, V> current = curAndNext.getFirst();
 		if (0 == this.comparator.compare(current.key, k)) {
 			return current;
 		} else {
 			return null;
 		}
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
	public Pair<Node<K, V>, Node<K, V>> searchFrom (
			final K k, 
			Node<K, V> currNode) {
		Node<K, V> nextNode = currNode.getNext().node;
		while (this.comparator.compare(nextNode.key, k) <= 0) {
			// Ensure that either nextNode is unmarked,
			// or both currNode and nextNode are marked
			//    and currNode was marked earlier.
			while (nextNode.getNext().marked &&
					(!currNode.getNext().marked ||
					currNode.getNext().node != nextNode)) {
				if (currNode == nextNode) {
					helpMarked (currNode, nextNode);
				}
				nextNode = currNode.getNext().node;
			}
			if (this.comparator.compare(nextNode.key, k) <= 0) {
				currNode = nextNode;
				nextNode = currNode.getNext().node;
			}
		}
		return new Pair<Node<K,V>, Node<K,V>>(currNode, nextNode);
	}

	private void helpMarked(Node<K, V> currNode, Node<K, V> nextNode) {
		// TODO Auto-generated method stub
		
	}

	static class Node<K, V> {
		public Node (final K key, final V value) {
			this.key = key;
			this.value = value;
			this.next = new NextLink<K, V> (null, false, false);
			this.backlink = null;
		}

		public V getValue() {
			return value;
		}
		
		public NextLink<K, V> getNext() {
			return next;
		}
		
		public Node<K, V> getBacklink() {
			return backlink;
		}

		final K key;
		final V value;
		volatile NextLink<K, V> next;
		final Node<K, V> backlink;
		
		@SuppressWarnings("rawtypes")
		static final AtomicReferenceFieldUpdater<Node, NextLink> NEXT_UPDATER = 
			AtomicReferenceFieldUpdater.newUpdater (Node.class, NextLink.class, "next");
	}
	
	static class NextLink<K, V> {
		public NextLink(
				final Node<K, V> node, 
				final boolean marked, 
				final boolean flagged) {
			this.node = node;
			this.marked = marked;
			this.flagged = flagged;
		}
		
		public boolean isMarked() {
			return marked;
		}
		
		public boolean isFlagged() {
			return flagged;
		}
		
		final Node<K, V> node;
		final boolean marked;
		final boolean flagged;
	}
}

