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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;

public class ResolveImportsUtil {
	private IProject iProject;
	private IFile iGeneratedFileListFile;
	private ASTParser iASTParser;
	
	public ResolveImportsUtil(ASTParser astParser, IProject project, IFile generatedFileListFile) {
		iASTParser = astParser;
		iProject = project;
		iGeneratedFileListFile = generatedFileListFile;
	}
	
	public boolean resolveImports(IProgressMonitor progressMonitor) throws CoreException {
		boolean importsResolved = false;
		try {
			progressMonitor.beginTask("resolve imports for " + iProject.getName(), IProgressMonitor.UNKNOWN);
			if (iGeneratedFileListFile.exists()) {
				InputStream inputStream = iGeneratedFileListFile.getContents(true);
				try {
					InputStreamReader reader = new InputStreamReader(inputStream);
					BufferedReader bufferedReader = new BufferedReader(reader);
					String portableString = bufferedReader.readLine();
					while (portableString != null && !progressMonitor.isCanceled()) {
						IPath path = Path.fromPortableString(portableString);
						IFile generatedFile = iProject.getFile(path);
						if (generatedFile.exists()) {
							importsResolved = true;
							ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(generatedFile);
							ImportUtil.resolveImports(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
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
			}
		}
		finally {
			progressMonitor.done();
		}
		return importsResolved;
	}
}
