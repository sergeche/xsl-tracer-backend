package ru.imobilco;

import java.util.ArrayList;
import java.util.List;

public class Tag {
	private String type;
	private String name;
	private List<Tag> children = new ArrayList<Tag>();
	
	private Tag parent;
	private Tag previousSibling;
	private Tag nextSibling;
	
	private ResourceReference sourceRef;
	private ResourceReference contextRef;
	private ResourceReference templateRef;
	
	private String xpath = null;
	
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
		contextRef = new ResourceReference("xml", fileUri, xpath, lineNum);
	}
	
	public ResourceReference getContextReference() {
		return contextRef;
	}
	
	public ResourceReference getSourceReference() {
		return sourceRef;
	}
	
	public ResourceReference getTemplateReference() {
		return templateRef;
	}
	
	public String toString() {
		String result = "{"
			+ "\"name\":\"" + getName() + "\","
			+ "\"type\":\"" + getType() + "\",";
		
		if (xpath != null) {
			result += "\"xpath\":\"" + getXpath() + "\",";
		}
		
		if (contextRef != null) {
			result += "\"ctx\":\"" + getContextReference() + "\",";
		}
		
		if (sourceRef != null) {
			result += "\"src\":\"" + getSourceReference() + "\",";
		}
		
		if (templateRef != null) {
			result += "\"tmpl\":\"" + getTemplateReference() + "\",";
		}
		
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
			if (prev.getName() == curName)
				pos++;
			prev = prev.getPreviousSibling();
		}
		
		return pos;
	}
}
