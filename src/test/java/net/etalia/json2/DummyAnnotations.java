package net.etalia.json2;

import net.etalia.json2.annotations.JsonGetter;
import net.etalia.json2.annotations.JsonIgnore;
import net.etalia.json2.annotations.JsonSetter;

public class DummyAnnotations {

	private String both;
	private String secretByGetter;
	private String secretBySetter;
	private String getOnly;
	private String getOnlyByGetter;
	private String setOnly;
	private String setOnlyBySetter;
	private String unusualGetter;
	
	public String getBoth() {
		return both;
	}
	public void setBoth(String both) {
		this.both = both;
	}

	@JsonIgnore
	public String getSecretByGetter() {
		return secretByGetter;
	}
	public void setSecretByGetter(String secretByGetter) {
		this.secretByGetter = secretByGetter;
	}
	
	public String getSecretBySetter() {
		return secretBySetter;
	}
	@JsonIgnore
	public void setSecretBySetter(String secretBySetter) {
		this.secretBySetter = secretBySetter;
	}

	@JsonIgnore(false)
	public String getGetOnly() {
		return getOnly;
	}
	@JsonIgnore
	public void setGetOnly(String getOnly) {
		this.getOnly = getOnly;
	}
	
	@JsonGetter
	public String getGetOnlyByGetter() {
		return getOnlyByGetter;
	}
	@JsonIgnore
	public void setGetOnlyByGetter(String getOnlyByGetter) {
		this.getOnlyByGetter = getOnlyByGetter;
	}
	
	@JsonIgnore
	public String getSetOnly() {
		return setOnly;
	}
	@JsonIgnore(false)
	public void setSetOnly(String setOnly) {
		this.setOnly = setOnly;
	}
	
	@JsonSetter
	public void setSetOnlyBySetter(String setOnlyBySetter) {
		this.setOnlyBySetter = setOnlyBySetter;
	}
	@JsonIgnore
	public String getSetOnlyBySetter() {
		return setOnlyBySetter;
	}
	
	@JsonGetter("unusual")
	public String unusualGetter() {
		return unusualGetter;
	}
	@JsonSetter("unusual")
	public void unusualSetter(String unusualGetter) {
		this.unusualGetter = unusualGetter;
	}
	
	
}
