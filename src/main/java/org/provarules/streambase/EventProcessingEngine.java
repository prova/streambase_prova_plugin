package org.provarules.streambase;

import java.util.Map;

import com.streambase.sb.Schema;

public interface EventProcessingEngine {

	Schema[][] init(String patternName)
			throws Exception;

	void start(String patternName, Map<String, Object> properties) throws Exception;

	public void addMsg(Map<String, Object> msg, Map<String, Object> properties);

	void stop(String patternName, Map<String, Object> properties) throws Exception;

	void shutdown();

}