package net.etalia.jalia;

import java.util.concurrent.ConcurrentHashMap;

public class JsonClassDataFactoryImpl implements JsonClassDataFactory {

	private ConcurrentHashMap<Class<?>, JsonClassData> cache = new ConcurrentHashMap<Class<?>, JsonClassData>();
	
	@Override
	public JsonClassData getClassData(Class<?> clazz, JsonContext context) {
		JsonClassData ret = cache.get(clazz);
		if (ret != null) return ret;
		ret = new JsonClassData(clazz);
		cache.put(clazz, ret);
		return ret;
	}

}
