package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sshtools.tinytemplate.bootstrap.forms.Form.FormFile;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.Validator;

public final class Field<T, F> extends AbstractElement {
	
	public record Option(String text, String value) {
		public Option(String value) {
			this(value, value);
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
	
	public final static class Builder<T, F> extends AbstractBuilder<Builder<T,F>> {
		private Optional<List<Span>> span = Optional.of(Collections.emptyList());
		private Optional<List<Validator<F>>> validators = Optional.of(new ArrayList<>());
		private Optional<Supplier<F>> value = Optional.empty();
		private Optional<Consumer<F>> update = Optional.empty();
		private Optional<T> instance;
		private Optional<Boolean> disabled = Optional.empty();
		private Optional<Boolean> readOnly = Optional.empty();
		private Optional<Boolean> floatingLabel = Optional.empty();
		private boolean noLabel;
		private boolean required;
		private Optional<String> pattern = Optional.empty();
		private Optional<String> name = Optional.empty();
		private Optional<InputType> inputType = Optional.empty();
		private Optional<Class<F>> type = Optional.empty();
		private Optional<Text> label = Optional.empty();
		private Optional<Text> help = Optional.empty();
		private Optional<Text> placeholder = Optional.empty();
		private Optional<Set<String>> groupCssClass = Optional.empty();
		private Map<String, String> attrs = new HashMap<>();
		private Optional<Supplier<List<Option>>> options = Optional.empty();
		private Optional<String> inputGroupBefore = Optional.empty();
		private Optional<String> inputGroupAfter = Optional.empty();
		private Optional<Text> validFeedback = Optional.empty();
		private Optional<Boolean> feedback = Optional.empty();
		
		Builder(Optional<T> instance) {
			this.instance = instance;
		}
		
		public T $() {
			return instance();
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
		
		public Field.Builder<T, F> pattern(String pattern) {
			this.pattern = Optional.of(pattern);
			return this;
		}
		
		public Field.Builder<T, F> name(String name) {
			this.name = Optional.of(name);
			return this;
		}
		
		public Field.Builder<T, F> type(Class<F> type) {
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
		
		public Field<T, F> build() {
			return new Field<>(this);
		}
	}

	final Optional<List<Span>> span;	
	private Optional<List<Validator<F>>> validators;
	private final Optional<Text> label;		
	private final boolean noLabel;				
	private final boolean required;
	private final Optional<String> pattern;
	private final Optional<Text> help;
	private final Optional<Class<F>> type;				
	private final Optional<Text> placeholder;		
	private final Optional<InputType> inputType;
	private final Map<String, String> attrs;
	private final Optional<Supplier<F>> value;
	private final Optional<Consumer<F>> update;
	private final Optional<Supplier<List<Option>>> options;
	private final Optional<Set<String>> groupCssClass;
	private final Optional<Boolean> disabled;
	private final Optional<Boolean> readOnly;
	private final Optional<Boolean> floatingLabel;
	private final Optional<String> inputGroupBefore;
	private final Optional<String> inputGroupAfter;
	private final Optional<String> name;
	private final Optional<Text> validFeedback;
	private final Optional<Boolean> feedback;
	
	private Field(Field.Builder<T, F> bldr) {
		super(bldr);
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
		this.attrs = Collections.unmodifiableMap(new HashMap<>(bldr.attrs));
		this.validators = bldr.validators.map(vals ->  Collections.unmodifiableList(new ArrayList<>(vals)));
		this.floatingLabel = resolveInputType().supportsFloating() ? bldr.floatingLabel : Optional.of(false);
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

	public Optional<Supplier<F>> value() {
		return value;
	}

	public Optional<Text> validFeedback() {
		return validFeedback;
	}

	public Optional<Boolean> feedback() {
		return feedback;
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
	
	public Optional<Boolean> disabled() {
		return disabled;
	}
	
	public Optional<Boolean> readOnly() {
		return disabled;
	}
	
	public Optional<Boolean> floatingLabel() {
		return floatingLabel;
	}
	
	public Optional<InputType> inputType() {
		return inputType;
	}
	
	public Optional<Class<F>> type() {
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
	Class<F> resolveType() {
		return type().orElseGet(() -> (Class<F>)(value.orElseGet(() -> ()-> {
			if(inputType.isPresent()) {
				switch(inputType.get()) {
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
		}).get().getClass()));
	}

	@Override
	public String toString() {
		return "Field [span=" + span + ", label=" + label + ", noLabel=" + noLabel + ", help=" + help + ", type=" + type
				+ ", placeholder=" + placeholder + ", inputType=" + inputType + ", attrs=" + attrs + ", value=" + value
				+ "]";
	}
}