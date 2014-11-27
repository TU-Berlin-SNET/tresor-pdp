package org.snet.tresor.pdp.additions.finder.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.AttributeFinderModule;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AttributeFinderModule which interfaces with the LocationServer-PIP of the TRESOR project
 * to provide location data. GeometryAttributes are limited to gml:point for now
 */
public class LocationAttributeFinderModule extends AttributeFinderModule implements ResponseHandler<Map<String, String>> {
	private static final Logger log = LoggerFactory.getLogger(LocationAttributeFinderModule.class);

	static final String GEOMETRY_POINT_PRE = "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:4326\"><gml:coordinates>";
	static final String GEOMETRY_POINT_POST = "</gml:coordinates></gml:Point>";

	private static final Set<String> supportedIds;
	static {
		Set<String> set = new HashSet<String>();
		set.add(FinderConstants.GEOLOCATION_ATTRIBUTEID);
		set.add(FinderConstants.GEOLOCATION_TIMESTAMP_ATTRIBUTEID);
		set.add(FinderConstants.WIFILOCATION_ATTRIBUTEID);
		set.add(FinderConstants.WIFILOCATION_TIMESTAMP_ATTRIBUTEID);
		set.add(FinderConstants.WIFISSID_ATTRIBUTEID);
		set.add(FinderConstants.WIFISSID_TIMESTAMP_ATTRIBUTEID);
		set.add(FinderConstants.BTLOCATION_ATTRIBUTEID);
		set.add(FinderConstants.BTLOCATION_TIMESTAMP_ATTRIBUTEID);
		set.add(FinderConstants.CELLID_ATTRIBUTEID);
		set.add(FinderConstants.CELLID_TIMESTAMP_ATTRIBUTEID);

		supportedIds = Collections.unmodifiableSet(set);
	}

	private String authentication;
	private ThreadLocal<Map<String, String>> cache;
	private ObjectMapper objMapper;
	private String url;

	public LocationAttributeFinderModule(String url, String authentication, ObjectMapper objMapper,
			ThreadLocal<Map<String, String>> cache) {
		if (url == null)
			throw new RuntimeException("PIP Url may not be null");
		if (cache == null)
			throw new RuntimeException("Cache may not be null");
		if (objMapper == null)
			throw new RuntimeException("ObjectMapper may not be null");

		this.authentication = authentication;
		this.cache = cache;
		this.objMapper = objMapper;
		this.url = url;
	}

	@Override
	public Set<String> getSupportedIds() {
		return supportedIds;
	}

	@Override
	public boolean isDesignatorSupported() {
		return true;
	}

	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer, URI category,
			EvaluationCtx context) {
		log.debug("Retrieving attribute with id {}, type {}, ", attributeId.toString(), attributeType.toString());

		// if attributeId is not supported, return empty bag
		String id = attributeId.toString();
		if (!supportedIds.contains(id))
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));

		// if we have the value cached, return from cache
		Map<String, String> cachemap = this.cache.get();
		if (cachemap.containsKey(id)) {
			log.debug("Returning attribute with id {} of type {} from cache", id, attributeType.toString());

			BagAttribute bag = XACMLHelper.makeBagAttribute(attributeType, cachemap.get(id));
			return new EvaluationResult(bag);
		}

		// arriving here means the attributeId is supported and we do not have it cached so
		// we fetch some necessary values to query the pip
		String subjectId = XACMLHelper.getSubjectID(context);
		String deviceId = XACMLHelper.getDeviceID(context);

		if (subjectId != null && deviceId != null) {
			try {
				// query the pip, cache retrieved values (reason: we always retrieve the actual attribute and a timestamp for it)
				Map<String, String> values = Request.Post(this.url).setHeader("Authentication", this.authentication)
												.bodyString("{ subjectID:"   + subjectId   + ","
														    + "deviceID:"    + deviceId    + ","
														    + "attributeID:" + attributeId.toString() + "}",
														    ContentType.APPLICATION_JSON)
												.execute().handleResponse(this);
				cachemap.putAll(values);

				String value = values.get(id);
				// if value is of type geometry, bring it in proper form and put back into cache
				if (attributeType.toString().equals(GeometryAttribute.identifier)) {
					value = GEOMETRY_POINT_PRE + value + GEOMETRY_POINT_POST;
					cachemap.put(id, value);
				}

				return new EvaluationResult(XACMLHelper.makeBagAttribute(attributeType, value));
			} catch (Exception e) {
				log.warn("Failed to retrieve attribute with id {} and type {} from pip", id, attributeType.toString(), e);
			}
		}

		// if we're here, then there was some error so we return an empty bag
		return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
	}

	public Map<String, String> handleResponse(HttpResponse response)
			throws ClientProtocolException, IOException {

		// check returned http code
		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() != HttpStatus.SC_OK)
			throw new HttpResponseException(status.getStatusCode(), status.getReasonPhrase());

		// check returned response entity
		HttpEntity entity = response.getEntity();
		if (entity == null)
			throw new ClientProtocolException("Response contains no content");

		// check type of returned content
		ContentType type = ContentType.getOrDefault(entity);
		if (!type.equals(ContentType.APPLICATION_JSON))
			throw new ClientProtocolException("Unexpected content type " + type.toString());

		// parse content int Map<String, String>
		return this.objMapper.readValue(entity.getContent(), FinderConstants.MAPTYPE_STRING);
	}

}
