package ru.imobilco;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RootTag extends Tag {
	private List<String> externalXSL = new ArrayList<String>();
	private List<String> externalXML = new ArrayList<String>();
	private String basePath = "";
	
	public RootTag(String tag_name) {
		// TODO Auto-generated constructor stub
		super(tag_name);
	}
	
	public static String getRelativePath(String targetPath, String basePath, 
	        String pathSeparator) {

	    //  We need the -1 argument to split to make sure we get a trailing 
	    //  "" token if the base ends in the path separator and is therefore
	    //  a directory. We require directory paths to end in the path
	    //  separator -- otherwise they are indistinguishable from files.
	    String[] base = basePath.split(Pattern.quote(pathSeparator), -1);
	    String[] target = targetPath.split(Pattern.quote(pathSeparator), 0);

	    //  First get all the common elements. Store them as a string,
	    //  and also count how many of them there are. 
	    String common = "";
	    int commonIndex = 0;
	    for (int i = 0; i < target.length && i < base.length; i++) {
	        if (target[i].equals(base[i])) {
	            common += target[i] + pathSeparator;
	            commonIndex++;
	        }
	        else break;
	    }

	    if (commonIndex == 0)
	    {
	        //  Whoops -- not even a single common path element. This most
	        //  likely indicates differing drive letters, like C: and D:. 
	        //  These paths cannot be relativized. Return the target path.
	        return targetPath;
	        //  This should never happen when all absolute paths
	        //  begin with / as in *nix. 
	    }

	    String relative = "";
	    if (base.length == commonIndex) {
	        //  Comment this out if you prefer that a relative path not start with ./
	        //relative = "." + pathSeparator;
	    }
	    else {
	        int numDirsUp = base.length - commonIndex - 1;
	        //  The number of directories we have to backtrack is the length of 
	        //  the base path MINUS the number of common path elements, minus
	        //  one because the last element in the path isn't a directory.
	        for (int i = 1; i <= (numDirsUp); i++) {
	            relative += ".." + pathSeparator;
	        }
	    }
	    relative += targetPath.substring(common.length());

	    return relative;
	}
	
	private String relativizePath(String fileURI) {
    	String file = null;
    	try {
			file = getRelativePath(new URI(fileURI).getPath(), basePath, "/");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return file;
    }
	
	public String getExternal(String systemId, String type) {
		List<String> collection = (type == "xsl") ? externalXSL : externalXML;
		
		int ix = collection.indexOf(systemId);
		if (ix == -1) {
			collection.add(systemId);
			ix = collection.size() - 1;
		}
		
		return ix + "";
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getBasePath() {
		return basePath;
	}
	
	private String outputCollection(String type, List<String> collection) {
		String result = "\"" + type + "\":[";
		
		for (int i = 0; i < collection.size(); i++) {
			if (i > 0)
				result += ",";
			result += "\"" + relativizePath(collection.get(i)) + "\"";
		}
		
		return result + "]";
	}
	
	protected String metaToString() {
		return outputCollection("xsl", externalXSL) + "," + outputCollection("xml", externalXML);
	}
}
