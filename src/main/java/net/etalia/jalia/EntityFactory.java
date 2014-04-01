package net.etalia.jalia;

public interface EntityFactory {

	public String getId(Object entity);
	
	public Object buildEntity(Class<?> clazz, String id);
	
	public Object prepare(Object obj, boolean serializing);
	
	public Object finish(Object obj, boolean serializing);
	
	
}
