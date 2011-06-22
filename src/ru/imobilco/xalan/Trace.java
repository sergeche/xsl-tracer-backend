package ru.imobilco.xalan;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.transformer.TransformerImpl;

/**
 * Sample for demonstrating Xalan "trace" interface. Usage: run in Trace
 * directory: java Trace For an extensions trace sample, run in extensions
 * directory: java Trace 3-java-namespace
 */
public class Trace {
	public static void main(String[] args) throws java.io.IOException,
			TransformerException, TransformerConfigurationException,
			java.util.TooManyListenersException, org.xml.sax.SAXException {
		String fileName = "foo";
		if (args.length > 0)
			fileName = args[0];

		// Set up a PrintTraceListener object to print to a file.
		JSONTraceListener ptl = new JSONTraceListener();

		// Set up the transformation
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer(new StreamSource("test/xsl/simple.xsl"));

		// Cast the Transformer object to TransformerImpl.
		if (transformer instanceof TransformerImpl) {
			TransformerImpl transformerImpl = (TransformerImpl) transformer;
			// Register the TraceListener with a TraceManager associated
			// with the TransformerImpl.
			TraceManager trMgr = transformerImpl.getTraceManager();
			trMgr.addTraceListener(ptl);

			// Perform the transformation --printing information to
			// the events log during the process.
			transformer
					.transform(new StreamSource("test/xml/simple.xml"),
							new StreamResult(new java.io.FileWriter(fileName
									+ ".out")));
			
			System.out.print(ptl.toJSON());
		}
	}
}
