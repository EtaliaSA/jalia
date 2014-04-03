package net.etalia.jalia;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonWriter;

public class JsonContext extends HashMap<String, Object>{

	private ObjectMapper mapper;
	private JsonWriter output;
	private JsonReader input;
	private OutField rootFields;
	
	private OutField currentFields;
	private int deserCount;
	
	
	public JsonContext(ObjectMapper mapper) {
		this.mapper = mapper;
	}
	
	
	public void setOutput(JsonWriter output) {
		this.output = output;
	}
	public JsonWriter getOutput() {
		return output;
	}
	
	public void setInput(JsonReader input) {
		this.input = input;
	}
	public JsonReader getInput() {
		return input;
	}
	
	public void setRootFields(OutField rootField) {
		this.rootFields = rootField;
		this.currentFields = rootField;
	}
	public OutField getRootFields() {
		return rootFields;
	}
	
	public ObjectMapper getMapper() {
		return mapper;
	}

	public boolean entering(String fieldName, String... defaults) {
		return this.entering(fieldName, defaults == null ? null : Arrays.asList(defaults));
	}

	public boolean entering(String fieldName, Collection<String> defaults) {
		//if (acsub == null) {
			if (!currentFields.hasSubs()) {
				if (defaults != null && defaults.size() > 0) {
					currentFields.setAll(false);
					for (String def : defaults) {
						currentFields.getCreateSub(def);
					}
					return entering(fieldName);
				}
			}
			OutField acsub = currentFields.getSub(fieldName);
			if (acsub == null)
				return false;
		//}
		currentFields = acsub;
		return true;
	}
	
	public Set<String> getCurrentSubs() {
		return currentFields.getSubsNames();
	}
	
	public void exited() {
		currentFields = currentFields.getParent();
	}

	public void deserializationEntering(String name) {
		this.deserCount++;
	}
	
	public void deserializationExited() {
		this.deserCount--;
	}
	

	public boolean isRoot() {
		if (currentFields != null) {
			return currentFields.getParent() == null || currentFields.getParent() == currentFields;
		} 
		return deserCount == 0;
	}


}
