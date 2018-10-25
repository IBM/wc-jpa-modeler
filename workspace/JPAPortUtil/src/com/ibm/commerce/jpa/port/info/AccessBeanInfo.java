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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class AccessBeanInfo {
	

	private String iAccessBeanId;
	private String iAccessBeanName;
	private String iAccessBeanPackage;
	private boolean iDataClassType = false;
	private Set<String> iExcludedPropertyNames;
	private Set<String> iExcludedPropertyMethods;
	private Map<String, CopyHelperProperty> iExcludedProperties = new HashMap<String, CopyHelperProperty>();
	private Set<String> iCopyHelperMethods;
	private Map<String, CopyHelperProperty> iCopyHelperProperties = new HashMap<String, CopyHelperProperty>();
	private Map<String, NullConstructor> iNullConstructors = new HashMap<String, NullConstructor>();
	private Map<String, NullConstructorParameter> iNullConstructorParameters = new HashMap<String, NullConstructorParameter>();
	private Collection<String> iAccessBeanInterfaces = new HashSet<String>();
	
	public AccessBeanInfo(String accessBeanId) {
		iAccessBeanId = accessBeanId;
	}
	
	public String getAccessBeanId() {
		return iAccessBeanId;
	}
	
	public void setAccessBeanName(String accessBeanName) {
		iAccessBeanName = accessBeanName;
	}
	
	public String getAccessBeanName() {
		return iAccessBeanName;
	}
	
	public void setAccessBeanPackage(String accessBeanPackage) {
		iAccessBeanPackage = accessBeanPackage;
	}
	
	public String getAccessBeanPackage() {
		return iAccessBeanPackage;
	}
	
	public String getQualifiedAccessBeanName() {
		return iAccessBeanPackage + "." + iAccessBeanName;
	}
	
	public void setDataClassType(boolean dataClassType) {
		iDataClassType = dataClassType;
	}
	
	public boolean getDataClassType() {
		return iDataClassType;
	}
	
	public CopyHelperProperty getCopyHelperProperty(String name, boolean create) {
		CopyHelperProperty copyHelperProperty = iCopyHelperProperties.get(name);
		if (copyHelperProperty == null && create) {
			copyHelperProperty = new CopyHelperProperty(name);
			iCopyHelperProperties.put(name, copyHelperProperty);
		}
		return copyHelperProperty;
	}
	
	public Collection<CopyHelperProperty> getCopyHelperProperties() {
		return iCopyHelperProperties.values();
	}
	
	public boolean isCopyHelperMethod(String methodKey) {
		if (iCopyHelperMethods == null) {
			iCopyHelperMethods = new HashSet<String>();
			for (CopyHelperProperty property : iCopyHelperProperties.values()) {
				if (property.getGetterName() != null) {
					iCopyHelperMethods.add(property.getGetterName());
				}
				if (property.getSetterName() != null) {
					iCopyHelperMethods.add(property.getSetterName() + "+" + property.getType());
					if (property.getConverterClassName() != null) {
						iCopyHelperMethods.add(property.getSetterName() + "+java.lang.String");
					}
				}
			}
		}
		return iCopyHelperMethods.contains(methodKey);
	}

	public CopyHelperProperty getExcludedProperty(String id, boolean create) {
		CopyHelperProperty copyHelperProperty = iExcludedProperties.get(id);
		if (copyHelperProperty == null && create) {
			copyHelperProperty = new CopyHelperProperty(id);
			iExcludedProperties.put(id, copyHelperProperty);
		}
		return copyHelperProperty;
	}
	
	public boolean isExcludedPropertyName(String propertyName) {
		if (iExcludedPropertyNames == null) {
			iExcludedPropertyNames = new HashSet<String>();
			for (CopyHelperProperty excludedProperty : iExcludedProperties.values()) {
				iExcludedPropertyNames.add(excludedProperty.getName());
			}
		}
		return iExcludedPropertyNames.contains(propertyName);
	}
	
	public boolean isExcludedPropertyMethod(String methodKey) {
		if (iExcludedPropertyMethods == null) {
			iExcludedPropertyMethods = new HashSet<String>();
			for (CopyHelperProperty property : iExcludedProperties.values()) {
				if (property.getGetterName() != null) {
					iExcludedPropertyMethods.add(property.getGetterName());
				}
				if (property.getSetterName() != null) {
					iExcludedPropertyMethods.add(property.getSetterName() + "+" + property.getType());
					if (property.getConverterClassName() != null) {
						iExcludedPropertyMethods.add(property.getSetterName() + "+java.lang.String");
					}
				}
			}
		}
		return iExcludedPropertyMethods.contains(methodKey);
	}
	
	public void removeExcludedPropertyName(String propertyName) {
		iExcludedPropertyNames.remove(propertyName);
	}
	
	public NullConstructor getNullConstructor(String id, boolean create) {
		NullConstructor nullConstructor = iNullConstructors.get(id);
		if (nullConstructor == null && create) {
			nullConstructor = new NullConstructor(id);
			iNullConstructors.put(id, nullConstructor);
		}
		return nullConstructor;
	}
	
	public NullConstructorParameter getNullConstructorParameter(String id, boolean create) {
		NullConstructorParameter nullConstructorParameter = iNullConstructorParameters.get(id);
		if (nullConstructorParameter == null && create) {
			nullConstructorParameter = new NullConstructorParameter(id);
			iNullConstructorParameters.put(id, nullConstructorParameter);
		}
		return nullConstructorParameter;
	}
	
	public Collection<NullConstructorParameter> getNullConstructorParameters() {
		return iNullConstructorParameters.values();
	}
	
	public NullConstructorParameter getNullConstructorParameterByName(String name) {
		NullConstructorParameter nullConstructorParameter = null;
		Collection<NullConstructorParameter> nullConstructorParameters = iNullConstructorParameters.values();
		for (NullConstructorParameter currentNullConstructorParameter : nullConstructorParameters) {
			if (name.equals(currentNullConstructorParameter.getName())) {
				nullConstructorParameter = currentNullConstructorParameter;
				break;
			}
		}
		return nullConstructorParameter;
	}
	
	public void addAccessBeanInterface(String interfaceName) {
		iAccessBeanInterfaces.add(interfaceName);
	}
	
	public Collection<String> getAccessBeanInterfaces() {
		return iAccessBeanInterfaces;
	}
	
	public static class CopyHelperProperty {
		private String iName;
		private String iType;
		private String iGetterName;
		private String iSetterName;
		private String iConverterClassName;
		private FieldInfo iFieldInfo;
		
		public CopyHelperProperty(String name) {
			iName = name;
		}
		
		public String getName() {
			return iName;
		}
		
		public void setType(String type) {
			iType = type;
		}
		
		public String getType() {
			return iType;
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
		
		public void setConverterClassName(String converterClassName) {
			iConverterClassName = converterClassName;
			if (iFieldInfo != null) {
				iFieldInfo.setCopyHelperProperty(this);
			}
		}
		
		public String getConverterClassName() {
			return iConverterClassName;
		}
		
		public void setFieldInfo(FieldInfo fieldInfo) {
			iFieldInfo = fieldInfo;
		}
		
		public FieldInfo getFieldInfo() {
			return iFieldInfo;
		}
	}
	
	public static class NullConstructor {
		private String iId;
		private String iName;
		private String iParms;
		private String iType;
		private EntityInfo iEntityInfo;
		
		public NullConstructor(String id) {
			iId = id;
		}
		
		public void setId(String id) {
			iId = id;
		}
		
		public String getId() {
			return iId;
		}
		
		public void setName(String name) {
			iName = name;
		}
		
		public String getName() {
			return iName;
		}
		
		public void setParms(String parms) {
			iParms = parms;
		}
		
		public String getParms() {
			return iParms;
		}
		
		public void setType(String type) {
			iType = type;
		}
		
		public String getType() {
			return iType;
		}
		
		public void setEntityInfo(EntityInfo entityInfo) {
			iEntityInfo = entityInfo;
		}
		
		public EntityInfo getEntityInfo() {
			return iEntityInfo;
		}
	}
	
	public static class NullConstructorParameter {
		private String iId;
		private String iName;
		private String iType;
		private String iConverterClassName;
		private boolean iIsFieldFromKey;
		
		public NullConstructorParameter(String id) {
			iId = id;
		}
		
		public String getId() {
			return iId;
		}
		
		public void setName(String name) {
			iName = name;
		}
		
		public String getName() {
			return iName;
		}
		
		public void setType(String type) {
			iType = type;
		}
		
		public String getType() {
			return iType;
		}
		
		public void setConverterClassName(String converterClassName) {
			iConverterClassName = converterClassName;
		}
		
		public String getConverterClassName() {
			return iConverterClassName;
		}
		
		public void setIsFieldFromKey(boolean isFieldFromKey) {
			iIsFieldFromKey = isFieldFromKey;
		}
		
		public boolean getIsFieldFromKey() {
			return iIsFieldFromKey;
		}
	}

	StringBuffer errors = null;
	
	public void addError(String error) {
		if(errors == null) {
			errors = new StringBuffer("");
		}
		errors.append(error + System.lineSeparator());
	}
	public String getErrors() {
		
		return (errors==null)?null:errors.toString();
	}
}
