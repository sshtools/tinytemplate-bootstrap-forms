package com.sshtools.tinytemplate.bootstrap.forms;

import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.bootstrap.forms.Form.FormFile;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.Validator;

public final class Field<T, F> extends AbstractElement {

	public static <F> String toString(Field<?, F> f) {
		var gtr = f.value().orElse(null);
		return toString(f, gtr == null ? null : gtr.get());
	}
	
	public static <F> String toString(Field<?, ?> f, F val) {
		if(val == null) {
			var clazz = f.resolveType();
			if(clazz == null) {
				return "";
			}
			else if(Boolean.class.isAssignableFrom(clazz)) {
				return "false";	
			}
			else if(Number.class.isAssignableFrom(clazz)) {
				return "0";	
			}
			else if(Character.class.isAssignableFrom(clazz)) {
				return "?";	
			}
			else if(Byte.class.isAssignableFrom(clazz)) {
				return "0";	
			}
			else {
				return "";	
			}
		}
		else {
			return val.toString();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <F> F valueOf(Field<?, F> f, String val) {
		Object obj;
		
		var clazz = f.resolveType();
		if(clazz == null)
			throw new IllegalArgumentException(MessageFormat.format("Field {0} has no type, which is required for String to Object conversion. This probably means the initial value is `null`, which means the type of the `Field` must be specified using `type()` when building the form.", f.resolveName()));
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
		else if(Enum.class.isAssignableFrom(clazz)) {
			obj = (F)Enum.valueOf((Class<Enum>)clazz, val);
		}
		else if(URL.class.isAssignableFrom(clazz)) {
			try {
				obj = URI.create(val).toURL();
			} catch (Exception e) {
				obj = null;
			}
		}
		else if(URI.class.isAssignableFrom(clazz)) {
			obj = URI.create(val);
		}
		else {
			throw new UnsupportedOperationException(MessageFormat.format("Unsupported type ''{0}''", clazz.getName()));
		}
		return (F)obj;
	}
	
	public record Option(Text text, String value) {
		public Option(String value) {
			this(Text.of(value), value);
		}

		@SuppressWarnings("unchecked")
		public <T> Field<T, ?> asField(Field<T,?> parent) {
			return (Field<T, ?>) new Field.Builder<>(Optional.empty()).
					name(parent.resolveName()).
					id(parent.resolveId().orElse("option-field") + "-" + Form.asId(value)).
					input(parent.resolveInputType()).
					groupCssClass().
					disabled(() -> parent.resolveDisabled()).
					label(text).
					value(value).
					build();
		}
	}
	
	public record Span(int size, Breakpoint breakpoint) {
		
		 public Span {
			if(size < 1 || size > 12) {
				throw new IllegalArgumentException();
			}
	        Objects.requireNonNull(breakpoint);
	    }
		 
		public Span(int size) {
			this(size, Breakpoint.NONE);
		}
		
		public String toCss() {
			if(breakpoint == Breakpoint.NONE)
				return "col-" + size;
			else 
				return "col-" + breakpoint.name().toLowerCase() + "-" + size;
		}
	}
	
	public record FieldDependency(String name, boolean negate, Object... values) {
		public FieldDependency(String name, Object... values) {
			this(name, false, values);
		}
	}
	
	public final static class Builder<T, F> extends AbstractBuilder<Builder<T,F>> {
		private Optional<List<Span>> span = Optional.of(Collections.emptyList());
		private Optional<List<Validator<F>>> validators = Optional.of(new ArrayList<>());
		private Optional<Supplier<F>> value = Optional.empty();
		private Optional<Consumer<F>> update = Optional.empty();
		private Optional<T> instance;
		private Optional<Supplier<Boolean>> disabled = Optional.empty();
		private Optional<Boolean> readOnly = Optional.empty();
		private Optional<Boolean> floatingLabel = Optional.empty();
		private boolean noLabel;
		private boolean required;
		private boolean multiple;
		private Optional<String> pattern = Optional.empty();
		private Optional<String> dropzone = Optional.empty();
		private Optional<String> name = Optional.empty();
		private Optional<InputType> inputType = Optional.empty();
		private Optional<Class<? extends F>> type = Optional.empty();
		private Optional<Text> label = Optional.empty();
		private Optional<Text> help = Optional.empty();
		private Optional<Text> placeholder = Optional.empty();
		private Optional<Set<String>> groupCssClass = Optional.empty();
		private Optional<Set<String>> labelCssClass = Optional.empty();
		private Map<String, String> attrs = new HashMap<>();
		private Optional<Supplier<List<Option>>> options = Optional.empty();
		private Optional<String> inputGroupBefore = Optional.empty();
		private Optional<String> inputGroupAfter = Optional.empty();
		private Optional<Text> validFeedback = Optional.empty();
		private Optional<Boolean> feedback = Optional.empty();
		private Set<FieldDependency> depends = new LinkedHashSet<>();
		private Optional<Function<F, TemplateModel>> renderer = Optional.empty();
		
		Builder(Optional<T> instance) {
			this.instance = instance;
		}
		
		public T $() {
			return instance();
		}
		
		public  Field.Builder<T, F> renderer(Function<F, TemplateModel> renderer) {
			this.renderer = Optional.of(renderer);
			return this;
		}

		public Field.Builder<T, F> depend(String name, Object... values) {
			return depends(Arrays.asList(values).stream().map(v -> new FieldDependency(name, values)).toList());
		}

		public Field.Builder<T, F> depends(FieldDependency... dependencies) {
			return depends(Arrays.asList(dependencies));
		}

		public Field.Builder<T, F> depends(Collection<FieldDependency> dependencies) {
			depends.addAll(dependencies);
			return this;
		}
	
		public Field.Builder<T, F> validFeedback(Text validFeedback) {
			this.validFeedback = Optional.of(validFeedback);
			return this;
		}
			
		public Field.Builder<T, F> noFeedback() {
			return feedback(false);
		}
			
		public Field.Builder<T, F> feedback(boolean feedback) {
			this.feedback = Optional.of(feedback);
			return this;
		}

		@SuppressWarnings("unchecked")
		public <E extends Enum<E>> Field.Builder<T, F> enums(Class<E> enums, E... values) {
			options(Arrays.asList(values).stream().map(en -> new Option( en.name())).toList());
			return this;
		}

		@SuppressWarnings("unchecked")
		public <E extends Enum<E>> Field.Builder<T, F> i18nEnums(Class<E> enums, E... values) {
			options(Arrays.asList(values).stream().map(en -> new Option(Text.ofI18n(en.getClass().getName() + "." + en.name() + ".name"), en.name())).toList());
			return this;
		}

		@SuppressWarnings("unchecked")
		public <E extends Enum<E>> Field.Builder<T, F> i18nEnums(Class<E> enums, Class<?> base, E... values) {
			options(Arrays.asList(values).stream().map(en -> new Option(Text.ofI18n(en.getClass().getName() + "." + en.name() + ".name", base), en.name())).toList());
			return this;
		}
		
		public Field.Builder<T, F> options(String... options) {
			return options(Arrays.asList(options).stream().map(Option::new).toList());
		}
		
		public Field.Builder<T, F> options(Option... options) {
			return options(Arrays.asList(options));
		}
		
		public Field.Builder<T, F> options(List<Option> options) {
			return options(() -> options);
		}
		
		public Field.Builder<T, F> options(Supplier<List<Option>> options) {
			this.options = Optional.of(options);
			return this;
		}
		
		public T instance() {
			return instance.orElseThrow(()-> new IllegalStateException("Cannot get an instance, as this form is for creation."));
		}
		
		public Field.Builder<T, F> noLabel() {
			this.noLabel = true;
			return this;
		}
		
		public Field.Builder<T, F> label(String label) {
			return label(Text.of(label));
		}
		
		public Field.Builder<T, F> label(Text label) {
			this.label = Optional.of(label);
			return this;
		}
		
		public Field.Builder<T, F> help(String help) {
			return help(Text.of(help));
		}
		
		public Field.Builder<T, F> help(Text help) {
			this.help = Optional.of(help);
			return this;
		}
		
		public Field.Builder<T, F> placeholder(String placeholder) {
			return placeholder(Text.of(placeholder));
		}
		
		public Field.Builder<T, F> placeholder(Text placeholder) {
			this.placeholder = Optional.of(placeholder);
			return this;
		}
		
		public Field.Builder<T, F> input(InputType inputType) {
			this.inputType = Optional.of(inputType);
			return this;
		}
		
		public Field.Builder<T, F> required() {
			return required(true);
		}
		
		public Field.Builder<T, F> required(boolean required) {
			this.required = required;
			return this;
		}
		
		public Field.Builder<T, F> multiple() {
			return multiple(true);
		}
		
		public Field.Builder<T, F> multiple(boolean multiple) {
			this.multiple = multiple;
			return this;
		}
		
		public Field.Builder<T, F> pattern(String pattern) {
			this.pattern = Optional.of(pattern);
			return this;
		}
		
		public Field.Builder<T, F> dropzone(String dropzone) {
			this.dropzone = Optional.of(dropzone);
			return this;
		}
		
		public Field.Builder<T, F> name(String name) {
			this.name = Optional.of(name);
			return this;
		}
		
		public Field.Builder<T, F> type(Class<? extends F> type) {
			this.type = Optional.of(type);
			return this;
		}
		
		public Field.Builder<T, F> attr(String name) {
			this.attrs.put(name, null);
			return this;
		}
		
		public Field.Builder<T, F> attr(String name, String val) {
			this.attrs.put(name, val);
			return this;
		}
		
		public Field.Builder<T, F> attrs(Map<String, String> attrs) {
			this.attrs = attrs;
			return this;
		}
		
		public Field.Builder<T, F> disabled() {
			return disabled(true);
		}
		
		public Field.Builder<T, F> disabled(boolean disabled) {
			return disabled(() -> disabled);
		}
		
		public Field.Builder<T, F> disabled(Supplier<Boolean> disabled) {
			this.disabled = Optional.of(disabled);
			return this;
		}
		
		public Field.Builder<T, F> readOnly() {
			return readOnly(true);
		}
		
		public Field.Builder<T, F> readOnly(boolean readOnly) {
			this.readOnly = Optional.of(readOnly);
			return this;
		}
		
		public Field.Builder<T, F> floatingLabel() {
			return floatingLabel(true);
		}
		
		public Field.Builder<T, F> floatingLabel(boolean floatingLabel) {
			this.floatingLabel = Optional.of(floatingLabel);
			return this;
		}
		
		public Field.Builder<T, F> inputGroupBefore(String inputGroupBefore) {
			this.inputGroupBefore = Optional.of(inputGroupBefore);
			return this;
		}
		
		public Field.Builder<T, F> inputGroupAfter(String inputGroupAfter) {
			this.inputGroupAfter = Optional.of(inputGroupAfter);
			return this;
		}
		
		public Field.Builder<T, F> fill() {
			this.span = Optional.of(Collections.emptyList());
			return this;
		}
		
		public Field.Builder<T, F> column() {
			this.span = Optional.empty();
			return this;
		}
		
		public Field.Builder<T, F> span(int span) {
			return span(6, Breakpoint.NONE);
		}
		public Field.Builder<T, F> span(int span, Breakpoint breakpoint) {
			return span(new Span(span, breakpoint));
		}
		
		public Field.Builder<T, F> span(Span... span) {
			this.span = Optional.of(Arrays.asList(span));
			return this;
		}
		
		public Field.Builder<T, F> value(F value) {
			return value(() -> value);
		}
		
		@SuppressWarnings("unchecked")
		public <G> Field.Builder<T, F> value(Supplier<G> value) {
			this.value = Optional.ofNullable((Supplier<F>)value);
			return (Builder<T, F>) this;
		}
		
		@SuppressWarnings("unchecked")
		public <G> Field.Builder<T, F> update(Consumer<G> value) {
			this.update = Optional.ofNullable((Consumer<F>)value);
			return (Builder<T, F>) this;
		}
		
		public <G> Field.Builder<T, F> bind(Supplier<G> value, Consumer<G> update) {
			return value(value).update(update);
		}
		
		public <G> Field.Builder<T, F> noValidation() {
			this.validators = Optional.empty();
			return this;
		}
		
		public <G> Field.Builder<T, F> validator(Validator<F> validators) {
			Objects.requireNonNull(validators);
			if(this.validators.isEmpty())
				this.validators = Optional.of(new ArrayList<>());
			this.validators.get().add(validators);
			return this;
		}
		
		public Field.Builder<T, F> groupCssClass(String... groupCssClasses) {
			return groupCssClass(Set.of(groupCssClasses));
		}
		
		public Field.Builder<T, F> groupCssClass(Set<String> groupCssClasses) {
			this.groupCssClass = Optional.of(groupCssClasses);
			return this;
		}
		
		public Field.Builder<T, F> labelCssClass(String... labelCssClasses) {
			return labelCssClass(Set.of(labelCssClasses));
		}
		
		public Field.Builder<T, F> labelCssClass(Set<String> labelCssClasses) {
			this.labelCssClass = Optional.of(labelCssClasses);
			return this;
		}
		
		public Field<T, F> build() {
			return new Field<>(this);
		}
	}

	final Optional<List<Span>> span;	
	private Optional<List<Validator<F>>> validators;
	private final Optional<Text> label;		
	private final boolean noLabel;				
	private final boolean required;			
	private final boolean multiple;
	private final Optional<String> pattern;
	private final Optional<Text> help;
	private final Optional<Class<? extends F>> type;				
	private final Optional<Text> placeholder;		
	private final Optional<InputType> inputType;
	private final Map<String, String> attrs;
	private final Optional<Supplier<F>> value;
	private final Optional<Consumer<F>> update;
	private final Optional<Supplier<List<Option>>> options;
	private final Optional<Set<String>> groupCssClass;
	private final Optional<Supplier<Boolean>> disabled;
	private final Optional<Boolean> readOnly;
	private final Optional<Boolean> floatingLabel;
	private final Optional<String> inputGroupBefore;
	private final Optional<String> inputGroupAfter;
	private final Optional<String> name;
	private final Optional<Text> validFeedback;
	private final Optional<Boolean> feedback;
	private final Set<FieldDependency> depends;
	private final Optional<Set<String>> labelCssClass;
	private final Optional<Function<F, TemplateModel>> renderer;
	private final Optional<String> dropzone;
	
	private Field(Field.Builder<T, F> bldr) {
		super(bldr);
		this.dropzone = bldr.dropzone;
		this.multiple = bldr.multiple;
		this.renderer = bldr.renderer;
		this.name = bldr.name;
		this.feedback = bldr.feedback;
		this.validFeedback = bldr.validFeedback;
		this.readOnly = bldr.readOnly;
		this.inputGroupAfter = bldr.inputGroupAfter;
		this.inputGroupBefore = bldr.inputGroupBefore;
		this.required = bldr.required;
		this.pattern = bldr.pattern;
		this.disabled = bldr.disabled;
		this.groupCssClass = bldr.groupCssClass;
		this.labelCssClass = bldr.labelCssClass;
		this.options = bldr.options;
		this.value = bldr.value;
		this.update  = bldr.update;
		this.inputType = bldr.inputType;
		this.type = bldr.type;
		this.span = bldr.span;
		this.label = bldr.label;
		this.help = bldr.help;
		this.noLabel = bldr.noLabel;
		this.placeholder = bldr.placeholder;
		this.depends = Collections.unmodifiableSet(new LinkedHashSet<>(bldr.depends));
		this.attrs = Collections.unmodifiableMap(new HashMap<>(bldr.attrs));
		this.validators = bldr.validators.map(vals ->  Collections.unmodifiableList(new ArrayList<>(vals)));
		this.floatingLabel = resolveInputType().supportsFloating() ? bldr.floatingLabel : Optional.of(false);
	}
	
	public Optional<Function<F, TemplateModel>> renderer() {
		return renderer;
	}

	public Optional<List<Validator<F>>> validators() {
		return validators;
	}
	
	public Optional<Supplier<List<Option>>> options() {
		return options;
	}
	
	public Optional<Set<String>> groupCssClass() {
		return groupCssClass;
	}
	
	public Optional<Set<String>> labelCssClass() {
		return labelCssClass;
	}

	public Optional<Supplier<F>> value() {
		return value;
	}

	public Optional<String> dropzone() {
		return dropzone;
	}

	public Optional<Text> validFeedback() {
		return validFeedback;
	}

	public Optional<Boolean> feedback() {
		return feedback;
	}

	public Set<FieldDependency> depends() {
		return depends;
	}

	public Optional<Consumer<F>> update() {
		return update;
	}

	public Optional<String> inputGroupBefore() {
		return inputGroupBefore;
	}

	public Optional<String> inputGroupAfter() {
		return inputGroupAfter;
	}

	public Optional<String> pattern() {
		return pattern;
	}
	
	public Optional<String> attr(String key) {
		return Optional.ofNullable(attrs.get(key));
	}
	
	public Map<String, String> attrs() {
		return attrs;
	}
	
	public boolean required() {
		return required;
	}
	
	public boolean multiple() {
		return multiple;
	}
	
	public Optional<Boolean> disabled() {
		return disabled.map(d -> d.get());
	}
	
	public Optional<Boolean> readOnly() {
		return readOnly;
	}
	
	public Optional<Boolean> floatingLabel() {
		return floatingLabel;
	}
	
	public Optional<InputType> inputType() {
		return inputType;
	}
	
	public Optional<Class<? extends F>> type() {
		return type;
	}
	
	public Optional<Text> label() {
		return label;
	}

	public Optional<Text> help() {
		return help;
	}

	public Optional<Text> placeholder() {
		return placeholder;
	}

	public Optional<List<Span>> span() {
		return span;
	}
	
	public boolean unsized() {
		return span.isEmpty();
	}
	
	public boolean fills() {
		return span.isPresent() && span.get().isEmpty();
	}
	
	public int spans() {
		if(fills()) 
			return 12;
		else if(unsized())
			return 0;
		else
			return span.get().iterator().next().size();
	}

	public boolean noLabel() {
		return noLabel;
	}

	public boolean resolveDisabled() {
		return disabled().orElseGet(() -> {
			return update().isEmpty() && !resolveInputType().supportsReadOnly();
		});
	}

	@Override
	public Optional<String> resolveId() {
		return super.resolveId().or(() -> name);
	}

	public InputType resolveInputType() {
		var inputType = inputType().orElse(InputType.AUTO);
		if(inputType == InputType.AUTO) {
			var type = resolveType();
			if(Number.class.isAssignableFrom(type)) {
				return InputType.NUMBER;
			}
			else if(Boolean.class.isAssignableFrom(type)) {
				return InputType.CHECKBOX;
			}
			else {
				return InputType.TEXT;
			}
		}
		return inputType;
	}
	
	boolean resolveReadOnly() {
		return resolveInputType().supportsReadOnly() && readOnly.orElseGet(() -> update.isEmpty());
	}
	
	String resolveName() {
		return name.or(() -> resolveId()).orElseThrow(() -> new IllegalStateException("Must have a 'name' or an 'id' to use as name."));
	}
	
	@SuppressWarnings("unchecked")
	Class<? extends F> resolveType() {
		return type().orElseGet(() -> {
			F val = value.orElseGet(() -> ()-> {
				if(inputType.isPresent()) {
					switch(inputType.get()) {
					case RADIO:
					case CHECKBOX:
					case SWITCH:
						return (F)Boolean.class;
					case FILE:
						return (F)FormFile.class;
					default:
						return (F)"";
					}
				}
				else {
					return (F)"";
				}
			}).get();
			return  val == null ? null : (Class<F>)(val.getClass());
		});
	}

	@Override
	public String toString() {
		return "Field [span=" + span + ", label=" + label + ", noLabel=" + noLabel + ", help=" + help + ", type=" + type
				+ ", placeholder=" + placeholder + ", inputType=" + inputType + ", attrs=" + attrs + ", value=" + value
				+ "]";
	}
}