package net.etalia.json2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.etalia.json2.annotations.JsonIgnore;


public class DummyPerson extends DummyEntity {

	private String name;
	private String surname;
	
	private List<DummyAddress> addresses = new ArrayList<>(); 
	
	public DummyPerson() {}
	
	public DummyPerson(String id, String name, String surname, DummyAddress... addresses) {
		super.setIdentifier(id);
		this.name = name;
		this.surname = surname;
		if (addresses != null) this.addresses.addAll(Arrays.asList(addresses));
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getSurname() {
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	
	public List<DummyAddress> getAddresses() {
		return addresses;
	}

	@Override
	public String toString() {
		return "DummyPerson [name=" + name + ", surname=" + surname
				+ ", addresses=" + addresses + ", id="
				+ getIdentifier() + "]";
	}
	
	
	
}
