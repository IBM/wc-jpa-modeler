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
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class EntitySearchPatternUtil {
	private static final Collection<String> EXEMPT_INTERFACES;
	static {
		EXEMPT_INTERFACES = new HashSet<String>();
		EXEMPT_INTERFACES.add("com.ibm.commerce.security.Protectable");
		EXEMPT_INTERFACES.add("com.ibm.commerce.security.Delegator");
		EXEMPT_INTERFACES.add("com.ibm.commerce.grouping.Groupable");
	}
	
	// ARVERA: Change to contain the efollett pckage
	private static final String COMMERCE_PACKAGE = "com.*";
	private EntityInfo iEntityInfo;
	private ApplicationInfo iApplicationInfo;
	private Collection<String> iBinarySearchResults;
	
	public EntitySearchPatternUtil(EntityInfo entityInfo, Collection<String> binarySearchResults) {
		iEntityInfo = entityInfo;
		iApplicationInfo = entityInfo.getModuleInfo().getApplicationInfo();
		iBinarySearchResults = binarySearchResults;
	}
	
	public SearchPattern createEntitySearchPattern(IProgressMonitor progressMonitor) {
		SearchPattern searchPattern = null;
		try {
			progressMonitor.beginTask("create search pattern for "+iEntityInfo.getEjbName(), 1000);
			if (iEntityInfo.getEjbAccessBeanType() != null) {
				searchPattern = SearchPattern.createPattern(iEntityInfo.getEjbAccessBeanType(), IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				SearchPattern indirectReferencesSearchPattern = getIndirectReferencesSearchPattern(iEntityInfo.getEjbAccessBeanType());
				if (indirectReferencesSearchPattern != null) {
					searchPattern = SearchPattern.createOrPattern(searchPattern, indirectReferencesSearchPattern);
				}
				ITypeHierarchy typeHierarchy = iEntityInfo.getEjbAccessBeanType().newTypeHierarchy(new SubProgressMonitor(progressMonitor, 1000));
				IType subtypes[] = typeHierarchy.getAllSubtypes(iEntityInfo.getEjbAccessBeanType());
				if (subtypes != null) {
					for (IType subtype : subtypes) {
						if (subtype.isBinary()) {
							synchronized (iBinarySearchResults) {
								if (!iBinarySearchResults.contains(subtype.getResource().getName())) {
									System.out.println("found binary search result: "+subtype.getResource().getName());
									iBinarySearchResults.add(subtype.getResource().getName());
									iApplicationInfo.incrementSearchResultCount();
								}
							}
						}
						else {
							String subtypeName = subtype.getFullyQualifiedName('.');
							iApplicationInfo.getProjectInfo(subtype.getResource().getProject()).getAccessBeanSubclassInfo(subtypeName, true);
							iApplicationInfo.incrementSearchResultCount();
						}
						searchPattern = SearchPattern.createOrPattern(searchPattern, SearchPattern.createPattern(subtype, IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE));
						indirectReferencesSearchPattern = getIndirectReferencesSearchPattern(subtype);
						if (indirectReferencesSearchPattern != null) {
							searchPattern = SearchPattern.createOrPattern(searchPattern, indirectReferencesSearchPattern);
						}
					}
				}
			}
			if (iEntityInfo.getHomeType() != null) {
				SearchPattern homeTypeSearchPattern = SearchPattern.createPattern(iEntityInfo.getHomeType(), IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				if (searchPattern == null) {
					searchPattern = homeTypeSearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, homeTypeSearchPattern);
				}
			}
			if (iEntityInfo.getLocalHomeType() != null) {
				SearchPattern localHomeTypeSearchPattern = SearchPattern.createPattern(iEntityInfo.getLocalHomeType(), IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				if (searchPattern == null) {
					searchPattern = localHomeTypeSearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, localHomeTypeSearchPattern);
				}
			}
			if (iEntityInfo.getPrimaryKeyType() != null && !iEntityInfo.getPrimaryKeyType().isBinary()) {
				SearchPattern primaryKeyTypeSearchPattern = SearchPattern.createPattern(iEntityInfo.getPrimaryKeyType(), IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				if (searchPattern == null) {
					searchPattern = primaryKeyTypeSearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, primaryKeyTypeSearchPattern);
				}
			}
			if (iEntityInfo.getRemoteType() != null) {
				SearchPattern remoteTypeSearchPattern = SearchPattern.createPattern(iEntityInfo.getRemoteType(), IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				if (searchPattern == null) {
					searchPattern = remoteTypeSearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, remoteTypeSearchPattern);
				}
			}
			if (iEntityInfo.getLocalType() != null) {
				SearchPattern localTypeSearchPattern = SearchPattern.createPattern(iEntityInfo.getLocalType(), IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				if (searchPattern == null) {
					searchPattern = localTypeSearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, localTypeSearchPattern);
				}
			}
			if (iEntityInfo.getFactoryType() != null) {
				SearchPattern factoryTypeSearchPattern = SearchPattern.createPattern(iEntityInfo.getFactoryType(), IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				if (searchPattern == null) {
					searchPattern = factoryTypeSearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, factoryTypeSearchPattern);
				}
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
		return searchPattern;
	}
	
	private Collection<IType> getExemptSuperInterfaces(IType type) throws CoreException {
		Collection<IType> exemptSuperInterfaces = new HashSet<IType>();
		String[] superInterfaceNames = type.getSuperInterfaceNames();
		if (superInterfaceNames != null) {
			for (String superInterfaceName : superInterfaceNames) {
				IType superInterfaceType = JavaUtil.resolveType(type, superInterfaceName);
				if (superInterfaceType != null) {
					String name = superInterfaceType.getFullyQualifiedName('.');
					if (EXEMPT_INTERFACES.contains(name) || !name.startsWith(COMMERCE_PACKAGE)) {
						exemptSuperInterfaces.add(superInterfaceType);
					}
					exemptSuperInterfaces.addAll(getExemptSuperInterfaces(superInterfaceType));
				}
			}
		}
		return exemptSuperInterfaces;
	}
	
	private SearchPattern getIndirectReferencesSearchPattern(IType type) throws CoreException {
		Collection<IType> superTypes = new HashSet<IType>();
		superTypes.addAll(getExemptSuperInterfaces(type));
		String superclassName = type.getSuperclassName();
		IType currentType = type;
		while (superclassName != null) {
			IType superType = JavaUtil.resolveType(currentType, superclassName);
			if (superType != null) {
				superTypes.add(superType);
				superTypes.addAll(getExemptSuperInterfaces(superType));
				superclassName = superType.getSuperclassName();
				currentType = superType;
			}
			else {
				System.out.println("could not resolve superclassName "+superclassName);
				break;
			}
		}
		SearchPattern searchPattern = null;
		IField[] fields = type.getFields();
		for (IField field : fields) {
			if (!Flags.isPrivate(field.getFlags())) {
				SearchPattern fieldSearchPattern = SearchPattern.createPattern(field, IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				if (searchPattern == null) {
					searchPattern = fieldSearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, fieldSearchPattern);
				}
			}
		}
		IMethod[] methods = type.getMethods();
		for (IMethod method : methods) {
			if (!Flags.isPrivate(method.getFlags())) {
				boolean extendedMethod = false;
				if (!method.isConstructor()) {
					for (IType superType : superTypes) {
						IMethod[] superMethods = superType.findMethods(method);
						if (superMethods != null && superMethods.length > 0) {
							extendedMethod = true;
							break;
						}
					}
				}
				if (!extendedMethod) {
					SearchPattern methodSearchPattern = SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
					if (methodSearchPattern != null) {
						if (searchPattern == null) {
							searchPattern = methodSearchPattern;
						}
						else {
							searchPattern = SearchPattern.createOrPattern(searchPattern, methodSearchPattern);
						}
					}
				}
			}
		}
		return searchPattern;
	}
}
