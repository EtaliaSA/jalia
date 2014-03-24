package net.etalia.json2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OutField {
	private OutField parent = null;
	private String name;
	private Map<String, OutField> subs;
	private boolean all;
	
	public OutField(OutField parent, String name) {
		this.parent = parent;
		this.name = name;
	}
	
	public OutField(OutField parent) {
		this(parent, "");
	}
	
	public OutField(OutField parent, boolean all) {
		this(parent, "");
		this.all = all;
	}
	
	public OutField(boolean all) {
		this(null,"*");
		this.all = all;
	}
	
	public void setAll(boolean all) {
		this.all = all;
	}
	
	public OutField getSub(String name) {
		if (all) return new OutField(this, true);
		if (subs == null) return null;
		int di = name.indexOf('.');
		if (di == -1) {
			return subs.get(name);
		} else {
			String prename = name.substring(0, di);
			OutField sub = subs.get(prename);
			if (sub == null) return null;
			return sub.getSub(name.substring(di+1));
		}
	}
	public OutField getCreateSub(String name) {
		if (all) throw new IllegalStateException("Can't add sub fields to *");
		if (subs == null) {
			subs = new HashMap<>();
		}
		int di = name.indexOf('.');
		String mname = name;
		if (di > -1) {
			mname = name.substring(0, di);
		}
		OutField ret = subs.get(mname);
		if (ret == null) {
			if (mname.equals("*")) {
				this.all = true;
				ret = this;
			} else {
				ret = new OutField(this, mname);
				subs.put(mname, ret);
			}
		}
		if (di == -1) {
			return ret;
		} else {
			return ret.getCreateSub(name.substring(di+1));
		}
	}

	public String getName() {
		return name;
	}
	public OutField getParent() {
		if (parent == null && all) return this;
		return parent;
	}
	public Map<String, OutField> getSubs() {
		return Collections.unmodifiableMap(subs);
	}
	
	public String getFullPath() {
		if (this.parent == null) return null;
		String pfp = this.parent.getFullPath();
		return (pfp != null ? (pfp + ".") : "") + this.name;
	}

	public boolean hasSubs() {
		if (all) return true;
		return subs != null && subs.size() > 0;
	}
	
}