package net.etalia.jalia;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

public class ListJsonDeSer implements JsonDeSer {

	@Override
	public int handlesSerialization(JsonContext context, Class<?> clazz) {
		if (Iterable.class.isAssignableFrom(clazz)) return 10;
		if (clazz.isArray()) return 10;
		return -1;
	}
	
	@Override
	public int handlesDeserialization(JsonContext context, TypeUtil hint) {
		if (hint != null) {
			if (hint.isArray()) return 10;
			try {
				if (Iterable.class.isAssignableFrom(hint.getConcrete())) return 10;
			} catch (Exception e) {}
		}
		try {
			if (context.getInput().peek() == JsonToken.BEGIN_ARRAY) return 10;
		} catch (Exception e) {}
		return -1;
	}

	@Override
	public void serialize(Object obj, JsonContext context) throws IOException {
		JsonWriter output = context.getOutput();
		if (obj.getClass().isArray()) {
			if (Array.getLength(obj) == 0 && !context.getMapper().isSendEmpty()) {
				output.clearName();
				return;
			}
			output.beginArray();
			for (int i = 0; i < Array.getLength(obj); i++) {
				context.getMapper().writeValue(Array.get(obj, i), context);
			}
		} else {
			if (!((Iterable)obj).iterator().hasNext() && !context.getMapper().isSendEmpty()) {
				output.clearName();
				return;
			}
			output.beginArray();
			for (Object so : (Iterable)obj) {
				context.getMapper().writeValue(so, context);
			}
		}
		output.endArray();
	}

	@Override
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException {
		Collection<Object> act = null;
		if (pre != null && pre.getClass().isArray()) {
			act = new ArrayList<Object>();
			for (int i = 0; i < Array.getLength(pre); i++) {
				act.add(Array.get(pre, i));
			}
		} else {
			act = (Collection<Object>) pre;
		}
		TypeUtil inner = null;
		if (act != null) {
			TypeUtil pretype = TypeUtil.get(pre.getClass());
			if (pretype.isArray()) {
				inner = pretype.getArrayType();
			} else {
				try {
					if (List.class.isAssignableFrom(pretype.getConcrete())) {
						inner = pretype.findReturnTypeOf("get", Integer.TYPE);
					} else if (Set.class.isAssignableFrom(pretype.getConcrete())) {
						inner = pretype.findParameterOf("add", 0);
					}
				} catch (Exception e) {
					// TODO log this
					e.printStackTrace();
				}
			}
		}
		if (inner == null || !inner.hasConcrete() || inner.getConcrete() == Object.class) {
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
						if (List.class.isAssignableFrom(hint.getConcrete())) {
							inner = hint.findReturnTypeOf("get", Integer.TYPE);
						} else if (Set.class.isAssignableFrom(hint.getConcrete())) {
							inner = hint.findParameterOf("add", 0);
						}
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
			List<Object> lst = null;
			if (act instanceof List) {
				lst = (List<Object>)act;
			} else {
				lst = new ArrayList<>(act);
			}
			List<Object> found = new ArrayList<>();
			while (input.hasNext()) {
				Object preval = null;
				if (lst != null) {
					if (i < lst.size()) preval = lst.get(i);
				}
				Object val = context.getMapper().readValue(context, preval, inner);
				found.add(val);
				try {
					if (act instanceof List) {
						while (i >= act.size()) act.add(null);
						((List<Object>)act).set(i, val);
					} else {
						act.add(val);
					}
				} catch (UnsupportedOperationException e) {
					// Could happen for unmodifiable collections
					if (act instanceof List) {
						act = new ArrayList<>();
						while (i >= act.size()) act.add(null);
						((List<Object>)act).set(i, val);
					} else {
						act = new HashSet<>();
						act.add(val);
					}
				}
				i++;
			}
			if (act instanceof List) {
				while (act.size() > i) ((List<Object>)act).remove(i);
			} else {
				act.retainAll(found);
			}
		}
		input.endArray();
		if ((pre != null && pre.getClass().isArray()) || (hint != null && hint.isArray())) {
			if (pre == null || Array.getLength(pre) != act.size()) {
				pre = Array.newInstance(inner.getConcrete(), act.size());
			}
			Iterator<Object> iter = act.iterator();
			for (int i = 0; i < Array.getLength(pre); i++) {
				Array.set(pre, i, iter.next());
			}
			return pre;
		}
		return act;
	}

}
