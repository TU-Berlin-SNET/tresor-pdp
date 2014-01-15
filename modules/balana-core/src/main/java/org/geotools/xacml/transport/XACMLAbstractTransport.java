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

package org.geotools.xacml.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wso2.balana.Indenter;
import org.wso2.balana.ctx.xacml2.RequestCtx;
import org.wso2.balana.ctx.ResponseCtx;

/**
 * Common base class for {@link XACMLTransport} implementations
 * 
 * @author Christian Mueller
 * 
 */
public abstract class XACMLAbstractTransport implements XACMLTransport {

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    public abstract ResponseCtx evaluateRequestCtx(RequestCtx request);

    public abstract List<ResponseCtx> evaluateRequestCtxList(List<RequestCtx> requests);

    protected void log(RequestCtx ctx) {

        if (logger.isLoggable(Level.FINE) == false)
            return;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ctx.encode(out, new Indenter(2));
        logger.fine(out.toString());
        try {
            out.close();
        } catch (IOException e) {
            // do nothing
        }

    }

    protected void log(ResponseCtx ctx) {

        if (logger.isLoggable(Level.FINE) == false)
            return;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ctx.encode(out, new Indenter(2));
        logger.fine(out.toString());
        try {
            out.close();
        } catch (IOException e) {
            // do nothing
        }

    }

}
