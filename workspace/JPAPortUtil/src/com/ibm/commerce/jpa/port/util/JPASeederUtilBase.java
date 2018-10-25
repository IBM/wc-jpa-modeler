package com.ibm.commerce.jpa.port.util;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.ibm.commerce.jpa.port.Activator;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;

public class JPASeederUtilBase {
	private ApplicationInfo iApplicationInfo;
	private Collection<IProject> iBuildPendingProjects;
	private String iSourceDirectoryName;
	private boolean iMarkAsGenerated;
	private Activator iPlugin;
	
	public JPASeederUtilBase(ApplicationInfo applicationInfo, Collection<IProject> buildPendingProjects, String sourceDirectoryName, boolean markAsGenerated) {
		iApplicationInfo = applicationInfo;
		iBuildPendingProjects = buildPendingProjects;
		iSourceDirectoryName = sourceDirectoryName;
		iMarkAsGenerated = markAsGenerated;
		iPlugin = Activator.getDefault();
	}
	
	public void seedNewClasses(IProgressMonitor progressMonitor) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			File pluginLocation = FileLocator.getBundleFile(iPlugin.getBundle());
			if (pluginLocation.isDirectory()) {
				File projectsDirectory = new File(pluginLocation, iSourceDirectoryName);
				File[] projects = projectsDirectory.listFiles();
				if (projects != null) {
					for (File projectSourceDirectory : projects) {
						IProject project = root.getProject(projectSourceDirectory.getName());
						if (project != null) {
							seedProject(project, projectSourceDirectory, progressMonitor);
							iBuildPendingProjects.add(project);
						}
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private void seedProject(IProject project, File sourceDirectory, IProgressMonitor progressMonitor) throws CoreException {
		File[] files = sourceDirectory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				IFolder folder = project.getFolder(file.getName());
				if (folder.exists()) {
					seedFolder(project, folder, file, progressMonitor);
				}
			}
		}
	}
	
	private void seedFolder(IProject project, IFolder targetFolder, File sourceDirectory, IProgressMonitor progressMonitor) throws CoreException {
		File[] files = sourceDirectory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				IFolder folder = targetFolder.getFolder(file.getName());
				if (!folder.exists()) {
					folder.create(true, true, new SubProgressMonitor(progressMonitor, 100));
				}
				seedFolder(project, folder, file, progressMonitor);
			}
			else {
				seedFile(project, targetFolder, file, progressMonitor);
			}
		}
	}
	
	private void seedFile(IProject project, IFolder targetFolder, File sourceFile, IProgressMonitor progressMonitor) throws CoreException {
		try {
			IFile file = targetFolder.getFile(sourceFile.getName());
			FileInputStream inputStream = new FileInputStream(sourceFile);
			if (file.exists()) {
				if (!iMarkAsGenerated) {
					saveGeneratedFile(project, file, new SubProgressMonitor(progressMonitor, 100));
				}
				file.setContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 100));
			}
			else {
				file.create(inputStream, true, new SubProgressMonitor(progressMonitor, 100));
			}
			if (iMarkAsGenerated) {
				iApplicationInfo.getBackupUtil(project).addGeneratedFile(file, new SubProgressMonitor(progressMonitor, 100));
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void saveGeneratedFile(IProject project, IFile generatedFile, IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("backup2 " + generatedFile.getName(), 200);
			IFolder generatedFilesBackupFolder = project.getFolder(".jpaGeneratedFilesBackupFolder");
			if (!generatedFilesBackupFolder.exists()) {
				generatedFilesBackupFolder.create(true, true, new SubProgressMonitor(progressMonitor, 100));
			}
			IFile backupFile = generatedFilesBackupFolder.getFile(generatedFile.getProjectRelativePath());
			if (!backupFile.exists()) {
				IFolder parentFolder = (IFolder) backupFile.getParent();
				createFolder(parentFolder, progressMonitor);
				backupFile.create(generatedFile.getContents(), true, new SubProgressMonitor(progressMonitor, 100));
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void createFolder(IFolder folder, IProgressMonitor progressMonitor) throws CoreException {
		if (!folder.exists()) {
			IFolder parentFolder = (IFolder) folder.getParent();
			createFolder(parentFolder, progressMonitor);
			folder.create(true, true, new SubProgressMonitor(progressMonitor, 100));
		}
	}
}
