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
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.ibm.commerce.jpa.port.info.AccessBeanSubclassInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class AccessBeanSubclassInfoXmlGenerator {
	private ProjectInfo iProjectInfo;
	private IProject iProject;
	
	public AccessBeanSubclassInfoXmlGenerator(ProjectInfo projectInfo) {
		iProjectInfo = projectInfo;
		iProject = projectInfo.getProject();
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		System.out.println("generate project info for "+iProject.getName());
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
		sb.append("<ProjectInfo>\r\n");
		Collection<AccessBeanSubclassInfo> accessBeanSubclasses = iProjectInfo.getAccessBeanSubclasses();
		for (AccessBeanSubclassInfo accessBeanSubclassInfo : accessBeanSubclasses) {
			sb.append("\t<AccessBeanSubclassInfo name=\"");
			sb.append(accessBeanSubclassInfo.getName());
			if (accessBeanSubclassInfo.getSuperclass() != null) {
				sb.append("\" superclassName=\"");
				sb.append(accessBeanSubclassInfo.getSuperclass().getName());
			}
			sb.append("\">\r\n");
			Collection<String> methodKeys = accessBeanSubclassInfo.getMethodKeys();
			for (String methodKey : methodKeys) {
				sb.append("\t\t<MethodInfo>\r\n");
				sb.append("\t\t\t<MethodKey><![CDATA[");
				sb.append(methodKey);
				sb.append("]]></MethodKey>\r\n");
				TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getAccessBeanSubclassMethodUnhandledTargetExceptions(accessBeanSubclassInfo, methodKey);
				if (targetExceptionInfo != null) {
					Collection<String> unhandledTargetExceptions = targetExceptionInfo.getTargetExceptions();
					if (unhandledTargetExceptions != null) {
						for (String exception : unhandledTargetExceptions) {
							sb.append("\t\t\t<TargetException name=\"");
							sb.append(exception);
							sb.append("\"/>\r\n");
						}
					}
					Collection<String> unhandledSourceExceptions = targetExceptionInfo.getSourceExceptions();
					if (unhandledSourceExceptions != null) {
						for (String exception : unhandledSourceExceptions) {
							sb.append("\t\t\t<SourceException name=\"");
							sb.append(exception);
							sb.append("\"/>\r\n");
						}
					}
				}
				sb.append("\t\t</MethodInfo>\r\n");
			}
			sb.append("\t</AccessBeanSubclassInfo>\r\n");
		}
		sb.append("</ProjectInfo>");
		IFile projectInfoXmlFile = iProject.getFile(".jpaAccessBeanSubclassInfo.xml");
		ByteArrayInputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes());
		try {
			if (projectInfoXmlFile.exists()) {
				projectInfoXmlFile.setContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 100));
			}
			else {
				projectInfoXmlFile.create(inputStream, true, new SubProgressMonitor(progressMonitor, 100));
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		iProjectInfo.getApplicationInfo().incrementGeneratedAssetCount();
	}
}
