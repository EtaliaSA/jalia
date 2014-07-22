package net.etalia.jalia;

import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonIgnore;

@JsonDefaultFields("identifier,type,address")
public class DummyAddress extends DummyEntity {

	public enum AddressType {
		HOME,
		OFFICE,
		EMAIL
	}
	
	public static AddressType getDefaultType() {
		return AddressType.HOME;
	}
	
	private AddressType type;
	private String address;
	private String notes;
	
	private DummyAddress() {}
	
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
	
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}

	@Override
	public String toString() {
		return "DummyAddress [type=" + type + ", address=" + address
				+ ", id=" + getIdentifier() + "]";
	}
	
	
}
