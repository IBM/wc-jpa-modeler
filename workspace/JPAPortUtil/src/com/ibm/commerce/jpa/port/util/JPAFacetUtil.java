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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class JPAFacetUtil {
	public static void addJPAFacet(IJavaProject javaProject, IProgressMonitor monitor) {
		try {
			IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet("jpt.jpa");
			IFacetedProject facetedProject = ProjectFacetsManager.create(javaProject.getProject());
			if (!facetedProject.getFixedProjectFacets().contains(jpaFacet)) {
				facetedProject.installProjectFacet(jpaFacet.getVersion("2.0"), null, monitor);
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
