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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

public class EjbProjectUtil {
	private static final String META_INF = "META-INF";
	private static final String MANIFEST = "MANIFEST.MF";
	private final static int STATE_PARSING = 0;
	private final static int STATE_NEW_LINE = 1;
	private final static int STATE_DONE = 2;
	private final static int LINE_LENGTH = 70;
	private IWorkspace iWorkspace;
	private IJavaProject iJavaProject;
	
	public EjbProjectUtil(IWorkspace workspace, IJavaProject javaProject) {
		iWorkspace = workspace;
		iJavaProject = javaProject;
	}
	
	public void addClasspathEntry(String classPathEntry) {
		try {
			IClasspathEntry[] classpathEntries = iJavaProject.getResolvedClasspath(true);
			for (IClasspathEntry entry : classpathEntries) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = entry.getPath();
					IFile manifestFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(MANIFEST));
					if (manifestFile.exists()) {
						InputStream inputStream = null;
						InputStreamReader reader = null;
						try {
							StringWriter stringWriter = new StringWriter();
							String charset = manifestFile.getCharset();
							inputStream = manifestFile.getContents();
							reader = new InputStreamReader(inputStream, charset);
							char[] buffer = new char[1024];
							int count = reader.read(buffer);
							if (count != -1) {
								stringWriter.write(buffer, 0, count);
							}
							reader.close();
							reader = null;
							inputStream.close();
							inputStream = null;
							String manifestContents = stringWriter.toString();
							int index = manifestContents.indexOf("Class-Path: ");
							if (index > -1) {
								StringBuilder sb = new StringBuilder();
								index += "Class-Path: ".length();
								int classPathStartIndex = index;
								int state = STATE_PARSING;
								while (state < 2 && index < manifestContents.length()) {
									char c = manifestContents.charAt(index);
									switch (c) {
									case '\r':
									case '\n':
										state = STATE_NEW_LINE;
										break;
									case ' ':
										if (state == STATE_NEW_LINE) {
											state = STATE_PARSING;
										}
										else {
											sb.append(c);
										}
										break;
									default:
										if (state == STATE_NEW_LINE) {
											state = STATE_DONE;
										}
										else {
											sb.append(c);
										}
									}
									index++;
								}
								if (sb.indexOf(classPathEntry) == -1) {
									sb.append(" ");
									sb.append(classPathEntry);
									String newClassPath = sb.toString().trim();
									sb = new StringBuilder(manifestContents.substring(0, classPathStartIndex));
									int newClassPathIndex = 0;
									int columnPos = "Class-Path: ".length();
									while (newClassPathIndex < newClassPath.length()) {
										if (columnPos == LINE_LENGTH) {
											sb.append("\r\n ");
											columnPos = 2;
										}
										else {
											columnPos++;
										}
										sb.append(newClassPath.charAt(newClassPathIndex));
										newClassPathIndex++;
									}
									sb.append("\r\n");
									ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(sb.toString().getBytes(charset));
									manifestFile.setContents(byteArrayInputStream, true, false, null);
								}
							}
						}
						catch (CoreException e) {
							e.printStackTrace();
						}
						catch (IOException e) {
							e.printStackTrace();
						}
						finally {
							if (reader != null) {
								try {
									reader.close();
								}
								catch (IOException e) {
									e.printStackTrace();
								}
								reader = null;
							}
							if (inputStream != null) {
								try {
									inputStream.close();
								}
								catch (IOException e) {
									e.printStackTrace();
								}
								inputStream = null;
							}
						}
					}
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
}
