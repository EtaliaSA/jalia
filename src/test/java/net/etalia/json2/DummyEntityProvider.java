package net.etalia.json2;

import net.etalia.json2.DummyAddress.AddressType;

public class DummyEntityProvider implements EntityFactory, EntityNameProvider {

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
		if (id != null) {
			if (id.equals("p1")) {
				DummyPerson dp = new DummyPerson();
				dp.setName("Simone");
				dp.setSurname("Gianni");
				dp.setIdentifier("p1");
				return dp;
			}
			if (id.equals("a1")) {
				DummyAddress da = new DummyAddress();
				da.setAddress("simoneg@apache.org");
				da.setType(AddressType.EMAIL);
				da.setIdentifier("a1");
				return da;
			}
		}
		DummyEntity ret = null;
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
