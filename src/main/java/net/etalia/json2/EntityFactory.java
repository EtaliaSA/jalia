package net.etalia.json2;

public interface EntityFactory {

	public void alterClassData(JsonClassData jcd);
	
	public String getId(Object entity);
	
	public Object buildEntity(Class<?> clazz, String id);
	
}
