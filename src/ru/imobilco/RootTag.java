package ru.imobilco;

public class RootTag extends Tag {
	
	public RootTag(String name) {
		super(name);
	}
	
	private String outputXslCollection() {
		return ResourceReference.collectionToJSON("xsl");
	}
	
	private String outputXmlCollection() {
		return ResourceReference.collectionToJSON("xml");
	}

	protected String collectionsToString() {
		return outputXslCollection() + "," + outputXmlCollection() + ",";
	}
}
