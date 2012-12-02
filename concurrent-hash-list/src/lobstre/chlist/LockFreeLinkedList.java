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
	
	public Node<K, V> search (final K k) {
		final Pair<Node<K, V>, Node<K, V>> curAndNext = searchFrom (k, head);
		Node<K, V> current = curAndNext.getFirst();
		Node<K, V> next = curAndNext.getSecond();
 		if (0 == this.comparator.compare(current.getKey(), k)) {
 			return current;
 		} else {
 			return null;
 		}
	}

	private Pair<Node<K, V>, Node<K, V>> searchFrom(final K k, final Node<K, V> head2) {
		return null;
	}

	static class Node<K, V> {
		public Node (final K key, final V value) {
			this.key = key;
			this.value = value;
			this.next = new NextLink<K, V> (null, false, false);
			this.backlink = null;
		}

		public K getKey() {
			return key;
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

		private final K key;
		private final V value;
		private volatile NextLink<K, V> next;
		private final Node<K, V> backlink;
		
		@SuppressWarnings("rawtypes")
		private static final AtomicReferenceFieldUpdater<Node, NextLink> NEXT_UPDATER = 
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
		
		public Node<K, V> getNode() {
			return node;
		}
		
		public boolean isMarked() {
			return marked;
		}
		
		public boolean isFlagged() {
			return flagged;
		}
		
		private final Node<K, V> node;
		private final boolean marked;
		private final boolean flagged;
	}
}

