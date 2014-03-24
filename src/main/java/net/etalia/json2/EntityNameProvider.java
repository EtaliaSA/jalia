package net.etalia.json2;

public interface EntityNameProvider {

	public String getEntityName(Class<?> clazz);
	
	public Class<?> getEntityClass(String name);
		
}
