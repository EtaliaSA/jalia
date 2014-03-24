package net.etalia.json2;

import java.io.IOException;
import java.lang.reflect.Type;

public interface JsonDeSer {

	public int handlesSerialization(JSONContext context, Class<?> clazz);
	
	public int handlesDeserialization(JSONContext context, TypeUtil hint);
	
	public void serialize(Object obj, JSONContext context) throws IOException;
	
	public Object deserialize(JSONContext context, Object pre, TypeUtil hint) throws IOException;
	
}
