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
package org.wso2.balana.cond.geoxacml;

import java.util.List;

import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.DoubleAttribute;
import org.wso2.balana.attr.geoxacml.GeometryAttribute;
import org.wso2.balana.cond.Evaluatable;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;

/**
 * Calculates the difference
 * 
 * @author Christian Mueller
 * 
 */
public class GeometryDistance extends GeometryScalarFunction {

    public static final String NAME = NAME_PREFIX + "geometry-distance";

    public GeometryDistance() {
        super(NAME, 0, new String[] { GeometryAttribute.identifier, GeometryAttribute.identifier },
                new boolean[] { false, false }, DoubleAttribute.identifier, false);

    }

//    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {
    public EvaluationResult evaluate(List<Evaluatable> inputs, EvaluationCtx context) {

        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        GeometryAttribute geomAttr1 = (GeometryAttribute) (argValues[0]);
        GeometryAttribute geomAttr2 = (GeometryAttribute) (argValues[1]);

        double distance = 0;

        try {
            distance = geomAttr1.getGeometry().distance(geomAttr2.getGeometry());
        } catch (Throwable t) {
            return exceptionError(t);
        }

        return new EvaluationResult(new DoubleAttribute(distance));
    }

}
