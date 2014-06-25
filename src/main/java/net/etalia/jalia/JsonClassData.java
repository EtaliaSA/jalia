package net.etalia.jalia;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonGetter;
import net.etalia.jalia.annotations.JsonIgnore;
import net.etalia.jalia.annotations.JsonIgnoreProperties;
import net.etalia.jalia.annotations.JsonOnDemandOnly;
import net.etalia.jalia.annotations.JsonSetter;
import net.etalia.utils.MissHolder;

public class JsonClassData {

	protected static final Set<String> ALL_SET = new HashSet<>();
	
	static {
		ALL_SET.add("*");
	}
	
	protected Class<?> clazz;
	
	protected Set<String> defaults = new HashSet<>();
	
	protected Map<String,Method> getters = new HashMap<>();
	protected Map<String,Method> ondemand = new HashMap<>();
	protected Map<String,Method> setters = new HashMap<>();
	
	// Caches
	protected Map<String,MissHolder<TypeUtil>> getHints = new HashMap<>();
	protected Map<String,MissHolder<TypeUtil>> setHints = new HashMap<>();
	
	protected boolean isNew = true;
	
	protected JsonClassData(JsonClassData other) {
		this.clazz = other.clazz;
		this.defaults.addAll(other.defaults);
		this.getters.putAll(other.getters);
		this.setters.putAll(other.setters);
		this.ondemand.putAll(other.ondemand);
	}
	
	protected JsonClassData(Class<?> clazz) {
		this.clazz = clazz;
		parse(clazz);
	}
	
	public boolean isNew() {
		return isNew;
	}
	public void unsetNew() {
		this.isNew = false;
	}
	
	private void parse(Class<?> c) {
		
		Set<String> ignore = new HashSet<String>();
		// Parse JsonIgnoreProperties
		{
			JsonIgnoreProperties ignoreAnn = c.getAnnotation(JsonIgnoreProperties.class);
			if (ignoreAnn != null) {
				ignore.addAll(Arrays.asList(ignoreAnn.value()));
			}
		}
		// Parse DefaultFieldsSerialization
		{
			JsonDefaultFields defaultfields = c.getAnnotation(JsonDefaultFields.class);
			if (defaultfields != null) {
				this.defaults.addAll(Arrays.asList(defaultfields.value().split(",")));
			}
		}
		// TODO parse JsonTypeInfo
		// TODO parse JsonSubTypes
		Method[] methods = c.getDeclaredMethods();
		// Parse annotated ones first, so that they get priority
		for (Method method : methods) {
			if (method.isAnnotationPresent(JsonGetter.class)) {
				parseGetter(method, ignore);
			}
			if (method.isAnnotationPresent(JsonSetter.class)) {
				parseSetter(method, ignore);
			}
		}
		// Parse not annotated after
		for (Method method : methods) {
			if (!Modifier.isPublic(method.getModifiers())) continue;
			if (method.getName().startsWith("get") || 
					(method.getName().startsWith("is") && 
							(method.getReturnType().equals(Boolean.class) || 
							method.getReturnType().equals(Boolean.TYPE))
				)) {
				parseGetter(method, ignore);
			}
			if (method.getName().startsWith("set")) {
				parseSetter(method, ignore);
			}
		}
		Class<?> sup = c.getSuperclass();
		if (sup != null) parse(sup);
		Class<?>[] interfaces = c.getInterfaces();
		for (Class<?> inter : interfaces) parse(inter);
	}

	private String methodName(Method method, Set<String> ignore) {
		String name = null;
		boolean explicitSet = false;
		{
			JsonGetter annotation = method.getAnnotation(JsonGetter.class);
			if (annotation != null) {
				explicitSet = true;
				name = annotation.value();
			}
		}
		{
			JsonSetter annotation = method.getAnnotation(JsonSetter.class);
			if (annotation != null) {
				explicitSet = true;
				name = annotation.value();
			}
		}
		JsonIgnore ignoreAnn = method.getAnnotation(JsonIgnore.class);
		if (name == null || name.length() == 0) {
			name = method.getName();
			if (name.startsWith("is")) {
				name = name.substring(2);
			} else {
				name = name.substring(3);
			}
			name = decapitalize(name);
			if (ignore.contains(name) && !explicitSet && (ignoreAnn == null || ignoreAnn.value())) return "!" + name;
		}
		if (ignoreAnn != null && ignoreAnn.value()) {
			ignore.add(name);
			return "!"+name;
		}		
		return name;
	}
	
    private static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                        Character.isUpperCase(name.charAt(0))){
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
	
	
	private void parseGetter(Method method, Set<String> ignore) {
		// Skip getClass
		if (method.getName().equals("getClass")) return;
		if (method.getReturnType().equals(Void.class)) return;
		if (method.getParameterTypes().length > 0) return;
		if (Modifier.isStatic(method.getModifiers())) return;
		method.setAccessible(true);
		String name = methodName(method, ignore);
		if (name.startsWith("!")) {
			// Check if to remove also the setter
			Method setter = this.setters.get(name.substring(1));
			if (setter != null) {
				if (methodName(setter, ignore).startsWith("!")) {
					this.setters.remove(name.substring(1));
				}
			}
			return;
		}
		if (this.ondemand.containsKey(name)) return;
		JsonOnDemandOnly onDemandAnn = method.getAnnotation(JsonOnDemandOnly.class);
		if (onDemandAnn != null) {
			this.ondemand.put(name, method);
			this.getters.remove(name);
		} else {
			if (this.getters.containsKey(name)) return;
			this.getters.put(name, method);
		}
	}
	
	private void parseSetter(Method method, Set<String> ignore) {
		if (method.getParameterTypes().length != 1) return;
		if (Modifier.isStatic(method.getModifiers())) return;
		method.setAccessible(true);
		String name = methodName(method, ignore);
		if (name.startsWith("!")) {
			// Check if to remove also the setter
			Method getter = this.getters.get(name.substring(1));
			if (getter != null) {
				if (methodName(getter, ignore).startsWith("!")) {
					this.getters.remove(name.substring(1));
				}
			}
			return;
		}
		if (this.setters.containsKey(name)) return;
		this.setters.put(name, method);
	}
	
	public Object getValue(String name, Object obj) {
		Method method = this.getters.get(name);
		if (method == null) method = this.ondemand.get(name);
		// TODO log this?
		if (method == null) return null;
		try {
			return method.invoke(obj);
		} catch (Throwable e) {
			// TODO log this?
			return null;
		}
	}

	public Set<String> getGettables() {
		return this.getters.keySet();
	}

	public Set<String> getSettables() {
		return this.setters.keySet();
	}
	
	public Set<String> getDefaults() {
		return this.defaults.size() > 0 ? this.defaults : ALL_SET;
	}

	public Class<?> getTargetClass() {
		return this.clazz;
	}

	public void ignoreSetter(String string) {
		this.setters.remove(string);
	}
	
	public void ignoreGetter(String string) {
		this.getters.remove(string);
	}

	public TypeUtil getSetHint(String name) {
		MissHolder<TypeUtil> found = setHints.get(name);
		if (found != null) return found.getVal();
		TypeUtil ret = null;
		if (setters.containsKey(name)) {
			ret = TypeUtil.get(setters.get(name).getGenericParameterTypes()[0]);
		}
		setHints.put(name, new MissHolder<>(ret));
		return ret;
	}

	public TypeUtil getGetHint(String name) {
		MissHolder<TypeUtil> found = getHints.get(name);
		if (found != null) return found.getVal();
		TypeUtil ret = null;
		if (getters.containsKey(name)) {
			ret = TypeUtil.get(getters.get(name).getGenericReturnType());
		} 
		getHints.put(name, new MissHolder<>(ret));
		return ret;
	}
	
	public boolean setValue(String name, Object nval, Object tgt) {
		Method method = this.setters.get(name);
		// TODO log this?
		if (method == null) return false;
		try {
			method.invoke(tgt, nval);
		} catch (Throwable e) {
			// TODO log this?
			return false;
		}
		return false;
	}
	
}
