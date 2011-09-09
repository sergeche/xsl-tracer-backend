package ru.imobilco.xalan;

import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.trace.EndSelectionEvent;
import org.apache.xalan.trace.ExtensionEvent;
import org.apache.xalan.trace.GenerateEvent;
import org.apache.xalan.trace.SelectionEvent;
import org.apache.xalan.trace.TraceListenerEx3;
import org.apache.xalan.trace.TracerEvent;
import org.apache.xml.dtm.ref.DTMNodeProxy;
import org.apache.xml.serializer.SerializerTrace;
import org.w3c.dom.Node;

import ru.imobilco.RootTag;
import ru.imobilco.Tag;

public class JSONTraceListener implements TraceListenerEx3 {
    private List<String> allowedXslTags = new ArrayList<String>();
    private int skipTag = 0;
    
    public static final String TYPE_LRE = "LRE";
    public static final String TYPE_XSL = "XSL";
    
    protected RootTag root;
    protected Tag cur_tag;
	private boolean collectGenerated;
	
	public JSONTraceListener() {
		root = new RootTag("root");
		
		allowedXslTags.add("xsl:template");
		allowedXslTags.add("xsl:call-template");
		allowedXslTags.add("xsl:apply-templates");
		allowedXslTags.add("xsl:apply-imports");
		allowedXslTags.add("xsl:element");
		allowedXslTags.add("xsl:copy-of");
		allowedXslTags.add("xsl:copy");
	}
	
	/**
    * Get an absolute XPath expression that identifies a given node within its document
    */
    public static String getPath(Node node) {
        String pre;
        if (node == null)
        	return "";
        
        
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
            	if (makeName(node).equals("xsl:stylesheet"))
					// a bit weird structure of XSL document
            		return "/xsl:stylesheet[1]";
            	
                return "/";
            case Node.ELEMENT_NODE:
                pre = getPath(node.getParentNode());
                return (pre.equals("/") ? "" : pre) +
                        "/" + makeName(node) + "[" + getNumberSimple(node) + "]";
            case Node.ATTRIBUTE_NODE:
                return getPath(node.getParentNode()) + "/@" + node.getLocalName();
            case Node.TEXT_NODE:
                pre = getPath(node.getParentNode());
                return (pre.equals("/") ? "" : pre) +
                        "/text()[" + getNumberSimple(node) + "]";
            case Node.COMMENT_NODE:
                pre = getPath(node.getParentNode());
                return (pre.equals("/") ? "" : pre) +
                    "/comment()[" + getNumberSimple(node) + "]";
            case Node.PROCESSING_INSTRUCTION_NODE:
                pre = getPath(node.getParentNode());
                return (pre.equals("/") ? "" : pre) +
                    "/processing-instruction()[" + getNumberSimple(node) + "]";
            default:
                return "";
        }
    }
    
    /**
     * Get simple node number. This is defined as one plus the number of previous siblings of the
     * same node type and name. It is not accessible directly in XSL.
     * @param context Used for remembering previous result, for performance
     */
     public static int getNumberSimple(Node node) {
    	 int i = 1;
         String name = makeName(node);
         
         Node prevNode = node;
         do {
        	prevNode = prevNode.getPreviousSibling();
			if (prevNode != null && makeName(prevNode).equals(name))
				i++;
         } while (prevNode != null);
         
         return i;
     }
	
	/**
     * Creates element's display name. It handles xsl:element properly, 
     * returning expanded element name. 
     * @param node
     * @return
     */
    public static String makeName(Node node) {
		String name = node.getNodeName();
		String prefix = "";
		
		if (node instanceof ElemTemplateElement && !isLRE((ElemTemplateElement) node))
			prefix = "xsl:";
		
		return prefix + name;
	}

    /**
     * Check if passed element is literal result
     * @param elem
     * @return
     */
	private static boolean isLRE(ElemTemplateElement elem) {
		return elem.getXSLToken() == Constants.ELEMNAME_TEXTLITERALRESULT
				|| elem.getXSLToken() == Constants.ELEMNAME_LITERALRESULT;
	}
	
	private boolean isAllowedElement(ElemTemplateElement elem) {
		return elem.getNodeType() == Node.ELEMENT_NODE
				&& elem.getSystemId() != null
				&& (isLRE(elem) || allowedXslTags.contains(makeName(elem)));
	}
	
	/**
     * Get the trace element tagname to be used for a particular construct. Return null for
     * trace events that are ignored by this trace listener.
     */
    protected String getNodeType(ElemTemplateElement elem) {
        if (isLRE(elem)) {
        	return TYPE_LRE;
        } else {
        	return TYPE_XSL;
        }
    }
    
	@Override
	public void trace(TracerEvent ev) {
		ElemTemplateElement style = ev.m_styleNode;
		Node source = ev.m_sourceNode;
		
		if (cur_tag == null) {
			cur_tag = root;
			root.setStartTime(System.currentTimeMillis());
		}
		
		if (style.getXSLToken() == Constants.ELEMNAME_VARIABLE) {
			skipTag++;
		} else if (skipTag == 0 && isAllowedElement(style)) {
			Tag tag = new Tag(makeName(style));
			tag.setType(getNodeType(style));
			cur_tag.addChild(tag);
			
			tag.setSourceReference("xsl", style.getSystemId(), getPath(style), style.getLineNumber());
			if (source.getNodeType() == Node.ELEMENT_NODE) {
				String src = "";
				SourceLocator locator = null;
				if (source instanceof DTMNodeProxy) {
					int nodeHandler = ((DTMNodeProxy) source).getDTMNodeNumber();
					locator = ((DTMNodeProxy) source).getDTM().getSourceLocatorFor(nodeHandler);
				}
				
				if (locator != null)
					src = locator.getSystemId();
				
				tag.setContextReference(src, getPath(source), -1);
			}
			
            cur_tag = tag;
            
            if (style.getXSLToken() == Constants.ELEMNAME_COPY_OF) {
            	collectGenerated = true;
            }
            
            tag.setStartTime(System.currentTimeMillis());
		}
	}

	@Override
	public void traceEnd(TracerEvent ev) {
		ElemTemplateElement style = ev.m_styleNode;
		if (style.getXSLToken() == Constants.ELEMNAME_VARIABLE) {
    		skipTag--;
		} else if (skipTag == 0 && isAllowedElement(style)) {
			cur_tag.setEndTime(System.currentTimeMillis());
			cur_tag = cur_tag.getParent();
			
			if (style.getXSLToken() == Constants.ELEMNAME_COPY_OF) {
            	collectGenerated = false;
            }
    	}
	}

	@Override
	public void generated(GenerateEvent ev) {
		if (collectGenerated) {
			switch (ev.m_eventtype) {
				case SerializerTrace.EVENTTYPE_STARTELEMENT:
					Tag tag = new Tag(ev.m_name);
					tag.setType(TYPE_LRE);
					cur_tag.addChild(tag);
					cur_tag = tag;
					tag.setStartTime(System.currentTimeMillis());
					break;
				case SerializerTrace.EVENTTYPE_ENDELEMENT:
					cur_tag.setEndTime(System.currentTimeMillis());
					cur_tag = cur_tag.getParent();
					break;
				default:
					break;
			}
		}
	}
	
	
	@Override
	public void selected(SelectionEvent ev) throws TransformerException {
		
	}

	@Override
	public void selectEnd(EndSelectionEvent ev) throws TransformerException {

	}

	@Override
	public void extension(ExtensionEvent ee) {

	}

	@Override
	public void extensionEnd(ExtensionEvent ee) {

	}
	
	public String toJSON() {
		return root.toString();
	}
}
