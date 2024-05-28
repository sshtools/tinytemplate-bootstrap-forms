package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.bootstrap.forms.Form.FormSection;
import com.sshtools.tinytemplate.bootstrap.forms.Form.Icon;

public abstract class AbstractFormBuilder<T, FORM, BLDR extends AbstractFormBuilder<T, FORM,BLDR>> extends AbstractBuilder<BLDR> {
	Optional<T> instance = Optional.empty();
	Optional<Class<T>> type = Optional.empty();
	Optional<Locale> locale = Optional.empty();
	List<Field<T, ?>> fields = new ArrayList<>();
	Map<Templatable, TemplateResource> templates = new HashMap<>(Form.DEFAULT_TEMPLATES);
	Map<Icon, Set<String>> icons = new HashMap<>(Form.DEFAULT_ICONS);
	Optional<ResourceBundle> bundle = Optional.empty();
	Optional<String> bundleName = Optional.empty();
	Optional<ClassLoader> bundleClassLoader = Optional.empty();
	Set<String> groupCssClass = Set.of("mb-3");
	Set<String> rowCssClass = Set.of("row", "mb-3");
	Set<String> optionsCssClass = Set.of("m-3");
	boolean disabled;
	boolean floatingLabel;
	Optional<Text> validFeedback = Optional.empty();
	boolean feedback = true;
	List<FormSection<T>> sections = new ArrayList<>();
	Optional<BiConsumer<FORM, String>> onUnknownField = Optional.empty();
		
	protected AbstractFormBuilder(Optional<T> instance, Optional<Class<T>> type) {
		super();
		this.instance = instance;
		this.type = type;
		sections.add(new FormSection<>());
	}
	
	@SuppressWarnings("unchecked")
	public BLDR onUnknownField(BiConsumer<FORM, String> onUnknownField) {
		this.onUnknownField = Optional.of(onUnknownField);
		return (BLDR)this;
	}
	
	@SuppressWarnings("unchecked")
	public BLDR renderer(Function<FormSection<T>, TemplateModel> renderer) {
		sections.add(sections.remove(sections.size() - 1).withRenderer(renderer));
		return (BLDR)this;
	}

	public BLDR section(String id, String label) {
		return section(id, Text.of(label));
	}
	public BLDR section(String id, Class<?> bundle, Object... args) {
		return section(id, Text.ofI18n(id, bundle, args));
	}
	
	@SuppressWarnings("unchecked")
	public BLDR section(String id, Text label) {
		sections.add(new FormSection<>(id, label, new ArrayList<>()));
		return (BLDR)this;
	}

	@SuppressWarnings("unchecked")
	public <F> BLDR field(Consumer<Field.Builder<T, F>> bldr) {
		var fbldr = new Field.Builder<T, F>(instance);
		bldr.accept(fbldr);
		sections.get(sections.size() - 1).fields().add(fbldr.build());
		return (BLDR)this;
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
	
	public BLDR optionsCssClass(String... optionsCssClass) {
		return optionsCssClass(Set.of(optionsCssClass));
	}
	
	@SuppressWarnings("unchecked")
	public BLDR optionsCssClass(Set<String> optionsCssClass) {
		this.optionsCssClass = optionsCssClass;
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
	public BLDR icon(Map<Icon, Set<String>> icons) {
		this.icons = icons;
		return (BLDR)this;
	}
				
	@SuppressWarnings("unchecked")
	public BLDR icon(Icon icon, Set<String> classes) {
		icons.put(icon, classes);
		return (BLDR)this;
	}

	public BLDR icon(Icon icon, String... classes) {
		return icon(icon, Set.of(classes));
	}

	@SuppressWarnings("unchecked")
	Class<T> resolveType() {
		return type.orElseGet(() -> (Class<T>)instance.orElseThrow(() -> new IllegalStateException("No type.")).getClass());
	}
	
	public abstract FORM build();
}
