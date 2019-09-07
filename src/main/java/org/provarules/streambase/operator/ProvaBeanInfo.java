package org.provarules.streambase.operator;

import java.beans.*;

import com.streambase.sb.operator.parameter.*;

/**
 * A BeanInfo class controls what properties are exposed, add 
 * metadata about properties (such as which properties are optional), and access 
 * special types of properties that can't be automatically derived via reflection. 
 * If a BeanInfo class is present, only the properties explicitly declared in
 * this class will be exposed by StreamBase.
 */
public class ProvaBeanInfo extends SBSimpleBeanInfo {

	/*
	 * The order of properties below determines the order they are displayed within
	 * the StreamBase Studio property view. 
	 */
	public SBPropertyDescriptor[] getPropertyDescriptorsChecked()
			throws IntrospectionException {
		SBPropertyDescriptor[] p = {
				new ResourceFilePropertyDescriptor(
				"rulebase", Prova.class).displayName("Prova rulebase")
				.description("The rulebase that processes the stream and ships out the results to a callback function"),
				new SBPropertyDescriptor(
				"properties", Prova.class).displayName("Run-time properties for initializing the operator")
				.description("key1=value1,key2=value2,...,keyn=valuen").optional(),
				};
		return p;
	}

}
