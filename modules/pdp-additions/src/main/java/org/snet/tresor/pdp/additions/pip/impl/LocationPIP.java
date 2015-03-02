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
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
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

    private static final URI DATATYPE_GEO = URI.create(GeometryAttribute.identifier);

    private static final Map<String, URI> supportedIdTypeMap;
    static {
        Map<String, URI> typeMap = new HashMap<>();
        typeMap.put("org:snet:tresor:attribute:current-geolocation", DATATYPE_GEO);
        typeMap.put("org:snet:tresor:attribute:current-wifi", DATATYPE_GEO);
        typeMap.put("org:snet:tresor:attribute:current-wifi-ssid", XACMLHelper.DATATYPE_STRING);
        typeMap.put("org:snet:tresor:attribute:current-bluetooth-location", XACMLHelper.DATATYPE_STRING);
        typeMap.put("org:snet:tresor:attribute:current-cell-id", XACMLHelper.DATATYPE_STRING);
        typeMap.put("org:snet:tresor:attribute:timestamp-geolocation", XACMLHelper.DATATYPE_INT);
        typeMap.put("org:snet:tresor:attribute:timestamp-wifi", XACMLHelper.DATATYPE_INT);
        typeMap.put("org:snet:tresor:attribute:timestamp-wifi-ssid", XACMLHelper.DATATYPE_INT);
        typeMap.put("org:snet:tresor:attribute:timestamp-bluetooth-location", XACMLHelper.DATATYPE_INT);
        typeMap.put("org:snet:tresor:attribute:timestamp-cell-id", XACMLHelper.DATATYPE_INT);

        supportedIdTypeMap = Collections.unmodifiableMap(typeMap);
    }

    private static final Map<String, URI> supportedIdUriMap;
    static {
        Map<String, URI> uriMap = new HashMap<>();
        for (String key : supportedIdTypeMap.keySet())
            uriMap.put(key, URI.create(key));

        supportedIdUriMap = Collections.unmodifiableMap(uriMap);
    }

    private String url;
    private String authentication;
    private ObjectMapper objectMapper;

    public LocationPIP(String url, String authentication, ObjectMapper objectMapper) {
        if (url == null)
            throw new RuntimeException("PIP Url may not be null");
        if (objectMapper == null)
            throw new RuntimeException("ObjectMapper may not be null");

        this.url = url;
        this.authentication = authentication;
        this.objectMapper = objectMapper;

        if (this.authentication == null)
            log.info("Initialized LocationPIP with url {}", this.url);
        else
            log.info("Initialized LocationPIP with url {} and authentication", this.url);
    }

    @Override
    public Set<String> getSupportedIds() {
        return supportedIdUriMap.keySet();
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
        Map<String, Attribute> attributeMap = new HashMap<>();

        String value;
        URI idUri, typeUri;
        Attribute attribute;
        for (String id : values.keySet()) {
            idUri = supportedIdUriMap.get(id);
            typeUri = supportedIdTypeMap.get(id);
            attribute = null;

            if (idUri != null && typeUri != null) {
                value = values.get(id);
                if (typeUri.toString().equals(GeometryAttribute.identifier))
                    value = GEOMETRY_POINT_PRE + value + GEOMETRY_POINT_POST;

                attribute = XACMLHelper.makeAttribute(idUri, typeUri, value, version);
            }

            if (attribute != null)
                attributeMap.put(id, attribute);
            else
                log.info("Failure in attribute creation for attribute with id "+ id + " type " + typeUri.toString());
        }

        return attributeMap;
    }

}
