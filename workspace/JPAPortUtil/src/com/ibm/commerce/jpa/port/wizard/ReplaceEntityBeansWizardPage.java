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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ReplaceEntityBeansWizardPage extends WizardPage implements IWizardPage {
	
	private Composite container = null;
	
	protected ReplaceEntityBeansWizardPage(String pageName) {
		super(pageName);
	}

	@Override
    public void createControl(Composite parent) {
		super.setTitle("Replace Entity Beans with JPA Entites");

		super.setDescription(
				"This wizard will remove the EJB artifacts and access beans in the workspace\n" + 
				"and replace them with the generated JPA entities and access beans."
		);

        container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 1;
        Label infoLabel = new Label(container, SWT.NONE);
        infoLabel.setText("There are currently no configurable options for this wizard.");

        Label restoreInfoLabel = new Label(container, SWT.NONE);
        restoreInfoLabel.setText("The Restore option can be used to reset the workspace afterwords, if needed.");

        setControl(container);
    }
	
	@Override
	public boolean canFlipToNextPage() {
		return false;
	}


	public boolean isDone() {
		return true;
	}

}
