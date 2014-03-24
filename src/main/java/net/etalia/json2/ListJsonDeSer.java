package net.etalia.json2;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.etalia.json2.stream.JsonReader;
import net.etalia.json2.stream.JsonToken;
import net.etalia.json2.stream.JsonWriter;

public class ListJsonDeSer implements JsonDeSer {

	@Override
	public int handlesSerialization(JSONContext context, Class<?> clazz) {
		if (Iterable.class.isAssignableFrom(clazz)) return 10;
		if (clazz.isArray()) return 10;
		return -1;
	}
	
	@Override
	public int handlesDeserialization(JSONContext context, TypeUtil hint) {
		if (hint != null) {
			if (hint.isArray() || Iterable.class.isAssignableFrom(hint.getConcrete())) return 10;
		}
		try {
			if (context.getInput().peek() == JsonToken.BEGIN_ARRAY) return 10;
		} catch (Exception e) {}
		return -1;
	}

	@Override
	public void serialize(Object obj, JSONContext context) throws IOException {
		JsonWriter output = context.getOutput();
		output.beginArray();
		if (obj.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(obj); i++) {
				context.getMapper().writeValue(Array.get(obj, i), context);
			}
		} else {
			for (Object so : (Iterable)obj) {
				context.getMapper().writeValue(so, context);
			}
		}
		output.endArray();
	}

	@Override
	public Object deserialize(JSONContext context, Object pre, TypeUtil hint) throws IOException {
		List<Object> act = null;
		if (pre != null && pre.getClass().isArray()) {
			act = new ArrayList<Object>();
			for (int i = 0; i < Array.getLength(pre); i++) {
				act.add(Array.get(pre, i));
			}
		} else {
			act = (List<Object>) pre;
		}
		TypeUtil inner = null;
		if (act != null) {
			TypeUtil pretype = TypeUtil.get(pre.getClass());
			if (pretype.isArray()) {
				inner = pretype.getArrayType();
			} else {
				try {
					inner = pretype.findReturnTypeOf("get", Integer.TYPE);
				} catch (Exception e) {
					// TODO log this
					e.printStackTrace();
				}
			}
		}
		if (inner == null || inner.getConcrete() == Object.class) {
			if (hint != null) {
				if (hint.isInstantiatable()) {
					try {
						act = (List<Object>) hint.getConcrete().newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						// TODO log this
						e.printStackTrace();
					}
				}
				if (hint.isArray()) {
					inner = hint.getArrayType();
				} else {
					try {
						inner = hint.findReturnTypeOf("get", Integer.TYPE);
					} catch (Exception e) {
						// TODO log this
						e.printStackTrace();
					}
				}				
			}
			if (act == null) act = new ArrayList<Object>();
		}
		
		JsonReader input = context.getInput();
		input.beginArray();
		{
			int i = 0;
			while (input.hasNext()) {
				Object preval = null;
				if (i < act.size()) preval = act.get(i);
				Object val = context.getMapper().readValue(context, preval, inner);
				while (i >= act.size()) act.add(null);
				act.set(i, val);
				i++;
			}
		}
		input.endArray();
		if ((pre != null && pre.getClass().isArray()) || (hint != null && hint.isArray())) {
			if (pre == null || Array.getLength(pre) != act.size()) {
				pre = Array.newInstance(inner.getConcrete(), act.size());
			}
			for (int i = 0; i < Array.getLength(pre); i++) {
				Array.set(pre, i, act.get(i));
			}
			return pre;
		}
		return act;
	}

}
