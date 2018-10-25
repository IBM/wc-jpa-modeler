package com.ibm.commerce.jpa.port.wizard;

/*
 *-----------------------------------------------------------------
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import com.ibm.commerce.jpa.port.GenerationProperties;
import com.ibm.commerce.jpa.port.JPAGenerateJob;

public class JPAGenerateWizard extends Wizard implements IWizard {

	@Override
	public boolean performFinish() {
		GenerationProperties.ARTIFACT_CLASS_QUALIFIER = selectPackagesPage.getPackageQualifier();
		JPAGenerateJob jpaPortJob = new JPAGenerateJob();
		jpaPortJob.schedule();

		return true;
	}
	
	private SelectPackagesWizardPage selectPackagesPage = new SelectPackagesWizardPage("selectPackagesToMigrate");

	@Override
	public void addPages() {
	  addPage(selectPackagesPage);
	}
	
	@Override
    public String getWindowTitle() {
        return "Generate JPA Artifacts from Entity Beans for WebSphere Commerce";
    }

	@Override
	public IWizardPage getNextPage(IWizardPage currentPage) {
	    if (!selectPackagesPage.isDone()) {
	       return selectPackagesPage;
	    }
	    return null;
	}

	@Override
	public boolean canFinish() {
		if(selectPackagesPage.isDone()) {
			return true;
		} else {
			return false;
		}
		
	}
}
