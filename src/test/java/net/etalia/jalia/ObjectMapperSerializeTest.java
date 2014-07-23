package net.etalia.jalia;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.etalia.jalia.JsonClassData;
import net.etalia.jalia.ObjectMapper;
import net.etalia.jalia.OutField;
import net.etalia.jalia.DummyAddress.AddressType;

import org.junit.Before;
import org.junit.Test;

public class ObjectMapperSerializeTest {
	
	public enum TestEnum {
		VAL1,
		VAL2
	}

	@Test
	public void simpleMap() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Map<String,Object> map = new HashMap<>();
		
		map.put("testString", "string");
		map.put("testInt", 1);
		map.put("testBoolean", true);
		map.put("testLong", 100l);
		map.put("testEnum", TestEnum.VAL1);
		
		Map<String,Object> submap = new HashMap<>();
		submap.put("subString", "string");
		
		map.put("testMap", submap);
		
		map.put("subEmptyList", new ArrayList<String>());
		
		StringWriter writer = new StringWriter();
		mapper.writeValue(writer, null, map);
		
		String json = writer.toString();
		System.out.println(json);
		
		assertThat(json, containsString("\"testEnum\":"));
		assertThat(json, containsString("\"VAL1\""));
		
		assertThat(json, containsString("\"testString\":"));
		assertThat(json, containsString("\"string\""));
		
		assertThat(json, containsString("\"testInt\":"));
		assertThat(json, containsString("1"));
		
		assertThat(json, containsString("\"testBoolean\":"));
		assertThat(json, containsString("true"));
		
		assertThat(json, containsString("\"testLong\":"));
		assertThat(json, containsString("100"));
		
		assertThat(json, containsString("\"testMap\":"));
		assertThat(json, containsString("\"subString\":"));
		
		assertThat(json, containsString("\"subEmptyList\":"));
		assertThat(json, containsString("[]"));
	}
	
	@Test
	public void simpleList() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();

		List<Object> list = new ArrayList<>();
		
		list.add("string");
		list.add(1);
		
		List<Object> sublist = new ArrayList<>();
		sublist.add("substring");
		list.add(sublist);
		
		list.add(new String[] { "arr1", "arr2" });
		
		StringWriter sw = new StringWriter();
		mapper.writeValue(sw, null, list);
		
		String json = sw.toString();
		System.out.println(json);
		
		assertThat(json, containsString("\"string\""));
		assertThat(json, containsString("\"substring\""));
		assertThat(json, containsString("\"arr1\""));
		assertThat(json, containsString("\"arr2\""));
		assertThat(json, containsString("1"));
		
	}
	
	@Test
	public void mapWithOutFields() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Map<String,Object> map = new HashMap<>();
		
		map.put("testString", "string");
		map.put("testInt", 1);
		map.put("testBoolean", true);
		map.put("testLong", 100l);
		map.put("testEnum", TestEnum.VAL1);
		
		Map<String,Object> submap = new HashMap<>();
		submap.put("subString", "string");
		
		map.put("testMap", submap);
		
		{
			OutField fields = new OutField(null, "");
			fields.getCreateSub("testString");
			fields.getCreateSub("testBoolean");
			
			StringWriter writer = new StringWriter();
			mapper.writeValue(writer, fields, map);
			
			String json = writer.toString();
			System.out.println(json);
			
			assertThat(json, containsString("\"testString\":"));
			assertThat(json, containsString("\"string\""));
			
			assertThat(json, containsString("\"testBoolean\":"));
			assertThat(json, containsString("true"));
			
			assertThat(json, not(containsString("\"testEnum\":")));
			assertThat(json, not(containsString("\"VAL1\"")));
			
			assertThat(json, not(containsString("\"testInt\":")));
			assertThat(json, not(containsString("1")));
			
			assertThat(json, not(containsString("\"testLong\":")));
			assertThat(json, not(containsString("100")));
			
			assertThat(json, not(containsString("\"testMap\":")));
			assertThat(json, not(containsString("\"subString\":")));
		}
		
		{
			OutField fields = new OutField(null, "");
			fields.getCreateSub("testString");
			fields.getCreateSub("testMap.subString");
			
			StringWriter writer = new StringWriter();
			mapper.writeValue(writer, fields, map);
			
			String json = writer.toString();
			System.out.println(json);
			
			assertThat(json, containsString("\"testString\":"));
			assertThat(json, containsString("\"string\""));
			
			assertThat(json, not(containsString("\"testBoolean\":")));
			assertThat(json, not(containsString("true")));
			
			assertThat(json, not(containsString("\"testEnum\":")));
			assertThat(json, not(containsString("\"VAL1\"")));
			
			assertThat(json, not(containsString("\"testInt\":")));
			assertThat(json, not(containsString("1")));
			
			assertThat(json, not(containsString("\"testLong\":")));
			assertThat(json, not(containsString("100")));
			
			assertThat(json, containsString("\"testMap\":"));
			assertThat(json, containsString("\"subString\":"));
		}

		{
			OutField fields = new OutField(null, "");
			fields.getCreateSub("testString");
			fields.getCreateSub("testMap");
			
			StringWriter writer = new StringWriter();
			mapper.writeValue(writer, fields, map);
			
			String json = writer.toString();
			System.out.println(json);
			
			assertThat(json, containsString("\"testString\":"));
			assertThat(json, containsString("\"string\""));
			
			assertThat(json, not(containsString("\"testBoolean\":")));
			assertThat(json, not(containsString("true")));
			
			assertThat(json, not(containsString("\"testEnum\":")));
			assertThat(json, not(containsString("\"VAL1\"")));
			
			assertThat(json, not(containsString("\"testInt\":")));
			assertThat(json, not(containsString("1")));
			
			assertThat(json, not(containsString("\"testLong\":")));
			assertThat(json, not(containsString("100")));
			
			assertThat(json, containsString("\"testMap\":"));
			assertThat(json, containsString("\"subString\":"));
		}
		
	}
	
	private DummyPerson makePerson() {
		DummyPerson person = new DummyPerson();
		person.setName("Simone");
		person.setSurname("Gianni");
		person.setIdentifier("p1");
		
		DummyAddress address = new DummyAddress("a1", AddressType.EMAIL, "simoneg@apache.org");
		address.setNotes("Doremi");
		person.getAddresses().add(address);
		
		return person;
	}
	
	@Test
	public void simpleObject() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();

		DummyPerson person = makePerson();
		person.initTags("tag1","tag2");
		
		String json = mapper.writeValueAsString(person, null);
		System.out.println(json);
		
		assertThat(json, containsString("\"name\":"));
		assertThat(json, containsString("\"Simone\""));
		
		assertThat(json, containsString("\"surname\":"));
		assertThat(json, containsString("\"Gianni\""));
		
		assertThat(json, containsString("\"addresses\":"));
		
		assertThat(json, containsString("\"address\":"));
		assertThat(json, containsString("\"simoneg@apache.org\""));
		
		assertThat(json, containsString("\"type\":"));
		assertThat(json, containsString("\"EMAIL\""));
		
		assertThat(json, containsString("\"identifier\":"));
		assertThat(json, containsString("\"p1\""));
		assertThat(json, containsString("\"a1\""));

		assertThat(json, containsString("\"tags\":"));
		assertThat(json, containsString("\"tag1\""));
		assertThat(json, containsString("\"tag2\""));
		
		assertThat(json, not(containsString("\"defaultType\"")));
		
	}
	
	@Test
	public void nullsOnObject() throws Exception {
		ObjectMapper om = new ObjectMapper();
		
		DummyPerson person = makePerson();
		person.setSurname(null);
		
		String json = om.writeValueAsString(person);
		assertThat(json, not(containsString("\"surname\":")));
		
		om.setOption(DefaultOptions.INCLUDE_NULLS, true);
		json = om.writeValueAsString(person);
		assertThat(json, containsString("\"surname\":"));
		assertThat(json, containsString("null"));
	}
	
	
	@Test
	public void empties() throws Exception {
		ObjectMapper om = new ObjectMapper();
		
		DummyPerson person = makePerson();
		person.getAddresses().clear();
		
		String json = om.writeValueAsString(person);
		assertThat(json, not(containsString("\"addresses\":")));
	}
	
	@Test
	public void emptyRoots() throws Exception {
		ObjectMapper om = new ObjectMapper();

		{
			String json = om.writeValueAsString(new ArrayList<String>());
			assertThat(json, equalTo("[]"));
		}
		{
			String json = om.writeValueAsString(new String[] {});
			assertThat(json, equalTo("[]"));
		}
		{
			String json = om.writeValueAsString(new HashMap<String,String>());
			assertThat(json, equalTo("{}"));
		}
	}
	
	@Test
	public void objectLoop() throws Exception {
		DummyPerson person1 = new DummyPerson();
		DummyPerson person2 = new DummyPerson();
		
		person1.setIdentifier("p1");
		person2.setIdentifier("p2");
		
		person1.getFriends().add(person2);
		person2.getFriends().add(person1);
		
		ObjectMapper om = new ObjectMapper();
		DummyEntityProvider prov = new DummyEntityProvider();
		om.setEntityNameProvider(prov);
		om.setEntityFactory(prov);
		om.setClassDataFactory(prov);
		
		String json = null;
		try {
			json = om.writeValueAsString(person1);
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getClass().getName() + " : " + t.getMessage());
		}
		
		System.out.println(json);
		
		assertThat(json,containsString("p1"));
		assertThat(json,containsString("p2"));
		assertThat(json,containsString("[\"p1\"]"));
	}

	@Test
	public void objectWithOutFields() throws Exception {
		DummyPerson person = makePerson();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();

		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses.address");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			assertThat(json, containsString("\"name\":"));
			assertThat(json, containsString("\"Simone\""));
			
			assertThat(json, not(containsString("\"surname\":")));
			assertThat(json, not(containsString("\"Gianni\"")));
			
			assertThat(json, containsString("\"addresses\":"));
			
			assertThat(json, containsString("\"address\":"));
			assertThat(json, containsString("\"simoneg@apache.org\""));
			
			assertThat(json, not(containsString("\"type\":")));
			assertThat(json, not(containsString("\"EMAIL\"")));
			assertThat(json, not(containsString("\"similars\":")));
			
		}
		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			assertThat(json, containsString("\"name\":"));
			assertThat(json, containsString("\"Simone\""));
			
			assertThat(json, not(containsString("\"surname\":")));
			assertThat(json, not(containsString("\"Gianni\"")));
			
			assertThat(json, containsString("\"addresses\":"));
			
			assertThat(json, containsString("\"address\":"));
			assertThat(json, containsString("\"simoneg@apache.org\""));
			
			assertThat(json, containsString("\"type\":"));
			assertThat(json, containsString("\"EMAIL\""));
		}
		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("similars.name");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			assertThat(json, containsString("\"name\":"));
			assertThat(json, containsString("\"Simone\""));
			
			assertThat(json, not(containsString("\"surname\":")));
			assertThat(json, not(containsString("\"Gianni\"")));
			
			assertThat(json, containsString("\"similars\":"));
		}
		
	}

	@Test
	public void objectWithOutFieldsOverride() throws Exception {
		DummyPerson person = makePerson();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();

		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			assertThat(json, not(containsString("Doremi")));
		}
		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses.*");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			assertThat(json, containsString("Doremi"));
		}
	}
	
	@Test
	public void entity() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DummyEntityProvider provider = new DummyEntityProvider();
		mapper.setEntityNameProvider(provider);
		mapper.setEntityFactory(provider);
		mapper.setClassDataFactory(provider);		
		mapper.init();

		DummyPerson person = makePerson();
		
		String json = mapper.writeValueAsString(person, null);
		System.out.println(json);
		
		assertThat(json, containsString("\"@entity\":"));
		assertThat(json, containsString("\"Person\""));

		assertThat(json, containsString("\"@entity\":"));
		assertThat(json, containsString("\"Address\""));
		
		assertThat(json, containsString("\"id\":"));
		assertThat(json, containsString("\"p1\""));
		assertThat(json, containsString("\"a1\""));
		
		assertThat(json, not(containsString("\"identifier\":")));
	}
	
	@Test
	public void includes() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		OutField of = OutField.getRoot("inclAlways","inclNotNull","inclNotEmpty");

		// Defaults to off
		mapper.setOption(DefaultOptions.INCLUDE_EMPTY, false);
		mapper.setOption(DefaultOptions.INCLUDE_NULLS, false);
		// all nulls
		{
			DummyAnnotations da = new DummyAnnotations();
			da.setInclAlways(null);
			da.setInclNotEmpty(null);
			da.setInclNotNull(null);
			String json = mapper.writeValueAsString(da, of);
			System.out.println(json);
			assertThat(json, containsString("inclAlways"));
			assertThat(json, not(containsString("inclNotNull")));
			assertThat(json, not(containsString("inclNotEmpty")));
		}
		// all empty
		{
			DummyAnnotations da = new DummyAnnotations();
			String json = mapper.writeValueAsString(da, of);
			System.out.println(json);
			assertThat(json, containsString("inclAlways"));
			assertThat(json, containsString("inclNotNull"));
			assertThat(json, not(containsString("inclNotEmpty")));
		}
		// all with elements
		{
			DummyAnnotations da = new DummyAnnotations();
			da.setInclAlways(Arrays.asList("test"));
			da.setInclNotEmpty(Arrays.asList("test"));
			da.setInclNotNull(Arrays.asList("test"));
			String json = mapper.writeValueAsString(da, of);
			System.out.println(json);
			assertThat(json, containsString("inclAlways"));
			assertThat(json, containsString("inclNotNull"));
			assertThat(json, containsString("inclNotEmpty"));
		}
	}
	

}
