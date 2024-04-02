package com.sshtools.tinytemplate.bootstrap.forms.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.sshtools.tinytemplate.bootstrap.forms.InputType;

@Retention(RUNTIME)
@Target({TYPE})
public @interface FormObject {
	String id() default "";
	
	boolean all() default true;
	
	InputType type() default InputType.AUTO; 
	
	boolean declared() default true;
	
	String[] classes() default {};
}
