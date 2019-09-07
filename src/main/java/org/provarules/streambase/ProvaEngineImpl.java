package org.provarules.streambase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.provarules.api2.ProvaCommunicator;
import org.provarules.api2.ProvaCommunicatorImpl;
import org.provarules.exchange.ProvaSolution;
import org.provarules.kernel2.ProvaList;
import org.provarules.kernel2.ProvaObject;
import org.provarules.reference2.ProvaConstantImpl;
import org.provarules.reference2.ProvaListImpl;
import org.provarules.reference2.ProvaMapImpl;

import com.streambase.sb.DataType;
import com.streambase.sb.Schema;
import com.streambase.sb.Schema.Field;

public class ProvaEngineImpl implements EventProcessingEngine {
	
	private static final ProvaConstantImpl CONST_ASYNC = ProvaConstantImpl.create("async");
	private static final ProvaConstantImpl CONST_EVENT = ProvaConstantImpl.create("event");
	private static final ProvaConstantImpl CONST_ZERO = ProvaConstantImpl.create(0);
	private ProvaCommunicator comm;

	public ProvaEngineImpl(final Object rulebase) {
		System.out.println(rulebase);
		comm = new ProvaCommunicatorImpl("prova", rulebase, ProvaCommunicatorImpl.SYNC);
	}

	@Override
	public Schema[][] init(final String patternName)
			throws Exception {
		final Schema[] inputSchemas = retrieveInputSchemas(patternName);
		final Schema[] outputSchemas = retrieveOutputSchemas(patternName);
		final Schema[] outputEnrichmentSchemas = retrieveOutputSchemas("enrich");
		return new Schema[][] {inputSchemas, outputSchemas, outputEnrichmentSchemas};
	}

	private Schema[] retrieveInputSchemas(final String patternName)
			throws Exception {
		final String input = ":- solve(input(_0,Fields)).";
		
		
		try (
				StringReader sr = new StringReader(input);
				BufferedReader in = new BufferedReader(sr)) {
			final List<ProvaSolution[]> solutions = comm.consultSync(in, "pattern", new Object[] {patternName});
			final Schema[] schemas = new Schema[solutions.get(0).length];
			for( int i=0; i<schemas.length; i++ ) {
				ProvaMapImpl f = (ProvaMapImpl) solutions.get(0)[i].getNv("Fields");
				Map<String, String> m = (Map<String, String>) f.unwrap();
//				System.out.println(m);
				List<Field> fields = new ArrayList<Field>();
				for( final Entry<String, String> e : m.entrySet() ) {
					Schema.Field ff = Schema.createField(DataType.forName(e.getValue()), e.getKey());
					fields.add(ff);
				}
				Schema schema = new Schema(null, fields);
//				System.out.println("ou"+schema.toHumanString());
				schemas[i] = schema;
			}
			return schemas;
		}

	}

	private Schema[] retrieveOutputSchemas(final String patternName)
			throws Exception {
		final String input = ":- solve(output(_0,Fields)).";
		
		try (
				StringReader sr = new StringReader(input);
				BufferedReader in = new BufferedReader(sr)) {
			final List<ProvaSolution[]> solutions = comm.consultSync(in, "pattern", new Object[] {patternName});
			final Schema[] schemas = new Schema[solutions.get(0).length];
			for( int i=0; i<schemas.length; i++ ) {
				ProvaMapImpl f = (ProvaMapImpl) solutions.get(0)[i].getNv("Fields");
				Map<String, String> m = (Map<String, String>) f.unwrap();
//				System.out.println(m);
				List<Field> fields = new ArrayList<Field>();
				for( final Entry<String, String> e : m.entrySet() ) {
					Schema.Field ff = Schema.createField(DataType.forName(e.getValue()), e.getKey());
					fields.add(ff);
				}
				Schema schema = new Schema(null, fields);
//				System.out.println("ou"+schema.toHumanString());
				schemas[i] = schema;
			}
			return schemas;
		}
	}


	@Override
	public void start(final String patternName, final Map<String, Object> properties)
			throws Exception {
		final String input = ":- eval(start(_0,_1)).";
		try (
				StringReader sr = new StringReader(input);
				BufferedReader in = new BufferedReader(sr)) {
			comm.consultSync(in, "pattern", new Object[] {patternName,properties});
		}
	}


	@Override
	public void addMsg(final Map<String, Object> msg, final Map<String, Object> properties) {
		final String queryId = properties.get("queryId").toString();
		final ProvaList terms = ProvaListImpl.create(new ProvaObject[] {
				ProvaConstantImpl.create(queryId),
				CONST_ASYNC,
				CONST_ZERO,
				CONST_EVENT,
				ProvaMapImpl.wrapValues(msg)
		});
		
		comm.addMsg(terms);
	}

	@Override
	public void stop(final String patternName, final Map<String, Object> properties) throws Exception {
		final String input = ":- eval(stop(_0,_1)).";
		final BufferedReader in = new BufferedReader(new StringReader(input));

		comm.consultSync(in,"pattern", new Object[] {patternName,properties});
	}

	@Override
	public void shutdown() {
		comm.shutdown();
		comm = null;
	}

}
