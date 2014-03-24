package net.etalia.json2.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface DefaultFieldsSerialization {
	
	public static String UNASSIGNED = "[unassigned]";

	public String value() default UNASSIGNED;

}
