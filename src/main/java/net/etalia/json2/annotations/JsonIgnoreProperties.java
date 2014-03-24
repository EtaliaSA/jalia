package net.etalia.json2.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnoreProperties
{
    /**
     * Names of properties to ignore.
     */
    public String[] value() default { };

    /**
     * Unused. For compile compatibility only.
     */
    public boolean ignoreUnknown() default false;
}
