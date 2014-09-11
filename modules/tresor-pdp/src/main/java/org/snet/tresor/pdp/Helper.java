package org.snet.tresor.pdp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.snet.tresor.pdp.contexthandler.handler.Handler;
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
    		log.error("Error getting JSONObject from request body", e);
    	}
    	
    	return out;
    }
    
//    /**
//     * Create response json
//     * @param error whether an error happened
//     * @param httpcode the http code
//     * @return json object containing response details
//     */
//    public static JSONObject createResponseJSON(boolean error, int httpcode) {
//		return new JSONObject().put(Handler.KEYJSON_ERROR, error)
//				.put(Handler.KEYJSON_STATUSCODE, httpcode);
//    }
//    
//    /**
//     * Create response json
//     * @param error whether an error happened
//     * @param httpcode the http code
//     * @param contenttype the contenttype
//     * @param content the actual content
//     * @return json object containing response details
//     */
//    public static JSONObject createResponseJSON(boolean error, int httpcode, String contenttype, String content) {
//		return new JSONObject().put(Handler.KEYJSON_ERROR, error)
//				.put(Handler.KEYJSON_STATUSCODE, httpcode)
//				.put(Handler.KEYJSON_CONTENTTYPE, contenttype)
//				.put(Handler.KEYJSON_CONTENT, content);
//    }
    
    /**
     * Responds with information given in the JSON, expects a specific structure
     * @param responseJSON jsonobject containing response information
     * @param response the httpservlet response
     */
//    public static void respondHTTP(JSONObject responseJSON, HttpServletResponse response) {
//    	if (responseJSON != null) {
//    		// add additional, optional headers
//    		addHeaders(responseJSON, response, Handler.KEYJSON_HEADER);
//
//        	boolean error = responseJSON.optBoolean(Handler.KEYJSON_ERROR, true);
//        	int statuscode = responseJSON.optInt(Handler.KEYJSON_STATUSCODE, 500);
//        	
//        	if (responseJSON.has(Handler.KEYJSON_CONTENTTYPE) && responseJSON.has(Handler.KEYJSON_CONTENT)) {        		
//        		respondHTTP(error, statuscode, responseJSON.getString(Handler.KEYJSON_CONTENTTYPE), 
//        				responseJSON.getString(Handler.KEYJSON_CONTENT), response);        		
//        	} else {
//        		respondHTTP(error, statuscode, response);
//        	}
//        
//    	} else {    		
//    		respondHTTP(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
//    	}    	    	
//    }
    
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
			// TODO logging
		} catch (Exception e) {
			// TODO logging
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
			// TODO logging
		} catch (Exception e) {
			// TODO logging
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
    
    public static <T> void createInstances(JSONArray classes, Collection<T> output, Class<T> cls) throws Exception {    	
    	for (int i = 0; i < classes.length(); i++) {
    		JSONObject classConfig = classes.getJSONObject(i);
    		output.add(Helper.createInstance(classConfig, cls));
    	}    	
    }
    
    public static <T> T createInstance(JSONObject config, Class<T> cls) throws Exception {
    	Class<?> c = Helper.class.getClassLoader().loadClass(config.getString("classname"));
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
     * Clear potentially cached data
     */
    public static void clearPDPCaches() {
    	LocationAttributeFinderModule.clearThreadCache();
    }
    
}
