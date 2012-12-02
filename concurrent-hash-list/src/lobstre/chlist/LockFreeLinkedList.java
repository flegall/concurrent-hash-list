package lobstre.chlist;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import lobstre.chlist.util.Pair;

public class LockFreeLinkedList<K, V> {
	private final Node<K, V> head;
	private final Comparator<K> comparator;

	public LockFreeLinkedList (
			final Comparator<K> comparator, 
			final K minusInfiniteKey,
			final K plusInfiniteKey) {
		this.comparator = comparator;
		this.head = new Node<K, V> (minusInfiniteKey, null);
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
		final Node<K, V> current = curAndNext.getFirst();
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
			// or both currNode and nextNode are mark
			//    and currNode was mark earlier.
			while (nextNode.getNext().mark &&
					(!currNode.getNext().mark ||
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

	private void helpMarked(final Node<K, V> prevNode, final Node<K, V> delNode) {
		final Node<K, V> nextNode = delNode.getNext().node;
		prevNode.compareAndSetNext (delNode, false, true, nextNode, false, false);
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
		
		@SuppressWarnings("unchecked")
		public NextLink<K, V> getNext() {
			return NEXT_UPDATER.get (this);
		}

		public Node<K, V> getBacklink() {
			return backlink;
		}

		public boolean compareAndSetNext(
				final Node<K, V> expectedNode,
				final boolean expectedFlag,
				final boolean expectedMark,
				final Node<K, V> replacementNode,
				final boolean replacementFlag,
				final boolean replacementMark) {
			final NextLink<K, V> currentLink = getNext();
			if (currentLink.node == expectedNode
				&& currentLink.flag == expectedFlag
				&& currentLink.mark == expectedMark) {
				final NextLink<K, V> update = new NextLink<K, V> (
						replacementNode,
						replacementFlag,
						replacementMark);
				return NEXT_UPDATER.compareAndSet(
						this, currentLink, update);
			} else {
				return false;
			}
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
				final boolean flag,
				final boolean mark) {
			this.node = node;
			this.mark = mark;
			this.flag = flag;
		}
		
		final Node<K, V> node;
		final boolean mark;
		final boolean flag;
	}
}

