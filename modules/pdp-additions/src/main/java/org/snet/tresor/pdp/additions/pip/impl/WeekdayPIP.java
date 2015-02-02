package org.snet.tresor.pdp.additions.pip.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.snet.tresor.pdp.additions.pip.PIP;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.EvaluationCtx;

import java.net.URI;
import java.util.*;

/**
 * PIP providing the attribute org:snet:tresor:attribute:weekday, datatype integer
 */
public class WeekdayPIP implements PIP {
    private static final Logger log = LoggerFactory.getLogger(WeekdayPIP.class);

    private static final Set<String> supportedIds;
    static {
        Set<String> set = new HashSet<String>();
        set.add("org:snet:tresor:attribute:weekday");
        supportedIds = Collections.unmodifiableSet(set);
    }

    public WeekdayPIP() {}

    @Override
    public Set<String> getSupportedIds() {
        return supportedIds;
    }

    @Override
    public Map<String, Attribute> getAttributes(URI attributeType, URI attributeId, URI category,
                                                EvaluationCtx context, int version) {
        Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();
        log.debug("Query for attribute {}, type {}, category {}",
                attributeId.toString(), attributeType.toString(), category.toString());

        String weekdayInt = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        Attribute attribute = XACMLHelper.makeAttribute(attributeId, attributeType, weekdayInt, version);
        if (attribute != null)
            attributeMap.put(attributeId.toString(), attribute);

        return attributeMap;
    }

}
