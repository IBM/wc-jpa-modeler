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

public class UserMethodInfo {
	private String iMethodName;
	private List<String> iParameterNames = new ArrayList<String>();
	private List<String> iParameterTypes = new ArrayList<String>();
	private String iReturnType;
	private String iKey;
	private RelatedEntityInfo iRelatedEntityInfo;
	private FieldInfo iFieldInfo;
	private EjbRelationshipRoleInfo iEjbRelationshipRoleInfo;
	private AccessBeanMethodInfo iAccessBeanMethodInfo;
	
	public UserMethodInfo(String methodName) {
		iMethodName = methodName;
	}
	
	public String getMethodName() {
		return iMethodName;
	}
	
	public void addParameter(String parameterName, String parameterType) {
		iParameterNames.add(parameterName);
		iParameterTypes.add(parameterType);
	}
	
	public List<String> getParameterNames() {
		return iParameterNames;
	}
	
	public List<String> getParameterTypes() {
		return iParameterTypes;
	}
	
	public void setReturnType(String returnType) {
		iReturnType = returnType;
	}
	
	public String getReturnType() {
		return iReturnType;
	}
	
	public void setRelatedEntityInfo(RelatedEntityInfo relatedEntityInfo) {
		iRelatedEntityInfo = relatedEntityInfo;
	}
	
	public RelatedEntityInfo getRelatedEntityInfo() {
		return iRelatedEntityInfo;
	}

	public void setFieldInfo(FieldInfo fieldInfo) {
		iFieldInfo = fieldInfo;
	}
	
	public FieldInfo getFieldInfo() {
		return iFieldInfo;
	}
	
	public void setEjbRelationshipRoleInfo(EjbRelationshipRoleInfo ejbRelationshipRoleInfo) {
		iEjbRelationshipRoleInfo = ejbRelationshipRoleInfo;
	}
	
	public EjbRelationshipRoleInfo getEjbRelationshipRoleInfo() {
		return iEjbRelationshipRoleInfo;
	}
	
	public void setAccessBeanMethodInfo(AccessBeanMethodInfo accessBeanMethodInfo) {
		iAccessBeanMethodInfo = accessBeanMethodInfo;
	}
	
	public AccessBeanMethodInfo getAccessBeanMethodInfo() {
		return iAccessBeanMethodInfo;
	}
	
	public String getKey() {
		if (iKey == null) {
			StringBuilder sb = new StringBuilder(iMethodName);
			for (String parameterType : iParameterTypes) {
				sb.append("+");
				sb.append(parameterType);
			}
			iKey = sb.toString();
		}
		return iKey;
	}
}
