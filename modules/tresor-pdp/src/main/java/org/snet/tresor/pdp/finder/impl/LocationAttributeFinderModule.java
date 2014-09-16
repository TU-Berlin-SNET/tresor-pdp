package org.snet.tresor.pdp.finder.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger log = LoggerFactory.getLogger(LocationAttributeFinderModule.class);

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

		log.debug("Initialized LocationAttributeFinderModule without PIPs");
	}

	/**
	 * Create new LocationAttributeFinderModule with given pips
	 * @param pipInfo array containing information about pips in the following form [ "attributeId", "pipURL", "attributeId", "pipURL", ...]
	 */
	public LocationAttributeFinderModule(String... pipInfo) {
		this();

		for (int i = 0; i < pipInfo.length - 1; i+=2)
			this.addPIP(pipInfo[i], pipInfo[i+1]);

		log.debug("Initialized LocationAttributeFinderModule with PIPs");
	}

	/**
	 * @return map containing mappings between attribute-ids and pip urls
	 */
	public Map<String, String> getPipUrlMap() {
		log.debug("Retrieving PIPUrlMap");
		return this.pipUrlMap;
	}

	@Override
	public Set<String> getSupportedIds() {
		log.debug("Retrieving supported attributeIDs");
		return this.pipUrlMap.keySet();
	}

	public void addPIP(String attributeid, String pipUrl) {
		log.debug("Adding a PIP at URL {} for attributeID {}", pipUrl, attributeid);
		this.pipUrlMap.put(attributeid, pipUrl);
	}

	@Override
	public boolean isDesignatorSupported() {
		return true;
	}

	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer, URI category, EvaluationCtx context) {
		log.debug("Retrieving attribute with id {}, type {}, category {}", attributeId.toString(), attributeType.toString(), category.toString());

		// check the cache
		String cachedValue = THREADCACHE.get().get(attributeId.toString());
		if (cachedValue != null) {
			log.info("Returning value from attribute cache");
			return this.makeAttributeBagResult(attributeType, cachedValue);
		}

		// retrieve url of pip that provides this attribute
		String pipUrl = this.pipUrlMap.get(attributeId.toString());
		if (pipUrl == null) {
			// return empty bag
			log.debug("No PIP found for attributes with id {}", attributeId.toString());
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
		}

		// retrieve attributes in preparation to query the pip
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

		try {
			// retrieve value from pip
			JSONObject pipResponse = this.queryPIP(pipUrl, attributeId, subjectID, deviceID, issuer, context);

			if (pipResponse != null) {
				// write all retrieved values into cache
				log.debug("Writing retrieved values into attribute cache");
				this.cacheValues(pipResponse);

				// retrieve value and wrap in a bag
				String attributeValue = pipResponse.getString(attributeId.toString());
				EvaluationResult bag = this.makeAttributeBagResult(attributeType, attributeValue);
				log.info("Retrieved value for attribute with id {}", attributeId.toString());
				return bag;
			}

		} catch (HttpException e) {
			log.warn("Communication with PIP failed", e);
		} catch (IOException e) {
			log.warn("Failed I/O", e);
		} catch (JSONException e) {
			log.warn("Attribute with id {} missing in pip response", attributeId.toString(), e);
		}

		log.debug("No value for attribute with id {} could be retrieved, returning empty bag", attributeId.toString());
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
	 * @throws IOException
	 * @throws HttpException
	 * @throws Exception
	 */
	private JSONObject queryPIP(String pipUrl, URI attributeId, String subjectID, String deviceID, String issuer, EvaluationCtx context) throws HttpException, IOException {

		if (subjectID == null || deviceID == null) {
			log.info("Missing attribute for request, subject-id is {} and device-id is {}", subjectID, deviceID);
			return null;
		}

		JSONObject response = null;

		// Prepare the query to the pip
		PostMethod query = new PostMethod(pipUrl);
		try {
			query.setRequestHeader("Content-Type", "application/json");
			// TODO hardcoded authorization is ugly, find a way around it
			query.setRequestHeader("Authorization", "Basic cGVfdXNlcjo5NTViMDYzMzY0ZDkxNTdjMDgzOTI1M2U4NDcwMjI2ODliNWVlMWRm");

			JSONObject jsonData = new JSONObject();
			jsonData.put("subjectID", subjectID);
			jsonData.put("deviceID", deviceID);
			jsonData.put("attributeID", attributeId.toString());
			query.setRequestEntity(new StringRequestEntity(jsonData.toString(), "application/json", "UTF-8"));

			// execute query
			this.httpClient.executeMethod(query);

			if (query.getStatusCode() == 200) {
				// parse the response Body
				JSONTokener tokener = new JSONTokener(new InputStreamReader(query.getResponseBodyAsStream()));
				response = new JSONObject(tokener).getJSONObject("response");
				log.debug("Received response from PIP: {}", response);
			}

		} finally {
			query.releaseConnection();
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
		catch (UnknownIdentifierException e) {
			log.info("Invalid DataType {}", attributeType, e);
		} catch (ParsingException e) {
			log.info("Failed to create attribute from given String", e);
		}

		// empty bag if it fails
		return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
	}

	private void cacheValues(JSONObject pipResponse) {
		// ...and cache all values the pip sent
		Map<String, String> threadCacheMap = THREADCACHE.get();
		Iterator iter = pipResponse.keys();

		while (iter.hasNext()) {
			String key = (String) iter.next();
			threadCacheMap.put(key, pipResponse.getString(key));
		}
	}

	public static void clearThreadCache() {
		THREADCACHE.get().clear();
	}

}
