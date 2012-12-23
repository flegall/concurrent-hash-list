package lobstre.chlist.util;

public class Pair<A, B> {
	/**
	 * Builds a {@link Pair} of two objects
	 * 
	 * @param first
	 *            a first object
	 * @param second
	 *            a second object
	 */
	public Pair(final A first, final B second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Gets the first object
	 * 
	 * @return the first object instance
	 */
	public A getFirst() {
		return first;
	}

	/**
	 * Gets the second object
	 * 
	 * @return the second object instance
	 */
	public B getSecond() {
		return second;
	}

	private final A first;
	private final B second;
}
