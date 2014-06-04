package org.snet.tresor.pdp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.snet.tresor.pdp.contexthandler.handler.Handler;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.ctx.EvaluationCtx;

/**
 * Helper class containing miscellaneous helper methods
 * @author malik
 */
public class Helper {
	private static Log log = LogFactory.getLog(Helper.class);

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
		Reader reader;
		String charset = request.getCharacterEncoding();
		
		if (charset != null) {
			// if there is a charset given, use it
			reader = new InputStreamReader(request.getInputStream(), charset);
		} else {
			// else fall back to UTF-8
			reader = new InputStreamReader(request.getInputStream(), ServletConstants.CHARSET_UTF8);
		}
		
		return reader;
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
    
    /**
     * Responds with information given in the JSON, expects a specific structure
     * @param responseJSON jsonobject containing response information
     * @param response the httpservlet response
     */
    public static void respondHTTP(JSONObject responseJSON, HttpServletResponse response) {
    	if (responseJSON != null) {
    		// add additional, optional headers
    		addHeaders(responseJSON, response, Handler.KEYJSON_HEADER);

        	boolean error = responseJSON.optBoolean(Handler.KEYJSON_ERROR, true);
        	int statuscode = responseJSON.optInt(Handler.KEYJSON_STATUSCODE, 500);
        	
        	if (responseJSON.has(Handler.KEYJSON_CONTENTTYPE) && responseJSON.has(Handler.KEYJSON_CONTENT)) {        		
        		respondHTTP(error, statuscode, responseJSON.getString(Handler.KEYJSON_CONTENTTYPE), 
        				responseJSON.getString(Handler.KEYJSON_CONTENT), response);        		
        	} else {
        		respondHTTP(error, statuscode, response);
        	}
        
    	} else {    		
    		respondHTTP(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
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
    	
    	response.setContentType(contenttype);
    	response.setCharacterEncoding(ServletConstants.CHARSET_UTF8);
    	
    	if (error) {
    		try { response.sendError(statuscode, content); } 
    		catch (Exception e) { log.error("Error sending error response", e); }
    	} else {
    		response.setStatus(statuscode);    		
    		printResponse(content, response);
    	}
    }
    
    /**
     * Respond with given information
     * @param error boolean value indicating whether an error happened
     * @param statuscode the http statuscode
     * @param response the httpservlet response
     */
    public static void respondHTTP(boolean error, int statuscode, HttpServletResponse response) {
    	if (error) {
    		try {
    			response.sendError(statuscode);
    		} catch (Exception e) { log.error("Error sending error response", e); }
    	} else {
    		response.setStatus(statuscode);
    	}
    }
    
    private static void addHeaders(JSONObject responseJSON, HttpServletResponse response, String[] headers) {
    	for (int i = 0; i < headers.length; i++) {
			if (responseJSON.has(headers[i]))
				response.setHeader(headers[i], responseJSON.getString(headers[i]));
		}
    	
    }
    
    private static void printResponse(String str, HttpServletResponse out) {
    	PrintWriter writer = null;
    	try {
    		writer = out.getWriter();
    		writer.print(str);
    		writer.flush();
    	} catch (IOException e) { log.error("Error while printing response to client", e); } 
    	finally {
    		try { writer.close(); }
    		catch (Exception e) { }
    	}
    }
    
}