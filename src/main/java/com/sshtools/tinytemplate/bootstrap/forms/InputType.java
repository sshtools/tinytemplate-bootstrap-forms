package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.sshtools.tinytemplate.bootstrap.forms.Validation.RangeValidator;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.Validator;

public enum InputType implements Templatable {
	AUTO, TEXT, FILE, TEXTAREA, NUMBER, EMAIL, SELECT, CHECKBOX, SWITCH, RANGE, PASSWORD;
	
	public enum Value {
		ATTRIBUTE, CHECKED, CONTENT
	}
	
	public Validator<?>[] defaultValidators() {
		switch(this) {
		case NUMBER:
		case RANGE:
			return new Validator<?>[] { new RangeValidator<Number>() };
		default:
			return new Validator<?>[0];
		}
	}
	
	public boolean supportsFloating() {
		switch(this) {
		case CHECKBOX:
		case SWITCH:
		case RANGE:
			return false;
		default:
			return true;
		}
	}
	
	public boolean supportsReadOnly() {
		switch(this) {
		case CHECKBOX:
		case SELECT:
		case SWITCH:
			return false;
		default:
			return true;
		}
	}

	public String tag() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		else if (this == InputType.SELECT || this == InputType.TEXTAREA)
			return name().toLowerCase();
		return "input";
	}
	
	
	public Value value() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		if (this == InputType.CHECKBOX || this == InputType.SWITCH)
			return Value.CHECKED;
		else if (this == InputType.TEXTAREA)
			return Value.CONTENT;
		return Value.ATTRIBUTE;
	}
	
	public Optional<String> role() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		else if (this == InputType.SWITCH)
			return Optional.of("switch");
		else
			return Optional.empty();
	}

	public String attribute() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		else if (this == InputType.SWITCH)
			return "checkbox";
		else
			return name().toLowerCase();
	}

	public boolean labelFirst() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		else if (this == InputType.CHECKBOX || this == InputType.SWITCH)
			return false;
		return true;
	}

	public Set<String> groupCssClass() {
		if (this == InputType.CHECKBOX)
			return Set.of("form-check");
		else if (this == InputType.SWITCH)
			return Set.of("form-check", "form-switch");
		else
			return Collections.emptySet();
	}

	public Set<String> labelCssClass() {
		if (this == InputType.CHECKBOX || this == InputType.SWITCH)
			return Set.of("form-check-label");
		else
			return Set.of("form-label");
	}

	public Set<String> cssClass() {
		if (this == InputType.CHECKBOX || this == InputType.SWITCH)
			return Set.of("form-check-input");
		else if (this == InputType.RANGE)
			return Set.of("form-range"); 
		else
			return Set.of("form-control");
	}
}