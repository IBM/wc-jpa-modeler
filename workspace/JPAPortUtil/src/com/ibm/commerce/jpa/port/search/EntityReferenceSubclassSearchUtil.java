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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class EntityReferenceSubclassSearchUtil {
	private ProjectInfo iProjectInfo;
	private ApplicationInfo iApplicationInfo;
	private IJavaProject iJavaProject;
	
	public EntityReferenceSubclassSearchUtil(ProjectInfo projectInfo) {
		iApplicationInfo = projectInfo.getApplicationInfo();
		iProjectInfo = projectInfo;
		iJavaProject = projectInfo.getJavaProject();
	}
	
	public void search(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("search for entity reference subclasses "+iProjectInfo.getProject().getName(), (iProjectInfo.getEntityReferencingTypes().size() * 1000));
			long start = new java.util.Date().getTime();
			System.out.println("searching for entity reference subclasses "+iProjectInfo.getProject().getName());
			Collection<String> entityReferences = new HashSet<String>(); 
			entityReferences.addAll(iProjectInfo.getEntityReferencingTypes());
			entityReferences.addAll(iProjectInfo.getIndirectEntityReferencingTypes());
			for (String entityReference : entityReferences) {
				IType type = iJavaProject.findType(entityReference);
				if (type.isClass()) {
					boolean checkSubclasses = true;
					IType currentType = type;
					while (currentType.getSuperclassName() != null) {
						currentType = JavaUtil.resolveType(currentType, currentType.getSuperclassName());
						if (!currentType.isBinary()) {
							ProjectInfo projectInfo = iApplicationInfo.getProjectInfo(currentType.getJavaProject().getProject());
							String qualifiedName = currentType.getFullyQualifiedName('.');
							if (projectInfo.getEntityReferencingTypes().contains(qualifiedName) || projectInfo.getIndirectEntityReferencingTypes().contains(qualifiedName)) {
								checkSubclasses = false;
								break;
							}
						}
						else {
							break;
						}
					}
					if (checkSubclasses) {
						ITypeHierarchy typeHierarchy = type.newTypeHierarchy(new SubProgressMonitor(progressMonitor, 1000));
						IType subtypes[] = typeHierarchy.getAllSubtypes(type);
						if (subtypes != null) {
							for (IType subtype : subtypes) {
								String qualifiedTypeName = subtype.getFullyQualifiedName('.');
								if (!iApplicationInfo.isDeleteIntendedType(qualifiedTypeName) && !qualifiedTypeName.contains("JPA")) {
									if (subtype.isBinary()) {
										System.out.println("found binary entity reference subclass: "+subtype.getFullyQualifiedName('.'));
										iApplicationInfo.incrementSearchResultCount();
									}
									else {
										while (subtype.getDeclaringType() != null) {
											subtype = subtype.getDeclaringType();
										}
										String subtypeName = subtype.getFullyQualifiedName('.');
										ProjectInfo projectInfo = iApplicationInfo.getProjectInfo(subtype.getJavaProject().getProject());
										if (!projectInfo.getEntityReferencingTypes().contains(subtypeName) && !projectInfo.getIndirectEntityReferencingTypes().contains(subtypeName)) {
											projectInfo.addEntityReferenceSubclass(subtypeName);
											iApplicationInfo.incrementSearchResultCount();
										}
									}
								}
							}
						}
					}
				}
				else if (type.isInterface()) {
					boolean checkSubinterfaces = true;
					Collection<IType> types = new HashSet<IType>();
					types.add(type);
					while (!types.isEmpty() && checkSubinterfaces) {
						Collection<IType> newTypes = new HashSet<IType>();
						for (IType currentType : types) {
							String[] superInterfaceNames = currentType.getSuperInterfaceNames();
							for (String superInterfaceName : superInterfaceNames) {
								IType superInterfaceType = JavaUtil.resolveType(currentType, superInterfaceName);
								if (!superInterfaceType.isBinary()) {
									newTypes.add(superInterfaceType);
									ProjectInfo projectInfo = iApplicationInfo.getProjectInfo(superInterfaceType.getJavaProject().getProject());
									String qualifiedName = superInterfaceType.getFullyQualifiedName('.');
									if (projectInfo.getEntityReferencingTypes().contains(qualifiedName) || projectInfo.getIndirectEntityReferencingTypes().contains(qualifiedName)) {
										checkSubinterfaces = false;
										break;
									}
								}
							}
							if (!checkSubinterfaces) {
								break;
							}
						}
						types = newTypes;
					}
					if (checkSubinterfaces) {
						ITypeHierarchy typeHierarchy = type.newTypeHierarchy(new SubProgressMonitor(progressMonitor, 1000));
						IType subtypes[] = typeHierarchy.getAllSubtypes(type);
						if (subtypes != null) {
							for (IType subtype : subtypes) {
								if (!iApplicationInfo.isDeleteIntendedType(subtype.getFullyQualifiedName('.'))) {
									if (subtype.isInterface()) {
										if (subtype.isBinary()) {
											System.out.println("found binary entity reference subinterface: "+subtype.getFullyQualifiedName('.'));
											iApplicationInfo.incrementSearchResultCount();
										}
										else {
											while (subtype.getDeclaringType() != null) {
												subtype = subtype.getDeclaringType();
											}
											String subtypeName = subtype.getFullyQualifiedName('.');
											ProjectInfo projectInfo = iApplicationInfo.getProjectInfo(subtype.getJavaProject().getProject());
											if (!projectInfo.getEntityReferencingTypes().contains(subtypeName) && !projectInfo.getIndirectEntityReferencingTypes().contains(subtypeName)) {
												projectInfo.addEntityReferenceSubclass(subtypeName);
												iApplicationInfo.incrementSearchResultCount();
											}
										}
									}
								}
							}
						}
					}
				}
			}
			System.out.println("end search for entity reference subclasses "+iProjectInfo.getProject().getName()+" "+((new java.util.Date().getTime() - start)/1000)+" seconds");
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
}
