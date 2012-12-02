package lobstre.chlist;

public class LockFreeLinkedList {
	
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

