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

import java.util.Collection;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.EjbProjectUtil;

/**
 * Loops through the entities in the ModuleInfo and calls the Entity*Generator classes to generate the relevant portions of each entity.
 * Also writes module info to .jpaModuleInfo.xml metadata file at the root of the module
 * 
 */
public class ModuleGenerator {
	private ASTParser iASTParser = ASTParser.newParser(AST.JLS3);
	private IWorkspace iWorkspace;
	private IJavaProject iJavaProject;
	private ModuleInfo iModuleInfo;
	private BackupUtil iBackupUtil;
	
	public ModuleGenerator(IWorkspace workspace, ModuleInfo moduleInfo) {
		iWorkspace = workspace;
		iJavaProject = moduleInfo.getJavaProject();
		iModuleInfo = moduleInfo;
		iBackupUtil = moduleInfo.getApplicationInfo().getBackupUtil(iJavaProject.getProject());
	}
	
	public IStatus generate(IProgressMonitor progressMonitor) {
		System.out.println("generating entities for "+iModuleInfo.getJavaProject().getElementName());
		IStatus status = Status.OK_STATUS;
		try {
			Collection<EntityInfo> entities = iModuleInfo.getEntities();
			System.out.println("  [ARVERA]:["+this.getClass().getCanonicalName()+"] Total entities to process on this job="+entities.size());
			progressMonitor.beginTask("Generate " + iJavaProject.getProject().getName(), entities.size() * 1000 + 100);
			if (entities.size() > 0) {
				EjbProjectUtil projectUtil = new EjbProjectUtil(iWorkspace, iJavaProject);
				//projectUtil.addClasspathEntry("WCPersistence.jar");
				for (EntityInfo entityInfo : entities) {
					if (progressMonitor.isCanceled()) {
						status = Status.CANCEL_STATUS;
						break;
					}
					if (entityInfo.getSubtypes() != null) {
						generateEntity(progressMonitor, entityInfo);
					}
				}
				for (EntityInfo entityInfo : entities) {
					if (progressMonitor.isCanceled()) {
						status = Status.CANCEL_STATUS;
						break;
					}
					if (entityInfo.getSubtypes() == null) {
						generateEntity(progressMonitor, entityInfo);
					}
				}
				ModuleInfoXmlGenerator moduleInfoXMLGenerator = new ModuleInfoXmlGenerator(iModuleInfo);
				moduleInfoXMLGenerator.generate(progressMonitor);
			}
		}
		finally {
			progressMonitor.done();
		}
		return status;
	}
	
	private void generateEntity(IProgressMonitor progressMonitor, EntityInfo entityInfo) {
		System.out.println("entity="+entityInfo.getEjbName());
		EntityClassGenerator entityClassGenerator = new EntityClassGenerator(iASTParser, iBackupUtil, entityInfo);
		entityClassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
		EntityKeyClassGenerator entityKeyClassGenerator = new EntityKeyClassGenerator(iBackupUtil, entityInfo);
		entityKeyClassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
		EntityQueryHelperClassGenerator entityQueryHelperClassGenerator = new EntityQueryHelperClassGenerator(iASTParser, iBackupUtil, entityInfo);
		entityQueryHelperClassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
		if (entityInfo.getAccessBeanInfo() != null && entityInfo.getAccessBeanInfo().getDataClassType()) {
			EntityDataClassGenerator entityDataClassGenerator = new EntityDataClassGenerator(iASTParser, iBackupUtil, entityInfo);
			entityDataClassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
		}
		else if(entityInfo.getAccessBeanInfo() != null){
			EntityAccessBeanClassGenerator entityAccessBeanClassGenerator = new EntityAccessBeanClassGenerator(iBackupUtil, entityInfo);
			entityAccessBeanClassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
		}
		if(entityInfo.getAccessBeanInfo()!= null) {
			EntityAccessHelperClassGenerator entityAccessHelperClassGenerator = new EntityAccessHelperClassGenerator(iASTParser, iBackupUtil, entityInfo);
			entityAccessHelperClassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
			EntityEntityCreationDataClassGenerator entityEntityCreationDataClassGenerator = new EntityEntityCreationDataClassGenerator(iBackupUtil, entityInfo);
			entityEntityCreationDataClassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
		}
	}
}
