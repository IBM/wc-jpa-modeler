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

import java.beans.Introspector;
import java.util.HashSet;
import java.util.Set;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo.CopyHelperProperty;

public class FieldInfo {
	private EntityInfo iEntityInfo;
	private String iFieldId;
	private String iFieldName;
	private String iTargetFieldName;
	private ColumnInfo iColumnInfo;
	private String iTargetClass;
	private String iTransformerClass;
	private String iTypeMapping;
	private String iTypeName;
	private Set<String> iGetterNames = new HashSet<String>();
	private String iTargetGetterName;
	private Set<String> iSetterNames = new HashSet<String>();
	private String iTargetSetterName;
	private RelatedEntityInfo iRelatedEntityInfo;
	private FieldInfo iReferencedFieldInfo;
	private CopyHelperProperty iCopyHelperProperty;
	private AccessBeanMethodInfo iGetterAccessBeanMethodInfo;
	private AccessBeanMethodInfo iSetterAccessBeanMethodInfo;
	private String iEntityCreationDataGetterName;
	private boolean iHasStringConversionAccessMethod;
	
	public FieldInfo(EntityInfo entityInfo, String fieldId) {
		iEntityInfo = entityInfo;
		iFieldId = fieldId;
	}

	public String getFieldId() {
		return iFieldId;
	}

	public void setFieldName(String fieldName) {
		iFieldName = fieldName;
		if (fieldName != null) {
			iEntityInfo.setFieldName(this, fieldName);
		}
	}

	public String getFieldName() {
		return iFieldName;
	}
	
	public void setTargetFieldName(String targetFieldName) {
		iTargetFieldName = targetFieldName;
	}
	
	public String getTargetFieldName() {
		if (iTargetFieldName == null && iFieldName != null) {
			iTargetFieldName = Introspector.decapitalize(iFieldName);
		}
		return iTargetFieldName;
	}

	public void setColumnInfo(ColumnInfo columnInfo) {
		iColumnInfo = columnInfo;
	}

	public ColumnInfo getColumnInfo() {
		return iColumnInfo;
	}

	public void setTargetClass(String targetClass) {
		iTargetClass = targetClass;
	}

	public String getTargetClass() {
		return iTargetClass;
	}

	public void setTransformerClass(String transformerClass) {
		iTransformerClass = transformerClass;
	}

	public String getTransformerClass() {
		return iTransformerClass;
	}

	public void setTypeMapping(String typeMapping) {
		iTypeMapping = typeMapping;
	}

	public String getTypeMapping() {
		return iTypeMapping;
	}
	
	public boolean getIsKeyField() {
		return iEntityInfo.getKeyFields().contains(this);
	}
	
	public void setTypeName(String typeName) {
		iTypeName = typeName;
		for (String setterName : iSetterNames) {
			iEntityInfo.setFieldSetterMethodKey(this, setterName + "+" + iTypeName);
		}
	}
	
	public String getTypeName() {
		return iTypeName;
	}
	
	public void setSetterName(String setterName) {
		iEntityInfo.setFieldSetterName(this, setterName);
		iSetterNames.add(setterName);
		if (iTypeName != null) {
			iEntityInfo.setFieldSetterMethodKey(this, setterName + "+" + iTypeName);
		}
	}
	
	public Set<String> getSetterNames() {
		return iSetterNames;
	}
	
	public void setTargetSetterName(String targetSetterName) {
		iTargetSetterName = targetSetterName; 
	}
	
	public String getTargetSetterName() {
		if (iTargetSetterName == null) {
			if (iSetterNames.size() == 1) {
				for (String setterName : iSetterNames) {
					iTargetSetterName = setterName;
				}
			}
			else if (iFieldName != null) {
				if (iFieldName.startsWith("i") && Character.toUpperCase(iFieldName.charAt(1)) == iFieldName.charAt(1)) {
					iTargetSetterName = "set" + iFieldName.substring(1);
				}
				else {
					iTargetSetterName = "set" + Character.toUpperCase(iFieldName.charAt(0)) + iFieldName.substring(1);
				}
				iEntityInfo.setFieldSetterName(this, iTargetSetterName);
				iEntityInfo.setFieldSetterMethodKey(this, iTargetSetterName + "+" + iTypeName);
			}
		}
		return iTargetSetterName;
	}
	
	public void setGetterName(String getterName) {
		iEntityInfo.setFieldGetterName(this, getterName);
		iGetterNames.add(getterName);
	}
	
	public Set<String> getGetterNames() {
		return iGetterNames;
	}
	
	public void setTargetGetterName(String targetGetterName) {
		iTargetGetterName = targetGetterName;
	}
	
	public String getTargetGetterName() {
		if (iTargetGetterName == null) {
			if (iGetterNames.size() == 1) {
				for (String getterName : iGetterNames) {
					iTargetGetterName = getterName;
				}
			}
			else if (iFieldName != null) {
				if (iFieldName.startsWith("i") && Character.toUpperCase(iFieldName.charAt(1)) == iFieldName.charAt(1)) {
					iTargetGetterName = "get" + iFieldName.substring(1);
				}
				else {
					iTargetGetterName = "get" + Character.toUpperCase(iFieldName.charAt(0)) + iFieldName.substring(1);
				}
				iEntityInfo.setFieldGetterName(this, iTargetGetterName);
			}
		}
		return iTargetGetterName;
	}
	
	public void setRelatedEntityInfo(RelatedEntityInfo parentEntityInfo) {
		iRelatedEntityInfo = parentEntityInfo;
	}
	
	public RelatedEntityInfo getRelatedEntityInfo() {
		return iRelatedEntityInfo;
	}
	
	public void setReferencedFieldInfo(FieldInfo referencedFieldInfo) {
		iReferencedFieldInfo = referencedFieldInfo;
	}
	
	public FieldInfo getReferencedFieldInfo() {
		return iReferencedFieldInfo;
	}
	
	public void setCopyHelperProperty(CopyHelperProperty copyHelperProperty) {
		setHasStringConversionAccessMethod(copyHelperProperty != null && !"java.lang.String".equals(iTypeName) && copyHelperProperty.getConverterClassName() != null);
		iCopyHelperProperty = copyHelperProperty;
	}
	
	public CopyHelperProperty getCopyHelperProperty() {
		return iCopyHelperProperty;
	}
	
	public void setGetterAccessBeanMethodInfo(AccessBeanMethodInfo getterAccessBeanMethodInfo) {
		iGetterAccessBeanMethodInfo = getterAccessBeanMethodInfo;
	}
	
	public AccessBeanMethodInfo getGetterAccessBeanMethodInfo() {
		return iGetterAccessBeanMethodInfo;
	}
	
	public void setSetterAccessBeanMethodInfo(AccessBeanMethodInfo setterAccessBeanMethodInfo) {
		iSetterAccessBeanMethodInfo = setterAccessBeanMethodInfo;
	}
	
	public AccessBeanMethodInfo getSetterAccessBeanMethodInfo() {
		return iSetterAccessBeanMethodInfo;
	}
	
	public void setEntityCreationDataGetterName(String entityCreationDataGetterName) {
		iEntityCreationDataGetterName = entityCreationDataGetterName;
	}
	
	public String getEntityCreationDataGetterName() {
		return iEntityCreationDataGetterName;
	}
	
	public void setHasStringConversionAccessMethod(boolean hasStringConversionAccessMethod) {
		iHasStringConversionAccessMethod = hasStringConversionAccessMethod;
	}
	
	public boolean getHasStringConversionAccessMethod() {
		return iHasStringConversionAccessMethod;
	}
}
