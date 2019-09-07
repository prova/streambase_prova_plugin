package org.provarules.streambase.transforms;

import java.util.HashMap;
import java.util.Map;

import com.streambase.sb.Timestamp;

public final class TransformFunctions {
	
	public static final Map<String, Object> multiplyFieldByTwo( final String field, final Map<String, Object> in) {
		final Map<String, Object> out = new HashMap<>();
		Timestamp time = (Timestamp) in.get("time");
		long millis = time.toMsecs();
		System.out.println(millis);
		System.out.println(in.get("time").getClass().getName());
		Timestamp newTime = Timestamp.now();
		out.putAll(in);
		out.put(field, (Integer) in.get(field) * 2);
		out.put("time", newTime);
		return out;
	}

}
