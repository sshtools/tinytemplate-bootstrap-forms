package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Optional;

public class DefaultFramework {

	private final static class Defaults {
		private final static Framework defaultFramework  = new Bootstrap5Framework();
	}
	
	private static Optional<Framework> framework = Optional.empty();
	
	public static Framework get() {
		return framework.orElseGet(() -> Defaults.defaultFramework);
	}
	
	public static void set(Framework framework) {
		DefaultFramework.framework = Optional.ofNullable(framework);
	}
}
