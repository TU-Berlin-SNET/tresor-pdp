package org.snet.tresor.finder.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.wso2.balana.Balana;
import org.wso2.balana.ParsingException;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.AttributeFinderModule;

/**
 * AttributeFinderModule which interfaces with the LocationServer-PIP to provide location data.
 * @author Zequeira, malik
 */
public class LocationAttributeFinderModule extends AttributeFinderModule {
	private static RuntimeException earlyException;
	
	private static URI SUBJECT_CATEGORY;
	private static URI STRING_DATATYPE;
	private static URI SUBJECT_ID;
	private static URI DEVICE_ID;
	
	/**
	 * Static Initializer for URIs to catch possible early Exceptions
	 */
	static {
		try {
			SUBJECT_CATEGORY = new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
			STRING_DATATYPE = new URI("http://www.w3.org/2001/XMLSchema#string");
			SUBJECT_ID = new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
			DEVICE_ID = new URI("device-id");			
		} catch (Exception e) {
			earlyException = new IllegalArgumentException();
			earlyException.initCause(e);
		}
	}
	
	private Map<String, String> pipUrlMap;
	
	/**
	 * Create new LocationAttributeFinderModule which can do nothing for all intents and purposes
	 * because there are no supportedIds
	 */
	public LocationAttributeFinderModule() {
		if (earlyException != null)
			throw earlyException;
		
		this.pipUrlMap = new ConcurrentHashMap<String, String>();
	}

    public Map<String, String> getPipUrlMap() {
    	return this.pipUrlMap;
    }
    
    @Override
    public Set<String> getSupportedIds() {
    	return this.pipUrlMap.keySet();
    }
    
    public void addPIP(String attributeid, String pipUrl) {
    	this.pipUrlMap.put(attributeid, pipUrl);
    }

    @Override
    public boolean isDesignatorSupported() {
        return true;
    }
    
    @Override
    public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer,
            URI category, EvaluationCtx context) {    	
    	String pipUrl = this.pipUrlMap.get(attributeId.toString());    	
    	
    	// if we have a url for a pip which can provide attributevalues for given attributeid...
    	if (pipUrl != null) {
    		// ...we query it for a response and get the response body...
    		String response = this.queryPIP(pipUrl, attributeId, issuer, context);
    		// ...and if we get a non-empty response body
    		if (!response.isEmpty()) {
    			// ..we parse the included json...
    			Map<String, String> values = this.simpleParseJson(response);
    			
    			// TODO: temporarily save timestamp and/or other value somewhere BUT there
    			// has to be a mechanism to make sure they get deleted after use to prevent bloating
    			
    			// ...and create/return the result in a bag in an evaluationresult
    			return this.makeAttributeBagResult(attributeType, values.get(attributeId));
    		}
    	}
    	
    	// if anything fails, return empty bag
    	return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
        
    }

	private String queryPIP(String pipUrl, URI attributeId, String issuer,	EvaluationCtx context) {
		String response = "";					
		
		String subjectID = getAttributeAsString(STRING_DATATYPE, SUBJECT_ID, issuer, SUBJECT_CATEGORY, context);
		String deviceID = getAttributeAsString(STRING_DATATYPE, DEVICE_ID, issuer, SUBJECT_CATEGORY, context);
		
		PostMethod query = new PostMethod(pipUrl);
		// TODO: change this into json
		query.addParameter("subject-id", subjectID);
		query.addParameter("device-id", deviceID);
		query.addParameter("attribute-id", attributeId.toString());
		
		if (issuer != null)
			query.addParameter("issuer", issuer);
		
		try {
			HttpClient client = new HttpClient();			
			client.executeMethod(query);
			if (query.getStatusCode() == 200) {
				response = query.getResponseBodyAsString();
			}			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			query.releaseConnection();
		}

		return response;
	}

	private Map<String, String> simpleParseJson(String json) {
		Map<String, String> map = new HashMap<String, String>();
		String[] temp = json.split("\".+\":\".+\"");
		for (String keyValuePair : temp) {
			String[] splitKeyValuePair = keyValuePair.split(":");
			map.put(splitKeyValuePair[0], splitKeyValuePair[1]);
		}
		return map;
	}
	
	private String getAttributeAsString(URI attributeType, URI attributeId,	String issuer, URI category, EvaluationCtx context) {
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
				// TODO what to do if there is more than one attribute with the same id and type? is that even allowed?				
			}
		}
		
		return value;
	}
	
	private EvaluationResult makeAttributeBagResult(URI attributeType, String attributeValue) {
		AttributeValue attribute;
		try {
			attribute = Balana.getInstance().getAttributeFactory().createValue(attributeType, attributeValue);
			Set<AttributeValue> set = new HashSet<AttributeValue>();
			set.add(attribute);
			
			return new EvaluationResult(new BagAttribute(attribute.getType(), set));
		} 
		catch (UnknownIdentifierException e) { e.printStackTrace(); } 
		catch (ParsingException e) { e.printStackTrace(); }
		
		// empty bag if it fails
		return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
	}

}
