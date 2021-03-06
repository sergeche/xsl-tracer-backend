package ru.imobilco;

import java.util.ArrayList;
import java.util.List;

import ru.imobilco.saxon.JSONTraceListener;

public class Tag {
	private String type = "";
	private String name;
	private List<Tag> children = new ArrayList<Tag>();
	
	private Tag parent;
	private Tag previousSibling;
	private Tag nextSibling;
	
	private ResourceReference sourceRef;
	private ResourceReference contextRef;
	
	private long startTime = 0;
	private long endTime = 0;
	
	private String xpath = "";
	
	public Tag(String name) {
		this.name = name;
 	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
	
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public String getXpath() {
		return xpath;
	}
	
	public Tag getPreviousSibling() {
		return previousSibling;
	}
	
	public Tag getNextSibling() {
		return nextSibling;
	}
	
	public List<Tag> getChildren() {
		return children;
	}

	/**
	 * Returns tag's parent
	 * @return Tag
	 */
	public Tag getParent() {
		return parent;
	}
	
	/**
	 * Returns root tag
	 * @return
	 */
	public RootTag getRoot() {
		Tag tag = getParent();
		while (tag.getParent() != null)
			tag = tag.getParent();
		
		if (tag != null && tag instanceof RootTag)
			return (RootTag) tag;
		
		return null;
	}
	
	/**
	 * Add child tag
	 * @param child
	 */
	public void addChild(Tag child) {
		child.previousSibling = null;
		child.nextSibling = null;
		
		if (children.size() > 0) {
			Tag lastChild = children.get(children.size() - 1);
			child.previousSibling = lastChild;
			lastChild.nextSibling = child;
		}
		
		children.add(child);
		child.parent = this;
	}
	
	public void setContextReference(String fileUri, String xpath, int lineNum) {
		contextRef = new ResourceReference("xml", fileUri, xpath, lineNum, getRoot());
	}
	
	public ResourceReference getContextReference() {
		return contextRef;
	}
	
	public void setSourceReference(String collectionName, String fileUri, String xpath, int lineNum) {
		sourceRef = new ResourceReference(collectionName, fileUri, xpath, lineNum, getRoot());
	}
	
	public ResourceReference getSourceReference() {
		return sourceRef;
	}
	
	protected String collectionsToString() {
		String result = "";
		
		if (contextRef != null) {
			result += "\"ctx\":" + getContextReference() + ",";
		}
		
		if (sourceRef != null) {
			result += "\"src\":" + getSourceReference() + ",";
		}
		
		return result;
	}
	
	public String toString() {
		String result = "{"
			+ "\"name\":\"" + getName() + "\","
			+ "\"type\":\"" + getType() + "\","
			+ "\"time\":" + Math.max(endTime - startTime, 0) + ",";
		
		result += collectionsToString();
		
		StringBuilder builder = new StringBuilder();
		builder.append("\"children\":[");
		for (int i = 0; i < children.size(); i++) {
			if (i != 0) {
				builder.append(',');
			}
			builder.append(children.get(i).toString()); 
		}
		builder.append("]");
		return result + builder.toString() + "}";
	}
	
	public String getParentResultXpath() {
		// find nearest LRE ancestor and get its xpath
		String prefix = "";
		Tag parent = this.getParent();
		while (parent != null) {
			if (JSONTraceListener.TYPE_LRE.equals(parent.getType())) {
				prefix = parent.getXpath();
				break;
			}
			parent = parent.getParent();
		}
		
		return prefix;
	}
	
	/**
	 * Copy tags from another subset into current tag, modifying result xpath
	 * @param subset
	 */
	public void copyTags(Tag subset) {
		// find nearest LRE ancestor and use its xpath as prefix
		for (Tag child : subset.getChildren()) {
			this.addChild(child);
		}
		
//		updateXPath(getParentResultXpath(), subset);
	}

	public static String getPath(Tag tag) {
		String pre;
		if (tag.getParent() == null)
			return "/";
		else {
			pre = getPath(tag.getParent());
			return (pre.equals("/") ? "" : pre) + 
            	"/" + tag.getName() + "[" + getNumberSimple(tag) + "]";
		}
	}
	
	public static int getNumberSimple(Tag tag) {
		String curName = tag.getName();
		int pos = 1;
		Tag prev = tag.getPreviousSibling();
		while (prev != null) {
			if (curName.equals(prev.getName()))
				pos++;
			prev = prev.getPreviousSibling();
		}
		
		return pos;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long time) {
		this.startTime = time;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long time) {
		this.endTime = time;
	}
}
