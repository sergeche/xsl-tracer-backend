package ru.imobilco.xalan;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.TooManyListenersException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.transformer.TransformerImpl;

public class XSLTracer {
	private TransformerImpl transformer;
	private ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
	private JSONTraceListener traceListener;
	
	public XSLTracer(TransformerImpl transformer) {
		this.transformer = transformer;
	}
	
	public String traceDocument(StreamSource source, String template) throws TooManyListenersException {
		traceListener = new JSONTraceListener();
		TraceManager trMgr = transformer.getTraceManager();
		trMgr.addTraceListener(traceListener);

		// force transformer to output XML result
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
		transformer.setOutputProperty(OutputKeys.ENCODING, "utf8");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		Result result = new StreamResult(resultStream);

		try {
			transformer.transform(source, result);
			return outputTraceDocument(template);
		} catch (Exception err) {
			return outputErrorDocument(template, err);
		}
	}
	
	public String getTraceJSON() {
		if (traceListener != null)
			return traceListener.toJSON();
		
		return null;
	}
	
	private String outputTraceDocument(String template) {
		return MessageFormat.format(template, "", 
				escapeHTML(resultStream.toString()), getTraceJSON(),
				"", "");
	}
	
	private String outputErrorDocument(String template, Exception e) {
		// construct error message
		String errMessage = e.toString();
		
		ByteArrayOutputStream errStream = new ByteArrayOutputStream();
		try {
			PrintStream errWriter = new PrintStream(errStream, true, "utf-8");
			e.printStackTrace(errWriter);
			errMessage = "<pre>" + errStream.toString() + "</pre>";
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		String error = "<div id=\"xt-global-error\">" +
				"<h2>Error</h2>" +
				"<div id=\"xt-global-error-content\">" +
				"<p>The XSL traces cannot be initialized because of the following server error:</p>" +
				"<div class=\"xt-error\">" +
				errMessage +
				"</div></div></div>";
		
		return MessageFormat.format(template, error, "", "{}", "", "");
	}
	
	public static String escapeHTML(String string) {
		StringBuffer sb = new StringBuffer(string.length());
		int len = string.length();
		char c;
		for (int i = 0; i < len; i++) {
			c = string.charAt(i);
			if (c == '&')
				sb.append("&amp;");
			else if (c == '<')
				sb.append("&lt;");
			else if (c == '>')
				sb.append("&gt;");
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
