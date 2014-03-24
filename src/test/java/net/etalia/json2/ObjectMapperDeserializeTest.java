package net.etalia.json2;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.etalia.json2.DummyAddress.AddressType;

import org.junit.Test;

public class ObjectMapperDeserializeTest {

	private String replaceQuote(String json) {
		return json.replace("'", "\"");
	}
	
	@Test
	public void simpleMap() throws Exception {
		String json = "{ 'testString':'string', 'testInt':1, 'testBoolean':true, 'subMap' : { 'subString':'subString' }}";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, null);
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(Map.class));
		
		Map<String,Object> map = (Map<String, Object>) ret;
		assertThat(map, hasEntry("testString", (Object)"string"));
		assertThat(map, hasEntry("testInt", (Object)1.0d));
		assertThat(map, hasEntry("testBoolean", (Object)true));
		
		Object subMapObj = map.get("subMap");
		assertThat(subMapObj, notNullValue());
		assertThat(subMapObj, instanceOf(Map.class));
		
		Map<String,String> subMap = (Map<String, String>) subMapObj;
		assertThat(subMap, hasEntry("subString", "subString"));
	}
	
	@Test
	public void intMap() throws Exception {
		String json = "{ 'a1' : 1, 'a2' : 2}";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<Map<String,Integer>>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(Map.class));
		
		Map<String,Integer> map = (Map<String, Integer>) ret;
		assertThat(map, hasEntry("a1", 1));
		assertThat(map, hasEntry("a2", 2));
	}
	
	@Test(expected=IllegalStateException.class)
	public void intMapError() throws Exception {
		String json = "{ 'a1' : 1, 'a2' : 'ciao'}";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		mapper.readValue(json, new TypeUtil.Specific<Map<String,Integer>>() {}.type());
	}

	@Test
	public void simpleList() throws Exception {
		String json = "[ 1, 'a2', true]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, null);
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(List.class));
		
		List<Object> list = (List<Object>) ret;
		assertThat(list, contains((Object)1.0d, "a2", true));
	}

	@Test
	public void intList() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<List<Integer>>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(List.class));
		
		List<Integer> list = (List<Integer>) ret;
		assertThat(list, contains(1,2,3));
	}
	
	@Test(expected=IllegalStateException.class)
	public void intListError() throws Exception {
		String json = "[ 1, 2, 'ciao']";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		mapper.readValue(json, new TypeUtil.Specific<List<Integer>>() {}.type());
	}
	
	@Test
	public void intLinkedList() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<LinkedList<Integer>>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(List.class));
		assertThat(ret, instanceOf(LinkedList.class));
		
		List<Integer> list = (List<Integer>) ret;
		assertThat(list, contains(1,2,3));
	}
	
	@Test
	public void intArray() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<int[]>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret.getClass().isArray(), equalTo(true));
		
		int[] list = (int[]) ret;
		assertThat(list[0], equalTo(1));
		assertThat(list[1], equalTo(2));
		assertThat(list[2], equalTo(3));
	}
	
	
	@Test
	public void simpleEntity() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'addresses':[" +
						"{" +
							"'type':'EMAIL'," +
							"'address':'m.rossi@gmail.com'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		Object val = om.readValue(json.replace("'", "\""), null);
		
		assertThat(val, notNullValue());
		assertThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		assertThat(person.getName(), equalTo("Mario"));
		assertThat(person.getSurname(), equalTo("Rossi"));
		
		assertThat(person.getAddresses(), hasSize(1));
		assertThat(person.getAddresses().get(0), notNullValue());
		assertThat(person.getAddresses().get(0), instanceOf(DummyAddress.class));
		
		assertThat(person.getAddresses().get(0).getType(), equalTo(AddressType.EMAIL));
		assertThat(person.getAddresses().get(0).getAddress(), equalTo("m.rossi@gmail.com"));
	}
	
	@Test
	public void entityFromExisting() throws Exception {
		String json = 
				"{" +
					"'id':'p1'," +
					"'@entity':'Person'," +
					"'addresses':[" +
						"{" +
							"'type':'EMAIL'," +
							"'address':'m.rossi@gmail.com'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.setEntityFactory(new DummyEntityProvider());
		om.init();
		Object val = om.readValue(json.replace("'", "\""), null);
		
		assertThat(val, notNullValue());
		assertThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		assertThat(person.getName(), equalTo("Simone"));
		assertThat(person.getSurname(), equalTo("Gianni"));
		
		assertThat(person.getAddresses(), hasSize(1));
		assertThat(person.getAddresses().get(0), notNullValue());
		assertThat(person.getAddresses().get(0), instanceOf(DummyAddress.class));
		
		assertThat(person.getAddresses().get(0).getType(), equalTo(AddressType.EMAIL));
		assertThat(person.getAddresses().get(0).getAddress(), equalTo("m.rossi@gmail.com"));
	}
	
	@Test(expected=IllegalStateException.class)
	public void wrongHint() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'addresses':[" +
						"{" +
							"'type':'EMAIL'," +
							"'address':'m.rossi@gmail.com'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		om.readValue(json.replace("'", "\""), new TypeUtil.Specific<Integer>(){}.type());
	}
	
	@Test(expected=IllegalStateException.class)
	public void wrongInnerType() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'addresses':[" +
						"{" +
							"'@entity':'Person'," +
							"'name':'wrong'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		om.readValue(json.replace("'", "\""), null);
	}
	
	@Test
	public void differentEntitiesInList() throws Exception {
		DummyPerson person = new DummyPerson();
		
	}
	
}
