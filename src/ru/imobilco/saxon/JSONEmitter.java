package ru.imobilco.saxon;

import javax.xml.transform.TransformerException;

import org.xml.sax.Attributes;

import ru.imobilco.Tag;

import com.icl.saxon.om.Navigator;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.output.Emitter;
import com.icl.saxon.style.StyleElement;

public class JSONEmitter extends Emitter {
	private String xpathPrefix;
	private NodeInfo sourceElement;
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
		
		Tag tag = new Tag(displayName);
		tag.setName(displayName);
		curTag.addChild(tag);
		
		String collectionName = (sourceElement instanceof StyleElement) ? "xsl" : "xml";
		tag.setXpath(Tag.getPath(tag));
		tag.setType(JSONTraceListener.TYPE_LRE);
		tag.setSourceReference(collectionName, sourceElement.getSystemId(), 
				Navigator.getPath(sourceElement) + tag.getXpath(), 
				sourceElement.getLineNumber());
		
        
        curTag = tag;
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

	public void setSourceElement(NodeInfo sourceElement) {
		this.sourceElement = sourceElement;
	}

	public NodeInfo getSourceElement() {
		return sourceElement;
	}
	
	public Tag getRootTag() {
		return root;
	}

}
