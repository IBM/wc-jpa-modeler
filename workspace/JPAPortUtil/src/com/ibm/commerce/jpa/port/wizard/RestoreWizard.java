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

import com.ibm.commerce.jpa.port.RestoreJob;

public class RestoreWizard extends Wizard implements IWizard {

	private RestoreWizardPage restorePage = new RestoreWizardPage("optionsToRestore");

	@Override
	public void addPages() {
	  addPage(restorePage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage currentPage) {
	    if (!restorePage.isDone()) {
	       return restorePage;
	    }
	    return null;
	}

	@Override
	public boolean canFinish() {
		if(restorePage.isDone()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getWindowTitle() {
		return "Restore All Artifacts";
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	@Override
	public boolean needsPreviousAndNextButtons() {
		return false;
	}

	@Override
	public boolean needsProgressMonitor() {
		return false;
	}

	@Override
	public boolean performCancel() {
		return true;
	}

	@Override
	public boolean performFinish() {
		RestoreJob restoreJob = new RestoreJob();
		restoreJob.schedule();
		return true;
	}

}
