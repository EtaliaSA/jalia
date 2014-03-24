package net.etalia.json2;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import net.etalia.json2.stream.JsonReader;
import net.etalia.json2.stream.JsonToken;
import net.etalia.json2.stream.JsonWriter;

public class MapJsonDeSer implements JsonDeSer {

	@Override
	public int handlesSerialization(JSONContext context, Class<?> clazz) {
		return (clazz != null && Map.class.isAssignableFrom(clazz)) ? 10 : 0;
	}
	
	@Override
	public int handlesDeserialization(JSONContext context, TypeUtil hint) {
		if (hint != null)
			if (Map.class.isAssignableFrom(hint.getConcrete())) return 10;
		try {
			if (context.getInput().peek() == JsonToken.BEGIN_OBJECT) return 5;
		} catch (IOException e) {}
		return 0;
	}

	@Override
	public void serialize(Object obj, JSONContext context) throws IOException {
		JsonWriter output = context.getOutput();
		output.beginObject();
		Map<String,?> map = (Map<String,?>) obj;
		for (Map.Entry<String,?> entry : map.entrySet()) {
			if (context.entering(entry.getKey(), "*")) {
				output.name(entry.getKey());
				context.getMapper().writeValue(entry.getValue(), context);
				context.exited();
			}
		}
		output.endObject();
	}

	@Override
	public Object deserialize(JSONContext context, Object pre, TypeUtil hint) throws IOException {
		Map<String,Object> act = (Map<String, Object>) pre;
		TypeUtil inner = null;
		if (act == null) {
			if (hint != null) {
				if (hint.isInstantiatable()) {
					try {
						act = (Map<String, Object>) hint.getConcrete().newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						// TODO log this
						e.printStackTrace();
					}
				}
				try {
					inner = hint.findReturnTypeOf("remove", Object.class);
				} catch (Exception e) {
					// TODO log this
					e.printStackTrace();
				}
			}
			if (act == null) act = new HashMap<String,Object>();
		}
		
		JsonReader input = context.getInput();
		input.beginObject();
		while (input.hasNext()) {
			String name = input.nextName();
			Object preval = act.get(name);
			Object val = context.getMapper().readValue(context, preval, inner);
			act.put(name, val);
		}
		input.endObject();
		return act;
	}

}
