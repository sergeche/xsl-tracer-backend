package ru.imobilco;

import java.io.StringWriter;

import javax.xml.transform.TransformerException;

import com.icl.saxon.Binding;
import com.icl.saxon.Context;
import com.icl.saxon.expr.Expression;
import com.icl.saxon.expr.FragmentValue;
import com.icl.saxon.expr.NodeSetExpression;
import com.icl.saxon.expr.Value;
import com.icl.saxon.expr.VariableReference;
import com.icl.saxon.om.NamePool;
import com.icl.saxon.om.NodeEnumeration;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.output.NamespaceEmitter;
import com.icl.saxon.style.XSLCopyOf;
import com.icl.saxon.style.XSLVariable;

/**
 * Processes xsl:copy-of element, expanding outputted result fragment when needed
 * @author Sergey Chikuyonok (serge.che@gmail.com)
 * @link http://chikuyonok.ru
 *
 */
public class XSLCopyOfPrecessor {
	private NamespaceEmitter emitter;
    private JSONEmitter jsonEmitter;
    private StringWriter buf;
    
    public XSLCopyOfPrecessor() {
    	jsonEmitter = new JSONEmitter();
    	buf = new StringWriter();
    	jsonEmitter.setWriter(buf);
    	
    	emitter = new NamespaceEmitter();
    	emitter.setUnderlyingEmitter(jsonEmitter);
    	emitter.setNamePool(NamePool.getDefaultNamePool());
    }
    
    public void process(XSLCopyOf node, Context context, Tag tag) {
    	String selectAttr = node.getAttribute("select");
    	String result = "";
    	if (selectAttr != null) {
    		try {
    			Expression select = node.makeExpression(selectAttr);

    			if (select instanceof NodeSetExpression) {
    				NodeEnumeration enm = select.enumerate(context, true);
    				// TODO write down  
    				while (enm.hasMoreElements()) {
    					NodeInfo n = enm.nextElement();
    					result += n.getDisplayName() + ";";
    				}
    				
    			} else {
    				Value value = select.evaluate(context);
    				if (value instanceof FragmentValue) {

    					if (select instanceof VariableReference) {
    						Binding binding = ((VariableReference) select).getBinding();
    						if (binding instanceof XSLVariable) {
    							convertLREToTraceData((NodeInfo) binding, value, tag);
    						}
    					}

    				}
    			}
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
	}
    
    /**
     * Expands LRE fragment defined in xsl:variable into trace objects
     * @param info
     * @param value
     * @param tag
     */
    private void convertLREToTraceData(NodeInfo info, Value value, Tag tag) {
    	// expand LRE fragment in xsl:variable
		buf.getBuffer().setLength(0);
		jsonEmitter.setSourceElement(info);
		
		try {
			emitter.startDocument();
			((FragmentValue)value).replay(emitter);
			emitter.endDocument();
			jsonEmitter.copyTags(tag);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
