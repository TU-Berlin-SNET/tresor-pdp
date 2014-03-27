package org.snet.finder.impl;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
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

	/**
	 * Thread-specific cache for the retrieved values
	 * MUST BE CLEARED AFTER EVALUATION IS FINISHED!
	 */
	private static ThreadLocal<Map<String, String>> THREADCACHE = new ThreadLocal<Map<String,String>>() {
		protected synchronized Map<String, String> initialValue() {
			return new HashMap<String, String>();
		}
	};

	private Map<String, String> pipUrlMap;	
	private MultiThreadedHttpConnectionManager connectionManager;
	private HttpClient httpClient;

	/**
	 * Create new LocationAttributeFinderModule which can do nothing for all intents and purposes
	 * because there are no supportedIds
	 */
	public LocationAttributeFinderModule() {
		if (earlyException != null)
			throw earlyException;

		this.connectionManager = new MultiThreadedHttpConnectionManager();
		this.httpClient = new HttpClient(this.connectionManager);		
		this.pipUrlMap = new ConcurrentHashMap<String, String>();
	}

	/**
	 * @return map containing mappings between attribute-ids and pip urls
	 */
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

		// TODO remove
		System.out.println("Looking for attribute: " + attributeId.toString());

		// if we have a url for a pip which can provide attribute values for given attributeid...
		if (pipUrl != null) {
			// TODO remove
			System.out.println("Have a pip");
			try {
				// ...we query the pip and get the response body as json,...
				JSONObject jsonResponse = this.queryPIP(pipUrl, attributeId, issuer, context); 

				if (jsonResponse != null) {
					// ...extract the value we are looking for (and hope it does not throw an exception)...
					String attributeValue = jsonResponse.getString(attributeId.toString());					

					// ...and create/return the result in a bag in an evaluationresult
					return this.makeAttributeBagResult(attributeType, attributeValue);
				}
			} catch (Exception e) {	e.printStackTrace(); }							
		}

		// if we're here, then there was some error so we return an empty bag
		return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
	}

	/**
	 * Query PIP for given attribute, return parsed json response
	 * @param pipUrl, url at which the pip can be found
	 * @param attributeId, the id of the attribute we are looking for
	 * @param issuer, the issuer
	 * @param context, the evaluationcontext, in case we need to look up additional attributes
	 * @return JSONObject, parsed json response or null
	 * @throws Exception
	 */
	private JSONObject queryPIP(String pipUrl, URI attributeId, String issuer,	EvaluationCtx context) throws Exception {
		JSONObject response = null;
		
		String subjectID = getAttributeAsString(STRING_DATATYPE, SUBJECT_ID, issuer, SUBJECT_CATEGORY, context);
		String deviceID = getAttributeAsString(STRING_DATATYPE, DEVICE_ID, issuer, SUBJECT_CATEGORY, context);

		// try to get the value from cache
		String cachedValue = THREADCACHE.get().get(attributeId.toString());
		// TODO remove
		System.out.println("Cached Value is: " + cachedValue);
		if (cachedValue != null)
			response = new JSONObject().put(attributeId.toString(), cachedValue);		
		else {
			
			// TODO remove
			System.out.println("No cached value, asking pip");
			
			// if that fails, try getting the value from the pip
			PostMethod query = new PostMethod(pipUrl);
			
			try {
				// create json data and set in query
				JSONObject jsonData = new JSONObject();
				jsonData.put("subject-id", subjectID);
				jsonData.put("device-id", deviceID);
				jsonData.put("attribute-id", attributeId.toString());
				jsonData.putOpt("issuer", issuer);
				query.setRequestEntity(new StringRequestEntity(jsonData.toString(), "application/json", "UTF-8"));

				// fire query
				this.httpClient.executeMethod(query);

				// if query was successful...
				if (query.getStatusCode() == 200) {
					// ...parse the response Body...
					JSONTokener tokener = new JSONTokener(new InputStreamReader(query.getResponseBodyAsStream()));
					response = new JSONObject(tokener);
					
					// TODO remove
					System.out.println("Adding to cache");
					
					// ...cache all values the pip sent...
					Iterator iter = response.keys();
					Map<String, String> threadCacheMap = THREADCACHE.get();
					while (iter.hasNext()) {
						String key = (String) iter.next();
						threadCacheMap.put(key, response.getString(key));
					}				
				}
			} finally {
				query.releaseConnection();
			}
		}

		return response;
	}				

	
	/**
	 * Searches in given EvaluationContext for an attribute
	 * @param attributeType, type of attribute to look for
	 * @param attributeId, id of attribute to look for
	 * @param issuer, issuer of attribute to look for
	 * @param category, category of attribute to look for
	 * @param context, context in which to look
	 * @return a string representation of the value or null
	 */
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
				// TODO: what if we have, for example, two subjects with same dataType and Id?				
			}
		}

		return value;
	}

	/**
	 * Create Attribute, pack it in a bag and create Evaluationresult from it
	 * @param attributeType, type of attribute to create
	 * @param attributeValue, value of attribute to create
	 * @return EvaluationResult containing a bag which contains the attribute or is empty
	 */
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
	
	public static void clearThreadCache() {
		// TODO remove
		System.out.println("clearing cache");
		
		THREADCACHE.get().clear();
	}

}
