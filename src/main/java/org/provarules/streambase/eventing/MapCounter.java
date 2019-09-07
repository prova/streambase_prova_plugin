package org.provarules.streambase.eventing;

import java.util.Map;
import java.util.TreeMap;

public class MapCounter {

	private Map<String,Long> map;
	
	// True if there are no buckets with count>1, otherwise False
	private boolean flag = true;
	
	public MapCounter() {
		this.map = new TreeMap<String,Long>();
	}
	
	public MapCounter(MapCounter counter) {
		this.map = new TreeMap<String,Long>(counter.map);
	}
	
	public Map<String,Long> getMap() {
		return map;
	}

	public long incrementAt(String key) {
		Long count = map.get(key);
		if( count==null ) {
			map.put(key, 1L);
			return 1L;
		}
		map.put(key, ++count);
		return count;
	}

	public long addAt(String key, long delta) {
		Long count = map.get(key);
		if( count==null ) {
			map.put(key, delta);
			return delta;
		}
		map.put(key, count+delta);
		return count+delta;
	}

	public boolean update(final String key, final int status ) {
		if( status==3 ) {
			long newCount = incrementAt(key);
			if( newCount!=1 )
				flag = false;
			return flag;
		}
		Long count = map.get(key);
		if( count==null )
			return flag;
		if( count==1 )
			map.remove(key);
		else if( count>1 ) {
			map.remove(key);
			if( !flag ) {
				// Work out if any more buckets with count>1 are left
				for( long n : map.values() )
					if( n>1 )
						return false;
				flag = true;
			}
		}
		return flag;
	}
	
	public boolean flag() {
		return flag;
	}
	
	public void clear() {
		flag = true;
		map.clear();
	}
	
	@Override
	public MapCounter clone() {
		return new MapCounter(this);
	}
	
	@Override
	public String toString() {
		return map.toString();
	}

}
