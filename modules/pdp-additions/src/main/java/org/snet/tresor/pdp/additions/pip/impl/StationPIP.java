package org.snet.tresor.pdp.additions.pip.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.snet.tresor.pdp.additions.finder.impl.FinderConstants;
import org.snet.tresor.pdp.additions.pip.PIP;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.EvaluationCtx;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class StationPIP implements PIP, ResponseHandler<String[]> {
    private static final Logger log = LoggerFactory.getLogger(StationPIP.class);

    private static final Set<String> supportedIds;
    static {
        Set<String> set = new HashSet<String>();
        set.add(FinderConstants.ATTRIBUTE_ID_DOCTOR_STATION);
        set.add(FinderConstants.ATTRIBUTE_ID_DOCTOR_ROLE);
        set.add(FinderConstants.ATTRIBUTE_ID_PATIENT_STATION);
        set.add(FinderConstants.ATTRIBUTE_ID_PATIENT_ROLE);

        supportedIds = Collections.unmodifiableSet(set);
    }

    private String url;
    private ObjectMapper objectMapper;

    public StationPIP(String url, ObjectMapper objectMapper) {
        if (url == null)
            throw new RuntimeException("PIP Url may not be null");
        if (objectMapper == null)
            throw new RuntimeException("ObjectMapper may not be null");

        this.url = url;
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<String> getSupportedIds() {
        return supportedIds;
    }

    @Override
    public Map<String, Attribute> getAttributes(URI attributeType, URI attributeId, URI category,
                                                EvaluationCtx context, int version) throws IOException{
        String id = attributeId.toString();
        log.debug("Retrieving attribute with id {}, type {}, ", id, attributeType.toString());

        String url = null;
        if (id.equals(FinderConstants.ATTRIBUTE_ID_DOCTOR_STATION) || id.equals(FinderConstants.ATTRIBUTE_ID_PATIENT_STATION)) {
            String subject = (id.equals(FinderConstants.ATTRIBUTE_ID_DOCTOR_STATION)) ?
                    "/doctor/" +  XACMLHelper.getSubjectID(context) :
                    "/patient/" + XACMLHelper.getServiceID(context);
            url = this.url+subject+"?station=*&pdp=on";
        }

        if (id.equals(FinderConstants.ATTRIBUTE_ID_DOCTOR_ROLE) || id.equals(FinderConstants.ATTRIBUTE_ID_PATIENT_ROLE)) {
            String subject = (id.equals(FinderConstants.ATTRIBUTE_ID_DOCTOR_ROLE)) ?
                    "/doctor/" +  XACMLHelper.getSubjectID(context) :
                    "/patient/" + XACMLHelper.getServiceID(context);
            url = this.url+subject+"?role=*";
        }

        Map<String, Attribute> attributeMap = new HashMap<>();
        if (url != null) {
                String[] values = Request.Get(url).execute().handleResponse(this);
                BagAttribute bag = XACMLHelper.makeBagAttribute(attributeType, values);
                Attribute attribute = XACMLHelper.makeAttribute(attributeId, attributeType, bag, version);

                if (attribute != null)
                    attributeMap.put(id, attribute);
        }

        return attributeMap;
    }

    @Override
    public String[] handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
        // check returned http code
        StatusLine status = httpResponse.getStatusLine();
        if (status.getStatusCode() != HttpStatus.SC_OK)
            throw new HttpResponseException(status.getStatusCode(), status.getReasonPhrase());

        // check returned response entity
        HttpEntity entity = httpResponse.getEntity();
        if (entity == null)
            throw new ClientProtocolException("Response contains no content");

        // check type of returned content
        ContentType type = ContentType.getOrDefault(entity);
        if (!type.equals(ContentType.APPLICATION_JSON))
            throw new ClientProtocolException("Unexpected content type " + type.toString());

        // parse content
        return this.objectMapper.readValue(entity.getContent(), String[].class);
    }
}
