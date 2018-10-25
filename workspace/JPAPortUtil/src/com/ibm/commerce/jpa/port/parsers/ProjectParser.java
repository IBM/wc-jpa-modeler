package com.ibm.commerce.jpa.port.parsers;

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

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import com.ibm.commerce.jpa.port.info.AccessBeanSubclassInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;

public class ProjectParser {
	private ASTParser iASTParser = ASTParser.newParser(AST.JLS3);
	private ProjectInfo iProjectInfo;
	
	public ProjectParser(ProjectInfo projectInfo) {
		iProjectInfo = projectInfo;
	}
	
	public IStatus parse(IProgressMonitor progressMonitor) {
		System.out.println("Parsing project " + iProjectInfo.getProject().getName());
		IStatus status = Status.OK_STATUS;
		try {
			Collection<AccessBeanSubclassInfo> accessBeanSubclasses = iProjectInfo.getAccessBeanSubclasses();
			progressMonitor.beginTask("parse " + iProjectInfo.getProject().getName(), accessBeanSubclasses.size() * 1000);
			for (AccessBeanSubclassInfo accessBeanSubclassInfo : accessBeanSubclasses) {
				if (progressMonitor.isCanceled()) {
					status = Status.CANCEL_STATUS;
					break;
				}
				AccessBeanSubclassParser parser = new AccessBeanSubclassParser(iASTParser, accessBeanSubclassInfo);
				parser.parse(new SubProgressMonitor(progressMonitor, 1000));
			}
		}
		finally {
			progressMonitor.done();
			System.out.println("done parsing " + iProjectInfo.getProject().getName());
		}
		return status;
	}
}
