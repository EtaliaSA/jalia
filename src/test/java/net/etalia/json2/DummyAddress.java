package net.etalia.json2;

import net.etalia.json2.annotations.JsonIgnore;

public class DummyAddress extends DummyEntity {

	public enum AddressType {
		HOME,
		OFFICE,
		EMAIL
	}
	
	private AddressType type;
	private String address;
	
	public AddressType getType() {
		return type;
	}
	public void setType(AddressType type) {
		this.type = type;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
}
