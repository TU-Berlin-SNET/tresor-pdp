/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.geoxacml.cond;

import java.util.List;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.cond.Evaluatable;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;

/**
 * Check if a geometry is simple
 * 
 * @author Christian Mueller
 * 
 */
public class GeometryIsSimple extends GeometryCheckFunction {

    public static final String NAME = NAME_PREFIX + "geometry-is-simple";

    public GeometryIsSimple() {
        super(NAME);
    }

//    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {
    public EvaluationResult evaluate(List<Evaluatable> inputs, EvaluationCtx context) {

        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        GeometryAttribute geomAttr = (GeometryAttribute) (argValues[0]);

        boolean evalResult = false;

        try {
            evalResult = geomAttr.getGeometry().isSimple();
        } catch (Throwable t) {
            return exceptionError(t);
        }
        return EvaluationResult.getInstance(evalResult);

    }

}
