package ru.imobilco;

public class RootTag extends Tag {
	private String basePath = "";
	
	public RootTag(String name) {
		super(name);
	}
	
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getBasePath() {
		return basePath;
	}

	protected String collectionsToString() {
		
		return ResourceReference.collectionToJSON("xsl", getBasePath()) + 
			"," +
			ResourceReference.collectionToJSON("xml", getBasePath()) + 
			",";
	}
}
