package com.sshtools.tinytemplate.bootstrap.forms;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public final class Text {
	public final static class Builder {
		private Optional<String> text = Optional.empty();
		private Optional<String> key = Optional.empty();
		private Optional<Object[]> args = Optional.empty();
		private Optional<ResourceBundle> bundle = Optional.empty();
		
		public Text.Builder text(String text) {
			this.text = Optional.of(text);
			return this;
		}
		
		public Text.Builder key(String key) {
			this.key = Optional.of(key);
			return this;
		}
		
		public Text.Builder bundle(ResourceBundle bundle) {
			this.bundle = Optional.of(bundle);
			return this;
		}
		
		public Text.Builder args(Object... args) {
			this.args = Optional.of(args);
			return this;
		}
		
		public Text build() {
			return new Text(this);
		}
	}
	
	public final static Text of(String text) {
		return new Builder().text(text).build();
	}
	
	public final static Text ofI18n(String key, Object... args) {
		return new Builder().key(key).args(args).build();
	}
	
	public final static Text ofI18n(String key, ResourceBundle bundle, Object... args) {
		return new Builder().key(key).bundle(bundle).args(args).build();
	}

	public final static Text ofI18n(String key, Class<?> bundle, Object... args) {
		return ofI18n(key, bundle, Locale.getDefault(), args);
	}
	
	public final static Text ofI18n(String key, Class<?> bundle, Locale locale, Object... args) {
		return new Builder().key(key).bundle(ResourceBundle.getBundle(bundle.getName(), locale, bundle.getClassLoader())).args(args).build();
	}
	
	final Optional<String> text;
	final Optional<String> key;
	final Optional<Object[]> args;
	final Optional<ResourceBundle> bundle;
	
	private Text(Text.Builder builder) {
		if(builder.text.isEmpty()  && builder.key.isEmpty()) {
			throw new IllegalStateException("Must either provide text or key (and bundle)");
		}
		this.text = builder.text;
		this.key = builder.key;
		this.args = builder.args;
		this.bundle = builder.bundle;
	}

	public Optional<String> text() {
		return text;
	}

	public Optional<String> key() {
		return key;
	}

	public Optional<Object[]> args() {
		return args;
	}

	public Optional<ResourceBundle> bundle() {
		return bundle;
	}
	
	
	public String resolveString() {
		return resolveString(Optional.empty());
	}
	
	public String resolveString(Optional<ResourceBundle> defBundle) {
		if(text.isPresent()) {
			/* Fixed text, easy */
			return text.get();
		}
		else {
			var key = this.key.get();
			var bundle = this.bundle.or(() ->  defBundle).orElseThrow(() -> new IllegalStateException(MessageFormat.format("No bundle for key {}", key)));
			/* Fixed key, just use the that */
			try {
				return MessageFormat.format(bundle.getString(key), args.orElse(new Object[0]));
			}
			catch(MissingResourceException mre) {
				/* Caller was explicit, so they should know the key is missing */
				return "i18n:" + bundle.getBaseBundleName() + "@" + key;
			}
		}
	}
}