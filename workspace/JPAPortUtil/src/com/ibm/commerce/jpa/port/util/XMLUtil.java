package com.ibm.commerce.jpa.port.util;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class XMLUtil {
	private DocumentBuilder iDocumentBuilder = null;
	private LSSerializer iLSSerializer = null;
	private LSOutput iLSOutput = null;
	private DOMImplementationLS iDOMImplementationLS = null;

	public XMLUtil() {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			iDocumentBuilder = documentBuilderFactory.newDocumentBuilder();
			iDOMImplementationLS = (DOMImplementationLS) iDocumentBuilder.getDOMImplementation().getFeature("LS", "3.0");
			iLSOutput = iDOMImplementationLS.createLSOutput();
			iLSSerializer = iDOMImplementationLS.createLSSerializer();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Write out an XML document.
	 * @param document XML document
	 * @param file target file
	 */
	public void writeXml(Document document, IFile file, IProgressMonitor progressMonitor) {
		ByteArrayOutputStream outputStream = null;
		ByteArrayInputStream inputStream = null;
		try {
			outputStream = new ByteArrayOutputStream();
			iLSOutput.setByteStream(outputStream);
			iLSSerializer.write(document, iLSOutput);
			byte[] byteArray = outputStream.toByteArray();
			outputStream.close();
			inputStream = new ByteArrayInputStream(byteArray);
			file.setContents(inputStream, true, false, progressMonitor);
			inputStream = null;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Read an XML document.
	 * @param file the source file
	 * @return The parsed document.
	 */
	public Document readXml(IFile file) {
		Document document = null;
		try {
			document = readXml(file.getContents());
		}
		catch (Exception e) {
			System.out.println("problem reading "+file.getFullPath().toString()+" "+file.getName());
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * Read an XML document.
	 * @param file the source file
	 * @return the parsed document
	 */
	public Document readXml(File file) {
		Document document = null;
		try {
			document = iDocumentBuilder.parse(file);
		}
		catch (Exception e) {
			System.out.println("problem reading "+file);
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * Read an XML document from an input stream.
	 * @param inputStream the input stream
	 * @return the parsed document
	 */
	public Document readXml(InputStream inputStream) {
		Document document = null;
		try {
			document = iDocumentBuilder.parse(inputStream);
			inputStream.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * Create a new XML document.
	 * @return the new document
	 */
	public Document createXml() {
		return iDocumentBuilder.newDocument();
	}
	
	/**
	 * Merge the specified XML document into the specified file.
	 * @param document the document
	 * @param file the target file
	 */
	public void mergeXml(Document document, IFile file, IProgressMonitor progressMonitor) {
		Document targetDocument = readXml(file);
		Element targetElement = targetDocument.getDocumentElement();
		NodeList nodeList = document.getDocumentElement().getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			targetDocument.adoptNode(node);
			targetElement.appendChild(node);
			i--;
		}
		writeXml(targetDocument, file, progressMonitor);
	}
	
	public static String getElementText(Element element) {
		String text = "";
		Node node = element.getFirstChild();
		while (node != null) {
			text += ((Text)node).getData();
			node = node.getNextSibling();
		}
		return text;
	}
	
	public static int removeElement(Element element) {
		int newPosition = -1;
		Node parentNode = element.getParentNode();
		if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
			Element parentElement = (Element) parentNode;
			NodeList nodeList = parentElement.getChildNodes();
			Collection<Node> deletePendingNodes = new HashSet<Node>();
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i) == element) {
					parentElement.removeChild(element);
					newPosition = i - 1;
					while (newPosition >= 0) {
						Node node = nodeList.item(i);
						if (node == null) {
							System.out.println("newPosition="+newPosition+" length="+nodeList.getLength());
						}
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							break;
						}
						else {
							deletePendingNodes.add(node);
							newPosition--;
						}
					}
					break;
				}
			}
			for (Node node : deletePendingNodes) {
				parentElement.removeChild(node);
			}
		}
		return newPosition;
	}
}
