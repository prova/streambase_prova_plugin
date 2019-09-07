package org.provarules.streambase.processors;

import java.util.HashMap;
import java.util.Map;

import org.provarules.streambase.EngineResults;

public final class AggregationProcessor {
	
	private String groupByField;
	
	private String field;
	
	private String aggregateField;
	
	private Object oldValue = null;
	
	private Number aggregate = 0;

	public AggregationProcessor(String groupByField, String field, String aggregateField) {
		this.groupByField = groupByField;
		this.field = field;
		this.aggregateField = aggregateField;
	}
	
	public int intsum(final Map<String, Object> in, final EngineResults operator) {
		final Object value = in.get(groupByField);
		if (oldValue == null ) {
			aggregate = (Number) in.get(field);
			oldValue = value;
			return 0;
		}
		if (oldValue.equals(value)) {
			aggregate = aggregate.intValue() + (int) in.get(field);
			return 0;
		}
		final Map<String, Object> out = new HashMap<>();
		out.putAll(in);		
		out.put(aggregateField, aggregate.intValue());
		oldValue = value;
		aggregate = (Number) in.get(field);
		operator.callback(0, out);
		return 0;
	}

}
