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

public class EjbRelationInfo {
	private String iEjbRelationName;
	private List<EjbRelationshipRoleInfo> iEjbRelationshipRoles = new ArrayList<EjbRelationshipRoleInfo>();
	private ConstraintInfo iConstraintInfo;

	public EjbRelationInfo() {
	}
	
	public void setEjbRelationName(String ejbRelationName) {
		iEjbRelationName = ejbRelationName;
	}
	
	public String getEjbRelationName() {
		return iEjbRelationName;
	}
	
	public void addEjbRelationshipRole(EjbRelationshipRoleInfo ejbRelationshipRoleInfo) {
		iEjbRelationshipRoles.add(ejbRelationshipRoleInfo);
	}
	
	public List<EjbRelationshipRoleInfo> getEjbRelationshipRoles() {
		return iEjbRelationshipRoles;
	}
	
	public void setConstraintInfo(ConstraintInfo constraintInfo) {
		iConstraintInfo = constraintInfo;
	}
	
	public ConstraintInfo getConstraintInfo() {
		return iConstraintInfo;
	}
}
