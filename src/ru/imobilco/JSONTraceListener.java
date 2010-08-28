package ru.imobilco;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.TransformerConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.icl.saxon.Bindery;
import com.icl.saxon.Binding;
import com.icl.saxon.Context;
import com.icl.saxon.NodeHandler;
import com.icl.saxon.expr.Expression;
import com.icl.saxon.expr.FragmentValue;
import com.icl.saxon.expr.NodeSetExpression;
import com.icl.saxon.expr.NodeSetValue;
import com.icl.saxon.expr.TextFragmentValue;
import com.icl.saxon.expr.Value;
import com.icl.saxon.expr.VariableReference;
import com.icl.saxon.expr.XPathException;
import com.icl.saxon.om.AbstractNode;
import com.icl.saxon.om.Axis;
import com.icl.saxon.om.NamePool;
import com.icl.saxon.om.Navigator;
import com.icl.saxon.om.NodeEnumeration;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.output.Emitter;
import com.icl.saxon.output.GeneralOutputter;
import com.icl.saxon.output.NamespaceEmitter;
import com.icl.saxon.output.Outputter;
import com.icl.saxon.output.StringOutputter;
import com.icl.saxon.output.XMLEmitter;
import com.icl.saxon.pattern.NameTest;
import com.icl.saxon.pattern.NodeTest;
import com.icl.saxon.pattern.NodeTypeTest;
import com.icl.saxon.style.LiteralResultElement;
import com.icl.saxon.style.StyleElement;
import com.icl.saxon.style.XSLCopyOf;
import com.icl.saxon.style.XSLElement;
import com.icl.saxon.style.XSLVariable;
import com.icl.saxon.trace.TraceListener;

/**
 * This is the standard trace listener used when the -T option is specified on the command line.
 * There are two variants, represented by subclasses: one for XSLT, and one for XQuery. The two variants
 * differ in that they present the trace output in terms of constructs used in the relevant host language.
 */

public class JSONTraceListener implements TraceListener {
    private PrintStream out = System.err;
    private NodeInfo tmp_source;
    private List<String> allowedXslTags = new ArrayList<String>();
    
    private NamespaceEmitter emitter;
    private JSONEmitter jsonEmitter;
    private StringWriter buf;
    
    protected RootTag root;
    protected Tag cur_tag;
    
    public JSONTraceListener() {
		this.root = new RootTag("root");
		allowedXslTags.add("xsl:template");
		allowedXslTags.add("xsl:call-template");
		allowedXslTags.add("xsl:apply-templates");
		allowedXslTags.add("xsl:apply-imports");
		allowedXslTags.add("xsl:element");
		
		jsonEmitter = new JSONEmitter();
    	buf = new StringWriter();
    	jsonEmitter.setWriter(buf);
    	
    	emitter = new NamespaceEmitter();
    	emitter.setUnderlyingEmitter(jsonEmitter);
    	emitter.setNamePool(NamePool.getDefaultNamePool());
	}
	
	/**
     * Called at start
     */
    public void open() {
    	cur_tag = root;
    }

    /**
     * Called at end
     */
    public void close() {
    	out.println(root.toString());
    }
    
    public void toplevel(NodeInfo element) {
    	
    }
    
    /**
     * Called when a node of the source tree gets processed
     */
    public void enterSource(NodeHandler handler, Context context) {
    	tmp_source = context.getContextNodeInfo();
    }
    
    public void leaveSource(NodeHandler handler, Context context) {
    	
    }
    
    /**
     * Called when an element of the stylesheet gets processed
     */
    public void enter(NodeInfo element, Context context) {
    	if (element.getNodeType() == NodeInfo.ELEMENT && allowedElement(element)) {
    		String type = this.getNodeType(element);
    		
    		Tag json_tag = new Tag(makeName(element, context));
    		json_tag.setType(type);
    		
    		if (json_tag.getType() == "LRE")
    			json_tag.setXpath(getLREPath(element, context, null));
    		
//    		json_tag.addMetaInfo(element.getLineNumber(), Navigator.getPath(element), root.getExternal(element.getSystemId(), "xsl"));
            
            if (tmp_source != null) {
            	json_tag.setContextReference(tmp_source.getSystemId(), Navigator.getPath(tmp_source), tmp_source.getLineNumber());
            	tmp_source = null;
            }
            
            cur_tag.addChild(json_tag);
            cur_tag = json_tag;
    	}
    }
    
    /**
     * Called after an instruction of the stylesheet got processed
     */

    public void leave(NodeInfo element, Context context) {
        if (element.getNodeType() == NodeInfo.ELEMENT && allowedElement(element)) {
        	if (element instanceof XSLCopyOf)
        		cur_tag.setXpath(processCopyOf((XSLCopyOf) element, context));
        	
        	cur_tag = cur_tag.getParent();
        	
        }
    }
    

    private boolean allowedElement(NodeInfo element) {
    	return true;
//    	String name = tag(element);
//		return name == "L" || xslTags.contains(name);
	}
    
    /**
     * Get the trace element tagname to be used for a particular construct. Return null for
     * trace events that are ignored by this trace listener.
     */
    protected String getNodeType(NodeInfo info) {
        if (info instanceof LiteralResultElement || info instanceof XSLElement) {
        	return "LRE";
        } else {
        	return "XSL";
        }
    }

    /**
     * Set the output destination (default is System.err)
     * @param stream the output destination for tracing output
     */

    public void setOutputDestination(PrintStream stream) {
        out = stream;
    }

    /**
     * Get the output destination
     */

    public PrintStream getOutputDestination() {
        return out;
    }
    
    /**
     * Creates element's display name. It handles xsl:element properly, 
     * returning expanded element name. 
     * @param node
     * @param context
     * @return
     */
    private String makeName(NodeInfo node, Context context) {
		if (node instanceof XSLElement) {
			XSLElement elem = (XSLElement) node;
			String nameAttr = elem.getAttribute("name");
			if (nameAttr != null) {
				Expression elementName;
				try {
					elementName = elem.makeAttributeValueTemplate(nameAttr);
					return elementName.evaluateAsString(context);
				} catch (Exception e) {
					return nameAttr;
				}
			} else {
				return nameAttr;
			}
		} else {
			return node.getDisplayName();
		}
	}
    
    /**
     * Get an absolute XPath expression that identifies a given node within 
     * result document
     */

     public String getLREPath(NodeInfo node, Context context, NodeInfo parent) {
         String xpath = "";
         NodeInfo iterNode = node;
         do {
        	 if (iterNode.getNodeType() == NodeInfo.ELEMENT) {
        		 if (iterNode instanceof LiteralResultElement || iterNode instanceof XSLElement) {
        			 xpath = "/" + makeName(iterNode, context) + "[" + getLRENumber(iterNode, context) + "]" + xpath;
        		 }
        	 }
        	 
        	 iterNode = iterNode.getParent();
         } while(iterNode != parent);
         
         return xpath;
     }
     
     /**
      * Get simple node number. This is defined as one plus the number of previous siblings of the
      * same node type and name. It is not accessible directly in XSL. This version doesn't require
      * the context, and therefore doesn't remember previous results
      */

      public int getLRENumber(NodeInfo node, Context context) {
    	  int pos = 1;
    	  String curName = makeName(node, context);
    	  if (node instanceof Node) {
    		  Node prev = (Node) node;
    		  prev = prev.getPreviousSibling();
    		  while (prev != null) {
    			  if (curName.equals(makeName((NodeInfo) prev, context))) {
    				  pos++;
    			  }
    			  prev = prev.getPreviousSibling();
    		  }
    	  }
    	  
    	  return pos;
      }
      
      private String processCopyOf(XSLCopyOf node, Context context) {
    	  String selectAttr = node.getAttribute("select");
    	  String result = "";
    	  if (selectAttr != null) {
    		  try {
				Expression select = node.makeExpression(selectAttr);
				
				if (select instanceof NodeSetExpression) {
					NodeEnumeration enm = select.enumerate(context, true);
			    	while (enm.hasMoreElements()) {
			    		NodeInfo n = enm.nextElement();
			    		result += n.getDisplayName() + ";";
//			    		n.copy(out);
			    	}
		    		return result + " nse";
		    	} else {
			        Value value = select.evaluate(context);
			        if (value instanceof FragmentValue) {
			        	String prefix = "";
			        	
			        	if (select instanceof VariableReference) {
			        		Bindery bindery = context.getBindery();
			        		Value val = bindery.getValue(((VariableReference)select).getBinding());
			        		
			        		Binding binding = ((VariableReference) select).getBinding();
			        		if (binding instanceof XSLVariable) {
			        			prefix = Navigator.getPath((NodeInfo) binding);
			        		} else {
			        			prefix = ((VariableReference) select).getBinding().toString();
			        		}
			        		
			        	}
			        	
			        	buf.getBuffer().setLength(0);
			        	jsonEmitter.setXpathPrefix(prefix);
			        	
			            emitter.startDocument();
			            ((FragmentValue)value).replay(emitter);
			            emitter.endDocument();
			            jsonEmitter.copyTags(cur_tag);
			            
			        	result += prefix +  ";" + escapeHTML(buf.toString());
			        }
			    }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		  
    	  }
    	  
    	  return result;
      }
      
      public static String escapeHTML(String text) {
    	  final StringBuilder result = new StringBuilder();
    	  final StringCharacterIterator iterator = new StringCharacterIterator(text);
    	  char character =  iterator.current();
    	  while (character != CharacterIterator.DONE ) {
    		  if (character == '\'') {
    			  result.append("\\'");
    		  } else if (character == '"') {
    			  result.append("\\\"");
    		  } else if (character != '\n' && character != '\r') {
    			  result.append(character);
    		  }
    		  
    		  character = iterator.next();
    	  }
    	  
    	  return result.toString();
      }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//