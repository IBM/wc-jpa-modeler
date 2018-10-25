package com.ibm.commerce.jpa.port.generators;

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

import com.ibm.commerce.jpa.port.info.EntityInfo;

public class MemberAttributeEntityBaseGenerator {
	public static final String TYPE_NAME = "com.ibm.commerce.user.objimpl.MemberAttributeJPAEntityBase";
	
	public static class AddAttributeValueMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "addAttributeValue+java.lang.String+java.lang.String+java.lang.String";
		}
		
		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\t/**\r\n");
			sb.append("\t * Add attribute value\r\n");
			sb.append("\t * @param astrMemberId java.lang.String\r\n");
			sb.append("\t)* @param astrStoreEntityId java.lang.String\r\n");
			sb.append("\t * @param astrAttributeValue java.lang.String\r\n");
			sb.append("\t */\r\n");
			sb.append("\tpublic void addAttributeValue(String astrMemberId,	String astrStoreEntityId, String astrAttributeValue) {\r\n");
			sb.append("\t\tfinal String USER_PACKAGE_PREFIX = \"com.ibm.commerce.user.objects.MemberAttribute\";\r\n");
			sb.append("\t\tfinal String USER_PACKAGE_SUFFIX = \"ValueAccessBean\";\r\n");
			sb.append("\t\tLong nMemberId = new Long(astrMemberId);\r\n");
			sb.append("\t\tInteger nStoreEntityId = null;\r\n");
			sb.append("\t\tif (astrStoreEntityId != null)\r\n");
			sb.append("\t\t\tnStoreEntityId = new Integer(astrStoreEntityId);\r\n");
			sb.append("\t\tString strAttributeTypeId = getAttributeTypeId().trim();\r\n");
			sb.append("\t\tstrAttributeTypeId = strAttributeTypeId.substring(0, 1) + strAttributeTypeId.substring(1).toLowerCase();\r\n");
			sb.append("\t\tString strAtributeValueChild = USER_PACKAGE_PREFIX + strAttributeTypeId + USER_PACKAGE_SUFFIX;\r\n");
			sb.append("\t\ttry {\r\n");
			sb.append("\t\t\tClass cAttributeValueChildAccessBean = Class.forName(strAtributeValueChild);\r\n");
			sb.append("\t\t\tClass[] ConstructorArgumentsClass = null;\r\n");
			sb.append("\t\t\tObject[] ConstructorArguments = null;\r\n");
			sb.append("\t\t\tif (astrStoreEntityId != null) {\r\n");
			sb.append("\t\t\t\tConstructorArgumentsClass = new Class[] { Long.class, Long.class, Integer.class };\r\n");
			sb.append("\t\t\t\tConstructorArguments = new Object[] { nMemberId, getMemberAttributeId(), nStoreEntityId };\r\n");
			sb.append("\t\t\t} else {\r\n");
			sb.append("\t\t\t\tConstructorArgumentsClass = new Class[] { Long.class, Long.class };\r\n");
			sb.append("\t\t\t\tConstructorArguments = new Object[] { nMemberId, getMemberAttributeId() };\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\tjava.lang.reflect.Constructor constructor = cAttributeValueChildAccessBean.getConstructor(ConstructorArgumentsClass);\r\n");
			sb.append("\t\t\tObject abAttributeValueChildAccessBean = (Object) constructor.newInstance(ConstructorArguments);\r\n");
			sb.append("\t\t\tif (astrAttributeValue != null) {\r\n");
			sb.append("\t\t\t\tClass[] parameterTypes = new Class[] { Object.class };\r\n");
			sb.append("\t\t\t\tObject[] methodArguments = new Object[] {};\r\n");
			sb.append("\t\t\t\tjava.lang.reflect.Method setValueMethod = null;\r\n");
			sb.append("\t\t\t\tif (strAttributeTypeId.equalsIgnoreCase(\"STRING\")) {\r\n");
			sb.append("\t\t\t\t\tmethodArguments = new Object[] { astrAttributeValue };\r\n");
			sb.append("\t\t\t\t} else if (strAttributeTypeId.equalsIgnoreCase(\"INTEGER\")) {\r\n");
			sb.append("\t\t\t\t\tmethodArguments = new Object[] { new Integer( astrAttributeValue) };\r\n");
			sb.append("\t\t\t\t} else if (strAttributeTypeId.equalsIgnoreCase(\"FLOAT\")) {\r\n");
			sb.append("\t\t\t\t\tmethodArguments = new Object[] { new Double( astrAttributeValue) };\r\n");
			sb.append("\t\t\t\t} else if (strAttributeTypeId.equalsIgnoreCase(\"DATETIME\")) {\r\n");
			sb.append("\t\t\t\t\tmethodArguments = new Object[] { java.sql.Timestamp.valueOf(astrAttributeValue) };\r\n");
			sb.append("\t\t\t\t} else\r\n");
			sb.append("\t\t\t\t\tthrow new PersistenceException(\"Can not add new attribute value because no attribute type is found.\");\r\n");
			sb.append("\t\t\t\tsetValueMethod = cAttributeValueChildAccessBean.getMethod(\"setAttributeValue\", parameterTypes);\r\n");
			sb.append("\t\t\t\tsetValueMethod.invoke(abAttributeValueChildAccessBean, methodArguments);\r\n");
			sb.append("\t\t\t} else\r\n");
			sb.append("\t\t\t\tthrow new PersistenceException(\"Can not add new attribute value because it is null.\");\r\n");
			sb.append("\t\t} catch (ClassNotFoundException e) {\r\n");
			sb.append("\t\t\tthrow new PersistenceException(e.toString());\r\n");
			sb.append("\t\t} catch (NoSuchMethodException e) {\r\n");
			sb.append("\t\t\tthrow new PersistenceException(e.toString());\r\n");
			sb.append("\t\t} catch (InstantiationException e) {\r\n");
			sb.append("\t\t\tthrow new PersistenceException(e.toString());\r\n");
			sb.append("\t\t} catch (IllegalAccessException e) {\r\n");
			sb.append("\t\t\tthrow new PersistenceException(e.toString());\r\n");
			sb.append("\t\t} catch (IllegalArgumentException e) {\r\n");
			sb.append("\t\t\tthrow new PersistenceException(e.toString());\r\n");
			sb.append("\t\t} catch (java.lang.reflect.InvocationTargetException e) {\r\n");
			sb.append("\t\t\tthrow new PersistenceException(e.toString());\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t}\r\n");
		}
	}
}
