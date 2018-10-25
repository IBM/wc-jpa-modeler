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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.commerce.jpa.port.info.PersistenceUnitInfo;

public class PersistenceXmlGenerator {
	private PersistenceUnitInfo iPersistenceUnitInfo;
	
	public PersistenceXmlGenerator(PersistenceUnitInfo persistenceUnitInfo) {
		iPersistenceUnitInfo = persistenceUnitInfo;
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
		sb.append("<persistence version=\"2.0\" xmlns=\"http://java.sun.com/xml/ns/persistence\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd\">\r\n");
		//sb.append("<persistence version=\"1.0\" xmlns=\"http://java.sun.com/xml/ns/persistence\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd\">\r\n");
		//sb.append("<persistence>\r\n");
		sb.append("\t<persistence-unit name=\"WC\">\r\n");
		sb.append("\t\t<jta-data-source>jdbc/WebSphere Commerce Cloudscape DataSource demo</jta-data-source>\r\n");
		sb.append("\t\t<jar-file>../Enablement-BaseComponentsData.jar</jar-file>\r\n");
		List<IJavaProject> entityJarProjects = iPersistenceUnitInfo.getEntityJarProjects();
		for (IJavaProject entityJarProject : entityJarProjects) {
			if (!"Enablement-BaseComponentsData".equals(entityJarProject.getElementName())) {
				sb.append("\t\t<jar-file>../");
				sb.append(entityJarProject.getElementName());
				sb.append(".jar</jar-file>\r\n");
			}
		}
		sb.append("\t\t<properties>\r\n");
		sb.append("\t\t\t<property name=\"openjpa.jdbc.DBDictionary\" value=\"storeCharsAsNumbers=false\"/>\r\n");
		//caiduan-fix-start
		sb.append("\t\t\t<property name=\"openjpa.Multithreaded\" value=\"true\"/>\r\n");
		//caiduan-fix-end
		sb.append("\t\t</properties>\r\n");
		sb.append("\t</persistence-unit>\r\n");
		sb.append("</persistence>");
		IFolder metaInfFolder = iPersistenceUnitInfo.getPersistenceUnitRootProject().getProject().getFolder("src").getFolder("META-INF");
		IFile persistenceXmlFile = metaInfFolder.getFile("persistence.xml");
		ByteArrayInputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes());
		try {
			if (persistenceXmlFile.exists()) {
				persistenceXmlFile.setContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 100));
			}
			else {
				persistenceXmlFile.create(inputStream, true, new SubProgressMonitor(progressMonitor, 100));
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
