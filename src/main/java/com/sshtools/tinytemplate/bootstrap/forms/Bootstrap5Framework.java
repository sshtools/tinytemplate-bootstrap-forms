package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sshtools.tinytemplate.bootstrap.forms.Form.Icon;

public class Bootstrap5Framework implements Framework {


	private static Map<Icon, Set<String>> DEFAULT_ICONS = Map.of(
		Icon.TRASH, Set.of("bi", "bi-trash")
	);

	private static Map<Template, TemplateResource> DEFAULT_TEMPLATES = Map.of(
				Template.FORM, new TemplateResource(Form.class, "form.template.html"),
				Template.ROW, new TemplateResource(Form.class, "row.template.html"),
				Template.GROUP, new TemplateResource(Form.class, "group.template.html"),
				Template.INPUT, new TemplateResource(Form.class, "input.template.html"),
				Template.FIELD, new TemplateResource(Form.class, "field.template.html"),
				Template.COLUMN, new TemplateResource(Form.class, "column.template.html"),
				Template.TEMPLATE, new TemplateResource(Form.class, "template.template.html"));


	@Override
	public Map<Icon, Set<String>> defaultIcons() {
		return DEFAULT_ICONS;
	}

	@Override
	public Map<Template, TemplateResource> defaultTemplates() {
		return DEFAULT_TEMPLATES;
	}

	@Override
	public Set<String> groupCssClass(InputType type) {
		if (type == InputType.CHECKBOX || type == InputType.RADIO) {
			return Set.of("form-check");
		} else if (type == InputType.SWITCH) {
			return Set.of("form-check", "form-switch");
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<String> labelCssClass(InputType type) {
		if (type == InputType.CHECKBOX || type == InputType.SWITCH || type == InputType.RADIO) {
			return Set.of("form-check-label");
		} else {
			return Set.of("form-label");
		}
	}

	@Override
	public Set<String> cssClass(InputType type) {
		if (type == InputType.CHECKBOX || type == InputType.SWITCH || type == InputType.RADIO) {
			return Set.of("form-check-input");
		} else if (type == InputType.RANGE) {
			return Set.of("form-range");
		} else {
			return Set.of("form-control");
		}
	}

}
