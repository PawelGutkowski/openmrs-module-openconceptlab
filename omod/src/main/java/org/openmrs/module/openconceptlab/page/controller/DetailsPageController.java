/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openconceptlab.page.controller;

import org.openmrs.module.openconceptlab.Update;
import org.openmrs.module.openconceptlab.UpdateService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Page controller for the details page
 */
public class DetailsPageController {
	
	public void get(PageModel model, @RequestParam(value = "updateId", required = false) Long updateId) {
		
		model.addAttribute("updateId", updateId);
	}
	
	public void post(PageModel model, @RequestParam(value = "updateId", required = false) Long updateId,
	        @RequestParam(value = "ignoreAllErrors", required = false) Boolean ignoreAllErrors,
	        @SpringBean("openconceptlab.updateService") UpdateService updateService) {
		
		if (Boolean.TRUE.equals(ignoreAllErrors)) {
			Update update = updateService.getUpdate(updateId);
			updateService.ignoreAllErrors(update);
		}
		
		model.addAttribute("updateId", updateId);
	}
}
