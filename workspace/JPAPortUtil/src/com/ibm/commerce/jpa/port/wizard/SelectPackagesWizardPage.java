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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.commerce.jpa.port.GenerationProperties;

public class SelectPackagesWizardPage extends WizardPage implements IWizardPage {

	private Composite container = null;
	private Text packageQualifierTextBox = null;
	
	protected SelectPackagesWizardPage(String pageName) {
		super(pageName);
	}

	@Override
    public void createControl(Composite parent) {
		super.setTitle("Entity Bean to JPA Entity Migration Wizard");

		super.setDescription(
				"This wizard will scan the workspace for Entity beans and generate JPA entites and access beans.\n" + 
				"Use the Replace Entity Beans task to finalize the JPA entity setup when complete."
		);

        container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        Label packageQualifierLabel = new Label(container, SWT.NONE);
        packageQualifierLabel.setText("Enter Class Qualifier");

        packageQualifierTextBox = new Text(container, SWT.BORDER | SWT.SINGLE);
        packageQualifierTextBox.setText(GenerationProperties.ARTIFACT_CLASS_QUALIFIER);
        packageQualifierTextBox.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
               // no action for now
            }

            @Override
            public void keyReleased(KeyEvent e) {
        		if(packageQualifierTextBox.getText()!= null && !packageQualifierTextBox.getText().isEmpty()) {
                    setPageComplete(true);
                }
            }

        });
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        packageQualifierTextBox.setLayoutData(gd);
        setControl(container);
    }
	
	public String getPackageQualifier() {
		return packageQualifierTextBox.getText();
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}


	public boolean isDone() {
		if(packageQualifierTextBox.getText()!= null && !packageQualifierTextBox.getText().isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

}
