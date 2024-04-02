package com.sshtools.tinytemplate.bootstrap.forms;

import static com.sshtools.tinytemplate.Templates.TemplateModel.ofContent;
import static com.sshtools.tinytemplate.Templates.TemplateModel.ofResource;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.Templates.TemplateProcessor;
import com.sshtools.tinytemplate.bootstrap.forms.InputType.Value;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.ValidationException;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.Validator;


public final class Form<T> extends AbstractElement implements FormType<T> {
	
	public final static TemplateResource INPUT_TEMPLATE = new TemplateResource(Form.class, "input.template.html");
	
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
	
	public final static Map<Template, TemplateResource> DEFAULT_TEMPLATES = Map.of(
		Template.FORM, new TemplateResource(Form.class, "form.template.html"),
		Template.ROW, new TemplateResource(Form.class, "row.template.html"),
		Template.GROUP, new TemplateResource(Form.class, "group.template.html"),
		Template.FIELD, new TemplateResource(Form.class, "field.template.html"),
		Template.COLUMN, new TemplateResource(Form.class, "column.template.html")
	);
	
	public final static class Builder<T> extends AbstractFormBuilder<T, Form<T>, Builder<T>> {
		
		private List<Field<T, ?>> fields = new ArrayList<>();
		
		public static <T> Builder<T> create(Class<T> type) {
			return new Builder<>(Optional.empty(), Optional.of(type));
		}
		
		public static <T> Builder<T> edit(T instance) {
			return new Builder<>(Optional.of(instance), Optional.empty());
		}

		Builder(Optional<T> instance, Optional<Class<T>> type) {
			super(instance, type);
		}						
		
		public <F> Builder<T> field(Consumer<Field.Builder<T, F>> bldr) {
			var fbldr = new Field.Builder<T, F>(instance);
			bldr.accept(fbldr);
			fields.add(fbldr.build());
			return this;
		}

		@Override
		public Form<T> build() {
			return new Form<>(this);
		}
	}	

	private final Map<String, Field<T, ?>> fields;
	private final Map<Templatable, TemplateResource> templates;
	private final Optional<ResourceBundle> bundle;
	private final Set<String> groupCssClass;
	private final Set<String> rowCssClass;
	private final boolean disabled;
	private final boolean feedback;
	private final boolean floatingLabel;
	private final Locale locale;
	private final ResourceBundle defaultBundle;
	private final Map<Field<T, ?>, ValidationResult<T>> errors = new HashMap<>();
	private final Optional<Text> validFeedback;
	
	private Form(Builder<T> bldr) {
		super(bldr);
		this.validFeedback = bldr.validFeedback;
		this.feedback = bldr.feedback;
		this.floatingLabel = bldr.floatingLabel;
		this.disabled = bldr.disabled;
		this.groupCssClass = bldr.groupCssClass;
		this.rowCssClass = bldr.rowCssClass;
		
		this.locale = bldr.locale.orElse(Locale.getDefault());
		this.bundle = bldr.bundle.or(() -> bldr.bundleName.map(
				name ->ResourceBundle.getBundle(
					name, 
					locale, 
					bldr.bundleClassLoader.orElseGet(() -> Form.class.getClassLoader()))
				));
		
		this.defaultBundle = ResourceBundle.getBundle(Form.class.getName(), locale, Form.class.getClassLoader());
		
		var fm = new LinkedHashMap<String, Field<T, ?>>();
		bldr.fields.stream().forEach(f -> fm.put(f.resolveName(), f));
		this.fields = Collections.unmodifiableMap(fm);
		
		this.templates = Collections.unmodifiableMap(new HashMap<>(bldr.templates));
	}

	public Field<T, ?> field(String id) {
		return fields.get(id);
	}
	
	public TemplateModel model() {
		var formTemplate = templates.get(Template.FORM);
		return ofResource(formTemplate.base(), formTemplate.resource()).list("rows", this::buildRows);
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
					throw new IllegalArgumentException(MessageFormat.format("No field ''{0}'' in form.", fieldName));					
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
				Field<T, Object> f = (Field<T, Object>)field;  
				Object obj;
				
				var clazz = f.resolveType();
				if(Boolean.class.isAssignableFrom(clazz)) {
					obj = "ON".equalsIgnoreCase(val) || Boolean.valueOf(val);	
				}
				else if(Double.class.isAssignableFrom(clazz)) {
					obj = Double.valueOf(val);	
				}
				else if(Float.class.isAssignableFrom(clazz)) {
					obj = Float.valueOf(val);	
				}
				else if(Long.class.isAssignableFrom(clazz)) {
					obj = Long.valueOf(val);	
				}
				else if(Integer.class.isAssignableFrom(clazz)) {
					obj = Integer.valueOf(val);	
				}
				else if(Short.class.isAssignableFrom(clazz)) {
					obj = Short.valueOf(val);	
				}
				else if(Character.class.isAssignableFrom(clazz)) {
					obj = val.length() == 0 ? (char)0 : val.charAt(0);	
				}
				else if(Byte.class.isAssignableFrom(clazz)) {
					obj = val.length() == 0 ? (byte)0 : (byte)Integer.parseInt(val);	
				}
				else if(String.class.isAssignableFrom(clazz)) {
					obj = val;	
				}
				else {
					throw new UnsupportedOperationException(MessageFormat.format("Unsupported type ''{0}''", clazz.getName()));
				}
				
				doUpdate(validate, processed, commit, values, f, obj);
			}

			@SuppressWarnings("unchecked")
			private void doUpdate(boolean validate, ArrayList<Field<T, ?>> processed, ArrayList<Runnable> commit,
					HashMap<Field<T, ?>, Object> values, Field<T, Object> f, Object obj) {
				var was = f.value().map(Supplier::get).orElse(null);
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
				if(!processed.contains(field) && field.required() && isEmpty(field.value().map(Supplier::get).orElse(null))) {
					addError(field, new ValidationException(field, Text.of("field.required", defaultBundle, resolveText(field))));
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
			var s = String.valueOf(v);
			return s.replace("\r", "[cr]").replace("\n", "[nl]");
		}
	}
	
	private List<TemplateModel> buildRows(String content) {
		var rows = new ArrayList<TemplateModel>();
		var cols = new ArrayList<TemplateModel>();
		var spans = 0;
		var rowIndex = 0;
		var colIndex = 0;		
		
		for(var field : fields.values()) {
			if(field.fills()) {
				if(!cols.isEmpty()) {
					rows.add(buildRow(rowIndex, cols));
					rowIndex++;
					colIndex = 0;
				}
				
				rows.add(buildGroup(rowIndex, colIndex, field));
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
				include("field", () -> buildField(rowIndex, colIndex, field));
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
		return field.disabled().orElseGet(() -> {
			return field.update().isEmpty() && !field.resolveInputType().supportsReadOnly();
		});
	}
	
	private String resolveGroupCssClass(Field<T, ?> field) {
		var fieldClasses = new LinkedHashSet<>(field.resolveInputType().groupCssClass());
		fieldClasses.addAll(field.groupCssClass().orElse(groupCssClass));
		
		if(field.inputGroupAfter().isPresent() || field.inputGroupBefore().isPresent()) {
			fieldClasses.add("input-group");
		}
		
		return String.join(" ", fieldClasses);
	}
	
	private TemplateModel buildGroup(int rowIndex, int colIndex, Field<T, ?> field) {
		var groupTemplate = templates.get(Template.GROUP);
		var id =  resolveId().orElse("form") + "-group-" + ( field.resolveId().orElse(String.valueOf(rowIndex + "-" + colIndex)) );
		return ofResource(groupTemplate.base(), groupTemplate.resource()).
				variable("id", id).
				variable("groupCssClass", () -> resolveGroupCssClass(field)).
				include("field", () -> buildField(rowIndex, colIndex, field));
	}
	
	private TemplateModel buildField(int rowIndex, int colIndex, Field<T, ?> field) {
		var fieldTemplate = templates.get(Template.FIELD);
		
		var resolvedId = field.resolveId().orElse(String.valueOf(rowIndex + "-" + colIndex));
		var id =  resolveId().orElse("form") + "-field-" + resolvedId;
		var inputId =  field.resolveId().orElse(resolveId().orElse("form") + "-" + ( field.resolveId().orElse("input-" + rowIndex + "-" + colIndex) ));
		
		var type = field.resolveInputType();
		var mdl = ofResource(fieldTemplate.base(), fieldTemplate.resource());
		var floatingLabel = resolveFloatingLabel(field);
		
		mdl.variable("id", id);
		mdl.variable("input.id", inputId);
		mdl.variable("label.class", String.join(" ", type.labelCssClass()));
		if(!field.noLabel()) {
			resolveText(field, resolvedId).ifPresent(str -> mdl.variable("label", str));
		}
		
		mdl.condition("input.group", field.inputGroupBefore().isPresent() || field.inputGroupAfter().isPresent());
		resolveString("help", resolvedId, field.help(), null).ifPresent(str -> mdl.variable("help", str));

		mdl.condition("label.first", () ->  type.labelFirst() && !floatingLabel );
		mdl.condition("label.floating", floatingLabel);
		mdl.condition("has.label", mdl.hasVariable("label"));			
		mdl.condition("has.placeholder", mdl.hasVariable("placeholder"));			
		mdl.condition("has.help", mdl.hasVariable("help"));
		
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
				mdl.variable("feedback.text", firstVe.text().orElse(Text.of("field.error", defaultBundle, resolveText(field))).resolveString());			
			}
		}
				
		var hasHelp = mdl.hasVariable("help");
		var hasFeedback= mdl.hasVariable("feedback");
		
		mdl.include("input", () -> buildInput(id, rowIndex, colIndex, resolvedId, inputId, field, type, hasHelp, hasFeedback));
		return mdl;
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
		var l = new LinkedHashSet<>(type.cssClass());
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
	
	private TemplateModel buildInput(String fieldId, int rowIndex, int colIndex, String inputId, String resolvedId, Field<T, ?> field, InputType type, boolean hasHelp, boolean hasFeedback) {
		var inputTemplate = templates.get(type);
		if(inputTemplate == null) {
			inputTemplate = INPUT_TEMPLATE;
		}
		var mdl = ofResource(inputTemplate.base(), inputTemplate.resource());
		
		mdl.variable("type", type.attribute());
		mdl.variable("name", field.resolveName());
		mdl.variable("id", inputId);
		mdl.variable("class", String.join(" ", resolveInputClass(field, type)));
		if(type.value() == Value.CHECKED) {
			mdl.variable("checked", () -> field.value().map(s -> evalAsBoolean(s.get())).orElse(false));
		}
		
		mdl.condition(type.tag(), true);
		mdl.condition("disabled", () -> resolveDisabled(field));
		mdl.condition("readonly", field::resolveReadOnly);
		mdl.condition("required", field::required);
		
		field.pattern().ifPresent(p -> mdl.variable("pattern", p));
		field.value().ifPresent(sup -> mdl.variable("value", sup.get()));
		
		field.inputGroupBefore().ifPresent(ig -> mdl.variable("input.group.before", ig));
		field.inputGroupAfter().ifPresent(ig -> mdl.variable("input.group.after", ig));
		
		field.attrs().forEach((k,v) -> {
			mdl.variable("attr." + k, v);
		});
		
		field.options().ifPresent(opts -> {
			mdl.list("options", (content) ->
				opts.get().stream().map(opt -> ofContent(content).
					variable("text", opt.text()).
					variable("value", opt.value()).
					condition("selected", opt.value().equals(field.value().map(v -> String.valueOf(v.get())).orElse("")))
				).toList()
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
			var val = sup.get();
			mdl.variable("value", val);
			if(type.value() == Value.ATTRIBUTE) {
				attrs.put("value", String.valueOf(val));
			} 
		});

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
		mdl.variable("attrs", buf.toString());
		
		return mdl;
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


