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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

public class PersistenceUnitInfo {
	private IJavaProject iPersistenceUnitRootProject;
	private List<IJavaProject> iEntityJarProjects = new ArrayList<IJavaProject>();
	
	public PersistenceUnitInfo(IJavaProject persistenceUnitRootProject) {
		iPersistenceUnitRootProject = persistenceUnitRootProject;
	}
	
	public IJavaProject getPersistenceUnitRootProject() {
		return iPersistenceUnitRootProject;
	}
	
	public void addEntityJarProject(IJavaProject entityJarProject) {
		iEntityJarProjects.add(entityJarProject);
	}
	
	public List<IJavaProject> getEntityJarProjects() {
		return iEntityJarProjects;
	}
}
