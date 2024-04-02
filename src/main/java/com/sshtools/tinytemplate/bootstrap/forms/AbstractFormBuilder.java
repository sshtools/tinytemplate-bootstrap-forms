package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public abstract class AbstractFormBuilder<T, FORM, BLDR extends AbstractFormBuilder<T, FORM,BLDR>> extends AbstractBuilder<BLDR> {
	Optional<T> instance = Optional.empty();
	Optional<Class<T>> type = Optional.empty();
	Optional<Locale> locale = Optional.empty();
	List<Field<T, ?>> fields = new ArrayList<>();
	Map<Templatable, TemplateResource> templates = new HashMap<>(Form.DEFAULT_TEMPLATES);
	Optional<ResourceBundle> bundle = Optional.empty();
	Optional<String> bundleName = Optional.empty();
	Optional<ClassLoader> bundleClassLoader = Optional.empty();
	Set<String> groupCssClass = Set.of("mb-3");
	Set<String> rowCssClass = Set.of("row", "mb-3");
	boolean disabled;
	boolean floatingLabel;
	Optional<Text> validFeedback = Optional.empty();
	boolean feedback = true;
		
	protected AbstractFormBuilder(Optional<T> instance, Optional<Class<T>> type) {
		super();
		this.instance = instance;
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	public BLDR validFeedback(Text validFeedback) {
		this.validFeedback = Optional.of(validFeedback);
		return (BLDR)this;
	}
		
	public BLDR noFeedback() {
		return feedback(false);
	}
		
	@SuppressWarnings("unchecked")
	public BLDR feedback(boolean feedback) {
		this.feedback = feedback;
		return (BLDR)this;
	}
	
	public BLDR disabled() {
		return disabled(true);
	}
		
	@SuppressWarnings("unchecked")
	public BLDR disabled(boolean disabled) {
		this.disabled = disabled;
		return (BLDR)this;
	}
		
	public BLDR floatingLabel() {
		return floatingLabel(true);
	}
		
	@SuppressWarnings("unchecked")
	public BLDR floatingLabel(boolean floatingLabel) {
		this.floatingLabel = floatingLabel;
		return (BLDR)this;
	}
		
	public BLDR groupCssClass(String... groupCssClasses) {
		return groupCssClass(Set.of(groupCssClasses));
	}
		
	@SuppressWarnings("unchecked")
	public BLDR groupCssClass(Set<String> groupCssClasses) {
		this.groupCssClass = groupCssClasses;
		return (BLDR)this;
	}
		
	public BLDR rowCssClass(String... rowCssClasses) {
		return rowCssClass(Set.of(rowCssClasses));
	}
		
	@SuppressWarnings("unchecked")
	public BLDR rowCssClass(Set<String> rowCssClasses) {
		this.rowCssClass = rowCssClasses;
		return (BLDR)this;
	}
	
	@SuppressWarnings("unchecked")
	public BLDR bundle(ResourceBundle bundle) {
		this.bundle = Optional.of(bundle);
		return (BLDR)this;
	}
	
	@SuppressWarnings("unchecked")
	public BLDR locale(Locale locale) {
		this.locale = Optional.of(locale);
		return (BLDR)this;
	}
	
	public BLDR bundle(Class<?> clazz) {
		return bundle(clazz.getName()).bundleClassLoader(clazz.getClassLoader());
	}
	
	@SuppressWarnings("unchecked")
	public BLDR bundle(String name) {
		this.bundleName = Optional.of(name);
		return (BLDR)this;
	}
	
	@SuppressWarnings("unchecked")
	public BLDR bundleClassLoader(ClassLoader loader) {
		this.bundleClassLoader = Optional.of(loader);
		return (BLDR)this;
	}

	@SuppressWarnings("unchecked")
	public BLDR templates(Map<Templatable, TemplateResource> templates) {
		this.templates = templates;
		return (BLDR)this;
	}
				
	@SuppressWarnings("unchecked")
	public BLDR template(Template template, TemplateResource resource) {
		templates.put(template, resource);
		return (BLDR)this;
	}

	@SuppressWarnings("unchecked")
	Class<T> resolveType() {
		return type.orElseGet(() -> (Class<T>)instance.orElseThrow(() -> new IllegalStateException("No type.")).getClass());
	}
	
	public abstract FORM build();
}
