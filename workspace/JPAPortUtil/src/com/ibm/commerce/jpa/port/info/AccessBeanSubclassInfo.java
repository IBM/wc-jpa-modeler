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
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.ibm.commerce.jpa.port.util.JavaUtil;

public class AccessBeanSubclassInfo {
	private ProjectInfo iProjectInfo;
	private String iName;
	private ApplicationInfo iApplicationInfo;
	private IType iType;
	private EntityInfo iEntityInfo;
	private AccessBeanSubclassInfo iSuperclass;
	private Set<AccessBeanSubclassInfo> iSubclasses = new HashSet<AccessBeanSubclassInfo>();
	private Collection<String> iMethodKeys = new HashSet<String>();
	private Map<String, MethodDeclaration> iMethodDeclarations = new HashMap<String, MethodDeclaration>();
	private Map<String, TargetExceptionInfo> iMethodUnhandledTargetExceptions = new HashMap<String, TargetExceptionInfo>();
	
	public AccessBeanSubclassInfo(ProjectInfo projectInfo, String name) {
		iProjectInfo = projectInfo;
		iApplicationInfo = projectInfo.getApplicationInfo();
		iName = name;
	}
	
	public ProjectInfo getProjectInfo() {
		return iProjectInfo;
	}
	
	public String getName() {
		return iName;
	}
	
	public IType getType() {
		if (iType == null) {
			try {
				iType = iProjectInfo.getJavaProject().findType(iName);
				if (iType == null) {
					System.out.println("unable to find type: "+iName+" in "+iProjectInfo.getProject().getName());
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iType;
	}
	
	public EntityInfo getEntityInfo() {
		if (iEntityInfo == null) {
			IType type = getType();
			try {
				while (type != null && type.getSuperclassName() != null) {
					type = JavaUtil.resolveType(type, type.getSuperclassName());
					EntityInfo entityInfo = iApplicationInfo.getEntityInfoForType(type.getFullyQualifiedName('.'));
					if (entityInfo != null) {
						iEntityInfo = entityInfo;
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEntityInfo;
	}
	
	public void setSuperclass(AccessBeanSubclassInfo superclass) {
		iSuperclass = superclass;
	}
	
	public AccessBeanSubclassInfo getSuperclass() {
		return iSuperclass;
	}
	
	public void addSubclass(AccessBeanSubclassInfo subclass) {
		iSubclasses.add(subclass);
	}
	
	public Set<AccessBeanSubclassInfo> getSubclasses() {
		return iSubclasses;
	}
	
	public void setMethodDeclaration(String methodKey, MethodDeclaration methodDeclaration) {
		iMethodKeys.add(methodKey);
		iMethodDeclarations.put(methodKey, methodDeclaration);
	}
	
	public MethodDeclaration getMethodDeclaration(String methodKey) {
		return iMethodDeclarations.get(methodKey);
	}
	
	public void releaseMethodDeclaration(String methodKey) {
		iMethodDeclarations.remove(methodKey);
	}
	
	public void setMethodUnhandledTargetExceptions(String methodKey, TargetExceptionInfo unhandledTargetExceptions) {
		iMethodKeys.add(methodKey);
		iMethodUnhandledTargetExceptions.put(methodKey, unhandledTargetExceptions);
	}
	
	public TargetExceptionInfo getMethodUnhandledTargetExceptions(String methodKey) {
		return iMethodUnhandledTargetExceptions.get(methodKey);
	}
	
	public Collection<String> getMethodKeys() {
		return iMethodKeys;
	}
	
	public boolean hasMethod(String methodKey) {
		return iMethodKeys.contains(methodKey);
	}
}
