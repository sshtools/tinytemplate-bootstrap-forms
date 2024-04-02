package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Optional;

public final class Validation {
	
	@SuppressWarnings("serial")
	public static class ValidationException extends RuntimeException {
		private final Field<?, ?> field;
		private final Optional<Text> text;
		
		public ValidationException(Field<?, ?> field) {
			this(field, (Text)null);
		}
		
		public ValidationException(Field<?, ?> field, String message) {
			this(field, Text.of(message));
		}
		
		public ValidationException(Field<?, ?> field, Text text) {
			this.field = field;
			this.text = Optional.ofNullable(text);
		}
		
		public Optional<Text> text() {
			return text;
		}
		
		public Field<?, ?> field() {
			return field;
		}
	}

	@FunctionalInterface
	public interface Validator<F>  {
		void validate(Field<F, ?> field, F value);
	}
	
	public final static class RangeValidator<F extends Number> implements Validator<F> {

		@Override
		public void validate(Field<F, ?> field, F value) {
			var min = field.attr("min").map(Double::parseDouble).orElse(0d);
			var max = field.attr("max").map(Double::parseDouble).orElse(Double.MAX_VALUE);
			var val = value.doubleValue();
			if(val < min || val > max) {
				throw new ValidationException(field);
			}
		}
		
	}
}
