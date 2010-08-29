package ru.imobilco;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class RootTag extends Tag {
	private String basePath = "";
	
	private URI baseXml;
    private URI baseXsl;
	
	public RootTag(String name) {
		super(name);
	}
	
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getBasePath() {
		return basePath;
	}
	
	/**
     * Setup base XML and XSL files used for transformation
     * @param xmlFile
     * @param xslFile
     */
	public void setBaseFiles(String xmlFile, String xslFile) {
		try {
			baseXml = new URI(xmlFile);
		} catch (URISyntaxException e) {}
		try {
			baseXsl = new URI(xslFile);
		} catch (URISyntaxException e) {}
	}
	
	private String outputXslCollection() {
		String baseXslPath = "";
		if (baseXsl != null)
			baseXslPath = new File(baseXsl).getParent() + "/";
		
		return ResourceReference.collectionToJSON("xsl", baseXslPath);
	}
	
	private String outputXmlCollection() {
		String sourceXml = "";
		String baseXmlPath = "";
		String result = "";
		
		if (baseXml != null) {
			sourceXml = baseXml.toString();
		}
		
		// basically, all external xml files (except SOURCE) are included with
		// document() xpath function, so we have to use baseXsl as staring point
		if (baseXsl != null) {
			baseXmlPath = new File(baseXsl).getParent() + "/";
		}
		
		// output xml collection in other way
		result += "\"xml\":[";
		List<String> files = ResourceReference.getCollection("xml");
		String file;
		
		if (files != null) {
			for (int i = 0; i < files.size(); i++) {
				if (i > 0)
					result += ",";
				file = files.get(i);
				if (file.equals(sourceXml))
					result += "\"SOURCE\"";
				else
					result += "\"" + ResourceReference.relativizePath(file, baseXmlPath) + "\"";
			}
		}
		
		result += "]";
		
		return result;
	}

	protected String collectionsToString() {
		return outputXslCollection() + "," + outputXmlCollection() + ",";
	}
}
