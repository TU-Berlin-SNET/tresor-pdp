package org.snet.tresor.pdp.finder.impl;

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
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.snet.tresor.pdp.Helper;
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
 * GeometryAttributes only work with gml:Point coordinates for now
 * @author Zequeira, malik
 */
public class LocationAttributeFinderModule extends AttributeFinderModule {
	private static RuntimeException earlyException;

	static String GEOMETRY_POINT_PRE = "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:4326\"><gml:coordinates>";
	static String GEOMETRY_POINT_POST = "</gml:coordinates></gml:Point>";

	/**
	 * Thread-specific cache for the retrieved values
	 * MUST BE CLEARED AFTER EVALUATION IS FINISHED!
	 */
	private static ThreadLocal<Map<String, String>> THREADCACHE = new ThreadLocal<Map<String,String>>() {
		protected synchronized Map<String, String> initialValue() {
			return new HashMap<String, String>();
		}
	};
	
	// TODO reimplement threadcache clearing

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
	 * Create new LocationAttributeFinderModule with given pips
	 * @param pipInfo array containing information about pips in the following form [ "attributeId", "pipURL", "attributeId", "pipURL", ...]
	 */
	public LocationAttributeFinderModule(String... pipInfo) {
		this();

		for (int i = 0; i < pipInfo.length - 1; i+=2)
			this.addPIP(pipInfo[i], pipInfo[i+1]);
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

		// if we have a url for a pip which can provide attribute values for given attributeid...
		if (pipUrl != null) {
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

		// try to get the value from cache
		String cachedValue = THREADCACHE.get().get(attributeId.toString());

		if (cachedValue != null) {
			response = new JSONObject().put(attributeId.toString(), cachedValue);
		} else {
			// else get the value from pip

			String subjectID = Helper.getAttributeAsString(FinderConstants.DATATYPE_STRING_URI, 
														   FinderConstants.ID_SUBJECT_URI, 
														   issuer, 
														   FinderConstants.CATEGORY_SUBJECT_URI, 
														   context);
			
			String deviceID = Helper.getAttributeAsString(FinderConstants.DATATYPE_STRING_URI, 
												   		  FinderConstants.ID_DEVICE_URI, 
												   		  issuer, 
												   		  FinderConstants.CATEGORY_SUBJECT_URI,
												   		  context);			

			if (subjectID != null && deviceID != null) {				
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

						// ...and cache all values the pip sent
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
		}

		return response;
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
			// if attribute is geometry, add additional gml:Point info to attributeValue
			if (attributeType.toString().equals(GeometryAttribute.identifier))
				attributeValue = GEOMETRY_POINT_PRE + attributeValue + GEOMETRY_POINT_POST;
			
			attribute = Balana.getInstance().getAttributeFactory().createValue(attributeType, attributeValue);
			Set<AttributeValue> set = new HashSet<AttributeValue>();
			set.add(attribute);

			return new EvaluationResult(new BagAttribute(attributeType, set));
		}
		catch (UnknownIdentifierException e) { e.printStackTrace(); }
		catch (ParsingException e) { e.printStackTrace(); }

		// empty bag if it fails
		return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
	}

	public static void clearThreadCache() {
		THREADCACHE.get().clear();
	}

}
