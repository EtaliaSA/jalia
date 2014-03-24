package net.etalia.json2;

import java.util.HashMap;
import java.util.Map;

import net.etalia.json2.DummyAddress.AddressType;

public class DummyEntityProvider implements EntityFactory, EntityNameProvider {

	private Map<String, DummyEntity> db = new HashMap<>();
	
	public void addToDb(DummyEntity... entities) {
		for (DummyEntity de : entities) {
			if (de.getIdentifier() == null) throw new IllegalArgumentException("No id on " + de);
			db.put(de.getIdentifier(), de);
		}
	}
	
	@Override
	public void alterClassData(JsonClassData jcd) {
		if (DummyEntity.class.isAssignableFrom(jcd.getTargetClass())) {
			jcd.ignoreSetter("identifier");
			jcd.ignoreGetter("identifier");
		}
	}
	
	@Override
	public String getEntityName(Class<?> clazz) {
		if (clazz == DummyPerson.class) return "Person";
		if (clazz == DummyAddress.class) return "Address";
		throw new IllegalArgumentException("Can't find a name for " + clazz);
	}

	@Override
	public Class<?> getEntityClass(String name) {
		if (name.equals("Person")) return DummyPerson.class;
		if (name.equals("Address")) return DummyAddress.class;
		throw new IllegalArgumentException("Can't find a class for " + name);
	}

	@Override
	public String getId(Object entity) {
		if (!(entity instanceof DummyEntity)) throw new IllegalArgumentException("Cannot get id from " + entity);
		return ((DummyEntity)entity).getIdentifier();
	}

	@Override
	public Object buildEntity(Class<?> clazz, String id) {
		if (clazz == null) return null;
		DummyEntity ret = null;
		if (id != null) {
			ret = db.get(id);
			if (ret != null) return ret;
		}
		try {
			ret = (DummyEntity) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		ret.setIdentifier(id);
		return ret;
	}
	
}
