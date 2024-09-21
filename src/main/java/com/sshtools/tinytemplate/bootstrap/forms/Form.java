package com.sshtools.tinytemplate.bootstrap.forms;

import static com.sshtools.tinytemplate.Templates.TemplateModel.ofContent;
import static com.sshtools.tinytemplate.Templates.TemplateModel.ofResource;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sshtools.tinytemplate.Templates.CloseableTemplateModel;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.Templates.TemplateProcessor;
import com.sshtools.tinytemplate.bootstrap.forms.Field.FieldDependency;
import com.sshtools.tinytemplate.bootstrap.forms.Field.Option;
import com.sshtools.tinytemplate.bootstrap.forms.InputType.Value;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.ValidationException;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.Validator;


public final class Form<T> extends AbstractElement implements FormType<T> {
	
	public interface Results<T> {
		
		default boolean ok() {
			return results().isEmpty();
		}
		
		default boolean errors() {
			return !ok();
		}
		
		Optional<String> submitButton();
		
		List<ValidationResult<T>> results();
		
		<F> F value(String field);
		
		<F> F value(Field<T, ?> field);
	}
	
	public interface FormDataReceiver {

		void field(Field<?, ?> field, String val);

		Field<?, ?> field(String fieldName);

		void file(Field<?, ?> field, FormFile file);
		
	}
	
	public record FormFile(String filename, String contentType, long contentLength, InputStream in) {
		public final static FormFile EMPTY = new FormFile("", "application/octet-stream", -1, InputStream.nullInputStream());
		
		public final static FormFile of(String filename) {
			return new FormFile(filename, "application/octet-stream", -1, InputStream.nullInputStream());
		}
	}
	
	public record ValidationResult<T>(Field<T,?> field, List<ValidationException> errors) {
		public ValidationException firstError() {
			return errors.get(0);
		}
	}
	
	public record FormSection<T>(String id, Text label, List<Field<T, ?>> fields, Function<FormSection<T>, TemplateModel> renderer) {
		FormSection() {
			this(null, Text.of(""), new ArrayList<>(), null);
		}
		
		FormSection(String id, Text label, List<Field<T, ?>> fields) {
			this(id, label, fields, null);
		}
		
		FormSection<T> withRenderer(Function<FormSection<T>, TemplateModel> renderer) {
			return new FormSection<>(id, label, fields, renderer);
		}
	}
	
	public enum Icon {
		TRASH
	}
	
	public final static class Builder<T> extends AbstractFormBuilder<T, Form<T>, Builder<T>> {
		
		public static <T> Builder<T> create(Class<T> type) {
			return new Builder<>(Optional.empty(), Optional.of(type));
		}
		
		public static <T> Builder<T> edit(T instance) {
			return new Builder<>(Optional.of(instance), Optional.empty());
		}
		
		public static Builder<Void> typeless() {
			return new Builder<>(Optional.empty(), Optional.empty());
		}

		Builder(Optional<T> instance, Optional<Class<T>> type) {
			super(instance, type);
		}	

		@Override
		public Form<T> build() {
			return new Form<>(this);
		}
	}	

	private final Map<String, Field<T, ?>> fields;
	private final List<FormSection<T>> sections;
	private final Map<Templatable, TemplateResource> templates;
	private final Map<Icon, Set<String>> icons;
	private final Optional<ResourceBundle> bundle;
	private final Framework framework;
	private final Set<String> groupCssClass;
	private final Set<String> optionsCssClass;
	private final Set<String> rowCssClass;
	private final boolean disabled;
	private final boolean feedback;
	private final boolean floatingLabel;
	private final Locale locale;
	private final ResourceBundle defaultBundle;
	private final Map<Field<T, ?>, ValidationResult<T>> errors = new HashMap<>();
	private final Optional<Text> validFeedback;
	private final Optional<BiConsumer<Form<T>, String>> onUnknownField;
	
	private Form(Builder<T> bldr) {
		super(bldr);
		this.framework = bldr.framework.orElseGet(DefaultFramework::get);
		this.onUnknownField = bldr.onUnknownField;
		this.validFeedback = bldr.validFeedback;
		this.feedback = bldr.feedback;
		this.floatingLabel = bldr.floatingLabel;
		this.disabled = bldr.disabled;
		this.groupCssClass = bldr.groupCssClass;
		this.rowCssClass = bldr.rowCssClass;
		this.optionsCssClass = bldr.optionsCssClass;
		
		this.locale = bldr.locale.orElse(Locale.getDefault());
		this.bundle = bldr.bundle.or(() -> bldr.bundleName.map(
				name ->ResourceBundle.getBundle(
					name, 
					locale, 
					bldr.bundleClassLoader.orElseGet(() -> Form.class.getClassLoader()))
				));
		
		this.defaultBundle = ResourceBundle.getBundle(Form.class.getName(), locale, Form.class.getClassLoader());
		
		var fm = new LinkedHashMap<String, Field<T, ?>>();
		bldr.sections.stream().forEach(grp -> {
			for(var fld : grp.fields)
				fm.put(fld.resolveName(), fld); 
		});
		this.fields = Collections.unmodifiableMap(fm);
		this.sections = Collections.unmodifiableList(bldr.sections);
		
		var allTemplates = new HashMap<Templatable, TemplateResource>(framework.defaultTemplates());
		allTemplates.putAll(bldr.templates);
		this.templates = Collections.unmodifiableMap(allTemplates);

		var allIcons = new HashMap<Icon, Set<String>>(framework.defaultIcons());
		allIcons.putAll(bldr.icons);
		this.icons = Collections.unmodifiableMap(allIcons);
	}

	public Field<T, ?> field(String id) {
		return fields.get(id);
	}
	
	public List<FormSection<T>> sections() {
		return sections;
	}
	
	public TemplateModel model() {
		var formTemplate = templates.get(Template.FORM);
		var model = ofResource(formTemplate.base(), formTemplate.resource());
		
		model.variable("id", resolveId().orElse("form")); 
		if(sections.size() > 1) {
			model.list("sections", (content) -> 
				sections.subList(1, sections.size()).stream().map(sec -> 
					TemplateModel.ofContent(content).
						condition("active", sections.indexOf(sec) == 1).
						variable("index", sections.indexOf(sec)).
						variable("id", sec.id()).
						variable("label", sec.label().resolveString()).
						list("rows", rowcontent -> buildRows(rowcontent, sec.fields(), sec))
					).
					toList());
			model.list("rows", content -> buildRows(content, sections.get(0).fields(), null));
		}
		else {
			model.list("rows", content -> buildRows(content, fields.values(), null));
		}
		return model;
	}
	
	public Results<T> receive(Consumer<FormDataReceiver> receiver) {
		return receive(receiver, true);
	}
	
	public void reset() {
		errors.clear();
	}
	
	public void addError(Field<T, ?> field, ValidationException ve) {
		var l = errors.get(field);
		if(l == null) {
			l = new ValidationResult<>(field, new ArrayList<>());
			errors.put(field, l);
		}
		l.errors.add(ve);
	}
	
	public Results<T> receive(Consumer<FormDataReceiver> receiver, boolean validate) {

		if(validate) {
			this.errors.clear();;
		}
		
		var processed = new ArrayList<Field<T, ?>>();
		var commit = new ArrayList<Runnable>();
		var values = new HashMap<Field<T, ?>, Object>();
		var submitButton = new StringBuilder();
		
		receiver.accept(new FormDataReceiver() {
				
			@Override
			public Field<T, ?> field(String fieldName) {
				var v = fields.get(fieldName);
				if(v == null) {
					onUnknownField.orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("No field ''{0}'' in form.", fieldName))).accept(Form.this, fieldName);
				}
				return v;
			}
				
			@SuppressWarnings("unchecked")
			@Override
			public void file(Field<?, ?> field, FormFile file) {
				doUpdate(validate, processed, commit, values, (Field<T, Object>)field, file);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void field(Field<?, ?> field, String val) {
				doUpdate(validate, processed, commit, values, (Field<T, Object>) field, Field.valueOf(framework, field, val));
			}

			@SuppressWarnings("unchecked")
			private void doUpdate(boolean validate, ArrayList<Field<T, ?>> processed, ArrayList<Runnable> commit,
					HashMap<Field<T, ?>, Object> values, Field<T, Object> f, Object obj) {
				var was = f.value().map(Supplier::get).orElse(null);
				
				if(!Objects.equals(obj, was)) {
				
					if(validate) {
						resolveValidators(f).ifPresent(vs -> {
							for(var v : vs) {
								try {
									v.validate((Field<Object, ?>) f, obj);
								}
								catch(ValidationException ve) {
									addError(f, ve);
								}
							}
						});
					}
						
					values.put(f, obj);
					
					if(validate) {
						commit.add(() -> update(f, obj, was));
					}
					else {
						update(f, obj, was);
					}
				}
				
				processed.add(f);
			}

			private void update(Field<T, Object> f, Object obj, Object was) {
				f.update().ifPresentOrElse(u -> {
					if(f.readOnly().orElse(false)) {
						throw new IllegalStateException(MessageFormat.format("Value has changed from ''{0}'' to ''{1}'', but ''{2}'' is read only.", debugVal(was), debugVal(obj), f.resolveName()));
					}
					u.accept(obj);
				}, () -> {
					// Must be retrieved by Result.value()
				});
			}
		});
						
		if(validate) {
			fields.values().forEach(field -> {
				var maybeRequired = !processed.contains(field) && field.required();
				
				if(maybeRequired && !field.depends().isEmpty()) {
					/* Not posted, but required. See if it is actually required given 
					 * the state of the other fields it depends on
					 */
					maybeRequired = isActuallyRequired(field, values);
				}
				
				if(maybeRequired && isEmpty(field.value().map(Supplier::get).orElse(null))) {
					addError(field, new ValidationException(field, Text.ofI18n("field.required", defaultBundle, resolveText(field))));
				}
			});
		}
		
		if(errors.isEmpty()) {
			commit.forEach(Runnable::run);
		}
		
		return new Results<>() {

			@Override
			public List<ValidationResult<T>> results() {
				return Collections.unmodifiableList(new ArrayList<>(errors.values()));
			}

			@Override
			public <F> F value(String field) {
				return value(Objects.requireNonNull(fields.get(field),() -> MessageFormat.format("No such field ''{0}''.", field)));
			}

			@SuppressWarnings("unchecked")
			@Override
			public <F> F value(Field<T, ?> field) {
				return Objects.requireNonNull((F)values.get(field),() -> MessageFormat.format("No such value for ''{0}''.", field));
			}

			@Override
			public Optional<String> submitButton() {
				return submitButton.length() == 0 ? Optional.empty() : Optional.of(submitButton.toString());
			}
			
		};
	}

	private boolean isActuallyRequired(Field<T, ?> field, HashMap<Field<T, ?>, Object> values) {
		for(var depend : field.depends()) {
			var otherField = fields.get(depend.name());
			if(otherField != null) {
				var otherValue = values.get(otherField);
				var match = Arrays.asList(depend.values()).contains(otherValue);
				if(depend.negate())
					match = !match;
				if(match) {
					return true;
				}
				
			}
		}
		return false;
	}

	private <F> Optional<List<Validator<F>>> resolveValidators(Field<T, F> field) {
		if(field.validators().isPresent()) {
			return field.validators();			
		}
		@SuppressWarnings("unchecked")
		var defs = Arrays.asList((Validator<F>[])field.resolveInputType().defaultValidators());
		if(defs.size() == 0)
			return Optional.empty();
		else
			return Optional.of(defs);
	}
	
	private static boolean isEmpty(Object val) {
		return val == null || ( val instanceof String str && str.equals("") ); 
	}
	
	private static String debugVal(Object v) {
		if(v == null) {
			return "<null>";
		}
		else {
			var s = Field.toValString(v);
			return s.replace("\r", "[cr]").replace("\n", "[nl]");
		}
	}
	
	private List<TemplateModel> buildRows(String content, Collection<Field<T, ?>> fields, FormSection<T> sec) {
		if(sec != null && sec.renderer != null) {
			return Arrays.asList(sec.renderer.apply(sec));
		}
		
		var rows = new ArrayList<TemplateModel>();
		var cols = new ArrayList<TemplateModel>();
		var spans = 0;
		var rowIndex = 0;
		var colIndex = 0;		
		
		for(var field : fields) {
			if(field.fills()) {
				if(!cols.isEmpty()) {
					rows.add(buildRow(rowIndex, cols));
					rowIndex++;
					colIndex = 0;
				}
				
				rows.add(buildGroup(rowIndex, colIndex, field, false));
					rowIndex++;
				spans = 0;
			}
			else {
				var span = field.spans();
				if(span + spans > 12) {
					rows.add(buildRow(rowIndex, cols));
					rowIndex++;
					colIndex = 0;
					spans = 0;
				}
				cols.add(buildColumn(rowIndex, colIndex, field));
				colIndex++;
				spans += span;
			}
		}
		
		if(!cols.isEmpty()) {
			rows.add(buildRow(rowIndex, cols));
		}
		
		return rows;
	}
	
	private TemplateModel buildRow(int rowIndex, List<TemplateModel> columns) {
		try {
			var rowTemplate = templates.get(Template.ROW);
			var id =  resolveId().orElse("form") + "-row-" + rowIndex;
			return ofResource(rowTemplate.base(), rowTemplate.resource()).
					variable("id", id).
					variable("rowCssClass", () -> String.join(" ", rowCssClass)).
					list("columns", new ArrayList<>(columns));
		}
		finally {
			columns.clear();
		}
	}
	
	private TemplateModel buildColumn(int rowIndex, int colIndex, Field<T, ?> field) {
		var groupTemplate = templates.get(Template.COLUMN);
		var id =  resolveId().orElse("form") + "-column-" + ( field.resolveId().orElse(rowIndex + "-" + colIndex) );
		return ofResource(groupTemplate.base(), groupTemplate.resource()).
				variable("id", id).
				variable("span", () -> resolveSpan(field)).
				include("field", () -> buildField(rowIndex, colIndex, field, false));
	}
	
	private String resolveSpan(Field<T, ?> field) {
		if(field.span.isPresent()) {
			var l = field.span.get();
			if(l.isEmpty()) {
				return "";
			}
			else {
				return String.join(" ", l.stream().map(Field.Span::toCss).toList());
			}
		}
		else {
			return "col";
		}
	}
	
	private boolean resolveFloatingLabel(Field<T, ?> field) {
		return field.floatingLabel().orElse(floatingLabel);
	}
	
	private boolean resolveDisabled(Field<T, ?> field) {
		if(disabled)
			return true;
		return field.resolveDisabled();
	}
	
	private String resolveGroupCssClass(Field<T, ?> field) {
		var fieldClasses = new LinkedHashSet<String>();
		if(!field.resolveInputType().options())
			fieldClasses.addAll(framework.groupCssClass(field.resolveInputType()));
		fieldClasses.addAll(field.groupCssClass().orElse(groupCssClass));
		
		if(isInputGroup(field)) {
			fieldClasses.add("input-group");
		}
		
		return String.join(" ", fieldClasses);
	}

	private boolean isInputGroup(Field<T, ?> field) {
		return field.inputGroupAfter().isPresent() || field.inputGroupBefore().isPresent();
	}

	private boolean isMultiTemplate(Field<T, ?> field) {
		return field.multiple().orElseGet(field::resolveMultiple) && field.resolveInputType().templatable();
	}
	
	private String resolveLabelCssClass(Field<T, ?> field) {
		var labelClasses = new LinkedHashSet<String>();
		field.labelCssClass().ifPresentOrElse(lcss -> {
			labelClasses.addAll(lcss);
		}, () -> {
			if(!hasOptions(field)) {
				labelClasses.addAll(framework.labelCssClass(field.resolveInputType()));
			}
		});
		return String.join(" ", labelClasses);
	}
	
	private TemplateModel buildGroup(int rowIndex, int colIndex, Field<T, ?> field, boolean selected) {
		var groupTemplate = templates.get(Template.GROUP);
		var id =  resolveId().orElse("form") + "-group-" + ( field.resolveId().orElse(String.valueOf(rowIndex + "-" + colIndex)) );
		var templ = ofResource(groupTemplate.base(), groupTemplate.resource()).
				variable("id", id).
				variable("groupCssClass", () -> resolveGroupCssClass(field)).
				include("field", () -> buildField(rowIndex, colIndex, field, selected));
		
		var deps = field.depends();
		if(!deps.isEmpty()) {
			var depIds = String.join(",", deps.stream().map(FieldDependency::name).toList());
			var bldr = new StringBuilder();
			for(var dep : deps) {
				if(bldr.length() > 0)
					bldr.append(',');
				if(dep.negate())
					bldr.append('!');
				bldr.append(String.join(":", Arrays.asList(dep.values()).stream().map(obj -> Field.toString(field, obj)).toList()));
			}
			templ.variable("groupAttrs", attrsToString(
					Map.of(
						"data-depends", depIds,
						"data-depends-value", bldr.toString()
					)
				)
			);
		}
		
		return templ;
	}
	
	private TemplateModel buildField(int rowIndex, int colIndex, Field<T, ?> field, boolean selected) {
		var fieldTemplate = templates.get(Template.FIELD);
		
		var resolvedId = field.resolveId().orElse(String.valueOf(rowIndex + "-" + colIndex));
		var id =  resolveId().orElse("form") + "-field-" + resolvedId;
		var inputId =  field.resolveId().orElse(resolveId().orElse("form") + "-" + ( field.resolveId().orElse("input-" + rowIndex + "-" + colIndex) ));
		
		var type = field.resolveInputType();
		
		/* TODO hrm */
		@SuppressWarnings("unchecked")
		var mdl = ((Field<T, Object>)field).renderer().map(r ->r.apply(field)).orElseGet(() -> ofResource(fieldTemplate.base(), fieldTemplate.resource()));
		
		var hasOptions = hasOptions(field);
		
		mdl.variable("id", id);
		mdl.variable("input.id", inputId);
		mdl.variable("input.value", () -> Field.toString(field));
		mdl.condition("input.group", field.inputGroupBefore().isPresent() || field.inputGroupAfter().isPresent());
		
		buildCommonFieldAndInput(field, resolvedId, type, mdl);
		
		if(hasOptions && type.optionsAsFields()) {
			var opts = field.options().get().get();
			mdl.variable("options.class", String.join(" ", optionsCssClass));
			mdl.list("options", (content) -> opts.stream().map(opt -> {
				return TemplateModel.ofContent(content).
						include("option", buildGroup(opts.indexOf(opt), 0, opt.asField(field), Field.toString(field).equals(opt.value()))); 
			}).toList());
		}
		
		if(field.feedback().orElse(feedback)) {
			var feedback = errors.get(field);
			if(feedback == null) {
				field.validFeedback().or(() -> validFeedback).ifPresent(val -> {
					mdl.variable("feedback", "valid-feedback");
					mdl.variable("feedback.text", val.resolveString());
				});				
			}
			else {
				var firstVe = feedback.firstError();
				mdl.variable("feedback", "invalid-feedback");
				mdl.variable("feedback.text", firstVe.text().orElse(Text.ofI18n("field.error", defaultBundle, resolveText(field))).resolveString());			
			}
		}
				
		var hasHelp = mdl.hasVariable("help");
		var hasFeedback= mdl.hasVariable("feedback");
		
		if(type != InputType.NONE) {
			mdl.include("input", () -> {
				if(isMultiTemplate(field)) {
					var temptemp = templates.get(Template.TEMPLATE);
					var tempres = ofResource(temptemp.base(), temptemp.resource());
					tempres.include("templated", () -> buildInput(id, rowIndex, colIndex, resolvedId, inputId, field, type, hasHelp, hasFeedback, selected));
					tempres.variable("id", resolvedId +  "-template");
					tempres.variable("input.id", resolvedId);
					tempres.variable("template.class", "input-group mb-3");
					return tempres;
				}
				else {
					return buildInput(id, rowIndex, colIndex, resolvedId, inputId, field, type, hasHelp, hasFeedback, selected);
				}
			});
		}
		return mdl;
	}

	private void buildCommonFieldAndInput(Field<T, ?> field, String resolvedId, InputType type, TemplateModel mdl) {
		var hasOptions = hasOptions(field);
		var floatingLabel = !hasOptions && resolveFloatingLabel(field);
		
		mdl.variable("label.class", resolveLabelCssClass(field));
		if(!field.noLabel()) {
			resolveText(field, resolvedId).ifPresent(str -> mdl.variable("label", str));
		}
		
		resolveString("help", resolvedId, field.help(), null).ifPresent(str -> mdl.variable("help", str));

		mdl.condition("label.first", () ->  
			hasOptions || ( type.labelFirst() && !floatingLabel ) 
		);
		mdl.condition("label.floating", floatingLabel);
		mdl.condition("has.label", mdl.hasVariable("label"));			
		mdl.condition("has.placeholder", mdl.hasVariable("placeholder"));			
		mdl.condition("has.help", mdl.hasVariable("help"));
	}

	protected boolean hasOptions(Field<T, ?> field) {
		return field.options().isPresent();
	}
	
	private String resolveText(Field<T, ?> field) {
		return resolveText(field, field.resolveId().orElse("[no-id]")).orElse("[no-text]");
	}

	private Optional<String> resolveText(Field<T, ?> field, String resolvedId) {
		return resolveString("label", resolvedId, field.label(), () -> english(resolvedId));
	}
	
	private Optional<String> resolveString(String suffix, String id, Optional<Text> textOr, Supplier<String> defaultValue) {
		if(textOr.isPresent()) {
			var text = textOr.get();
			return Optional.of(text.resolveString(this.bundle));
		}
		else {
			if(this.bundle.isPresent()) {
				var bundle = this.bundle.get();
				
				/* Generate key from id and suffix */
				var key = suffix == null || suffix.length() == 0 ? id : id + "." + suffix;
				try {
					return Optional.of(bundle.getString(key));
				}
				catch(MissingResourceException mre) {
				}
			}
		}
		
		
		return defaultValue == null ? Optional.empty() : Optional.of(defaultValue.get());
	}
	
	private Set<String> resolveInputClass(Field<T, ?> field, InputType type) {
		var l = new LinkedHashSet<String>();
		if(!type.tag().equalsIgnoreCase("input") || !type.options())
			l.addAll(framework.cssClass(type));
		if(field.feedback().orElse(feedback)) {
			var feedback = errors.get(field);
			if(feedback == null) {
				field.validFeedback().or(() -> validFeedback).ifPresent(val -> {
					l.add("is-valid");
				});				
			}
			else {
				l.add("is-invalid");
			}
		}
		return l;
	}
	
	private TemplateModel buildInput(String fieldId, int rowIndex, int colIndex, String inputId, String resolvedId, Field<T, ?> field, InputType type, boolean hasHelp, boolean hasFeedback, boolean selected) {
		var inputTemplate = templates.get(type);
		if(inputTemplate == null) {
			inputTemplate = framework.defaultTemplates().get(Template.INPUT);
		}
		var mdl = ofResource(inputTemplate.base(), inputTemplate.resource());
		
		mdl.variable("type", type.attribute());
		mdl.variable("name", field.resolveName());
		mdl.variable("id", inputId);
		mdl.variable("class", String.join(" ", resolveInputClass(field, type)));
		if(type.value() == Value.CHECKED) {
			mdl.variable("checked", () -> field.value().map(s -> evalAsBoolean(s.get())).orElse(false));
		}
		else if(type.value() == Value.CHECKED_VALUE) {
			mdl.variable("checked", selected);
		}
		
		buildCommonFieldAndInput(field, resolvedId, type, mdl);
		
		mdl.condition(type.tag(), true);
		mdl.condition("is." + type.name().toLowerCase(), true);
		mdl.condition("disabled", () -> resolveDisabled(field));
		mdl.condition("readonly", field::resolveReadOnly);
		mdl.condition("required", field::required);
		mdl.condition("multiple", () -> field.multiple().orElseGet(field::resolveMultiple));
		
		field.pattern().ifPresent(p -> mdl.variable("pattern", p));
		
		buildInputGroups(field, mdl);
		
		field.attrs().forEach((k,v) -> {
			mdl.variable("attr." + k, v);
		});
		
		field.options().ifPresent(opts -> {
			mdl.list("options", (content) ->
				opts.get().stream().map(opt -> {
					return ofContent(content).
						variable("type", type.attribute()).
						variable("name", field.resolveName()).
						variable("id", inputId).
						variable("text", opt.text().resolveString(bundle)).
						variable("value", framework.processValueRender(field, opt.value())).
						condition("selected", isSelected(field, opt));
					}).toList()
			);
		});
		
		var attrs = new HashMap<>(field.attrs());
		resolveString("placeholder", resolvedId, field.placeholder(), null).ifPresentOrElse(
		str -> {
			attrs.put("placeholder", str); 
		},
		() -> {
			if(resolveFloatingLabel(field)) {
				attrs.put("placeholder", resolveString("label", resolvedId, field.label(), () -> english(resolvedId)).orElse(english(resolvedId)));
			} 
		});
		
		field.dropzone().ifPresent(dz -> attrs.put("data-dropzone", dz));
	
		if(hasHelp) {
			if(hasFeedback)
				attrs.put("aria-describedby", fieldId + "Feedback " + fieldId + "Help");
			else
				attrs.put("aria-describedby", fieldId + "Help");
		}
		else if(hasFeedback) {			
			attrs.put("aria-describedby", fieldId + "Feedback");
		}
		
		field.value().ifPresent(sup -> { 
			var val = Field.toString(framework, field);
			mdl.variable("value", val);
			if((type.value() == Value.ATTRIBUTE || type.value() == Value.CHECKED_VALUE) && type.inputTag()) {
				attrs.put("value", val);
			} 
		});

		mdl.variable("attrs", attrsToString(attrs));
		
		return mdl;
	}

	private void buildInputGroups(Field<T, ?> field, CloseableTemplateModel mdl) {
		field.inputGroupBefore().ifPresent(ig -> mdl.include("input.group.before", ig));
		field.inputGroupAfter().ifPresentOrElse(ig -> mdl.include("input.group.after", ig), () -> {
			if(isMultiTemplate(field)) {
				mdl.include("input.group.after", TemplateModel.ofContent("<a href=\"#\" role=\"remove-templated-row\"><i class=\"" + String.join(" ", icons.get(Icon.TRASH)) + "\"></i></a>"));
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private boolean isSelected(Field<T, ?> field, Option opt) {
		if(field.value().isPresent()) {
			Object val = field.value().get().get();
			if(val.getClass().isArray()) {
				val = Arrays.asList((Object[])val);
			}
			if(val instanceof Collection col) {
				return col.stream().map(Field::toValString).toList().contains(opt.value());
			}
		}
		
		return opt.value().equals(field.value().map(v -> Field.toValString(v.get())).orElse(""));
	}

	protected String attrsToString(Map<String, String> attrs) {
		var buf = new StringBuilder();
		attrs.forEach((k,v) -> {
			if(buf.length() > 0)
				buf.append(' ');
			
			buf.append(k);
			if(v != null) {
				buf.append("=\"");
				buf.append(quoteattr(v.toString()));
				buf.append("\"");
			}
		});
		return buf.toString();
	}

	private boolean evalAsBoolean(Object obj) {
		if(obj instanceof Boolean bobj) {
			return bobj;
		}
		else if(obj instanceof String str) {
			return str.length() > 0 || str.equalsIgnoreCase("true");
		}
		else if(obj instanceof Number num) {
			return num.doubleValue() > 0;
		}
		else {
			return obj != null;
		}
	}
	
	private static String quoteattr(String s) {
		return quoteattr(s, false);
	}
	
	private static String quoteattr(String s, boolean preserveCR) {
        var preserveCRS = preserveCR ? "&#13;" : "\n";
        return s.replace("&", "&amp;")
	            .replace("'", "&apos;")
	            .replace("\"", "&quot;")
	            .replace("<", "&lt;")
	            .replace(">", "&gt;")
	            .replace("\r\n", preserveCRS)
	            .replaceAll("[\r\n]", preserveCRS);
    }
	
	public static String asId(String id) {
		return id.toLowerCase().replaceAll("\\s+", " ").replace(" ", "-").replaceAll("[^a-z0-9\\-_]+", "");
	}
	
	public static String english(String val) {
		return capitalizeFirst(spaceOnCaseChange(val, true));
	}

	public static String capitalizeFirst(String text) {
		return text == null || text.length() == 0 ? null
				: (text.length() == 1 ? text : (Character.toUpperCase(text.charAt(0)) + text.substring(1)));
	}
		
	public static String spaceOnCaseChange(String fieldName) {
		return spaceOnCaseChange(fieldName, false);
	}

	public static String spaceOnCaseChange(String fieldName, boolean capitals) {
		if (fieldName == null || fieldName.length() == 0)
			return fieldName;
		char[] strChr = fieldName.toCharArray();
		boolean upper = Character.isUpperCase(strChr[0]);
		StringBuilder b = new StringBuilder();
		b.append(Character.toUpperCase(strChr[0]));
		for (int i = 1; i < strChr.length; i++) {
			boolean nu = Character.isUpperCase(strChr[i]);
			if (upper != nu && nu) {
				b.append(' ');
			}
			upper = nu;
			if (capitals)
				b.append(strChr[i]);
			else
				b.append(Character.toLowerCase(strChr[i]));
		}
		return b.toString();
	}
	
	private final static record TestObject(String name, int age) {
	}

	public static void main(String[] args) {
		var form = Form.Builder.edit(new TestObject("Brett Smith", 27)).
				field(f-> f. 
//					    span(6).
					  label("Name").
					  placeholder("First name, Surname").
					  help("Your full name please").
					  value(f.$()::name)
				).
				field(f-> f.
//					    span(6).
//					  input(InputType.NUMBER).
					  label("Age").
					  placeholder("0-150").
					  help("Enter your age").
					  attrs(Map.of(
					    "min", "0",
					    "max", "150"
					  )).
					  disabled().
					  value(f.$()::age)
				).build();
		
		var tp = new TemplateProcessor.Builder().build();
		System.out.println(tp.process(form.model()));
	}
	
}


