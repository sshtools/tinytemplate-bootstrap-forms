package com.sshtools.tinytemplate.bootstrap.forms.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.sshtools.tinytemplate.bootstrap.forms.InputType;

@Retention(RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT })
public @interface FormField {
	String id() default "";

	InputType type() default InputType.AUTO;

	boolean exclude() default false;

	int span() default 0;

	String[] classes() default {};

	Class<?> labelBundle() default Void.class;

	String labelKey() default "";

	String label() default "";
	
	boolean noLabel() default false;

	Class<?> helpBundle() default Void.class;

	String helpKey() default "";

	String help() default "";

	Class<?> placeholderBundle() default Void.class;

	String placeholderKey() default "";

	String placeholder() default "";

	String[] attrs() default {};
}
