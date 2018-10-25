package com.ibm.commerce.jpa.port.generators;

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

import java.io.ByteArrayInputStream;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.ibm.commerce.jpa.port.info.ProjectInfo;

public class EntityReferencesXmlGenerator {
	private ProjectInfo iProjectInfo;
	private IProject iProject;
	
	public EntityReferencesXmlGenerator(ProjectInfo projectInfo) {
		iProjectInfo = projectInfo;
		iProject = projectInfo.getProject();
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		System.out.println("generate project info for "+iProject.getName());
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
		sb.append("<ProjectInfo>\r\n");
		Set<String> entityReferencingTypes = iProjectInfo.getEntityReferencingTypes();
		for (String entityReferencingType : entityReferencingTypes) {
			sb.append("\t<EntityReferencingType>");
			sb.append(entityReferencingType);
			sb.append("</EntityReferencingType>\r\n");
		}
		Set<String> indirectEntityReferencingTypes = iProjectInfo.getIndirectEntityReferencingTypes();
		for (String indirectEntityReferencingType : indirectEntityReferencingTypes) {
			sb.append("\t<IndirectEntityReferencingType>");
			sb.append(indirectEntityReferencingType);
			sb.append("</IndirectEntityReferencingType>\r\n");
		}
		sb.append("</ProjectInfo>");
		IFile projectInfoXmlFile = iProject.getFile(".jpaEntityReferences.xml");
		ByteArrayInputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes());
		try {
			if (projectInfoXmlFile.exists()) {
				projectInfoXmlFile.setContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 100));
			}
			else {
				projectInfoXmlFile.create(inputStream, true, new SubProgressMonitor(progressMonitor, 100));
			}
			iProjectInfo.getApplicationInfo().incrementGeneratedAssetCount();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
