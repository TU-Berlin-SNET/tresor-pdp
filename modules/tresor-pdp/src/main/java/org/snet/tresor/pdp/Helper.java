package org.snet.tresor.pdp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.snet.tresor.pdp.finder.impl.LocationAttributeFinderModule;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.ctx.EvaluationCtx;

/**
 * Helper class containing miscellaneous helper methods
 * @author malik
 */
public class Helper {
	private static final Logger log = LoggerFactory.getLogger(Helper.class);

	/**
	 * Creates multiple instances of given class and puts into given collection
	 * @param classes JSONArray containing configuration information for classes
	 * @param output Collection to output to
	 * @param cls the class of created instances, used for casting
	 * @throws Exception
	 */
    public static <T> void createInstances(JSONArray classes, Collection<T> output, Class<T> cls) throws Exception {
    	log.debug("Creating instances of class {}", cls.toString());
    	for (int i = 0; i < classes.length(); i++) {
    		JSONObject classConfig = classes.getJSONObject(i);
    		output.add(Helper.createInstance(classConfig, cls));
    	}
    }

    /**
     * Creates an instance of given class with given config
     * @param config JSONObject containing configuration for instance
     * @param cls class to cast to
     * @return created instance
     * @throws Exception
     */
    public static <T> T createInstance(JSONObject config, Class<T> cls) throws Exception {
    	Class<?> c = Helper.class.getClassLoader().loadClass(config.getString("classname"));
    	log.debug("Creating instance of class {} with classname {}", cls.toString(), config.getString("classname"));

    	Object instance;

    	if (config.has("parameters")) {
    		JSONArray paramsJSON = config.getJSONArray("parameters");

    		String[] params = new String[paramsJSON.length()];
    		for (int i = 0; i < paramsJSON.length(); i++) {
    			params[i] = paramsJSON.getString(i);
    		}

    		Object[] wrappedParams = { params };
    		instance = c.getConstructor(String[].class).newInstance(wrappedParams);
    	}
    	else
    		instance = c.newInstance();

    	return cls.cast(instance);
    }

	/**
	 * Searches in given EvaluationContext for an attribute
	 * @param attributeType type of attribute to look for
	 * @param attributeId id of attribute to look for
	 * @param issuer issuer of attribute to look for
	 * @param category category of attribute to look for
	 * @param context context in which to look
	 * @return a string representation of the value or null
	 */
	public static String getAttributeAsString(URI attributeType, URI attributeId,	String issuer, URI category, EvaluationCtx context) {
		String value = null;
		BagAttribute bag = (BagAttribute) context.getAttribute(attributeType, attributeId, issuer, category).getAttributeValue();

		if (!bag.isEmpty()) {
			AttributeValue val;
			Iterator it = bag.iterator();
			while (it.hasNext()) {
				val = (AttributeValue) it.next();
				if (!val.isBag() && val.getType().equals(attributeType)) {
					value = val.encode();
					break;
				}
			}
		}

		return value;
	}

	/**
	 * Gets the InputStream from the request, handles character encoding issues, wraps into Reader and returns
	 * @param request the httpservlet request
	 * @return InputStream of request wrapped in a reader with character encoding
	 * @throws IOException
	 */
	public static Reader getRequestInputStreamReader(HttpServletRequest request) throws IOException {
		// try to get the charset from request
		String charset = request.getCharacterEncoding();

		log.debug("Request charset is {}", charset);

		// if none is given, default to UTF-8
		if (charset == null)
			charset = ServletConstants.CHARSET_UTF8;

		return new InputStreamReader(request.getInputStream(), charset);
	}

	/**
	 * @param request the httpservlet request
	 * @return parsed JSONObject from HTTP body
	 */
    public static JSONObject getJSONFromBody(HttpServletRequest request) {
    	JSONObject out = null;
    	try {
    		Reader reader = Helper.getRequestInputStreamReader(request);
    		JSONTokener tok = new JSONTokener(reader);
    		out = new JSONObject(tok);
    	} catch (IOException e) {
    		log.error("Failed to retrieve JSONObject from request body", e);
    	}

    	return out;
    }

    /**
     * Respond with given information
     * @param error boolean value indicating whether an error happened
     * @param statuscode the http statuscode
     * @param response the httpservlet response
     */
	public static void respondHTTP(boolean error, int statuscode, HttpServletResponse response) {
		try {
			if (error) {
				response.sendError(statuscode);
			} else {
				response.setStatus(statuscode);
				response.flushBuffer();
			}
		} catch (IOException e) {
			log.error("Failed writing http response to stream", e);
		}
	}

    /**
     * Respond with given Information
     * @param error boolean value indicating whether an error happened
     * @param statuscode the http statuscode
     * @param contenttype the http mime type
     * @param content the actual content
     * @param response the httpservlet response
     */
    public static void respondHTTP(boolean error, int statuscode, String contenttype, String content, HttpServletResponse response) {
    	try {
        	response.setContentType(contenttype);
        	response.setCharacterEncoding(ServletConstants.CHARSET_UTF8);

        	if (error) {
        		response.sendError(statuscode, content);
        	} else {
        		response.setStatus(statuscode);
        		Helper.printResponse(content, response);
        	}
    	} catch (IOException e) {
    		log.error("Failed to write http response to stream", e);
		}
    }

    /**
     * Prints given string response to servlet response outputstream and closes stream afterwards
     * @param str the string to print
     * @param out the httpservlet response
     * @throws IOException
     */
    private static void printResponse(String str, HttpServletResponse out) throws IOException {
    	PrintWriter writer = null;
    	try {
    		writer = out.getWriter();
    		writer.print(str);
    		writer.flush();
    	} finally {
    		try { writer.close(); }
    		catch (Exception e) { }
    	}
    }

    /**
     * Clear potentially cached data
     */
    public static void clearPDPCaches() {
    	log.debug("Clearing PDP caches");
    	LocationAttributeFinderModule.clearThreadCache();
    }

}
