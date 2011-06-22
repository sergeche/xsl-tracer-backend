package ru.imobilco;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RootTag extends Tag {
	
	private Properties collections = new Properties();
	
	public RootTag(String name) {
		super(name);
	}
	
	@SuppressWarnings("unchecked")
	private String outputXslCollection() {
		return ResourceReference.collectionToJSON("xsl", (List<String>) collections.get("xsl"));
	}
	
	@SuppressWarnings("unchecked")
	private String outputXmlCollection() {
		return ResourceReference.collectionToJSON("xml", (List<String>) collections.get("xml"));
	}

	protected String collectionsToString() {
		return outputXslCollection() + "," + outputXmlCollection() + ",";
	}
	
	public int addToCollection(ResourceReference resource) {
		if (collections.get(resource.getCollectionName()) == null) {
			collections.put(resource.getCollectionName(), new ArrayList<String>());
		}
		
		@SuppressWarnings("unchecked")
		List<String> collection = (List<String>) collections.get(resource.getCollectionName());
		
		int ix = collection.indexOf(resource.getFileUri());
		if (ix == -1) {
			collection.add(resource.getFileUri());
			ix = collection.size() - 1;
		}
		
		return ix;
	}
	
	public String toString() {
		if (getEndTime() < getStartTime())
			setEndTime(System.currentTimeMillis());
		return super.toString();
	}
}
