package com.sshtools.tinytemplate.bootstrap.forms;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.Templates.TemplateProcessor;
import com.sshtools.tinytemplate.bootstrap.forms.annotations.FormField;
import com.sshtools.tinytemplate.bootstrap.forms.annotations.FormObject;

/**
 * Builds a Bootstrap form given an {@link Object} or {@link Class} using reflection. 
 * <p>
 * If a plain old Java object is passed, with no additional annotation, then
 * all public declared fields and getters will have fields generated.
 * <p>
 * Default fields will have their type inferred from the field type or
 * method return type, and be named based on their processed names.
 * <p>
 * Any fields behaviour may be altered using {@link FormField}, and the
 * behaviour of the object as a whole can be altered by {@link FormObject}.  
 * 
 * @param <T> type of object
 */
public final class ReflectiveForm<T> extends AbstractElement implements FormType<T> {

	public final static class Builder<T> extends AbstractFormBuilder<T, ReflectiveForm<T>, Builder<T>> {
		
		private Optional<Locale> locale = Optional.empty();
		
		public static <T> Builder<T> create(Class<T> type) {
			return new Builder<>(Optional.empty(), Optional.of(type));
		}
		
		public static <T> Builder<T> edit(T instance) {
			return new Builder<>(Optional.of(instance), Optional.empty());
		}
		
		private Builder(Optional<T> instance, Optional<Class<T>> type) {
			super(instance, type);
		}
		
		public Builder<T> locale(Locale locale) {
			this.locale = Optional.of(locale);
			return this;
		}

		@Override
		public ReflectiveForm<T> build() {
			return new ReflectiveForm<>(this);
		}
	}

	private final Form<T> form;
	private final Locale locale;
	
	@SuppressWarnings("unchecked")
	private ReflectiveForm(Builder<T> bldr) {
		super(bldr);

		locale = bldr.locale.orElseGet(Locale::getDefault);
		var formBldr = new Form.Builder<T>(bldr.instance, bldr.type);
		formBldr.templates(bldr.templates);
		
		var type = bldr.resolveType();
		var formObjectData = type.getAnnotation(FormObject.class);
		
		formBldr.id =  bldr.id.or(() -> Optional.ofNullable(
			formObjectData == null || formObjectData.id().equals("") ? null : formObjectData.id()
		));
		
		java.lang.reflect.Field[] fields;
		if(formObjectData == null || !formObjectData.declared()) {
			fields = type.getDeclaredFields();
		}
		else {
			fields = type.getFields();
		}
		
		var all = formObjectData == null || formObjectData.all();
		
		// Fields 
		
		for(var field : fields) {
			var annot = field.getAnnotation(FormField.class);
			
			if( (all && (annot == null || !annot.exclude())) || 
                (!all && (annot != null && !annot.exclude())) 					
			  ) {
				
				formBldr.field(fldbldr -> {
					fldbldr.type((Class<Object>) field.getType());
					configureFromAnnotation(field.getName(), annot, fldbldr);
				});
				
			}
		}
		
		// Methods
		
		Method[] methods;
		if(formObjectData == null || !formObjectData.declared()) {
			methods = type.getDeclaredMethods();
		}
		else {
			methods = type.getMethods();
		}
		
		for(var method : methods) {
			
			var annot = method.getAnnotation(FormField.class);
			
			if(method.getReturnType().equals(void.class) || method.getReturnType().equals(Void.class) || method.getParameterCount() > 0) {
				continue;
			}
			
			var name = isGetter(method.getName());
			if(name == null) {
				continue;
			}
			
			if(annot == null && ( 
					name.equals("class") || 
					name.equals("toString") || 
					name.equals("hashCode")
				) ) {
				continue;
			}
					
			
			if( (all && (annot == null || !annot.exclude())) || 
                (!all && (annot != null && !annot.exclude())) 					
			  ) {
			
				formBldr.field(fldbldr -> {
					fldbldr.type((Class<Object>) method.getReturnType());
					configureFromAnnotation(name, annot, fldbldr);
					
				});
				
			}
		}
		
		form = formBldr.build();
		
	}
	
	private String isGetter(String name) {
		if(name.startsWith("get")) {
			if(name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
				return Character.toLowerCase(name.charAt(3)) + name.substring(4);
			}
			else
				return null;
		}
		else if(name.startsWith("is")) {
			if(name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
				return Character.toLowerCase(name.charAt(2)) + name.substring(3);
			}
			else
				return null;
		}
		else if(Character.isLowerCase(name.charAt(0)))
			return name;
		else
			return null;
	}

	private void configureFromAnnotation(String id, FormField annot,
			Field.Builder<T, Object> fldbldr) {
		if(annot == null) {
			fldbldr.id(id);
		}
		else {
			if(annot.id().equals("")) {
				fldbldr.id(id);
			}
			else {
				fldbldr.id(annot.id());
			}
			fldbldr.input(annot.type());
			if(annot.span() >0) {
				fldbldr.span(annot.span());
			}
			
			if(annot.noLabel())
				fldbldr.noLabel();
			else {
				fieldText(annot.label(), annot.labelKey(), annot.labelBundle()).ifPresent(
						txt -> fldbldr.label(txt)
				);						
			}
			
			fieldText(annot.help(), annot.helpKey(), annot.helpBundle()).ifPresent(
					txt -> fldbldr.help(txt)
			);
			
			fieldText(annot.placeholder(), annot.placeholderKey(), annot.placeholderBundle()).ifPresent(
					txt -> fldbldr.placeholder(txt));

			for(var attr : annot.attrs()) {
				var idx = attr.indexOf('=');
				String name;
				String val;
				if(idx == -1) {
					name = attr;
					val = null;
				}
				else {
					name = attr.substring(0, idx);
					val = attr.substring(idx + 1);
				}
				fldbldr.attr(name, val);
			}
			
			/* TODO classes etc */
		}
	}

	private Optional<Text> fieldText(String text, String key, Class<?> bundle) {
		Text.Builder bldr = new Text.Builder();
		var empty = true;
		
		if(!text.equals(""))  {
			bldr.text(text);
			empty = false;
		}
		
		if(!key.equals("")) { 
			bldr.key(key);
			empty = false;
		}
		
		if(!bundle.equals(Void.class)) { 
			bldr.bundle(ResourceBundle.getBundle(bundle.getName(), locale, bundle.getClassLoader()));
			empty = false;
		}
		
		if(empty)
			return Optional.empty();
		else
			return Optional.of(bldr.build());
	}

	@Override
	public TemplateModel model() {
		return form.model();
	}
	
	@FormObject
	private final static record TestObject(
			@FormField(help = "Some help", noLabel = true)
			String name, int age
	) { }
	
	
	public static void main(String[] args) {
		var form = ReflectiveForm.Builder.edit(new TestObject("Brett Smith", 27)).build();
		var tp = new TemplateProcessor.Builder().build();
		System.out.println(tp.process(form.model()));
	}
}
