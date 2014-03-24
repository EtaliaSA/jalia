package net.etalia.json2;

import java.util.Arrays;
import java.util.Collection;

import net.etalia.json2.stream.JsonReader;
import net.etalia.json2.stream.JsonWriter;

public class JsonContext {

	private ObjectMapper mapper;
	private JsonWriter output;
	private JsonReader input;
	private OutField rootFields;
	
	private OutField currentFields;
	
	
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
		OutField acsub = currentFields.getSub(fieldName);
		if (acsub == null) {
			if (!currentFields.hasSubs()) {
				if (defaults != null && defaults.size() > 0) {
					for (String def : defaults) {
						currentFields.getCreateSub(def);
					}
					return entering(fieldName);
				}
			}
			return false;
		}
		currentFields = acsub;
		return true;
	}
	
	public void exited() {
		currentFields = currentFields.getParent();
	}
	
}
