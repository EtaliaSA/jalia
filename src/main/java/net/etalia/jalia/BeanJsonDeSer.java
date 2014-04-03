package net.etalia.jalia;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

public class BeanJsonDeSer implements JsonDeSer {

	@Override
	public int handlesSerialization(JsonContext context, Class<?> clazz) {
		if (clazz.isPrimitive() || clazz.isArray()) return 0;
		return 5;
	}
	
	@Override
	public int handlesDeserialization(JsonContext context, TypeUtil hint) {
		JsonReader la = context.getInput().lookAhead();
		try {
			if (la.peek() != JsonToken.STRING) { 
				if (la.peek() != JsonToken.BEGIN_OBJECT) return -1;
				la.beginObject();
				while (la.hasNext()) {
					// TODO check is the @entity can be identified by the factory or not, before saying we can deserialize it!
					if (la.nextName().equals("@entity")) return 10;
					la.skipValue();
				}
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
	public void serialize(Object obj, JsonContext context) throws IOException {
		JsonWriter output = context.getOutput();
		
		ObjectMapper mapper = context.getMapper();
		EntityFactory factory = context.getMapper().getEntityFactory();
		if (factory != null) {
			obj = factory.prepare(obj, true, context);
			if (obj == null) {
				output.nullValue();
				return;
			}			
			
			String id = factory.getId(obj, context);
			if (id != null) {
				Map<String,Object> sents = (Map<String, Object>) context.get("BeanJsonDeSer_Sents");
				if (sents == null) {
					sents = new HashMap<>();
					context.put("BeanJsonDeSer_Sents", sents);
				}
				if (sents.containsKey(id)) {
					output.value(id);
					return;
				}
				sents.put(id, obj);
			}
		}
		
		output.beginObject();
		boolean sentEntity = false;
		if (mapper.getEntityNameProvider() != null) {
			String entityName = mapper.getEntityNameProvider().getEntityName(obj.getClass());
			if (entityName != null) {
				output.name("@entity");
				output.value(entityName);
				sentEntity = true;
			}
		}
		if (!sentEntity) {
			output.name("@entity");
			output.value(obj.getClass().getSimpleName());
		}
		if (factory != null) {
			String id = factory.getId(obj, context);
			if (id != null) {
				output.name("id");
				output.value(id);
			}
		}
		JsonClassData cd = context.getMapper().getClassDataFactory().getClassData(obj.getClass(), context);
		Set<String> sents = new HashSet<>();
		for (String name : cd.getGettables()) {
			if (context.entering(name, cd.getDefaults())) {
				sents.add(name);
				output.name(name);
				try {
					context.getMapper().writeValue(cd.getValue(name, obj), context);
				} finally {
					context.exited();
				}
			}
		}
		for (String name : context.getCurrentSubs()) {
			if (sents.contains(name)) continue;
			if (context.entering(name, cd.getDefaults())) {
				Object val = null;
				try {
					val = cd.getValue(name, obj);
				} catch (Throwable t) {
					// TODO log this
					context.exited();
					continue;
				}
				output.name(name);
				try {
					context.getMapper().writeValue(val, context);
				} finally {
					context.exited();
				}
			}
		}
		output.endObject();
		
		if (factory != null) {
			obj = factory.finish(obj, true, context);
		}
	}

	@Override
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException {
		// Search for @entity and id
		String id = null;
		String entity = null;
		boolean embedded = false;
		
		JsonReader input = context.getInput();
		EntityFactory factory = context.getMapper().getEntityFactory();				
		if (input.peek() == JsonToken.STRING) {
			// Embedded object
			id = input.nextString();
			embedded = true;
			// Search in already deserialized ones
			Map<String,Object> dones = (Map<String, Object>) context.get("BeanJsonDeSer_Dones");
			if (dones != null) {
				Object done = dones.get(id);
				if (done != null) return done;
			}
		} else {
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
		}
		
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
			//if (clazz == null) throw new IllegalStateException("Cannot resolve @entity:" + entity);
		}
		
		// Try reusing pre
		if (clazz == null && pre != null) {
			clazz = pre.getClass();
		}
		if (pre != null) {
			if (factory != null) {
				String preid = factory.getId(pre, context);
				if (preid != null) {
					if (id != null && !preid.equals(id)) {
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
				if (hint.hasConcrete()) {
					if (!hint.getConcrete().isAssignableFrom(clazz)) throw new IllegalStateException("Was expecting " + hint + " or subclass, but based on @entity it's a " + clazz.getName());
				}
			}
		}
		
		// Now we should have a class to work on

		// Check if the pre is coherent
		if (pre != null) {
			if (!clazz.isAssignableFrom(pre.getClass())) {
				pre = null;
			}
		}
		if (pre == null && clazz != null) {
			// Try to obtain it from factory
			if (factory != null) {
				pre = factory.buildEntity(clazz, id, context);
			}
		}
		if (embedded) {
			if (pre == null) throw new IllegalStateException("Cannot deserialize embedded object " + id + " " + hint);
			return pre;
		}
		if (pre == null) {
			// Try to instantiate it
			pre = TypeUtil.get(clazz).newInstance();
		}
		
		// Now we should have a pre to work on
		JsonClassData cd = context.getMapper().getClassDataFactory().getClassData(pre.getClass(), context);
		if (factory != null) {
			pre = factory.prepare(pre, false, context);
		}		
		
		if (id != null) {
			Map<String,Object> dones = (Map<String, Object>) context.get("BeanJsonDeSer_Dones");
			if (dones == null) {
				dones = new HashMap<>();
				context.put("BeanJsonDeSer_Dones", dones);
			}
			dones.put(id, pre);
		}
		while (input.hasNext()) {
			String name = input.nextName();
			context.deserializationEntering(name);
			Object preval = null;
			try {
				preval = cd.getValue(name, pre);
			} catch (Exception e) {
				// TODO log this?
			}
			TypeUtil hintval = cd.getSetHint(name);
			if (hintval == null) hintval = cd.getGetHint(name);
			try {
				Object nval = context.getMapper().readValue(context, preval, hintval);
				try {
					cd.setValue(name, nval, pre);
				} catch (Exception e) {
					// TODO log this?
				}
			} finally {
				context.deserializationExited();
			}
		}
		input.endObject();

		if (factory != null) {
			pre = factory.finish(pre, false, context);
		}		

		return pre;
	}

}
