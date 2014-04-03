package net.etalia.jalia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TypeUtil {

	private static ConcurrentMap<Type, TypeUtil> cache = new ConcurrentHashMap<Type, TypeUtil>();
	
	public static TypeUtil get(Type type) {
		if (type == null) return null;
		TypeUtil ret = cache.get(type);
		if (ret != null) return ret;
		ret = new TypeUtil(type);
		TypeUtil pre = cache.putIfAbsent(type, ret);
		if (pre != null) ret = pre;
		return ret;
	}
	
	private Type type;
	private Class<?> concrete;
	
	public TypeUtil(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
	
	public Class<?> getConcrete() {
		if (concrete != null) return concrete;
		if (type instanceof Class) {
			concrete = (Class<?>)type;
		} else if (type instanceof ParameterizedType) {
			concrete = (Class<?>)((ParameterizedType) type).getRawType();
		} else if (type instanceof WildcardType) {
			concrete = (Class<?>)((WildcardType) type).getUpperBounds()[0];
		} else throw new IllegalArgumentException("Can't parse type " + type);		
		return concrete;
	}
	
	public boolean hasConcrete() {
		try {
			getConcrete();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private Type resolveType(Type type) {
		if (!(type instanceof TypeVariable)) return type;
		TypeUtil ptype = this;
		while (ptype != null && !(ptype.type instanceof ParameterizedType)) {
			ptype = get(ptype.getConcrete().getGenericSuperclass());
		}
		if (ptype == null) throw new IllegalStateException("Cannot resolve type variable " + type + " because my type is not a ParameterizedType : " + this.type); 
		TypeVariable var = (TypeVariable) type;
		Class<?> conc = ptype.getConcrete();
		TypeVariable<?>[] params = conc.getTypeParameters();
		int ind = -1;
		for (int i = 0; i < params.length; i++) {
			if (params[i].equals(var)) {
				ind = i;
				break;
			}
		}
		if (ind == -1) throw new IllegalStateException("Cant resolve type variable " + this.type + " " + type);
		return ((ParameterizedType)ptype.type).getActualTypeArguments()[ind];
	}
	
	public TypeUtil findReturnTypeOf(String methodName, Class<?>... params) {
		Method method;
		try {
			method = getConcrete().getMethod(methodName, params);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException("Cannot find method " + getConcrete().getName() + "." + methodName, e);
		}
		return get(resolveType(method.getGenericReturnType()));
	}
	
	public TypeUtil findParameterOf(String methodName, int paramIndex) {
		try {
			Method[] methods = getConcrete().getMethods();
			for (Method m : methods) {
				if (!m.getName().equals(methodName)) continue;
				Type[] params = m.getGenericParameterTypes();
				if (params.length <= paramIndex) continue;
				return get(resolveType(params[paramIndex]));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot find method " + getConcrete().getName() + "." + methodName, e);			
		}
		throw new IllegalArgumentException("Cannot find method " + getConcrete().getName() + "." + methodName + " with at least " + paramIndex + " parameters");			
	}
	
	public boolean isInstantiatable() {
		if (!hasConcrete()) return false;
		Class<?> concrete = getConcrete();
		if (concrete == null) return false;
		if (concrete.isAnnotation() || concrete.isArray() || concrete.isEnum() || concrete.isInterface() || concrete.isPrimitive() || concrete.isSynthetic()) return false;
		try {
			Constructor<?> constructor = concrete.getConstructor();
			return constructor != null;
		} catch (NoSuchMethodException e) {			
		}
		try {
			Constructor<?> constructor = concrete.getDeclaredConstructor();
			return constructor != null;
		} catch (NoSuchMethodException e) {			
		}
		return false;
	}
	
	public <T> T newInstance() {
		T ret = null;
		Class<T> clazz = (Class<T>) getConcrete();
		try {
			ret =clazz.newInstance();
		} catch (Exception e) {
			try {
				Constructor<T> con = clazz.getDeclaredConstructor();
				con.setAccessible(true);
				ret = con.newInstance();
			} catch (NoSuchMethodException nsm) {
			} catch (Exception e2) {
				e = e2;
			}
			if (ret == null)
				throw new IllegalStateException("Cannot instantiate a " + clazz, e);
		}
		return ret;
	}

	public boolean isNullable() {
		return !getConcrete().isPrimitive();
	}
	
	public boolean isEnum() {
		return Enum.class.isAssignableFrom(getConcrete());
	}

	public Enum<?> getEnumValue(String val) {
		Enum<?>[] enums=(Enum<?>[]) getConcrete().getEnumConstants();
		for (Enum<?> v : enums) {
			if (v.name().equals(val)) return v;
		}
		throw new IllegalStateException("Cannot find enum value : " + getConcrete().getName() + "." + val);
	}

	public boolean isCharSequence() {
		return CharSequence.class.isAssignableFrom(getConcrete());
	}

	public boolean isDouble() {
		return Double.class == getConcrete() || Double.TYPE == getConcrete();
	}

	public boolean isInteger() {
		return Integer.class == getConcrete() || Integer.TYPE == getConcrete();
	}
	
	public boolean isLong() {
		return Long.class == getConcrete() || Long.TYPE == getConcrete();
	}

	public boolean isArray() {
		Class<?> conc = null;
		try {
			conc = getConcrete();
		} catch (Exception e) {
			return false;
		}
		return conc.isArray();
	}
	
	public TypeUtil getArrayType() {
		return get(getConcrete().getComponentType());
	}
	
	public static abstract class Specific<T> {
		public T mockGet() {
			return null;
		}
		public TypeUtil type() {
			return get(this.getClass()).findReturnTypeOf("mockGet");
		}
	}
	
	@Override
	public String toString() {
		return "TypeUtil[" + this.type + "]";
	}

}
