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

public class RelatedEntityInfo {
	private EntityInfo iEntityInfo;
	private EntityInfo iParentEntityInfo;
	private String iFieldName;
	private List<FieldInfo> iMemberFields = new ArrayList<FieldInfo>();
	private List<FieldInfo> iReferencedFields = new ArrayList<FieldInfo>();
	private String iGetterName;
	private String iSetterName;
	private EjbRelationshipRoleInfo iEjbRelationshipRoleInfo;
	
	public RelatedEntityInfo(EntityInfo entityInfo, EntityInfo parentEntityInfo) {
		iEntityInfo = entityInfo;
		iParentEntityInfo = parentEntityInfo;
	}
	
	public EntityInfo getEntityInfo() {
		return iEntityInfo;
	}
	
	public EntityInfo getParentEntityInfo() {
		return iParentEntityInfo;
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
	
	public void addMemberField(FieldInfo memberFieldInfo, FieldInfo referencedFieldInfo) {
		iMemberFields.add(memberFieldInfo);
		iReferencedFields.add(referencedFieldInfo);
		memberFieldInfo.setRelatedEntityInfo(this);
		memberFieldInfo.setReferencedFieldInfo(referencedFieldInfo);
	}
	
	public List<FieldInfo> getMemberFields() {
		return iMemberFields;
	}
	
	public List<FieldInfo> getReferencedFields() {
		return iReferencedFields;
	}
	
	public boolean getIsKeyField() {
		boolean isKeyField = false;
		for (FieldInfo fieldInfo : iMemberFields) {
			if (fieldInfo.getIsKeyField()) {
				isKeyField = true;
			}
		}
		return isKeyField;
	}
	
	public String getKeyFieldType() {
		String keyFieldType = null;
		if (getIsKeyField()) {
			if (iReferencedFields.size() == 1) {
				keyFieldType = iReferencedFields.get(0).getTypeName();
			}
			else if (iReferencedFields.size() > 1 && iParentEntityInfo.getEntityKeyClassInfo() != null) {
				keyFieldType = iParentEntityInfo.getEntityKeyClassInfo().getQualifiedClassName();
			}
		}
		return keyFieldType;
	}
	
	public boolean getOptional() {
		boolean optional = true;
		for (FieldInfo fieldInfo : iMemberFields) {
			if (!fieldInfo.getColumnInfo().getNullable()) {
				optional = false;
			}
		}
		return optional;
	}
	
	public void setGetterName(String getterName) {
		iGetterName = getterName;
	}
	
	public String getGetterName() {
		return iGetterName;
	}
	
	public void setSetterName(String setterName) {
		iSetterName = setterName;
	}
	
	public String getSetterName() {
		return iSetterName;
	}
	
	public void setEjbRelationshipRoleInfo(EjbRelationshipRoleInfo ejbRelationshipRoleInfo) {
		iEjbRelationshipRoleInfo = ejbRelationshipRoleInfo;
	}
	
	public EjbRelationshipRoleInfo getEjbRelationshipRoleInfo() {
		return iEjbRelationshipRoleInfo;
	}
}
