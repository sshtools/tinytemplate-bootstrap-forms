package com.sshtools.tinytemplate.bootstrap.forms;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sshtools.tinytemplate.bootstrap.forms.Form.Icon;

public interface Framework {

	Map<Icon, Set<String>> defaultIcons();
	
	Map<Template, TemplateResource> defaultTemplates();
	
	default Set<String> groupCssClass(InputType type) {
		return Collections.emptySet();
	}

	default	Set<String> labelCssClass(InputType type){
		return Collections.emptySet();
	}
	
	default Set<String> cssClass(InputType type){
		return Collections.emptySet();
	}
	
	default String processValueRender(Field<?, ?> field, String raw) {
		return raw;
	}
	
	default String processValueSubmit(Field<?, ?> field, String raw) {
		return raw;
	}
}
