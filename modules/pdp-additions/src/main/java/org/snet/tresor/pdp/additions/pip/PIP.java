package org.snet.tresor.pdp.additions.pip;

import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.EvaluationCtx;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public interface PIP {

    public Set<String> getSupportedIds();

    /**
     * Get and return the attribute (or more, e.g time) from pip
     * @param attributeType the type of the attribute
     * @param attributeId the id of the attribute
     * @param category the category of the attribute
     * @param version the xacml version
     * @return map containing attributeId:attribute mappings or empty map
     * @throws IOException
     */
    public Map<String, Attribute> getAttributes(URI attributeType, URI attributeId, URI category,
                                                EvaluationCtx context, int version) throws IOException;

}
