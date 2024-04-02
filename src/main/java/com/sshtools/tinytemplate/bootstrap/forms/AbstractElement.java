package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Optional;

public abstract class AbstractElement implements Element {
	
	private final Optional<String> id;
	
	protected AbstractElement(AbstractBuilder<?> bldr) {
		this.id = bldr.id;
	}

	@Override
	public Optional<String> resolveId() {
		return id;
	}
}