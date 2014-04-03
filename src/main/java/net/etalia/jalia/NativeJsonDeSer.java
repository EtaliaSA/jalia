package net.etalia.jalia;

import java.io.IOException;
import java.lang.reflect.Type;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

public class NativeJsonDeSer implements JsonDeSer {

	@Override
	public int handlesSerialization(JsonContext context, Class<?> clazz) {
		if (clazz == null) return 10;
		if (clazz.isPrimitive()) return 10;
		if (Number.class.isAssignableFrom(clazz)) return 10;
		if (Boolean.class.isAssignableFrom(clazz)) return 10;
		if (CharSequence.class.isAssignableFrom(clazz)) return 10;
		if (Enum.class.isAssignableFrom(clazz)) return 10;
		if (Class.class.isAssignableFrom(clazz)) return 10;
		return -1;
	}
	
	@Override
	public int handlesDeserialization(JsonContext context, TypeUtil hint) {
		if (hint != null && hint.hasConcrete()) {
			return handlesSerialization(context, hint.getConcrete());
		}
		try {
			JsonToken peek = context.getInput().peek();
			if (peek == JsonToken.NUMBER) return 10;
			if (peek == JsonToken.BOOLEAN) return 10;
			if (peek == JsonToken.STRING) return 10;
			if (peek == JsonToken.NULL) return 10;
		} catch (IOException e) {}
		return -1;
	}

	@Override
	public void serialize(Object obj, JsonContext context) throws IOException {
		JsonWriter output = context.getOutput();
		if (obj == null) {
			output.nullValue();
		} else if (obj instanceof Number) {
			output.value((Number)obj);
		} else if (obj instanceof Boolean) {
			output.value(((Boolean)obj).booleanValue());
		} else if (obj instanceof CharSequence) {
			output.value(((CharSequence)obj).toString());
		} else if (obj instanceof Enum) {
			output.value(((Enum)obj).name());
		} else if (obj instanceof Class) {
			output.value(((Class)obj).getName());
		} else {
			throw new IllegalStateException("Cannot serialize " + obj);
		}
	}

	@Override
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException {
		JsonReader input = context.getInput();
		JsonToken peek = input.peek();
		Object ret = null;
		if (peek == JsonToken.NULL) {
			input.nextNull();
			if (hint != null && !hint.isNullable()) {
				throw new IllegalArgumentException("Was expecting a primitive " + hint + " but got null, can't set null on a primitive");
			}
			return null;
		}
		if (peek == JsonToken.STRING) {
			ret = input.nextString();
			// A string could be an enum
			if (hint != null) {
				if (hint.isEnum()) {
					ret = hint.getEnumValue((String)ret);
				} else if (hint.hasConcrete() && Class.class.isAssignableFrom(hint.getConcrete())) {
					try {
						ret = Class.forName((String)ret);
					} catch (ClassNotFoundException e) {
						throw new IllegalStateException("Cannot deserialize a class", e);
					}
				} else if (!hint.isCharSequence()) {
					throw new IllegalStateException("Found a string, but was expecting " + hint);
				}
			}
		} else if (peek == JsonToken.BOOLEAN) {
			ret = input.nextBoolean();
		} else if (peek == JsonToken.NUMBER) {
			// If we don't have a hint, use double
			if (hint == null || hint.isDouble()) {
				ret = input.nextDouble();
			} else if (hint.isInteger()) {
				ret = input.nextInt();
			} else if (hint.isLong()) {
				ret = input.nextLong();
			} else {
				throw new IllegalStateException("Found a number, but was expecting " + hint);
			}
		} else {
			throw new IllegalStateException("Was expecting a string, boolean or number, but got " + peek);
		}
		return ret;
	}

}
