package net.etalia.jalia;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

public class MapJsonDeSer implements JsonDeSer {

	@Override
	public int handlesSerialization(JsonContext context, Class<?> clazz) {
		return (clazz != null && Map.class.isAssignableFrom(clazz)) ? 10 : 0;
	}
	
	@Override
	public int handlesDeserialization(JsonContext context, TypeUtil hint) {
		if (hint != null)
			if (hint.hasConcrete() && Map.class.isAssignableFrom(hint.getConcrete())) return 10;
		try {
			if (context.getInput().peek() == JsonToken.BEGIN_OBJECT) return 5;
		} catch (IOException e) {}
		return 0;
	}

	@Override
	public void serialize(Object obj, JsonContext context) throws IOException {
		JsonWriter output = context.getOutput();
		Map<String,?> map = (Map<String,?>) obj;
		if (map.size() == 0 && !context.getMapper().isSendEmpty()) {
			output.clearName();
			return;
		}
		output.beginObject();
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
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException {
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
			if (act == null) act = new HashMap<>();
		}
		
		JsonReader input = context.getInput();
		input.beginObject();
		Set<String> keys = new HashSet<String>();
		Map<String,Object> read = act;
		while (input.hasNext()) {
			String name = input.nextName();
			keys.add(name);
			Object preval = read.get(name);
			Object val = context.getMapper().readValue(context, preval, inner);
			val = reduceNumber(val);
			try {
				act.put(name, val);
			} catch (UnsupportedOperationException e) {
				// Could happen because or a read only map, try using a normal map
				act = new HashMap<>();
				act.put(name, val);
			}
		}
		for (Iterator<String> iter = act.keySet().iterator(); iter.hasNext();) {
			if (!keys.contains(iter.next())) iter.remove();
		}
		input.endObject();
		return act;
	}

	private Object reduceNumber(Object val) {
		if (val == null) return null;
		if (!(val instanceof Number)) return val;
		if (val instanceof Double) {
			Double d = (Double)val;
			Long l = d.longValue();
			if (l.doubleValue() == d.doubleValue()) return reduceNumber(l);
			return val;
		}
		if (val instanceof Long) {
			Long l = (Long)val;
			if (l.longValue() > Integer.MAX_VALUE) return val;
			return reduceNumber(l.intValue());
		}
		return val;
	}

}
