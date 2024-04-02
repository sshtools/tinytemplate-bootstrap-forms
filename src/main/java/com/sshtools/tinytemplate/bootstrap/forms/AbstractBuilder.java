package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Optional;

class AbstractBuilder<BLDR extends AbstractBuilder<BLDR>> {

	Optional<String> id = Optional.empty();
	
	@SuppressWarnings("unchecked")
	public BLDR id(String id) {
		this.id = Optional.of(id);
		return (BLDR)this;
	}
	
}