package net.etalia.json2;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import net.etalia.json2.stream.JsonReader;
import net.etalia.json2.stream.JsonToken;
import net.etalia.json2.stream.JsonWriter;

public class BeanJsonDeSer implements JsonDeSer {

	@Override
	public int handlesSerialization(JSONContext context, Class<?> clazz) {
		if (clazz.isPrimitive() || clazz.isArray()) return 0;
		return 5;
	}
	
	@Override
	public int handlesDeserialization(JSONContext context, TypeUtil hint) {
		JsonReader la = context.getInput().lookAhead();
		try {
			if (la.peek() != JsonToken.BEGIN_OBJECT) return -1;
			la.beginObject();
			while (la.hasNext()) {
				if (la.nextName().equals("@entity")) return 10;
				la.skipValue();
			}
		} catch (IOException e) {
			// TODO what to do?
			e.printStackTrace();
		} finally {
			try {
				la.close();
			} catch (IOException e) {
				throw new IllegalStateException("Cannot close lookAhead json reader", e);
			}
		}
		
		// Map deserialize return 10 if hint say it's a map, and 5 if it's a json object, we get priority on concrete classes
		if (hint.isInstantiatable()) return 7;
		
		return -1;
	}

	@Override
	public void serialize(Object obj, JSONContext context) throws IOException {
		JsonWriter output = context.getOutput();
		ObjectMapper mapper = context.getMapper();
		JsonClassData cd = JsonClassData.get(obj.getClass(), context);
		output.beginObject();
		if (mapper.getEntityNameProvider() != null) {
			String entityName = mapper.getEntityNameProvider().getEntityName(obj.getClass());
			if (entityName != null) {
				output.name("@entity");
				output.value(entityName);
			}
		}
		if (mapper.getEntityFactory() != null) {
			String id = mapper.getEntityFactory().getId(obj);
			if (id != null) {
				output.name("id");
				output.value(id);
			}
		}
		for (String name : cd.getGettables()) {
			if (context.entering(name, cd.getDefaults())) {
				output.name(name);
				context.getMapper().writeValue(cd.getValue(name, obj), context);
				context.exited();
			}
		}
		output.endObject();
	}

	@Override
	public Object deserialize(JSONContext context, Object pre, TypeUtil hint) throws IOException {
		// Search for @entity and id
		String id = null;
		String entity = null;
		
		JsonReader input = context.getInput();
		input.beginObject();
		
		JsonReader la = input.lookAhead();
		while (la.hasNext()) {
			String name = la.nextName();
			if (name.equals("@entity")) {
				entity = la.nextString();
			} else if (name.equals("id")) {
				id = la.nextString();
			} else {
				la.skipValue();
			}
			// TODO if we can take for granted that the order is always id->@entity we could stop before, cause entities may not have an id
			if (entity != null && id != null) break;
		}
		la.close();
		
		Class<?> clazz = null;
		// If we have an @entity, try to resolve it
		if (entity != null) {
			EntityNameProvider provider = context.getMapper().getEntityNameProvider();
			if (provider != null) {
				clazz = provider.getEntityClass(entity);
			}
			if (clazz == null) {
				try {
					clazz = Class.forName(entity);
				} catch (Exception e) {}
			}
			if (clazz == null) throw new IllegalStateException("Cannot resolve @entity:" + entity);
		}
		
		// Try reusing pre
		if (clazz == null && pre != null) {
			clazz = pre.getClass();
		}
		if (pre != null) {
			EntityFactory factory = context.getMapper().getEntityFactory();
			if (factory != null) {
				String preid = factory.getId(pre);
				if (preid != null) {
					if (id == null || !preid.equals(id)) {
						pre = null;
					}
				} else if (id != null) {
					pre = null;
				}
			}
		}
		
		// If we don't have it, try based on the hint
		if (hint != null) {
			if (clazz == null) {
				if (!hint.isInstantiatable()) throw new IllegalStateException("No @entity in the json, and " + hint + " is not instantiatable");
				clazz = hint.getConcrete();
			} else {
				if (!hint.getConcrete().isAssignableFrom(clazz)) throw new IllegalStateException("Was expecting " + hint + " or subclass, but based on @entity it's a " + clazz.getName());
			}
		}
		
		// Now we should have a class to work on
		
		// Check if the pre is coherent
		if (pre != null) {
			if (!clazz.isAssignableFrom(pre.getClass())) {
				pre = null;
			}
		}
		if (pre == null) {
			// Try to obtain it from factory
			EntityFactory factory = context.getMapper().getEntityFactory();
			if (factory != null) {
				pre = factory.buildEntity(clazz, id);
			}
		}
		if (pre == null) {
			// Try to instantiate it
			try {
				pre = clazz.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Cannot instantiate a " + clazz, e);
			}
		}
		
		// Now we should have a pre to work on
		JsonClassData cd = JsonClassData.get(pre.getClass(), context);
		while (input.hasNext()) {
			String name = input.nextName();
			Object preval = null;
			try {
				preval = cd.getValue(name, pre);
			} catch (Exception e) {
				// TODO log this?
			}
			TypeUtil hintval = cd.getHint(name);
			Object nval = context.getMapper().readValue(context, preval, hintval);
			try {
				cd.setValue(name, nval, pre);
			} catch (Exception e) {
				// TODO log this?
			}
		}
		input.endObject();
		
		return pre;
	}

}
