package org.provarules.streambase;

import java.util.Map;

public interface EngineResults {

	/**
	 * Callback from the engine when an outbound event is ready
	 * @param result sent by the engine
	 */
	void callback(int port, Map<String, Object> result);

}