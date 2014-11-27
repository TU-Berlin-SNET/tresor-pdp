package org.snet.tresor.pdp.additions.finder.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.AttributeFinderModule;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * StationAttributeFinderModule for querying the stations a Doctor or a Patient belongs to. Also for
 * asking the Doctor's role on its stations.
 */
public class StationAttributeFinderModule extends AttributeFinderModule implements ResponseHandler<String[]> {
	private static final Logger log = LoggerFactory.getLogger(StationAttributeFinderModule.class);

	private static final Set<String> supportedIds;
	static {
		Set<String> set = new HashSet<String>();
		set.add(FinderConstants.DOCTOR_STATION_ATTRIBUTEID);
		set.add(FinderConstants.DOCTOR_ROLE_ATTRIBUTEID);
		set.add(FinderConstants.PATIENT_STATION_ATTRIBUTEID);
		set.add(FinderConstants.PATIENT_ROLE_ATTRIBUTEID);

		supportedIds = Collections.unmodifiableSet(set);
	}

	private ObjectMapper objMapper;
	private String url;

	public StationAttributeFinderModule(String url, ObjectMapper objMapper) {
		if (url == null)
			throw new RuntimeException("PIP Url may not be null");
		if (objMapper == null)
			throw new RuntimeException("ObjectMapper may not be null");

		this.url = url;
		this.objMapper = objMapper;
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

		String id = attributeId.toString();
		if (!supportedIds.contains(id))
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));

		String url = null;

		if (id.equals(FinderConstants.DOCTOR_STATION_ATTRIBUTEID) || id.equals(FinderConstants.PATIENT_STATION_ATTRIBUTEID)) {
			String subject = (id.equals(FinderConstants.DOCTOR_STATION_ATTRIBUTEID)) ?
					"/doctor/" +  XACMLHelper.getSubjectID(context) :
					"/patient/" + XACMLHelper.getServiceID(context);
			url = this.url+subject+"?station=*&pdp=on";
		}

		if (id.equals(FinderConstants.DOCTOR_ROLE_ATTRIBUTEID) || id.equals(FinderConstants.PATIENT_ROLE_ATTRIBUTEID)) {
			String subject = (id.equals(FinderConstants.DOCTOR_ROLE_ATTRIBUTEID)) ?
					"/doctor/" +  XACMLHelper.getSubjectID(context) :
					"/patient/" + XACMLHelper.getServiceID(context);
			url = this.url+subject+"?role=*";
		}

		if (url != null) {
			try {
				String[] values = Request.Get(url).execute().handleResponse(this);
				BagAttribute bag = XACMLHelper.makeBagAttribute(attributeType, values);
				return new EvaluationResult(bag);
			} catch (Exception e) {
				log.warn("Failed to retrieve attribute with id {} and type {} from pip", id, attributeType.toString(), e);
			}
		}

		return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
	}

	public String[] handleResponse(HttpResponse response)
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

		// parse content
		return this.objMapper.readValue(entity.getContent(), String[].class);
	}

}
