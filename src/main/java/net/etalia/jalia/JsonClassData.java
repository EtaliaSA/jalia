package net.etalia.jalia;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.annotations.DefaultFieldsSerialization;
import net.etalia.jalia.annotations.JsonGetter;
import net.etalia.jalia.annotations.JsonIgnore;
import net.etalia.jalia.annotations.JsonIgnoreProperties;
import net.etalia.jalia.annotations.JsonSetter;
import net.etalia.utils.LockHashMap;

public class JsonClassData {

	protected static final Set<String> ALL_SET = new HashSet<>();
	
	static {
		ALL_SET.add("*");
	}
	
	protected Class<?> clazz;
	
	protected Set<String> defaults = new HashSet<>();
	
	protected Map<String,Method> getters = new HashMap<>();
	protected Map<String,Method> setters = new HashMap<>();

	protected TypeUtil typeUtil = null;
	
	protected JsonClassData(JsonClassData other) {
		this.clazz = other.clazz;
		this.typeUtil = other.typeUtil;
		this.defaults.addAll(other.defaults);
		this.getters.putAll(other.getters);
		this.setters.putAll(other.setters);
	}
	
	protected JsonClassData(Class<?> clazz) {
		this.clazz = clazz;
		parse(clazz);
	}
	
	private void parse(Class<?> c) {
		this.typeUtil = TypeUtil.get(c);

		Set<String> ignore = new HashSet<String>();
		// Parse JsonIgnoreProperties
		{
			JsonIgnoreProperties ignoreAnn = c.getAnnotation(JsonIgnoreProperties.class);
			if (ignoreAnn != null) {
				ignore.addAll(Arrays.asList(ignoreAnn.value()));
			}
		}
		// Parse DefaultFieldsSerialization
		// TODO move this to the etalia custom factory or move the annotation to this package
		{
			DefaultFieldsSerialization defaultfields = c.getAnnotation(DefaultFieldsSerialization.class);
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
			if (method.getName().startsWith("get")) {
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
			name = name.substring(3);
			name = Introspector.decapitalize(name);
			if (ignore.contains(name) && !explicitSet && (ignoreAnn == null || ignoreAnn.value())) return "!" + name;
		}
		if (ignoreAnn != null && ignoreAnn.value()) {
			ignore.add(name);
			return "!"+name;
		}		
		return name;
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
		if (this.getters.containsKey(name)) return;
		this.getters.put(name, method);
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
		if (method == null) throw new IllegalStateException("Can't find a getter for " + name);
		try {
			return method.invoke(obj);
		} catch (Throwable e) {
			throw new IllegalStateException("Error invoking " + method, e);
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
		if (setters.containsKey(name)) {
			return TypeUtil.get(setters.get(name).getGenericParameterTypes()[0]);
		}
		return null;
	}

	public TypeUtil getGetHint(String name) {
		if (getters.containsKey(name)) {
			return TypeUtil.get(getters.get(name).getGenericReturnType());
		} 
		return null;
	}
	
	public void setValue(String name, Object nval, Object tgt) {
		Method method = this.setters.get(name);
		if (method == null) throw new IllegalStateException("Can't find a setter for " + name);
		try {
			method.invoke(tgt, nval);
		} catch (Throwable e) {
			throw new IllegalStateException("Error invoking " + method, e);
		}
	}
	
	public Object prepare(Object obj, boolean serializing) {
		return obj;
	}
	
	public Object finish(Object obj, boolean serializing) {
		return obj;
	}
	
}
