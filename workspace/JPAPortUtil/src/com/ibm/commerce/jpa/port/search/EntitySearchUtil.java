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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;

public class EntitySearchUtil {
	private Collection<EntityInfo> iEntities;
	private ApplicationInfo iApplicationInfo;
	private Collection<String> iBinarySearchResults;
	private Collection<IType> iTypeSearchResults;
	
	public EntitySearchUtil(ApplicationInfo applicationInfo, Collection<EntityInfo> entities, Collection<String> binarySearchResults, Collection<IType> typeSearchResults) {
		iApplicationInfo = applicationInfo;
		iEntities = entities;
		iBinarySearchResults = binarySearchResults;
		iTypeSearchResults = typeSearchResults;
	}
	
	public void search(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("search for "+iEntities.size()+" entities", (iEntities.size() * 1000) + 1000);
			long start = new java.util.Date().getTime();
			System.out.println("searching for "+iEntities.size()+" entities");
			SearchPattern searchPattern = null;
			for (EntityInfo entityInfo : iEntities) {
				EntitySearchPatternUtil entitySearchPatternUtil = new EntitySearchPatternUtil(entityInfo, iBinarySearchResults);
				SearchPattern entitySearchPattern = entitySearchPatternUtil.createEntitySearchPattern(new SubProgressMonitor(progressMonitor, 1000));
				if (searchPattern == null) {
					searchPattern = entitySearchPattern;
				}
				else {
					searchPattern = SearchPattern.createOrPattern(searchPattern, entitySearchPattern);
				}
			}
			SearchEngine searchEngine = new SearchEngine();
			Collection<IJavaProject> javaProjects = new HashSet<IJavaProject>();
			IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				//System.out.println(project.getName());
				IJavaProject javaProject = JavaCore.create(project);
				if (javaProject != null && javaProject.exists()) {
					javaProjects.add(javaProject);
				}
			}
			IJavaElement[] javaElements = javaProjects.toArray(new IJavaElement[javaProjects.size()]);
			//IProject wcProject = ResourcesPlugin.getWorkspace().getRoot().getProject("WC");
			//IJavaProject wcJavaProject = JavaCore.create(wcProject);
			//String[] referencedProjectNames = wcJavaProject.getRequiredProjectNames();
			searchEngine.search(searchPattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, SearchEngine.createJavaSearchScope(javaElements, IJavaSearchScope.SOURCES), new TypeSearchRequestor(), new SubProgressMonitor(progressMonitor, 1000));
			System.out.println("end search for "+iEntities.size()+" entities "+((new java.util.Date().getTime() - start)/1000)+" seconds");
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		catch (NullPointerException e) {
			System.out.println("null pointer exception");
			for (EntityInfo entityInfo : iEntities) {
				System.out.println(entityInfo.getEjbName());
			}
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private class TypeSearchRequestor extends SearchRequestor {
		public void acceptSearchMatch(SearchMatch searchMatch) throws CoreException {
			if (searchMatch.getAccuracy() == SearchMatch.A_ACCURATE && searchMatch.isExact()) {
				IJavaElement javaElement = (IJavaElement) searchMatch.getElement();
				if (javaElement instanceof IMember) {
					IMember member = (IMember) javaElement;
					IType type = member.getDeclaringType();
					if (type == null && member.getElementType() == IJavaElement.TYPE) {
						type = (IType) member;
					}
					if (type != null) {
						while (type.getDeclaringType() != null) {
							type = type.getDeclaringType();
						}
						if (!iApplicationInfo.isDeleteIntendedType(type.getFullyQualifiedName('.'))) {
							if (type.isBinary()) {
								synchronized (iBinarySearchResults) {
									if (!iBinarySearchResults.contains(searchMatch.getResource().getName())) {
										System.out.println("found binary search result: "+searchMatch.getResource().getName());
										iBinarySearchResults.add(searchMatch.getResource().getName());
										iApplicationInfo.incrementSearchResultCount();
									}
								}
							}
							else {
								synchronized (iTypeSearchResults) {
									if (!iTypeSearchResults.contains(type)) {
										//System.out.println("found type search result: "+type.getFullyQualifiedName());
										iTypeSearchResults.add(type);
										iApplicationInfo.incrementSearchResultCount();
									}
								}
							}
						}
					}
					else {
						System.out.println("no type found "+javaElement);
					}
				}
				else if (javaElement.getElementType() == IJavaElement.IMPORT_DECLARATION) {
					ICompilationUnit compilationUnit = (ICompilationUnit) javaElement.getParent().getParent();
					IType type = compilationUnit.getTypes()[0];
					if (!iApplicationInfo.isDeleteIntendedType(type.getFullyQualifiedName('.'))) {
						synchronized (iTypeSearchResults) {
							if (!iTypeSearchResults.contains(type)) {
								//System.out.println("found type search result: "+type.getFullyQualifiedName());
								iTypeSearchResults.add(type);
								iApplicationInfo.incrementSearchResultCount();
							}
						}
					}
				}
				else {
					System.out.println("unexpected search result "+javaElement);
				}
			}
		}
	}
}
