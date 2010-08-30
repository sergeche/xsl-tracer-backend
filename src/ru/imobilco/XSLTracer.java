package ru.imobilco;

import java.io.File;
import java.util.Date;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import com.icl.saxon.Controller;
import com.icl.saxon.FeatureKeys;
import com.icl.saxon.ParameterSet;
import com.icl.saxon.PreparedStyleSheet;
import com.icl.saxon.StyleSheet;
import com.icl.saxon.style.TerminationException;
import com.icl.saxon.trace.TraceListener;

public class XSLTracer extends StyleSheet {
	
	public static void main (String args[])
	    throws java.lang.Exception
	{
	    // the real work is delegated to another routine so that it can be used in a subclass
	    (new XSLTracer()).doMain(args, new XSLTracer(), " java ru.imobilco.XSLTracer");
	}
	
	/**
    * Process a single file using a supplied stylesheet
    */
    public void processFile(
        Source source, Templates sheet, File outputFile, ParameterSet params)
        throws TransformerException {
    	
    	TraceListener tracer = (TraceListener) factory.getAttribute(FeatureKeys.TRACE_LISTENER);
    	if (tracer != null && tracer instanceof JSONTraceListener) {
    		((JSONTraceListener) tracer).setBaseFiles(source.getSystemId(), 
    				((PreparedStyleSheet) sheet).getStyleSheetDocument().getSystemId());
    		
    		
    	}
    	
    	Transformer instance = sheet.newTransformer();
    	((Controller)instance).setParams(params);
    	
    	// force transformer to output XML result
    	instance.setOutputProperty(OutputKeys.ENCODING, "utf8");
    	instance.setOutputProperty(OutputKeys.METHOD, "xml");
    	instance.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    	instance.setOutputProperty(OutputKeys.INDENT, "no");
    	
    	Result result =
    		(outputFile==null ?
    				new StreamResult(System.out) :
    					new StreamResult(outputFile));
    	
    	try {
    		instance.transform(source, result);
    	} catch (TerminationException err) {
    		throw err;
    	} catch (TransformerException err) {
    		// The message will already have been displayed; don't do it twice
    		throw new TransformerException("Run-time errors were reported");
    	}
    }
}
