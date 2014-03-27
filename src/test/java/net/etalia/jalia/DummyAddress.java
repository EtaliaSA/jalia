package net.etalia.jalia;

import net.etalia.jalia.annotations.JsonIgnore;

public class DummyAddress extends DummyEntity {

	public enum AddressType {
		HOME,
		OFFICE,
		EMAIL
	}
	
	private AddressType type;
	private String address;
	
	public DummyAddress() {}
	
	public DummyAddress(String id, AddressType type, String address) {
		this.setIdentifier(id);
		this.type = type;
		this.address = address;
	}
	
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

	@Override
	public String toString() {
		return "DummyAddress [type=" + type + ", address=" + address
				+ ", id=" + getIdentifier() + "]";
	}
	
	
}
