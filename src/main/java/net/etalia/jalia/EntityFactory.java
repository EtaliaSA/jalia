package net.etalia.jalia;

public interface EntityFactory {

	public String getId(Object entity);
	
	public Object buildEntity(Class<?> clazz, String id);
	
}
