package ru.imobilco;

import java.util.ArrayList;
import java.util.List;

public class Tag {
	private String tag;
	private String name = "";
	private List<Tag> children = new ArrayList<Tag>();
	private List<String> attributes = new ArrayList<String>();
	private Tag parent;
	private int meta_line = 0;
	private int meta_column = -1;
	private String meta_xpath = null;
	private String meta_module = null;
	private String xpath = null;
	
	private String source_node;
	private int source_line = 0;
	private String source_file;
	
	public Tag(String tag_name) {
		this.tag = tag_name;
 	}
	
	public Tag(String tag_name, Tag parent) {
		this.tag = tag_name;
		this.parent = parent;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Add node's attribute
	 * @param name Attribute name
	 * @param value Attribute value
	 */
	public void addAttribute(String name, String value) {
		attributes.add("\"" + name + "\":\"" + value + "\"");
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
		children.add(child);
	}
	
	/**
	 * Add meta info about tag
	 * @param line Tag's line number in source
	 * @param col Tag's column number in source
	 * @param module Module (file) name where tag is
	 */
	public void addMetaInfo(int line, int col, String module) {
		meta_line = line;
		meta_column = col;
		meta_module = module;
	}
	
	/**
	 * Add meta info about tag
	 * @param line Tag's line number in source
	 * @param xpath Tag Xpath
	 * @param module Module (file) name where tag is
	 */
	public void addMetaInfo(int line, String xpath, String module) {
		meta_line = line;
		meta_xpath = xpath;
		meta_module = module;
	}
	
	public void addSource(String node, int line, String file) {
		source_node = node;
		source_line = line;
		source_file = file;
	}
	
	/**
	 * Output meta info as JSON string
	 * @return JSON string
	 */
	protected String metaToString() {
		if (meta_module != null) {
			String result = "\"meta\":{";
			result += "\"l\":" + meta_line + ",";
			if (meta_column >= 0) {
				result += "\"c\":" + meta_column + ",";
			}
			
			if (meta_xpath != null) {
				result += "\"x\":\"" + meta_xpath + "\",";
			}
			
			result += "\"m\":\"" + meta_module + "\"}";
			
			return result;
		}
		
		return null;
	}
	
	private String attributesToString() {
		if (attributes.size() > 0) {
			StringBuilder result = new StringBuilder();
			result.append("\"attrs\":{");
			
			for (int i = 0; i < attributes.size(); i++) {
				if (i != 0) {
					result.append(",");
				}
				result.append(attributes.get(i)); 
			}
			
			result.append("}");
			
			return result.toString();
		}
		
		return null;
	}
	
	private String sourceToString() {
		if (source_node != null) {
			return "\"src\":{" +
					"\"x\":\"" + source_node + "\"," +
					"\"l\":" + source_line + "," +
					"\"f\":\"" + source_file + "\"" +
					"}";
		} else {
			return null;
		}
	}
	
	public String toString() {
		String json_tag = "{"
				+ "\"tag\":\"" + tag + "\","
				+ "\"name\":\"" + name + "\",";
		
		String source = sourceToString();
		String attrs = attributesToString();
		String meta = metaToString();
		
		if (attrs != null) {
			json_tag += attrs + ",";
		}
		
		if (xpath != null) {
			json_tag += "\"lx\":\"" + xpath + "\",";
		}
		
		if (meta != null) {
			json_tag += meta + ",";
		}
		if (source != null) {
			json_tag += source + ",";
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("\"children\":[");
		for (int i = 0; i < children.size(); i++) {
			if (i != 0) {
				builder.append(',');
			}
			builder.append("" + children.get(i).toString()); 
		}
		builder.append("]");
		return json_tag + builder.toString() + "}";
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public String getXpath() {
		return xpath;
	}
}
