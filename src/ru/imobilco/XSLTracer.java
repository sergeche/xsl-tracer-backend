package ru.imobilco;

import java.io.File;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;

import com.icl.saxon.FeatureKeys;
import com.icl.saxon.ParameterSet;
import com.icl.saxon.PreparedStyleSheet;
import com.icl.saxon.StyleSheet;
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
    	
    	super.processFile(source, sheet, outputFile, params);
    }
}
