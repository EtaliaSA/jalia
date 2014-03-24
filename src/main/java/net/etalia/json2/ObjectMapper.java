package net.etalia.json2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.etalia.utils.LockHashMap;

import net.etalia.json2.stream.JsonReader;
import net.etalia.json2.stream.JsonWriter;

public class ObjectMapper {

	private Set<JsonDeSer> registeredDeSers = new HashSet<>();
	private LockHashMap<Class<?>,JsonDeSer> serializers = new LockHashMap<>();
	private LockHashMap<TypeUtil,JsonDeSer> deserializers = new LockHashMap<>();
	private EntityFactory entityProvider = null;
	private EntityNameProvider entityNameProvider = null;
	
	private boolean prettyPrint = false;
	private boolean sendNulls = false;
	
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}
	public void setSendNulls(boolean sendNulls) {
		this.sendNulls = sendNulls;
	}
	public void setEntityFactory(EntityFactory entityProvider) {
		this.entityProvider = entityProvider;
	}
	public EntityFactory getEntityFactory() {
		return entityProvider;
	}
	
	public void setEntityNameProvider(EntityNameProvider entityNameProvider) {
		this.entityNameProvider = entityNameProvider;
	}
	public EntityNameProvider getEntityNameProvider() {
		return entityNameProvider;
	}
	
	public void registerDeSer(JsonDeSer ds) {
		registeredDeSers.add(ds);
	}
	public void registerDeSer(Collection<? extends JsonDeSer> dss) {
		registeredDeSers.addAll(dss);
	}
	
	public void init() {
		registeredDeSers.add(new NativeJsonDeSer());
		registeredDeSers.add(new MapJsonDeSer());
		registeredDeSers.add(new ListJsonDeSer());
		registeredDeSers.add(new BeanJsonDeSer());
		// TODO add default desers
	}
	
	protected JsonDeSer getSerializerFor(JSONContext context, Object obj) {
		Class<?> clazz = null;
		if (obj != null) {
			clazz = obj.getClass();
		}
		
		serializers.lockRead();
		try {
			if (serializers.containsKey(clazz))
				return serializers.get(clazz);
		} finally {
			serializers.unlockRead();
		}
		JsonDeSer deser = null;
		
		int max = -1;
		for (JsonDeSer acds : registeredDeSers) {
			try {
				int ach = acds.handlesSerialization(context, clazz);
				if (ach > max) {
					deser = acds;
					max = ach;
				}
			} catch (NullPointerException e) {}
		}
		serializers.lockWrite();
		try {
			serializers.put(clazz, deser);
		} finally {
			serializers.unlockWrite();
		}
		
		return deser;
	}

	protected JsonDeSer getDeserializerFor(JSONContext context, TypeUtil hint) {
		if (hint != null) {
			deserializers.lockRead();
			try {
				if (deserializers.containsKey(hint))
					return deserializers.get(hint);
			} finally {
				deserializers.unlockRead();
			}
		}
		JsonDeSer deser = null;
		
		int max = -1;
		for (JsonDeSer acds : registeredDeSers) {
			try {
				int ach = acds.handlesDeserialization(context, hint);
				if (ach > max) {
					deser = acds;
					max = ach;
				}
			} catch (NullPointerException e) {}
		}
		if (hint != null) {
			deserializers.lockWrite();
			try {
				deserializers.put(hint, deser);
			} finally {
				deserializers.unlockWrite();
			}
		}		
		return deser;
	}
	
	public void writeValue(JsonWriter jsonOut, OutField fields, Object obj) {
		JSONContext ctx = new JSONContext(this);
		ctx.setOutput(jsonOut);
		if (fields == null) fields = new OutField(true);
		ctx.setRootFields(fields);
		writeValue(obj, ctx);
	}
	
	public void writeValue(Object obj, JSONContext context) {
		JsonDeSer deser = getSerializerFor(context, obj);
		if (deser == null) throw new IllegalStateException("Cannot find a JSON serializer for " + obj);
		try {
			deser.serialize(obj, context);
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}
	
	
	public Object readValue(JsonReader jsonIn, Object pre, TypeUtil hint) {
		JSONContext ctx = new JSONContext(this);
		ctx.setInput(jsonIn);
		return readValue(ctx, pre, hint);
	}
	
	public Object readValue(JSONContext ctx, Object pre, TypeUtil hint) {
		JsonDeSer deser = getDeserializerFor(ctx, hint);
		if (deser == null) throw new IllegalStateException("Cannot find a JSON deserializer for " + pre + " " + hint);
		try {
			return deser.deserialize(ctx, pre, hint);
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
		
	}
	
	// ---- Utility methods

	public void writeValue(Writer out, OutField fields, Object obj) {
		JsonWriter jw = new JsonWriter(out);
		if (prettyPrint)
			jw.setIndent("  ");
		if (sendNulls)
			jw.setSerializeNulls(sendNulls);
		writeValue(jw, fields, obj);
	}
		
	public void writeValue(OutputStream out, OutField fields, Object obj) {
		OutputStreamWriter osw = null;
		try {
			osw = new OutputStreamWriter(out, Charset.forName("UTF-8"));
			writeValue(osw, fields, obj);
		} finally {
			try {
				osw.close();
			} catch (IOException e) {}
		}
	}
	
	public String writeValueAsString(Object obj, OutField fields) {
		StringWriter writer = new StringWriter();
		writeValue(writer, fields, obj);
		return writer.toString();
	}
	
	
	
	public Object readValue(Reader r, TypeUtil hint) {
		JsonReader reader = new JsonReader(r);
		return readValue(reader, null, hint);
	}
	
	public Object readValue(String json, TypeUtil hint) {
		StringReader reader = new StringReader(json);
		return readValue(reader, hint);
	}
}
