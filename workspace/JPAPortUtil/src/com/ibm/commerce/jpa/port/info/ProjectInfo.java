package com.ibm.commerce.jpa.port.info;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class ProjectInfo {
	private ApplicationInfo iApplicationInfo;
	private IProject iProject;
	private IJavaProject iJavaProject;
	private HashSet<String> iEntityReferencingTypes = new HashSet<String>();
	private HashSet<String> iEntityReferenceSubclasses = new HashSet<String>();
	private HashSet<String> iIndirectEntityReferencingTypes = new HashSet<String>();
	private HashSet<String> iDeleteIntendedTypes = new HashSet<String>();
	private HashMap<String, AccessBeanSubclassInfo> iAccessBeanSubclasses = new HashMap<String, AccessBeanSubclassInfo>();
	
	public ProjectInfo(ApplicationInfo applicationInfo, IProject project) {
		iApplicationInfo = applicationInfo;
		iProject = project;
		iJavaProject = JavaCore.create(project);
	}
	
	public ApplicationInfo getApplicationInfo() {
		return iApplicationInfo;
	}
	
	public IProject getProject() {
		return iProject;
	}
	
	public IJavaProject getJavaProject() {
		return iJavaProject;
	}
	
	public void addEntityReferencingType(String entityReferencingType) {
		synchronized (iEntityReferencingTypes) {
			iEntityReferencingTypes.add(entityReferencingType);
		}
	}
	
	public Set<String> getEntityReferencingTypes() {
		return iEntityReferencingTypes;
	}

	public void addEntityReferenceSubclass(String entityReferenceSubclass) {
		synchronized (iEntityReferenceSubclasses) {
			iEntityReferenceSubclasses.add(entityReferenceSubclass);
		}
	}
	
	public Set<String> getEntityReferenceSubclasses() {
		return iEntityReferenceSubclasses;
	}
	
	public void addIndirectEntityReferencingType(String indirectEntityReferencingType) {
		synchronized (iIndirectEntityReferencingTypes) {
			iIndirectEntityReferencingTypes.add(indirectEntityReferencingType);
		}
	}
	
	public Set<String> getIndirectEntityReferencingTypes() {
		return iIndirectEntityReferencingTypes;
	}
	
	public void addDeleteIntendedType(String deleteIntendedType) {
		synchronized (iIndirectEntityReferencingTypes) {
			iDeleteIntendedTypes.add(deleteIntendedType);
		}
	}
	
	public Collection<String> getDeleteIntendedTypes() {
		return iDeleteIntendedTypes;
	}
	
	public AccessBeanSubclassInfo getAccessBeanSubclassInfo(String accessBeanSubclass, boolean create) {
		synchronized(iAccessBeanSubclasses) {
			AccessBeanSubclassInfo accessBeanSubclassInfo = iAccessBeanSubclasses.get(accessBeanSubclass);
			if (accessBeanSubclassInfo == null && create) {
				accessBeanSubclassInfo = new AccessBeanSubclassInfo(this, accessBeanSubclass);
				iAccessBeanSubclasses.put(accessBeanSubclass, accessBeanSubclassInfo);
				iApplicationInfo.addAccessBeanSubclass(accessBeanSubclass);
				iApplicationInfo.setAccessBeanSubclassInfoForType(accessBeanSubclass, accessBeanSubclassInfo);
			}
			return accessBeanSubclassInfo;
		}
	}
	
	public Collection<AccessBeanSubclassInfo> getAccessBeanSubclasses() {
		return iAccessBeanSubclasses.values();
	}
}
