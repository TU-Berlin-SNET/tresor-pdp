package org.snet.tresor.finder.impl;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
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

	// TODO: implement logging mechanism
	
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

		// if we have a url for a pip which can provide attributevalues for given attributeid...
		if (pipUrl != null) {
			try {
				// ...we query it and get the response body as json,...
				JSONObject jsonResponse = this.queryPIP(pipUrl, attributeId, issuer, context); 
				
				if (jsonResponse != null) {
					// ...extract the values...
					String attributeValue = jsonResponse.getString(attributeId.toString());
					long timestamp = jsonResponse.getLong("timestamp");									
															
					// TODO: remove
					System.out.println(attributeId.toString() + ": " + attributeValue);
					System.out.println("timestamp: " + timestamp);
					System.out.println(Thread.currentThread().getId());
					System.out.println(Thread.currentThread().getName());
					
					// TODO: temporarily save timestamp and/or other value somewhere BUT there
					// has to be a mechanism to make sure they get deleted after use to prevent bloating
					// Note: we can use ThreadLocal variable for that but then we need a definitive connection
					// from/to the servlet from/to this finderModule due to the servlet using a threadpool
					// Then why not copy the FinderModule over to the contexthandler completely?

					// ...and create/return the result in a bag in an evaluationresult
					return this.makeAttributeBagResult(attributeType, attributeValue);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
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
						
			// if query was successful, parse the response body
			if (query.getStatusCode() == 200) {
				JSONTokener tokener = new JSONTokener(new InputStreamReader(query.getResponseBodyAsStream()));
				response = new JSONObject(tokener);
			}
		} finally {
			query.releaseConnection();
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
				// TODO: what to do if there is more than one attribute with the same id and type? is that even allowed?				
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

}
