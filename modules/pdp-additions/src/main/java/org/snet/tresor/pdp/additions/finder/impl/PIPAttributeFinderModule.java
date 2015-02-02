package org.snet.tresor.pdp.additions.finder.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.pip.PIP;
import org.wso2.balana.XACMLConstants;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.ctx.xacml2.XACML2EvaluationCtx;
import org.wso2.balana.ctx.xacml3.XACML3EvaluationCtx;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.xacml3.Attributes;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * AttributeFinderModule retrieving attributes via PIPs
 */
public class PIPAttributeFinderModule extends AttributeFinderModule {
    private static final Logger log = LoggerFactory.getLogger(PIPAttributeFinderModule.class);

    private List<PIP> pips;
    private Set<String> supportedIds;

    public PIPAttributeFinderModule(List<PIP> pips) {
        this.pips = (pips == null) ? new ArrayList<PIP>() : pips;

        this.supportedIds = new HashSet<String>();
        for (int i = 0; i < this.pips.size(); i++)
            this.supportedIds.addAll(this.pips.get(i).getSupportedIds());

        if (this.pips.size() == 0)
            log.warn("Initialized empty PIPAttributeFinderModule");
    }

    @Override
    public boolean isDesignatorSupported() {
        return true;
    }

    @Override
    public Set<String> getSupportedIds() {
        return this.supportedIds;
    }

    @Override
    public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer, URI category, EvaluationCtx context) {
        String id = attributeId.toString();
        if (this.supportedIds.contains(id)) {
            int version = 0;
            if (context instanceof XACML3EvaluationCtx) version = XACMLConstants.XACML_VERSION_3_0;
            else if (context instanceof XACML2EvaluationCtx) version = XACMLConstants.XACML_VERSION_2_0;

            PIP pip;
            for (int i = 0; i < this.pips.size(); i++) {
                pip = this.pips.get(i);

                if (pip.getSupportedIds().contains(id)) {
                    try {
                        Map<String, Attribute> attributeMap = pip.getAttributes(
                                attributeType, attributeId, category, context, version);
                        // check for the attribute
                        Attribute attribute = attributeMap.get(id);
                        if (attribute != null) {
                            // inject all retrieved attributes into the evaluationCtx, effectively caching them for this request
                            this.injectAttributes(attributeMap, category, context, version);

                            log.debug("Successfully retrieved attribute with id {}, type {}, category {}",
                                    attributeId.toString(), attributeType.toString(), category.toString());
                            return new EvaluationResult(new BagAttribute(attributeType, attribute.getValues()));
                        }
                    } catch (IOException e) {
                        log.warn("Failed to retrieve attribute with id {} and type {} from PIP {}",
                                attributeId.toString(), attributeType.toString(), pip.getClass().toString(), e);
                    }
                }
            }
        }

        // if we're here then we couldn't find the attribute or we do not support it
        return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
    }

    private void injectAttributes(Map<String, Attribute> attributeMap, URI category, EvaluationCtx context, int version) {
        switch (version) {
            case XACMLConstants.XACML_VERSION_3_0:
                this.injectAttributesXACML3(attributeMap, category, (XACML3EvaluationCtx) context);
                break;
            case XACMLConstants.XACML_VERSION_2_0:
                this.injectAttributesXACML2(attributeMap, category, (XACML2EvaluationCtx) context);
                break;

            default: log.warn("Failed to inject Attributes, invalid EvaluationCtx");
        }
    }

    private void injectAttributesXACML2(Map<String, Attribute> attributeMap, URI category, XACML2EvaluationCtx context) {
        Map ctxAttributeMap;
        if (category.toString().equals(XACMLConstants.SUBJECT_CATEGORY))
            ctxAttributeMap = (Map) context.getSubjectMap().get(category);
        else
            ctxAttributeMap = context.getAttributeMap(category.toString());

        for (String key : attributeMap.keySet()) {
            Set<Attribute> set = new HashSet<Attribute>();
            set.add(attributeMap.get(key));
            ctxAttributeMap.put(key, set);
        }
    }

    private void injectAttributesXACML3(Map<String, Attribute> attributeMap, URI category, XACML3EvaluationCtx context) {
        List<Attributes> attributesList = context.getMapAttributes().get(category.toString());
        if (attributesList == null)
            attributesList = new ArrayList<Attributes>();

        Set<Attribute> attributeSet = new HashSet<Attribute>(attributeMap.values());

        if (attributesList.size() > 0)
            attributesList.get(0).getAttributes().addAll(attributeSet);
        else
            attributesList.add(new Attributes(category, attributeSet));

        context.getMapAttributes().put(category.toString(), attributesList);
    }

}
