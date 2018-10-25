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

public class PAttrAttachmentValueEntityGenerator {
	public static final String TYPE_NAME = "com.ibm.commerce.utf.objects.PAttrAttachmentValueJPAEntity";
	
	public static class SetPAttrValueMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "setPAttrValue+java.lang.Object";
		}
		
		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\t/**\r\n");
			sb.append("\t * The attribute value\r\n");
			sb.append("\t * @param aValue  Object\r\n");
			sb.append("\t */\r\n");
			sb.append("\tpublic void setPAttrValue(Object aValue) {\r\n");
			sb.append("\t\tString val = (String) aValue;\r\n");
			sb.append("\t\tif (val != null && val.length() != 0) {\r\n");
			sb.append("\t\t\tcom.ibm.commerce.contract.objects.AttachmentJPAAccessBean attachmentAccessBean = new com.ibm.commerce.contract.objects.AttachmentJPAAccessBean();\r\n");
			sb.append("\t\t\tattachmentAccessBean.setInitKey_attachmentId(val);\r\n");
			sb.append("\t\t\tsetAttachment(attachmentAccessBean.getEntity());\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\telse {\r\n");
			sb.append("\t\t\tsetAttachment(null);\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t}\r\n");
		}
	}
}
