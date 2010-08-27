package ru.imobilco;


import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.value.Whitespace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

/**
 * This is the standard trace listener used when the -T option is specified on the command line.
 * There are two variants, represented by subclasses: one for XSLT, and one for XQuery. The two variants
 * differ in that they present the trace output in terms of constructs used in the relevant host language.
 */

public class JSONTraceListener implements TraceListener {
    private PrintStream out = System.err;
    private NodeInfo tmp_source;
    private String fileName;
    private String basePath;
    private String baseSourcePath;
    protected RootTag root;
    protected Tag cur_tag;
    
    public JSONTraceListener() {
		this.root = new RootTag("root");
	}
    
    public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
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
    	if (getFileName() != null)
    		saveToFile();
    	else
    		out.println(root.toString());
    }
    
    /**
     * Called when an instruction in the stylesheet gets processed
     */

    public void enter(InstructionInfo info, XPathContext context) {
        int infotype = info.getConstructType();
        StructuredQName qName = info.getObjectName();
        String tag = tag(infotype);
        
        if (basePath == null) {
        	basePath = new File(info.getSystemId()).getParent() + "/";
        	
        	try {
				basePath = new URI(basePath).getPath();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			root.setBasePath(basePath);
        }
        
        if (tag==null) {
            // this TraceListener ignores some events to reduce the volume of output
            return;
        }
//        String file = ExpressionLocation.truncateURI(info.getSystemId());
        
        Tag json_tag = new Tag(tag, cur_tag);
        
        String name = (String)info.getProperty("name");
        if (name!=null) {
        	json_tag.setName(escape(name));
        } else if (qName != null) {
        	json_tag.setName(escape(qName.getDisplayName()));
        }
        
        Iterator props = info.getProperties();
        while (props.hasNext()) {
            String prop = (String)props.next();
            Object val = info.getProperty(prop);
            if (prop.startsWith("{")) {
                // It's a QName in Clark notation: we'll strip off the namespace
                int rcurly = prop.indexOf('}');
                if (rcurly > 0) {
                    prop = prop.substring(rcurly+1);
                }
            }
            if (val != null && !prop.equals("name") && !prop.equals("expression")) {
            	json_tag.addAttribute(prop, escape(val.toString()));
            }
        }
        
        json_tag.addMetaInfo(info.getLineNumber(), info.getColumnNumber(), root.getExternal(info.getSystemId(), "xsl"));
        
        if (tmp_source != null) {
        	json_tag.addSource(Navigator.getPath(tmp_source), tmp_source.getLineNumber(), 
        			baseSourcePath != tmp_source.getSystemId() 
        			? root.getExternal(tmp_source.getSystemId(), "xml")
        			: "SOURCE");
        	tmp_source = null;
        }
        
        cur_tag.addChild(json_tag);
        cur_tag = json_tag;
    }

    /**
     * Escape a string for XML output (in an attribute delimited by double quotes).
     * This method also collapses whitespace (since the value may be an XPath expression that
     * was originally written over several lines).
     */

    public String escape(String in) {
        if (in==null) {
            return "";
        }
        CharSequence collapsed = Whitespace.collapseWhitespace(in);
        StringBuffer sb = new StringBuffer(collapsed.length() + 10);
        for (int i=0; i<collapsed.length(); i++) {
            char c = collapsed.charAt(i);
            if (c=='"') {
                sb.append("\\\"");
            } else if (c=='\n') {
                sb.append("\\n");
            } else if (c=='\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Called after an instruction of the stylesheet got processed
     */

    public void leave(InstructionInfo info) {
        int infotype = info.getConstructType();
        String tag = tag(infotype);
        if (tag==null) {
            // this TraceListener ignores some events to reduce the volume of output
            return;
        }
        
        cur_tag = cur_tag.getParent();
    }

    /**
     * Get the trace element tagname to be used for a particular construct. Return null for
     * trace events that are ignored by this trace listener.
     */
    protected String tag(int construct) {
        if (construct < 1024) {
            return StandardNames.getDisplayName(construct);
        }
        switch (construct) {
            case Location.LITERAL_RESULT_ELEMENT:
                return "L";
            case Location.LITERAL_RESULT_ATTRIBUTE:
                return "A";
            case Location.LET_EXPRESSION:
                return "xsl:variable";
            case Location.EXTENSION_INSTRUCTION:
                return "extension-instruction";
            case Location.TRACE_CALL:
                return "user-trace";
            default:
                return null;
            }
    }

    /**
    * Called when an item becomes the context item
    */

    public void startCurrentItem(Item item) {
    	if (item instanceof NodeInfo) {
    		tmp_source = (NodeInfo) item;
    		if (baseSourcePath == null) {
    			baseSourcePath = tmp_source.getSystemId();
    		}
    	}
    }

   private void saveToFile() {
		try {
			// Create file
			FileWriter fstream = new FileWriter(getFileName());
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(root.toString());
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
   }

    /**
     * Called after a node of the source tree got processed
     */

    public void endCurrentItem(Item item) {
    	 
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
     * Sets parent stylesheet that is used for transformation.
     * Its parent is used as a base path 
     * @param ss
     */
    public void setParentStylesheet(File ss) {
    	basePath = ss.getParent() + "/";
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