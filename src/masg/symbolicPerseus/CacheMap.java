package masg.symbolicPerseus;

import java.util.LinkedHashMap;
import java.util.Map;

public class CacheMap<K,V> extends LinkedHashMap<K,V> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int maxCapacity;

	public CacheMap() {
		super();
		maxCapacity = 100000;
	}

	public CacheMap(int maxCapacity) {
		super();
		this.maxCapacity = maxCapacity;
	}

	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
		return size() > maxCapacity;
	}
}
