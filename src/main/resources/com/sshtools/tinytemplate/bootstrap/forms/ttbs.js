class TinyTemplateBoostrapForms {
	
    constructor() {
		this.findFormControls();
    }
    
    findFormControls() {
		let forms = document.querySelectorAll('form');
		forms.forEach((frm) => {
			
			/* Deal with field dependencies */
			let deps = frm.querySelectorAll('[data-depends]');
			deps.forEach((depEl) => {
				const depNames = depEl.dataset.depends.split(',');
				const depValues = depEl.dataset.dependsValue.split(',');
				if(depNames.length != depValues.length) {
					alert('Mismatch of data-depends and data-depends-value');
					throw 'Mismatch of data-depends and data-depends-value';
				}
				else {
					for(let i = 0 ; i < depNames.length; i++) {
						const dep = depNames[i];
						let depValuesStr = depValues[i];
						
						let negate = false;
						if(depValuesStr.startsWith('!')) {
							depValuesStr = depValuesStr.substring(1);
							negate = true;
						}
						
						let input = frm.elements[dep];
						let fieldset = this.findFieldset(input);
						this.checkState(depEl, dep, depValues, input, false, negate);
						if(fieldset) {
							fieldset.addEventListener('change', (evt) => { 
								this.checkState(depEl, dep, depValues, input, fieldset, negate);
							}); 
						}
						else {
							input.addEventListener('change', (evt) => { 
								this.checkState(depEl, dep, depValues, input, false, negate);
							});
						}
					}
				}
			});	
			
			/* Deal with file uploads (drag and drop and multiple files) */
			let templates  = frm.querySelectorAll('template');
			templates.forEach((t) => {
				this.addTemplatedRow(t);
			});
			let dropzones = frm.querySelectorAll('[data-dropzone]');
			dropzones.forEach((dz) => {
				
				let rz = document.getElementById(dz.dataset.dropzone);
				
				rz.classList.add('drop-zone');
				rz.addEventListener('drop', (evt) => {
					dz.files = evt.dataTransfer.files;
					rz.classList.remove('border', 'border-2', 'p-3');
					evt.preventDefault();
				});
				rz.addEventListener('dragover', (evt) => {
					rz.classList.add('border', 'border-2', 'p-3');
					evt.preventDefault();
				});
				rz.addEventListener('dragleave', (evt) => {
					rz.classList.remove('border', 'border-2', 'p-3');
				});
			});
			
			/* Deal with field actions */
			let fieldActions = frm.querySelectorAll('[data-field-op]');
			fieldActions.forEach((fa) => {
				let trigger = fa.dataset.fieldTrigger; 
				fa.addEventListener(trigger ? trigger : 'click', (evt) => {
					
					/* TODO one way at the moment */
					fa.classList.add('d-none');
					
					/* Apply visual effect */
					let effect = fa.dataset.fieldEffect;
					if(effect) {
						let effectTarget = fa.dataset.fieldEffectTarget;
						if(effectTarget) {
							let effectEl = document.getElementById(effectTarget);
							if(effectEl) {
								if(effect === 'hide') {
									effectEl.classList.add('d-none');
								}
								else if(effect === 'strikethru') {
									effectEl.classList.add('text-decoration-line-through');
								}
								else {
									console.log('Effect "' + effect + '" does not exist, specified for a target "' + effectTarget + '"');
								}
							}
							else {
								console.log('Effect specified for a target "' + effectTarget + '" that does not exist.');
							}
						}
						else {
							console.log('Effect specified, but no effect target.');
						}
					}
					
					/* Update state (i.e. adjust targeted form field values) */
					let op = fa.dataset.fieldOp;
					let fieldName = fa.dataset.fieldName;
					let field = document.querySelector('[name=' + fieldName + ']');
					if(field) {
						if(op === 'remove-from-string-list') {
							field.value = field.value.split(',').filter(item => item !== fa.dataset.fieldValue).join(",");
						}
						else if(op === 'add-to-string-list') {
							let currentList = field.value.split(',');
							currentList.append(fa.dataset.fieldValue);
							field.value = currentList.join(",");
						}
						else {
							console.log('Unknown field op "' + op + '".');
						}
					}
					else {
						console.log('Unknown field "' + fieldName + '".');
					}
						
					
					evt.preventDefault();
				});
			});
		});
		
        return true;
    }
	
	addTemplatedRow(templ) {
	    //fileIndex++;
		var container = document.getElementById(templ.id + '-container');
		let parent = document.createElement("div");
	    parent.append(templ.content.cloneNode(true));
	    container.append(parent);
		var input = parent.querySelector('input');
		var removeRowLink = parent.querySelector('[role=remove-templated-row]');
		removeRowLink.addEventListener('click', (evt) => {
			parent.remove();
			evt.preventDefault();
		});
		removeRowLink.parentElement.classList.add('d-none');
		input.addEventListener('change', (evt) => {
			this.addTemplatedRow(templ);
			if(removeRowLink) {
				removeRowLink.parentElement.classList.remove('d-none');
			}
		});
		input.focus();
			
		/*
	    var newRow = $(templ.html());
	    newRow.children('#file1').eq(0).attr('id', 'file' + fileIndex).attr('name', 'file' + fileIndex);
	    var file1Remove = newRow.children('#file1Remove').eq(0);
	    file1Remove.attr('id', 'file' + fileIndex + 'Remove');
	    container.append(newRow);
	    file1Remove.children('a').eq(0).on('click', function(e) {
	        e.preventDefault();
	        if($('#files').children().length > 2)
	            newRow.remove();
	    });
	    var input = newRow.children('input').eq(0);
	    input.change(function () {
	       addFileRow();
		   file1Remove.removeClass('d-none');
	     });
	    input.focus();
		*/
	}
	
	findFieldset(el) {
		while(el) {
			if(el instanceof RadioNodeList) {
				el = el[0];	
			}
			if(el.tagName === 'FIELDSET')
				return el;
			else if(el.tagName === 'FORM')
				return null;
			else
				el = el.parentElement;
		}
		return false;
	}
	
	checkState(el, dep, depValues, input, fieldset, negate) {
		let inputVal = input.value;
		let beVisible = depValues.includes(inputVal);
		if(negate)
			beVisible = !beVisible;
		let isVisible = !el.classList.contains('d-none');
		
		if(beVisible && !isVisible) {
			el.classList.remove('d-none');
			
			/* Re-enabled any fields that were disabled when being hidden */
			let deps = el.querySelectorAll('input');
			deps.forEach((el) => {
				if(el.dataset.hiddenDepends === 'true') {
					delete  el.dataset.hiddenDepends;
					el.disabled = false;
				}	
			});
		}
		else if(!beVisible && isVisible) {
			el.classList.add('d-none');
			
			/* Stop client side validation by making any non-disabled input
			   fields as disabled while they are hidden.
			   
			   If the become visible again, the disabled attribute will be 
			   removed */
			    
			let deps = el.querySelectorAll('input');
			deps.forEach((el) => {
				if(!el.disabled) {
					el.dataset.hiddenDepends = 'true';
					el.disabled = true;
				}	
			});
			
		}
	}
}

new TinyTemplateBoostrapForms();