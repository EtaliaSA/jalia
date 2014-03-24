package net.etalia.json2;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import net.etalia.json2.DummyAddress.AddressType;

import org.junit.Before;
import org.junit.Test;

public class JsonClassDataTest {

	@Before
	public void clearCache() {
		JsonClassData.clearCache();
	}
	
	@Test
	public void gettables() throws Exception {
		JsonClassData jcd = JsonClassData.get(DummyPerson.class, null);
		assertThat(jcd, notNullValue());
		
		JsonClassData jcd2 = JsonClassData.get(DummyPerson.class, null);
		assertThat(jcd2, sameInstance(jcd));
		
		assertThat(jcd.getGettables(), notNullValue());
		System.out.println(jcd.getGettables());
		
		assertThat(jcd.getGettables(), hasItem("name"));
		assertThat(jcd.getGettables(), hasItem("surname"));
		assertThat(jcd.getGettables(), hasItem("addresses"));
		assertThat(jcd.getGettables(), hasItem("identifier"));
		assertThat(jcd.getGettables(), not(hasItem("class")));
		assertThat(jcd.getGettables(), not(hasItem("password")));
	}
	
	@Test
	public void getting() throws Exception {
		JsonClassData jcd = JsonClassData.get(DummyPerson.class, null);

		DummyPerson person = new DummyPerson();
		person.setName("Simone");
		person.setSurname("Gianni");
		
		DummyAddress address = new DummyAddress();
		address.setType(AddressType.EMAIL);
		address.setAddress("simoneg@apache.org");
		
		person.getAddresses().add(address);
		
		assertThat((String)jcd.getValue("name", person), equalTo("Simone"));
		assertThat((String)jcd.getValue("surname", person), equalTo("Gianni"));
	}
	
	@Test
	public void altered() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setEntityFactory(new DummyEntityProvider());
		JSONContext ctx = new JSONContext(mapper);
		
		JsonClassData jcd = JsonClassData.get(DummyPerson.class, ctx);
		assertThat(jcd, notNullValue());
		
		assertThat(jcd.getGettables(), notNullValue());
		System.out.println(jcd.getGettables());
		
		assertThat(jcd.getGettables(), hasItem("name"));
		assertThat(jcd.getGettables(), hasItem("surname"));
		assertThat(jcd.getGettables(), hasItem("addresses"));
		assertThat(jcd.getGettables(), not(hasItem("class")));
		assertThat(jcd.getGettables(), not(hasItem("identifier")));
	}
	
	@Test
	public void settings() throws Exception {
		JsonClassData jcd = JsonClassData.get(DummyPerson.class, null);

		DummyPerson person = new DummyPerson();
		jcd.setValue("name", "Simone", person);
		
		assertThat(person.getName(), equalTo("Simone"));
	}

	@Test
	public void annotations() throws Exception {
		JsonClassData jcd = JsonClassData.get(DummyAnnotations.class, null);
		
		assertThat(jcd.getGettables(), containsInAnyOrder("both","getOnly","getOnlyByGetter","unusual"));
		assertThat(jcd.getSettables(), containsInAnyOrder("both","setOnly","setOnlyBySetter","unusual"));
	}
	
}
