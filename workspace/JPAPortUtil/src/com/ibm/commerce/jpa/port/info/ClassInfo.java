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

import org.eclipse.jdt.core.IPackageFragment;

public class ClassInfo {
	private String iClassName;
	private IPackageFragment iPackageFragment;
	private String iSuperclassName;
	private String iSuperclassPackage;
	
	public ClassInfo(String className, IPackageFragment packageFragment) {
		iClassName = className;
		iPackageFragment = packageFragment;
	}
	
	public String getClassName() {
		return iClassName;
	}
	
	public IPackageFragment getPackageFragment() {
		return iPackageFragment;
	}
	
	public String getQualifiedClassName() {
		return iPackageFragment.getElementName() + "." + iClassName;
	}
	
	public void setSuperclassName(String superclassName) {
		iSuperclassName = superclassName;
	}
	
	public String getSuperclassName() {
		return iSuperclassName;
	}
	
	public void setSuperclassPackage(String superclassPackage) {
		iSuperclassPackage = superclassPackage;
	}
	
	public String getSuperclassPackage() {
		return iSuperclassPackage;
	}
	
	public String getQualifiedSuperclassName() {
		String qualifiedName = null;
		if (iSuperclassName != null && iSuperclassPackage != null) {
			qualifiedName = iSuperclassPackage + "." + iSuperclassName;
		}
		return qualifiedName;
	}
}
