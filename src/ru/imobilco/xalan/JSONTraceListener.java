package ru.imobilco.xalan;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemTextLiteral;
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
	
	private PrintStream out = System.err;
    private List<String> allowedXslTags = new ArrayList<String>();
    private int skipTag = 0;
    
    public static final String TYPE_LRE = "LRE";
    public static final String TYPE_XSL = "XSL";
    
    protected RootTag root;
    protected Tag cur_tag;
	
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
	   * Print information about a TracerEvent.
	   *
	   * @param ev the trace event.
	   */
	public void _trace(TracerEvent ev) {
//		out.print("xsl: " + makeName(ev.m_styleNode) + " (" + ev.m_styleNode.getXSLToken() + "), ");
//		out.println("xml: " + ev.m_sourceNode.getLocalName() + "(" + ev.m_sourceNode.getNodeType() + ")");
		

		switch (ev.m_styleNode.getXSLToken()) {
		case Constants.ELEMNAME_TEXTLITERALRESULT:
				out.print(ev.m_styleNode.getSystemId() + " Line #"
						+ ev.m_styleNode.getLineNumber() + ", " + "Column #"
						+ ev.m_styleNode.getColumnNumber() + " -- "
						+ ev.m_styleNode.getNodeName() + ": ");

				ElemTextLiteral etl = (ElemTextLiteral) ev.m_styleNode;
				String chars = new String(etl.getChars(), 0,
						etl.getChars().length);

				out.println("    " + chars.trim());
			break;
		case Constants.ELEMNAME_TEMPLATE:
			ElemTemplate et = (ElemTemplate) ev.m_styleNode;
			
			out.print(et.getSystemId() + " Line #" + et.getLineNumber()
					+ ", " + "Column #" + et.getColumnNumber() + ": "
					+ et.getNodeName() + " ");
			
			if (null != et.getMatch()) {
				out.print("match='" + et.getMatch().getPatternString() + "' ");
			}
			
			if (null != et.getName()) {
				out.print("name='" + et.getName() + "' ");
			}
			
			out.println();
			break;
		default:
			out.println(ev.m_styleNode.getSystemId() + " Line #"
					+ ev.m_styleNode.getLineNumber() + ", " + "Column #"
					+ ev.m_styleNode.getColumnNumber() + ": "
					+ ev.m_styleNode.getNodeName());
		}
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
			tag.setStartTime(System.currentTimeMillis());
			tag.setType(getNodeType(style));
			cur_tag.addChild(tag);
			
//			// TODO get correct source reference
//			if (style.getSystemId() == null) {
//				_trace(ev);
//			}
			
			
			
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
				
//				out.println(source.toString() + ", " + ev.m_processor.getBaseURLOfSource());
				
				tag.setContextReference(src, getPath(source), -1);
			}
			
            cur_tag = tag;
		}
		
//		out.print("OPEN ");
//		_trace(ev);
		
//		if (!isLRE(style)) {
//			out.print("OPEN ");
//			_trace(ev);
//		}
	}

	@Override
	public void traceEnd(TracerEvent ev) {
		ElemTemplateElement style = ev.m_styleNode;
		if (style.getXSLToken() == Constants.ELEMNAME_VARIABLE) {
    		skipTag--;
		} else if (skipTag == 0 && isAllowedElement(style)) {
			cur_tag.setEndTime(System.currentTimeMillis());
			cur_tag = cur_tag.getParent();
    	}
		
//		if (!isLRE(ev.m_styleNode)) {
//			out.print("CLOSE ");
//			_trace(ev);
//		}
	}
	
	@Override
	public void selected(SelectionEvent ev) throws TransformerException {
		// TODO Auto-generated method stub
	}

	@Override
	public void selectEnd(EndSelectionEvent ev) throws TransformerException {
		// TODO Auto-generated method stub

	}

	@Override
	public void generated(GenerateEvent ev) {
//		switch (ev.m_eventtype) {
//			case SerializerTrace.EVENTTYPE_STARTDOCUMENT:
////				cur_tag = root;
//		    	root.setStartTime(System.currentTimeMillis());
//				break;
//			case SerializerTrace.EVENTTYPE_ENDDOCUMENT:
//				root.setEndTime(System.currentTimeMillis());
//		    	out.println(root.toString());
//		    	break;
//	
//			default:
//				break;
//		}
	}

	@Override
	public void extension(ExtensionEvent ee) {
		// TODO Auto-generated method stub

	}

	@Override
	public void extensionEnd(ExtensionEvent ee) {
		// TODO Auto-generated method stub

	}
	
	public String toJSON() {
		return root.toString();
	}
}
