package com.ibm.commerce.jpa.port.search;

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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;

public class AccessBeanSubclassSearchUtil {
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	
	public AccessBeanSubclassSearchUtil(ModuleInfo moduleInfo) {
		iApplicationInfo = moduleInfo.getApplicationInfo();
		iModuleInfo = moduleInfo;
	}
	
	public void search(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("search for accessbean subclasses "+iModuleInfo.getJavaProject().getElementName(), (iModuleInfo.getEntities().size() * 1000));
			long start = new java.util.Date().getTime();
			System.out.println("searching for accessbean subclasses "+iModuleInfo.getJavaProject().getElementName());
			Collection<EntityInfo> entities = iModuleInfo.getEntities();
			for (EntityInfo entityInfo : entities) {
				if (entityInfo.getEjbAccessBeanType() != null) {
					ITypeHierarchy typeHierarchy = entityInfo.getEjbAccessBeanType().newTypeHierarchy(new SubProgressMonitor(progressMonitor, 1000));
					IType subtypes[] = typeHierarchy.getAllSubtypes(entityInfo.getEjbAccessBeanType());
					if (subtypes != null) {
						for (IType subtype : subtypes) {
							if (subtype.isBinary()) {
								System.out.println("found binary access bean subclass: "+subtype.getFullyQualifiedName('.'));
							}
							else {
								String subtypeName = subtype.getFullyQualifiedName('.');
								iApplicationInfo.getProjectInfo(subtype.getResource().getProject()).getAccessBeanSubclassInfo(subtypeName, true);
							}
							iApplicationInfo.incrementSearchResultCount();
						}
					}
				}
			}
			System.out.println("end search for accessbean subclasses "+iModuleInfo.getJavaProject().getElementName()+" "+((new java.util.Date().getTime() - start)/1000)+" seconds");
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
}
