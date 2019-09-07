package org.provarules.streambase.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.provarules.algorithms.streams.TopNAlgorithmFactory;
import org.provarules.algorithms.streams.TopNAlgorithmFactory.CompoundKey;

public final class TopNViewIncrementalProcessor {

	private static Logger LOG = LoggerFactory
			.getLogger(TopNViewIncrementalProcessor.class.getName());

	// Double field to rank on
	private String rankOn;
	
	// Rows to maintain in the view
	private int rows;

	private ConcurrentMap<String, TopNAlgorithmFactory.TopView> views = new ConcurrentHashMap<>();

	public TopNViewIncrementalProcessor(String rankOn, int rows) {
		super();
		this.rankOn = rankOn;
		this.rows = rows;
	}

	public List<Map<String, Object>> process(final Map<String, Object> in) {
		final List<Map<String, Object>> results = new ArrayList<>();
		final String query = (String) in.get("query");
		TopNAlgorithmFactory.TopView topView = views.get(query);
		final String signal = (String) in.get("signal");
		if (signal != null && "terminate".equals(signal) && topView != null) {
			views.remove(query);
			return results;
		}
		if (topView == null) {
			topView = new TopNAlgorithmFactory.TopView(new String[] { "key" },
					true, rows);
			views.put(query, topView);
		}
		final String operation = (String) in.get("operation");
		if ("update".equals(operation))
			insert(in, topView, results);
		else if ("delete".equals(operation))
			delete(in, topView, results);
		return results;
	}

	/**
	 * Add an entry to the view
	 * 
	 * @param in
	 * @param topView
	 * @param results
	 *            List to fill with results
	 */
	private void insert(final Map<String, Object> in,
			final TopNAlgorithmFactory.TopView topView,
			final List<Map<String, Object>> results) {
		final String key = (String) in.get("key");
		final Double value = (Double) in.get(rankOn);
		final CompoundKey updatedKey = getKey(key);
		topView.insert(updatedKey, value, in);

		assembleEditEvents(topView, results, updatedKey);
	}

	/**
	 * Delete an entry from the view
	 * 
	 * @param in
	 * @param topView
	 * @param results
	 *            List to fill with results
	 */
	private void delete(final Map<String, Object> in,
			final TopNAlgorithmFactory.TopView topView,
			final List<Map<String, Object>> results) {
		final String key = (String) in.get("key");
		final CompoundKey updatedKey = getKey(key);
		topView.delete(updatedKey);

		assembleEditEvents(topView, results, updatedKey);
	}

	/**
	 * Accumulate in <code>results</code> the edit actions resulting from
	 * updating the top view with a new event
	 * 
	 * @param topView
	 * @param results
	 * @param updatedKey
	 */
	private void assembleEditEvents(final TopNAlgorithmFactory.TopView topView,
			final List<Map<String, Object>> results,
			final CompoundKey updatedKey) {
		final List<Map<String, Object>> editActions = topView
				.makeUpdateMessages(updatedKey, rankOn);
		for (Map<String, Object> out : editActions) {
			out.put("seq", topView.getSeq());
			final String keyFromEdit = (String) out.get("key");
			final Map<String, Object> record = topView.get(keyFromEdit);
			out.putAll(record);
			results.add(out);
		}
	}

	private CompoundKey getKey(String key) {
		return new CompoundKey(new Comparable[] { key });
	}

}
