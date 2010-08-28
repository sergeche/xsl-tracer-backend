package ru.imobilco;

import javax.xml.transform.TransformerException;

import org.xml.sax.Attributes;

import com.icl.saxon.output.Emitter;

public class JSONEmitter extends Emitter {
	private String xpathPrefix;
	private Tag root;
	private Tag curTag;
	
	// a little cache...
	protected int lastNameCode = -1;
	protected String lastDisplayName;
	
	@Override
	public void startDocument() throws TransformerException {
		root = new Tag("");
		curTag = root;
	}

	@Override
	public void endDocument() throws TransformerException {

	}

	@Override
	public void startElement(int nameCode, Attributes attributes,
			int[] namespaces, int nscount) throws TransformerException {
		
    	String displayName;
    	
		if (nameCode==lastNameCode) {
    		displayName = lastDisplayName;
    	} else {
	    	displayName = namePool.getDisplayName(nameCode);
	    	lastNameCode = nameCode;
    		lastDisplayName = displayName;
	    }
		
		Tag tag = new Tag(displayName, curTag);
		tag.setName(displayName);
		curTag.addChild(tag);
        curTag = tag;
        
        curTag.setXpath(xpathPrefix + Tag.getPath(curTag));
	}

	@Override
	public void endElement(int nameCode) throws TransformerException {
		curTag = curTag.getParent();
	}

	@Override
	public void characters(char[] chars, int start, int len)
			throws TransformerException {
	}

	@Override
	public void processingInstruction(String name, String data)
			throws TransformerException {
	}

	@Override
	public void comment(char[] chars, int start, int length)
			throws TransformerException {
	}

	public void setXpathPrefix(String xpathPrefix) {
		this.xpathPrefix = xpathPrefix;
	}

	public String getXpathPrefix() {
		return xpathPrefix;
	}
	
	public void copyTags(Tag parentTag) {
		for (Tag child : root.getChildren()) {
			parentTag.addChild(child);
		}
	}

}
