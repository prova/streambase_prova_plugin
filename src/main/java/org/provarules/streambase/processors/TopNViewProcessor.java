package org.provarules.streambase.processors;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.provarules.algorithms.streams.TopNAlgorithmFactory;
import org.provarules.algorithms.streams.TopNAlgorithmFactory.TopView;

import static org.provarules.algorithms.streams.TopNAlgorithmFactory.KeyValEntry;
import static org.provarules.algorithms.streams.TopNAlgorithmFactory.CompoundKey;

public final class TopNViewProcessor {

	private static Logger _logger = LoggerFactory
			.getLogger(TopNViewProcessor.class.getName());

	private static final long MIN_UPDATE_INTERVAL = 1000000L;

	// The key for selecting the entries 
	private String keyOn;

	// Double field to rank on
	private String rankOn;

	// Rows to maintain in the view
	private int rows;

	private long lastUpdate = 0;

	private ConcurrentMap<String, TopNAlgorithmFactory.TopView> views = new ConcurrentHashMap<>();
	
	public TopNViewProcessor(String keyOn, String rankOn, int rows) {
		super();
		this.keyOn = keyOn;
		this.rankOn = rankOn;
		this.rows = rows;
	}

	public synchronized List<Map<String, Object>> process(final Map<String, Object> in) {
		final List<Map<String, Object>> results = new ArrayList<>();
		try {
			final String signal = (String) in.get("signal");
			if ("cleanup".equals(signal)) {			
				for ( Entry<String, TopView> e: views.entrySet()) {
					cleanupOldEntries(e.getValue(), results);
				}
				return results;
			}
			final String query = (String) in.get("query");
			TopNAlgorithmFactory.TopView topView = views.get(query);
			boolean sendSnapshot = false;
			String subscription_id = "";
			if (signal != null) {
				if ("terminate".equals(signal) && topView != null) {
					views.remove(query);
					return results;
				} else if ("snapshot".equals(signal)) {
					sendSnapshot = true;
					subscription_id = (String) in.get("subscription_id");
				}
			}
			if (topView == null) {
				topView = new TopNAlgorithmFactory.TopView(
						new String[] { "key" }, true, rows);
				views.put(query, topView);
			}
			final String operation = (String) in.get("operation");
			final String key = (String) in.get(keyOn);
			if (operation == null) {
				// See if we have the rankOn field
				final Double rankOnValue = (Double) in.get(rankOn);
				if (rankOnValue == null) {
					delete(in, key, topView, results, sendSnapshot, subscription_id);
				} else {
					insert(in, key, topView, results, sendSnapshot, subscription_id);
				}
			} else if ("update".equals(operation))
				insert(in, key, topView, results, sendSnapshot, subscription_id);
			else if ("delete".equals(operation))
				delete(in, key, topView, results, sendSnapshot, subscription_id);
			sendSnapshot = false;
		} catch (Exception e) {
			e.printStackTrace();
			_logger.error("Exception: {}", in);
		}
		return results;
	}

	/**
	 * Add an entry to the view
	 * 
	 * @param in
	 * @param key
	 * @param topView
	 * @param results
	 *            List to fill with results
	 * @param sendSnapshot
	 *            whether to force sending out the snapshot
	 * @param subscription_id
	 *            only non-empty if the sendSnapshot is set
	 */
	private void insert(final Map<String, Object> in,
			final String key,
			final TopNAlgorithmFactory.TopView topView,
			final List<Map<String, Object>> results,
			final boolean sendSnapshot, final String subscription_id) {
		final Double value = (Double) in.get(rankOn);
		final CompoundKey updatedKey = getKey(key);
		topView.insert(updatedKey, value, in);
		final List<Map<String, Object>> editActions = topView
				.makeUpdateMessages(updatedKey, rankOn);
		final long timeNow = System.currentTimeMillis();
		if (!sendSnapshot && editActions.isEmpty()
				&& timeNow < lastUpdate + MIN_UPDATE_INTERVAL) {
			return;
		}
		lastUpdate = timeNow;
		final List<KeyValEntry> topEntries = topView.getTop();
		int i = 0;
		for (KeyValEntry entry : topEntries) {
			// System.out.println(entry);
			final Map<String, Object> out = new HashMap<>();
			out.put("count", i++);
			final String keyFromView = entry.getKey().toString();
			final Map<String, Object> record = topView.get(keyFromView);
			out.put("seq", topView.getSeq());
			out.put("subscription_id", subscription_id);
			out.putAll(record);
			results.add(out);
		}
	}

	/**
	 * Delete an entry from the view
	 * 
	 * @param in
	 * @param key2 
	 * @param topView
	 * @param results
	 *            List to fill with results
	 * @param sendSnapshot
	 *            whether to force sending out the snapshot
	 * @param subscription_id
	 *            only set if the sendSnapshot is set
	 */
	private void delete(final Map<String, Object> in,
			final String key,
			final TopNAlgorithmFactory.TopView topView,
			final List<Map<String, Object>> results,
			final boolean sendSnapshot, final String subscription_id) {
		final CompoundKey updatedKey = getKey(key);
		topView.delete(updatedKey);
		final List<Map<String, Object>> editActions = topView
				.makeUpdateMessages(updatedKey, rankOn);
		final long timeNow = System.currentTimeMillis();
		if (!sendSnapshot && editActions.isEmpty()
				&& timeNow < lastUpdate + MIN_UPDATE_INTERVAL) {
			return;
		}
		lastUpdate = timeNow;
		final List<KeyValEntry> topEntries = topView.getTop();
		int i = 0;
		for (KeyValEntry entry : topEntries) {
			// System.out.println(entry);
			final Map<String, Object> out = new HashMap<>();
			out.put("count", i++);
			final String keyFromView = entry.getKey().toString();
			final Map<String, Object> record = topView.get(keyFromView);
			out.put("seq", topView.getSeq());
			out.put("subscription_id", subscription_id);
			out.putAll(record);
			results.add(out);
		}
	}

	/**
	 * Update the top records by removing the old records
	 * 
	 * @param topView
	 * @param results
	 *            List to fill with results
	 */
	private void cleanupOldEntries(final TopNAlgorithmFactory.TopView topView,
			final List<Map<String, Object>> results) {
		final boolean changed = topView.removeOldRecords();
		if (!changed) {
			return;
		}
		final List<KeyValEntry> topEntries = topView.getTop();
		int i = 0;
		for (KeyValEntry entry : topEntries) {
			// System.out.println(entry);
			final Map<String, Object> out = new HashMap<>();
			out.put("count", i++);
			final String keyFromView = entry.getKey().toString();
			final Map<String, Object> record = topView.get(keyFromView);
			out.put("seq", topView.getSeq());
			out.put("subscription_id", "");
			out.putAll(record);
			results.add(out);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private CompoundKey getKey(String key) {
		return new CompoundKey(new Comparable[] { key });
	}

}
