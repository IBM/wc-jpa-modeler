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

public class EmailConfigurationEntityBaseGenerator {
	public static final String TYPE_NAME = "com.ibm.commerce.emarketing.objimpl.EmailConfigurationJPAEntityBase";

//	public static final Integer TYPE_OUTBOUND = new Integer(0);
//	public static final Integer TYPE_INBOUND = new Integer(1);
	
	public static class TypeOutboundFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "TYPE_OUTBOUND";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\tpublic static final Integer TYPE_OUTBOUND = new Integer(0);\r\n");
		}
	}

	public static class TypeInboundFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "TYPE_INBOUND";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\tpublic static final Integer TYPE_INBOUND = new Integer(1);\r\n");
		}
	}
}
