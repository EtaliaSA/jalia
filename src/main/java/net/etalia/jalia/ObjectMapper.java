package net.etalia.jalia;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonWriter;
import net.etalia.utils.LockHashMap;
import net.etalia.utils.MissHolder;

public class ObjectMapper {

	private List<JsonDeSer> registeredDeSers = new ArrayList<>();
	private JsonDeSer nullDeSer = null;
	private LockHashMap<Class<?>,MissHolder<JsonDeSer>> serializers = new LockHashMap<>();
	private LockHashMap<TypeUtil,MissHolder<JsonDeSer>> deserializers = new LockHashMap<>();
	
	private EntityFactory entityProvider = null;
	private EntityNameProvider entityNameProvider = null;
	private JsonClassDataFactory classDataFactory = new JsonClassDataFactoryImpl();
	
	private Map<String,Object> defaultOptions = new HashMap<String, Object>();
	
	{
		defaultOptions.put(DefaultOptions.PRETTY_PRINT.toString(), false);
		defaultOptions.put(DefaultOptions.INCLUDE_EMPTY.toString(), false);
		defaultOptions.put(DefaultOptions.INCLUDE_NULLS.toString(), false);
	}
	
	protected boolean inited = false;
	
	public <X> ObjectMapper setOption(Option<X> option, X val) {
		defaultOptions.put(option.toString(), val);
		return this;
	}
	
	// TODO support more than one factory or cascading factories, and/or a factory that self configures based on typeinfo annotations
	public ObjectMapper setEntityFactory(EntityFactory entityProvider) {
		this.entityProvider = entityProvider;
		return this;
	}
	public EntityFactory getEntityFactory() {
		return entityProvider;
	}
	
	public ObjectMapper setEntityNameProvider(EntityNameProvider entityNameProvider) {
		this.entityNameProvider = entityNameProvider;
		return this;
	}
	public EntityNameProvider getEntityNameProvider() {
		return entityNameProvider;
	}
	
	public void setClassDataFactory(JsonClassDataFactory classDataFactory) {
		this.classDataFactory = classDataFactory;
	}
	public JsonClassDataFactory getClassDataFactory() {
		return classDataFactory;
	}
	
	public void registerDeSer(JsonDeSer ds) {
		registeredDeSers.add(ds);
	}
	public void registerDeSer(Collection<? extends JsonDeSer> dss) {
		registeredDeSers.addAll(dss);
	}
	public List<JsonDeSer> getRegisteredDeSers() {
		return registeredDeSers;
	}
	
	@PostConstruct
	public void init() {
		if (inited) return;
		inited = true;
		NativeJsonDeSer nativeDeSer = new NativeJsonDeSer();
		if (nullDeSer == null) nullDeSer = nativeDeSer;
		registeredDeSers.add(nativeDeSer);
		registeredDeSers.add(new MapJsonDeSer());
		registeredDeSers.add(new ListJsonDeSer());
		registeredDeSers.add(new BeanJsonDeSer());
	}
	
	protected JsonReader configureReader(JsonReader reader) {
		return reader;
	}
	protected JsonWriter configureWriter(JsonWriter writer) {
		if ((Boolean)defaultOptions.get(DefaultOptions.PRETTY_PRINT.toString()))
			writer.setIndent("  ");
		return writer;
	}
	
	protected JsonDeSer getSerializerFor(JsonContext context, Object obj) {
		Class<?> clazz = null;
		if (obj == null) return nullDeSer;
		clazz = obj.getClass();

		MissHolder<JsonDeSer> holder = null;
		serializers.lockRead();
		try {
			holder = serializers.get(clazz);
		} finally {
			serializers.unlockRead();
		}
		if (holder != null) return holder.getVal();
		
		JsonDeSer deser = null;
		int max = -1;
		for (JsonDeSer acds : registeredDeSers) {
			try {
				int ach = acds.handlesSerialization(context, clazz);
				if (ach > max) {
					deser = acds;
					max = ach;
					if (max >= 10) break;
				}
			} catch (NullPointerException e) {}
		}
		serializers.lockWrite();
		try {
			serializers.put(clazz, new MissHolder<>(deser));
		} finally {
			serializers.unlockWrite();
		}
		
		return deser;
	}

	protected JsonDeSer getDeserializerFor(JsonContext context, TypeUtil hint) {
		if (hint != null) {
			MissHolder<JsonDeSer> holder = null;
			deserializers.lockRead();
			try {
				holder = deserializers.get(hint);
			} finally {
				deserializers.unlockRead();
			}
			if (holder != null) return holder.getVal();
		}
		JsonDeSer deser = null;
		
		int max = -1;
		for (JsonDeSer acds : registeredDeSers) {
			try {
				int ach = acds.handlesDeserialization(context, hint);
				if (ach > max) {
					deser = acds;
					max = ach;
					if (max >= 10) break;
				}
			} catch (NullPointerException e) {}
		}
		if (hint != null) {
			deserializers.lockWrite();
			try {
				deserializers.put(hint, new MissHolder<>(deser));
			} finally {
				deserializers.unlockWrite();
			}
		}
		return deser;
	}
	
	public void writeValue(JsonWriter jsonOut, OutField fields, Object obj) {
		init();
		configureWriter(jsonOut);
		JsonContext ctx = new JsonContext(this);
		ctx.initInheritStack(this.defaultOptions);
		ctx.setOutput(jsonOut);
		if (fields == null) fields = new OutField(true);
		ctx.setRootFields(fields);
		writeValue(obj, ctx);
	}
	
	public void writeValue(Object obj, JsonContext context) {
		JsonDeSer deser = getSerializerFor(context, obj);
		if (deser == null) throw new IllegalStateException("Cannot find a JSON serializer for " + obj);
		try {
			deser.serialize(obj, context);
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}
	
	
	public Object readValue(JsonReader jsonIn, Object pre, TypeUtil hint) {
		init();
		configureReader(jsonIn);
		JsonContext ctx = new JsonContext(this);
		ctx.setInput(jsonIn);
		try {
			return readValue(ctx, pre, hint);
		} catch (Exception e) {
			throw new IllegalArgumentException("Error parsing " + jsonIn.getLineNumber() + ":" + jsonIn.getColumnNumber(), e);
		}
	}
	
	public Object readValue(JsonContext ctx, Object pre, TypeUtil hint) {
		// Don't consider a hint == Object.class
		if (hint != null && hint.getType().equals(Object.class)) hint = null;
		
		JsonDeSer deser = getDeserializerFor(ctx, hint);
		if (deser == null) throw new IllegalStateException("Cannot find a JSON deserializer for " + pre + " " + hint);
		try {
			return deser.deserialize(ctx, pre, hint);
		} catch (Throwable t) {
			if (t instanceof IllegalStateException) throw (IllegalStateException)t;
			throw new IllegalStateException(t);
		}
		
	}
	
	// ---- Utility methods

	public void writeValue(Writer out, OutField fields, Object obj) {
		JsonWriter jw = new JsonWriter(out);
		writeValue(jw, fields, obj);
	}

	public void writeValue(Writer out, Object obj) {
		writeValue(out, null, obj);
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
	
	public String writeValueAsString(Object obj) {
		return writeValueAsString(obj, null);
	}	
	
	public void writeValue(OutputStream stream, Object obj) {
		writeValue(stream, null, obj);
	}

	public byte[] writeValueAsBytes(Object obj, OutField fields) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		//BufferedOutputStream bos = new BufferedOutputStream(baos);
		try {
			writeValue(baos, fields, obj);
		} finally {
			try {
				//bos.flush();
				baos.close();
			} catch (IOException e) {};
		}
		return baos.toByteArray();
	}
	
	
	public byte[] writeValueAsBytes(Object obj) {
		return writeValueAsBytes(obj, null);
	}
	
	
	
	
	public <T> T readValue(InputStream in, TypeUtil hint) {
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(in, Charset.forName("UTF-8"));
			return (T)readValue(isr, hint);
		} finally {
			try {
				isr.close();
			} catch (IOException e) {}
		}
	}	
	
	public <T> T readValue(InputStream in, Class<T> clazz) {
		return readValue(in, TypeUtil.get(clazz));
	}
	
	public <T> T readValue(Reader r, TypeUtil hint) {
		JsonReader reader = new JsonReader(r);
		return (T)readValue(reader, null, hint);
	}
	
	public <T> T readValue(String json, TypeUtil hint) {
		StringReader reader = new StringReader(json);
		return readValue(reader, hint);
	}
	
	public <T> T readValue(String json, Class<T> clazz) {
		return readValue(json, TypeUtil.get(clazz));
	}
	
	public <T> T readValue(byte[] json, TypeUtil hint) {
		ByteArrayInputStream bais = new ByteArrayInputStream(json);
		try {
			return readValue(bais, hint);
		} finally {
			try {
				bais.close();
			} catch (IOException e) {}
		}
	}
	
	public <T> T readValue(byte[] json, Class<T> clazz) {
		return readValue(json, TypeUtil.get(clazz));
	}

	public <T> T readValue(byte[] json) {
		return readValue(json, (TypeUtil)null);
	}	
	public <T> T readValue(String json) {
		return readValue(json, (TypeUtil)null);
	}
}
