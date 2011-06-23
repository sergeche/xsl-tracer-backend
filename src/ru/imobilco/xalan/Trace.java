package ru.imobilco.xalan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.transformer.TransformerImpl;

public class Trace {
	public static void main(String[] args) throws java.io.IOException,
			TransformerException, TransformerConfigurationException,
			java.util.TooManyListenersException, org.xml.sax.SAXException {
		
		StreamSource xsl = new StreamSource("test/xsl/simple.xsl");
		StreamSource xml = new StreamSource("test/xml/simple.xml");

		// Set up the transformation
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer(xsl);

		if (transformer instanceof TransformerImpl) {
			
			// init XSL tracer
			XSLTracer tracer = new XSLTracer((TransformerImpl) transformer);
			
			// get tracer template
			InputStream templateStream = tracer.getClass().getResourceAsStream("/template.html");
			String template = convertStreamToString(templateStream);
			
			// trace document
			String traceDoc = tracer.traceDocument(xml, template);
			
			
			// save result
			File f = new File("trace.html");
			Writer output = new BufferedWriter(new FileWriter(f));
			try {
				output.write(traceDoc);
			} finally {
				output.close();
			}
			
			System.out.println(traceDoc);
		}
	}
	
	public static String convertStreamToString(InputStream is)
			throws IOException {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}
}
