package org.snet.tresor.pdp.additions.pip.impl;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.EvaluationCtx;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * PIP interfacing with the LocationServer-PIP of the tresor-project to provide location data.
 * Geometry Attributes limited to gml:point for now.
 */
public class LocationPIP implements PIP, ResponseHandler<Map<String, String>> {
    private static final Logger log = LoggerFactory.getLogger(LocationPIP.class);

    private static final String GEOMETRY_POINT_PRE = "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:4326\"><gml:coordinates>";
    private static final String GEOMETRY_POINT_POST = "</gml:coordinates></gml:Point>";

    // supported attributes with type urn:ogc:def:dataType:geoxacml:1.0:geometry
    private static final Set<String> supportedGeoAttributes;
    static {
        Set<String> set = new HashSet<>();
        set.add(FinderConstants.ATTRIBUTE_ID_GEOLOCATION);
        set.add(FinderConstants.ATTRIBUTE_ID_WIFILOCATION);

        supportedGeoAttributes = Collections.unmodifiableSet(set);
    }

    // supported attributes with type http://www.w3.org/2001/XMLSchema#string
    private static final Set<String> supportedStringAttributes;
    static {
        Set<String> set = new HashSet<>();
        set.add(FinderConstants.ATTRIBUTE_ID_WIFISSID);
        set.add(FinderConstants.ATTRIBUTE_ID_BTLOCATION);
        set.add(FinderConstants.ATTRIBUTE_ID_CELLID);

        supportedStringAttributes = Collections.unmodifiableSet(set);
    }

    // supported attributes with type http://www.w3.org/2001/XMLSchema#integer
    private static final Set<String> supportedIntegerAttributes;
    static {
        Set<String> set = new HashSet<>();
        set.add(FinderConstants.ATTRIBUTE_ID_GEOLOCATION_TIMESTAMP);
        set.add(FinderConstants.ATTRIBUTE_ID_WIFILOCATION_TIMESTAMP);
        set.add(FinderConstants.ATTRIBUTE_ID_WIFISSID_TIMESTAMP);
        set.add(FinderConstants.ATTRIBUTE_ID_BTLOCATION_TIMESTAMP);
        set.add(FinderConstants.ATTRIBUTE_ID_CELLID_TIMESTAMP);

        supportedIntegerAttributes = Collections.unmodifiableSet(set);
    }

    private String url;
    private String authentication;
    private Set<String> supportedIds;
    private ObjectMapper objectMapper;

    // map containing the URIs for attributeIds
    private Map<String, URI> supportedIdURIMap;

    public LocationPIP(String url, String authentication, ObjectMapper objectMapper) {
        if (url == null)
            throw new RuntimeException("PIP Url may not be null");
        if (objectMapper == null)
            throw new RuntimeException("ObjectMapper may not be null");

        this.url = url;
        this.authentication = authentication;
        this.objectMapper = objectMapper;

        this.populateSupportedIds();
        this.populateSupportedIdURIs();

        if (this.authentication == null)
            log.info("Initialized LocationPIP with url {}", this.url);
        else
            log.info("Initialized LocationPIP with url {} and authentication", this.url);
    }


    private void populateSupportedIds() {
        Set<String> supportedIdSet = new HashSet<>();
        supportedIdSet.addAll(supportedGeoAttributes);
        supportedIdSet.addAll(supportedStringAttributes);
        supportedIdSet.addAll(supportedIntegerAttributes);
        this.supportedIds = Collections.unmodifiableSet(supportedIdSet);
    }

    private void populateSupportedIdURIs() {
        Map<String, URI> map = new HashMap<>();
        for (String attributeId : this.supportedIds)
            map.put(attributeId, URI.create(attributeId));
        this.supportedIdURIMap = Collections.unmodifiableMap(map);
    }

    @Override
    public Set<String> getSupportedIds() {
        return this.supportedIds;
    }

    @Override
    public Map<String, Attribute> getAttributes(URI attributeType, URI attributeId, URI category,
                                                EvaluationCtx context, int version) throws IOException {
        log.debug("Retrieving attribute with id {}, type {}, ", attributeId.toString(), attributeType.toString());
        Map<String, Attribute> attributeMap = new HashMap<>();

        String subjectId = XACMLHelper.getSubjectId(context);
        String deviceId = XACMLHelper.getDeviceId(context);

        if (subjectId != null && deviceId != null) {
            Request req = Request.Post(this.url).setHeader("Accept", ContentType.APPLICATION_JSON.toString())
                    .setHeader("Accept-Charset", "UTF-8");
            // set authorization if applicable
            if (this.authentication != null)
                req.setHeader("Authorization", this.authentication);

            // do & handle the actual request
            Map<String, String> values = req.bodyString(
                    "{ \"subjectID\":\"" + subjectId + "\","
                            + "\"deviceID\":\"" + deviceId + "\","
                            + "\"attributeID\":\"" + attributeId.toString() + "\"}",
                    ContentType.APPLICATION_JSON
            ).execute().handleResponse(this);

            Map<String, Attribute> madeAttributesMap = this.makeAttributes(values, version);

            if (madeAttributesMap != null)
                attributeMap.putAll(madeAttributesMap);
        }

        return attributeMap;
    }

    @Override
    public Map<String, String> handleResponse(HttpResponse httpResponse) throws IOException {
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
        if (!type.toString().equals(ContentType.APPLICATION_JSON.toString()))
            throw new ClientProtocolException("Unexpected content type " + type.toString());

        // retrieve values from response
        JsonNode dataNode = this.objectMapper.readTree(entity.getContent()).path("response");
        Iterator<Map.Entry<String, JsonNode>> fields = dataNode.fields();
        Map<String, String> values = new HashMap<>();
        Map.Entry<String, JsonNode> field;
        while(fields.hasNext()) {
            field = fields.next();
            values.put(field.getKey(), field.getValue().asText());
        }

        return values;
    }

    private Map<String, Attribute> makeAttributes(Map<String, String> values, int version) {
        String key, value;
        URI id, type;
        Attribute attribute;
        Map<String, Attribute> attributeMap = new HashMap<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            id = this.supportedIdURIMap.get(key);
            type = null;
            attribute = null;

            if (supportedGeoAttributes.contains(key)) {
                value = GEOMETRY_POINT_PRE + value + GEOMETRY_POINT_POST;
                type = FinderConstants.DATATYPE_GEO;
            }
            if (supportedIntegerAttributes.contains(key))
                type = FinderConstants.DATATYPE_INTEGER;
            if (supportedStringAttributes.contains(key))
                type = FinderConstants.DATATYPE_STRING;

            attribute = XACMLHelper.makeAttribute(id, type, value, version);

            if (attribute != null)
                attributeMap.put(key, attribute);
            else
                log.info("Failure in attribute creation for attribute with id "+ key + " type " + type);

        }

        return attributeMap;
    }

}
