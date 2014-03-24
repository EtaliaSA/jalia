package net.etalia.json2;

import java.util.ArrayList;
import java.util.List;

import net.etalia.json2.annotations.JsonIgnore;


public class DummyPerson extends DummyEntity {

	private String name;
	private String surname;
	
	private List<DummyAddress> addresses = new ArrayList<>(); 
	
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
	
}
