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

public class CatalogEntryEntityBaseGenerator {
	public static final String TYPE_NAME = "com.ibm.commerce.catalog.objimpl.CatalogEntryJPAEntityBase";
	
	public static class TypeSuffixFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "TYPE_SUFFIX";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\tprivate final static String TYPE_SUFFIX = \"Bean\";\r\n");
		}
	}
	
	public static class GetTypeMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "getType";
		}
		
		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\t/**\r\n");
			sb.append("\t * Type of this catalog entry\r\n");
			sb.append("\t * @return  java.lang.String\r\n");
			sb.append("\t */\r\n");
			sb.append("\t@Transient\r\n");
			sb.append("\tpublic String getType() {\r\n");
			sb.append("\t\tStringBuilder sb = new StringBuilder(this.getClass().getName());\r\n");
			sb.append("\t\tsb.append(TYPE_SUFFIX);\r\n");
			sb.append("\t\treturn sb.substring(sb.lastIndexOf(\".\") + 1);\r\n");
			sb.append("\t}\r\n");
		}
	}
}
