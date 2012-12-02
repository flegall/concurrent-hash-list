package lobstre.chlist;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeLinkedList {
	
	static class Node<K, V> {
		public Node (K key, V value) {
			this.key = key;
			this.value = value;	
			this.next = new AtomicMarkableReference<LockFreeLinkedList.Node<K,V>>(null, false);
			this.backlink = null;
		}

		private final K key;
		private final V value;
		private final AtomicMarkableReference<Node<K, V>> next;
		private final Node<K, V> backlink;
	}
}

