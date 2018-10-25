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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.updaters.GeneratedTypeUpdater;

public class EntityReferencingProjectGenerator {
//	private static final String EJB_DEPLOY = "ejbdeploy";
//	private static final String WEBSPHERE_DEPLOY = "websphere_deploy";
	
	private ASTParser iASTParser = ASTParser.newParser(AST.JLS3);
	private ApplicationInfo iApplicationInfo;
	private ProjectInfo iProjectInfo;
	
	public EntityReferencingProjectGenerator(ApplicationInfo applicationInfo, ProjectInfo projectInfo) {
		iApplicationInfo = applicationInfo;
		iProjectInfo = projectInfo;
	}
	
	public boolean generate(IProgressMonitor progressMonitor) {
		boolean referencesGenerated = false;
		System.out.println("generating entity referencing types " + iProjectInfo.getProject().getName());
		try {
			progressMonitor.beginTask("Generate " + iProjectInfo.getProject().getName(), IProgressMonitor.UNKNOWN);
			IFile generatedFileList = iProjectInfo.getProject().getFile(".jpaGeneratedFileList");
			if (generatedFileList.exists()) {
				Collection<IType> generatedTypes = new HashSet<IType>();
				try {
					InputStream inputStream = generatedFileList.getContents(true);
					try {
						InputStreamReader reader = new InputStreamReader(inputStream);
						BufferedReader bufferedReader = new BufferedReader(reader);
						String portableString = bufferedReader.readLine();
						while (portableString != null) {
							IPath path = Path.fromPortableString(portableString);
							IFile generatedFile = iProjectInfo.getProject().getFile(path);
							if (generatedFile.getName().endsWith(".java")) {
								IJavaElement javaElement = JavaCore.create(generatedFile);
								generatedTypes.add(((ICompilationUnit) javaElement).getTypes()[0]);
							}
							portableString = bufferedReader.readLine();
						}
						iApplicationInfo.incrementParsedAssetCount();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					finally {
						try {
							inputStream.close();
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				catch (CoreException e) {
					e.printStackTrace();
				}
				for (IType generatedType : generatedTypes) {
					GeneratedTypeUpdater generatedTypeUpdater = new GeneratedTypeUpdater(iASTParser, iApplicationInfo, generatedType);
					generatedTypeUpdater.update(new SubProgressMonitor(progressMonitor, 200));
					referencesGenerated = true;
				}
			}
			
//			Collection<AccessBeanSubclassInfo> accessBeanSubclasses = iProjectInfo.getAccessBeanSubclasses();
//			for (AccessBeanSubclassInfo accessBeanSubclassInfo : accessBeanSubclasses) {
//				EntityAccessBeanSubclassGenerator entityAccessBeanSubclassGenerator = new EntityAccessBeanSubclassGenerator(iASTParser, iApplicationInfo, accessBeanSubclassInfo);
//				entityAccessBeanSubclassGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
//			}
			Set<String> entityReferencingTypes = new HashSet<String>();
			entityReferencingTypes.addAll(iProjectInfo.getEntityReferencingTypes());
			entityReferencingTypes.addAll(iProjectInfo.getIndirectEntityReferencingTypes());
			for (String entityReferencingType : entityReferencingTypes) {
				IType type = iProjectInfo.getJavaProject().findType(entityReferencingType);
				if (type == null) {
					System.out.println("type not found "+entityReferencingType);
				}
				EntityReferencingTypeGenerator entityReferencingTypeGenerator = new EntityReferencingTypeGenerator(iASTParser, iApplicationInfo, type);
				entityReferencingTypeGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
				referencesGenerated = true;
			}
			
			
//			IClasspathEntry[] classpathEntries = iJavaProject.getResolvedClasspath(true);
//			for (IClasspathEntry entry : classpathEntries) {
//				if (progressMonitor.isCanceled()) {
//					status = Status.CANCEL_STATUS;
//					break;
//				}
//				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
//					IPath path = entry.getPath();
//					System.out.println("path="+path);
//					IFolder folder = iWorkspace.getRoot().getFolder(path);
//					System.out.println("folder="+folder);
//					generateEntityReferencingResources(progressMonitor, folder.members());
//				}
//			}
			
			// getPackageFragmentRoots isn't what I want - it returns all of the package fragment roots on the classpath
//			IPackageFragmentRoot[] packageFragmentRoots = iJavaProject.getPackageFragmentRoots();
//			for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
//				if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
//					// getChildren doesn't do what I want either - it returns all of the package fragments - but it doesn't seem to know how to return the classes in those fragments
//					generateEntityReferencingJavaElements(progressMonitor, packageFragmentRoot.getChildren()); 
//					if (progressMonitor.isCanceled()) {
//						status = Status.CANCEL_STATUS;
//						break;
//					}
//				}
//			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
//		catch (CoreException e) {
//			e.printStackTrace();
//		}
		finally {
			progressMonitor.done();
		}
		return referencesGenerated;
	}
	
//	private void generateEntityReferencingResources(IProgressMonitor progressMonitor, IResource[] resources) throws CoreException, JavaModelException {
//		for (IResource resource : resources) {
//			switch (resource.getType()) {
//				case IResource.FILE: {
//					IFile file = (IFile) resource;
//					if (file.getName().endsWith(".java")) {
//						IJavaElement javaElement = JavaCore.create(file);
//						generateEntityReferencingType(progressMonitor, (ICompilationUnit) javaElement);
//					}
//					break;
//				}
//				case IResource.FOLDER: {
//					IFolder folder = (IFolder) resource;
//					if (!EJB_DEPLOY.equals(folder.getName()) && !WEBSPHERE_DEPLOY.equals(folder.getName())) {
//						generateEntityReferencingResources(progressMonitor, folder.members());
//					}
//					break;
//				}
//			}
//		}
//	}
//	
//	private void generateEntityReferencingType(IProgressMonitor progressMonitor, ICompilationUnit compilationUnit) throws JavaModelException {
//		IType[] types = compilationUnit.getAllTypes();
//		if (types != null && types.length > 0) {
//			IType type = types[0];
//			String typeName = type.getFullyQualifiedName();
//			if (iApplicationInfo.getTypeMapping(typeName) == null &&
//				!iApplicationInfo.isDeleteIntendedType(typeName) &&
//				(iModuleInfo == null || !iModuleInfo.getGeneratedTypes().contains(typeName)) &&
//				!FinderResultCacheUtil.isFinderResultCacheUtil(type) &&
//				!AccessBeanUtil.isCachedEntityAccessBean(type)) {
//				EntityReferencingTypeGenerator entityReferencingTypeGenerator = new EntityReferencingTypeGenerator(iASTParser, iApplicationInfo, type);
//				entityReferencingTypeGenerator.generate(new SubProgressMonitor(progressMonitor, 200));
//			}
//		}
//	}
}
