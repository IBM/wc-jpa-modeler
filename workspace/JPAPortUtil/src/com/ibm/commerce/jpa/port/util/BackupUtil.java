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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

public class BackupUtil {
//	private static final String NULL_BACKUP = "NULL_BACKUP";
	private IProject iProject;
	private IFolder iBackupFolder2;
	private IFolder iBackupFolder3;
//	private IFolder iNullBackupFolder;
	private IFile iGeneratedFileListFile;
	private IFile iGeneratedFileList2File;
	private IFile iGeneratedFileList3File;
	
	public BackupUtil(IProject project) {
		iProject = project;
		iBackupFolder2 = iProject.getFolder(".jpaBackup2");
		iBackupFolder3 = iProject.getFolder(".jpaBackup3");
//		iNullBackupFolder = iProject.getFolder(".jpaNullBackup");
		iGeneratedFileListFile = iProject.getFile(".jpaGeneratedFileList");
		iGeneratedFileList2File = iProject.getFile(".jpaGeneratedFileList2");
		iGeneratedFileList3File = iProject.getFile(".jpaGeneratedFileList3");
	}
	
	public void backupFile2(IFile file, IProgressMonitor progressMonitor) throws CoreException {
		try {
			progressMonitor.beginTask("backup2 " + file.getName(), 200);
			if (!iBackupFolder2.exists()) {
				iBackupFolder2.create(true, true, new SubProgressMonitor(progressMonitor, 100));
			}
			IFile backupFile = iBackupFolder2.getFile(file.getProjectRelativePath());
			if (!backupFile.exists()) {
				IFolder parentFolder = (IFolder) backupFile.getParent();
				createFolder(parentFolder, progressMonitor);
				backupFile.create(file.getContents(), true, new SubProgressMonitor(progressMonitor, 100));
			}
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
	
	public void backupFile3(IFile file, IProgressMonitor progressMonitor) throws CoreException {
		if (file.toString().contains("ProcessPromotionActivateActionCmdImpl")) {
			System.out.println("ProcessPromotionActivateActionCmdImpl");
		}
		try {
			progressMonitor.beginTask("backup " + file.getName(), 200);
//			if (file.exists()) {
				if (!iBackupFolder3.exists()) {
					iBackupFolder3.create(true, true, new SubProgressMonitor(progressMonitor, 100));
				}
				IFile backupFile = iBackupFolder3.getFile(file.getProjectRelativePath());
				if (!backupFile.exists()) {
					IFolder parentFolder = (IFolder) backupFile.getParent();
					createFolder(parentFolder, progressMonitor);
					backupFile.create(file.getContents(), true, new SubProgressMonitor(progressMonitor, 100));
				}
//			}
//			else {
//				if (!iNullBackupFolder.exists()) {
//					iNullBackupFolder.create(true, true, new SubProgressMonitor(progressMonitor, 100));
//				}
//				IFile backupFile = iNullBackupFolder.getFile(file.getProjectRelativePath());
//				if (!backupFile.exists()) {
//					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(NULL_BACKUP.getBytes());
//					backupFile.create(byteArrayInputStream, true, new SubProgressMonitor(progressMonitor, 100));
//				}
//			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
	public void backupFolder3(IFolder folder, IProgressMonitor progressMonitor) throws CoreException {
		IResource[] members = folder.members();
		if (members != null) {
			try {
				progressMonitor.beginTask("backup " + folder.getName(), members.length * 100);
				for (IResource member : members) {
					if (member.getType() == IResource.FILE) {
						backupFile3((IFile) member, new SubProgressMonitor(progressMonitor, 100));
					}
					else if (member.getType() == IResource.FOLDER) {
						backupFolder3((IFolder) member, new SubProgressMonitor(progressMonitor, 100));
					}
				}
			}
			finally {
				progressMonitor.done();
			}
		}
	}
	
	public void addGeneratedFile(IFile file, IProgressMonitor progressMonitor) throws CoreException {
		try {
			progressMonitor.beginTask("add generated file "+file.getName(), 1000);
			IPath path = file.getProjectRelativePath();
			String portableString = path.toPortableString() + "\r\n";
			ByteArrayInputStream inputStream = new ByteArrayInputStream(portableString.getBytes());
			if (iGeneratedFileListFile.exists()) {
				iGeneratedFileListFile.appendContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 1000));
			}
			else {
				iGeneratedFileListFile.create(inputStream, true, new SubProgressMonitor(progressMonitor, 1000));
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
	public void addGeneratedFile2(IFile file, IProgressMonitor progressMonitor) throws CoreException {
		try {
			progressMonitor.beginTask("add generated file2 "+file.getName(), 1000);
			IPath path = file.getProjectRelativePath();
			String portableString = path.toPortableString() + "\r\n";
			ByteArrayInputStream inputStream = new ByteArrayInputStream(portableString.getBytes());
			synchronized(iGeneratedFileList2File) {
				if (iGeneratedFileList2File.exists()) {
					iGeneratedFileList2File.appendContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 1000));
				}
				else {
					iGeneratedFileList2File.create(inputStream, true, new SubProgressMonitor(progressMonitor, 1000));
				}
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
	public void addGeneratedFile3(IFile file, IProgressMonitor progressMonitor) throws CoreException {
		try {
			progressMonitor.beginTask("add generated file 3 "+file.getName(), 1000);
			IPath path = file.getProjectRelativePath();
			String portableString = path.toPortableString() + "\r\n";
			ByteArrayInputStream inputStream = new ByteArrayInputStream(portableString.getBytes());
			synchronized(iGeneratedFileList3File) {
				if (iGeneratedFileList3File.exists()) {
					iGeneratedFileList3File.appendContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 1000));
				}
				else {
					iGeneratedFileList3File.create(inputStream, true, new SubProgressMonitor(progressMonitor, 1000));
				}
			}
		}
		finally {
			progressMonitor.done();
		}
	}

	/**
	 * Calls restore2 to delete and restore the 2nd generated and backup list.
	 * Then deletes all files that are recorded in the .jpaGeneratedFileList metadata file for the project.
	 * The deletes the metadata file .jpaGeneratedFileList
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	public boolean restore(IProgressMonitor progressMonitor) throws CoreException {
		boolean restored = false;
		try {
			progressMonitor.beginTask("restore " + iProject.getName(), IProgressMonitor.UNKNOWN);
			System.out.println("restore "+iProject.getName());
			restored = restore2(new SubProgressMonitor(progressMonitor, 100));
			if (iGeneratedFileListFile.exists()) {
				InputStream inputStream = iGeneratedFileListFile.getContents(true);
				try {
					InputStreamReader reader = new InputStreamReader(inputStream);
					BufferedReader bufferedReader = new BufferedReader(reader);
					String portableString = bufferedReader.readLine();
					while (portableString != null) {
						IPath path = Path.fromPortableString(portableString);
						IFile generatedFile = iProject.getFile(path);
						if (generatedFile.exists()) {
							generatedFile.delete(true, new SubProgressMonitor(progressMonitor, 10));
						}
						portableString = bufferedReader.readLine();
					}
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
				iGeneratedFileListFile.delete(true, new SubProgressMonitor(progressMonitor, 10));
				restored = true;
			}
		}
		finally {
			progressMonitor.done();
		}
		return restored;
	}
	
	/**
	 * 	
	 * Deletes all files that are recorded in the .jpaGeneratedFileList2 metadata file for the project.
	 * Then copies all folders and files from the metadata backup directory, .jpaBackup2, to their original location(s) in the project.
	 * It then deletes the metadata backup directory.
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	public boolean restore2(IProgressMonitor progressMonitor) throws CoreException {
		boolean restored = false;
		try {
			progressMonitor.beginTask("restore2 " + iProject.getName(), IProgressMonitor.UNKNOWN);
			restored = restore3(new SubProgressMonitor(progressMonitor, 100));
			if (iGeneratedFileList2File.exists()) {
				InputStream inputStream = iGeneratedFileList2File.getContents(true);
				try {
					InputStreamReader reader = new InputStreamReader(inputStream);
					BufferedReader bufferedReader = new BufferedReader(reader);
					String portableString = bufferedReader.readLine();
					while (portableString != null) {
						IPath path = Path.fromPortableString(portableString);
						IFile generatedFile = iProject.getFile(path);
						if (generatedFile.exists()) {
							generatedFile.delete(true, new SubProgressMonitor(progressMonitor, 10));
						}
						portableString = bufferedReader.readLine();
					}
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
				iGeneratedFileList2File.delete(true, new SubProgressMonitor(progressMonitor, 10));
				restored = true;
			}
			if (iBackupFolder2.exists()) {
				IResource[] members = iBackupFolder2.members();
				for (IResource member : members) {
					restoreFolder((IFolder) member, iProject.getFolder(member.getName()), new SubProgressMonitor(progressMonitor, 100));
				}
				iBackupFolder2.delete(true, new SubProgressMonitor(progressMonitor, 10));
				restored = true;
			}
		}
		finally {
			progressMonitor.done();
		}
		return restored;
	}
	
	/**
	 * Deletes all files that are recorded in the .jpaGeneratedFileList3 metadata file for the project.
	 * Then copies all folders and files from the metadata backup directory, .jpaBackup3, to their original location(s) in the project.
	 * It then deletes the metadata backup directory.
	 * 
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	public boolean restore3(IProgressMonitor progressMonitor) throws CoreException {
		boolean restored = false;
		try {
			progressMonitor.beginTask("restore3 " + iProject.getName(), IProgressMonitor.UNKNOWN);
			if (iGeneratedFileList3File.exists()) {
				Collection<String> portableStrings = new HashSet<String>();
				InputStream inputStream = iGeneratedFileList3File.getContents(true);
				try {
					InputStreamReader reader = new InputStreamReader(inputStream);
					BufferedReader bufferedReader = new BufferedReader(reader);
					String portableString = bufferedReader.readLine();
					while (portableString != null) {
						portableStrings.add(portableString);
						portableString = bufferedReader.readLine();
					}
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
				iGeneratedFileList3File.delete(true, new SubProgressMonitor(progressMonitor, 10));
				for (String portableString : portableStrings) {
					IPath path = Path.fromPortableString(portableString);
					IFile generatedFile = iProject.getFile(path);
					if (generatedFile.exists()) {
						generatedFile.delete(true, new SubProgressMonitor(progressMonitor, 10));
						restored = true;
					}
				}
			}
			if (iBackupFolder3.exists()) {
				IResource[] members = iBackupFolder3.members();
				for (IResource member : members) {
					restoreFolder((IFolder) member, iProject.getFolder(member.getName()), new SubProgressMonitor(progressMonitor, 100));
				}
				iBackupFolder3.delete(true, new SubProgressMonitor(progressMonitor, 10));
				iProject.build(IncrementalProjectBuilder.CLEAN_BUILD, new SubProgressMonitor(progressMonitor, 1000));
				restored = true;
			}
//			if (iNullBackupFolder.exists()) {
//				restoreNullBackupFolder(iNullBackupFolder, iProject, progressMonitor);
//			}
		}
		finally {
			progressMonitor.done();
		}
		return restored;
	}
	
	/**
	 * Recursively copies a folder and it's sub-contents from "folder" to "targetFolder"
	 * 
	 * @param folder
	 * @param targetFolder
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private void restoreFolder(IFolder folder, IFolder targetFolder, IProgressMonitor progressMonitor) throws CoreException {
		try {
			progressMonitor.beginTask("restore folder " + folder.getName(), 200);
			if (!targetFolder.exists()) {
				targetFolder.create(true, true, new SubProgressMonitor(progressMonitor, 100));
			}
			IResource[] members = folder.members();
			for (IResource member : members) {
				if (member.getType() == IResource.FOLDER) {
					restoreFolder((IFolder) member, targetFolder.getFolder(member.getName()), new SubProgressMonitor(progressMonitor, 100));
				}
				else if (member.getType() == IResource.FILE) {
					IFile file = (IFile) member;
					IFile targetFile = targetFolder.getFile(member.getName());
					if (targetFile.exists()) {
						targetFile.setContents(file.getContents(), true, false, new SubProgressMonitor(progressMonitor, 100));
					}
					else {
						targetFile.create(file.getContents(), true, new SubProgressMonitor(progressMonitor, 100));
					}
				}
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
//	private void restoreNullBackupFolder(IFolder nullBackupFolder, IContainer targetContainer, IProgressMonitor progressMonitor) {
//		try {
//			IResource[] members = nullBackupFolder.members();
//			for (IResource member : members) {
//				if (member.getType() == IResource.FILE) {
//					IFile file = targetContainer.getFile(new Path(member.getName()));
//					if (file.exists()) {
//						file.delete(true, new SubProgressMonitor(progressMonitor, 10));
//					}
//				}
//				else if (member.getType() == IResource.FOLDER) {
//					restoreNullBackupFolder((IFolder) member, targetContainer.getFolder(new Path(member.getName())), progressMonitor);
//				}
//			}
//		}
//		catch (CoreException e) {
//			e.printStackTrace();
//		}
//	}
}
