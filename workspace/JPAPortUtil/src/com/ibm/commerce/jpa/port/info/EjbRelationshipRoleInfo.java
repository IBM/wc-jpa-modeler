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

public class EjbRelationshipRoleInfo {
	private String iId;
	private EjbRelationInfo iEjbRelationInfo;
	private String iEjbRelationshipRoleName;
	private String iMultiplicity;
	private String iRelatedMultiplicity;
	private boolean iCascadeDelete;
	private String iEjbName;
	private String iFieldName;
	private String iRelatedFieldName;
	private String iFieldType;
	private String iGetterName;
	private String iSetterName;
	private EntityInfo iEntityInfo;
	private EntityInfo iRelatedEntityInfo;
	private boolean iIsKeyField;
	
	public EjbRelationshipRoleInfo(String id) {
		iId = id;
	}
	
	public String getId() {
		return iId;
	}
	
	public void setEjbRelationInfo(EjbRelationInfo ejbRelationInfo) {
		iEjbRelationInfo = ejbRelationInfo;
	}
	
	public EjbRelationInfo getEjbRelationInfo() {
		return iEjbRelationInfo;
	}
	
	public void setEjbRelationshipRoleName(String ejbRelationshipRoleName) {
		iEjbRelationshipRoleName = ejbRelationshipRoleName;
	}
	
	public String getEjbRelationshipRoleName() {
		return iEjbRelationshipRoleName;
	}
	
	public void setMultiplicity(String multiplicity) {
		iMultiplicity = multiplicity;
	}
	
	public String getMultiplicity() {
		return iMultiplicity;
	}

	public void setRelatedMultiplicity(String relatedMultiplicity) {
		iRelatedMultiplicity = relatedMultiplicity;
	}
	
	public String getRelatedMultiplicity() {
		return iRelatedMultiplicity;
	}
	
	public void setCascadeDelete(boolean cascadeDelete) {
		iCascadeDelete = cascadeDelete;
	}
	
	public boolean getCascadeDelete() {
		return iCascadeDelete;
	}
	
	public void setEjbName(String ejbName) {
		iEjbName = ejbName;
	}
	
	public String getEjbName() {
		return iEjbName;
	}
	
	public void setFieldName(String fieldName) {
		iFieldName = fieldName;
		if (iGetterName == null) {
			iGetterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
		}
		if (iSetterName == null) {
			iSetterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
		}
	}
	
	public String getFieldName() {
		return iFieldName;
	}
	
	public void setRelatedFieldName(String relatedFieldName) {
		iRelatedFieldName = relatedFieldName;
	}
	
	public String getRelatedFieldName() {
		return iRelatedFieldName;
	}
	
	public void setFieldType(String fieldType) {
		iFieldType = fieldType;
	}
	
	public String getFieldType() {
		return iFieldType;
	}
	
	public void setEntityInfo(EntityInfo entityInfo) {
		iEntityInfo = entityInfo;
		if (iGetterName != null) {
			iEntityInfo.setEjbRelationshipRoleInfoGetterName(this, iGetterName);
		}
		if (iSetterName != null) {
			iEntityInfo.setEjbRelationshipRoleInfoSetterName(this, iSetterName);
		}
	}
	
	public EntityInfo getEntityInfo() {
		return iEntityInfo;
	}
	
	public void setRelatedEntityInfo(EntityInfo relatedEntityInfo) {
		iRelatedEntityInfo = relatedEntityInfo;
	}
	
	public EntityInfo getRelatedEntityInfo() {
		return iRelatedEntityInfo;
	}
	
	public void setSetterName(String setterName) {
		iSetterName = setterName;
	}
	
	public String getSetterName() {
		return iSetterName;
	}
	
	public void setGetterName(String getterName) {
		iGetterName = getterName;
	}
	
	public String getGetterName() {
		return iGetterName;
	}

	public void setIsKeyField(boolean isKeyField) {
		iIsKeyField = isKeyField;
	}
	
	public boolean getIsKeyField() {
		return iIsKeyField;
	}
}
