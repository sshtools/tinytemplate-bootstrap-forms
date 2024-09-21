package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Optional;

import com.sshtools.tinytemplate.bootstrap.forms.Validation.RangeValidator;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.Validator;

public enum InputType implements Templatable {
	AUTO, TEXT, FILE, TEXTAREA, NUMBER, TEL, EMAIL, SELECT, CHECKBOX, RADIO, SWITCH, RANGE, PASSWORD, URL,
	DATE, DATETIME_LOCAL, MONTH, TIME, NONE;
	
	public enum Value {
		ATTRIBUTE, CHECKED, CONTENT, CHECKED_VALUE, NONE
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
		case NONE:
		case CHECKBOX:
		case RADIO:
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
		case RADIO:
		case SELECT:
		case SWITCH:
			return false;
		default:
			return true;
		}
	}
	
	public boolean templatable() {
		switch(this) {
		case FILE:
			return true;
		default:
			return false;
		}
	}

	public String tag() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		else if (this == InputType.SELECT || this == InputType.TEXTAREA)
			return name().toLowerCase();
		return "input";
	}


	public boolean inputTag() {
		return tag().equals("input");
	}
	
	
	public Value value() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		if(this == InputType.FILE)
			return Value.NONE;
		if (this == InputType.CHECKBOX || this == InputType.SWITCH)
			return Value.CHECKED;
		else if (this == InputType.TEXTAREA)
			return Value.CONTENT;
		else if (this == InputType.RADIO)
			return Value.CHECKED_VALUE;
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
			return name().toLowerCase().replace('_', '-');
	}
	
	public boolean options() {
		return this == RADIO || this == CHECKBOX || this == SELECT;
	}
	
	public boolean optionsAsFields() {
		return this == RADIO || this == CHECKBOX;
	}

	public boolean labelFirst() {
		if (this == InputType.AUTO)
			throw new IllegalStateException();
		else if (this == InputType.RADIO || this == InputType.CHECKBOX || this == InputType.SWITCH)
			return false;
		return true;
	}
}