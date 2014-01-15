/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.xacml.role;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.geoserver.xacml.geoxacml.GeoXACMLConfig;
import org.geoserver.xacml.geoxacml.XACMLConstants;
import org.geoserver.xacml.geoxacml.XACMLUtil;
import org.geotools.xacml.geoxacml.attr.GMLVersion;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;

import org.wso2.balana.AbstractObligation;
import org.wso2.balana.xacml2.Obligation;
import org.wso2.balana.attr.AnyURIAttribute;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BooleanAttribute;
import org.wso2.balana.attr.DateTimeAttribute;
import org.wso2.balana.attr.DoubleAttribute;
import org.wso2.balana.attr.IntegerAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.xacml2.RequestCtx;
import org.wso2.balana.ctx.ResponseCtx;
import org.wso2.balana.ctx.xacml2.Result;
import com.vividsolutions.jts.geom.Geometry;
import org.wso2.balana.ObligationResult;
import org.wso2.balana.ctx.AbstractResult;
//import org.wso2.balana.ctx.AbstractResult;

/**
 * Spring Security implementation for {@link XACMLRoleAuthority}
 * 
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLDefaultRoleAuthority implements XACMLRoleAuthority {

    private static InheritableThreadLocal<Set<Authentication>> AlreadyPrepared = new InheritableThreadLocal<Set<Authentication>>();

    public <T extends UserDetails> void transformUserDetails(T details) {
        //for (int i = 0; i < details.getAuthorities().length; i++) {
        for (int i = 0; i < details.getAuthorities().size(); i++) {
            //commented temporary to see if we need it
            //details.getAuthorities()[i] = new XACMLRole(details.getAuthorities()[i].getAuthority());
        }
    }

    public void prepareRoles(Authentication auth) {

        // Trying to avoid multiple processing within one thread, result cannot change
        if (AlreadyPrepared.get() == null) {
            AlreadyPrepared.set(new HashSet<Authentication>());
        }

        if (AlreadyPrepared.get().contains(auth)) {
            return; // nothing todo
        }
        List<RequestCtx> requests = new ArrayList<RequestCtx>(auth.getAuthorities().size());
        String userName = null;
        if (auth.getPrincipal() instanceof UserDetails)
            userName = ((UserDetails) auth.getPrincipal()).getUsername();
        if (auth.getPrincipal() instanceof String) {
            userName = auth.getPrincipal().toString();
        }

        for (GrantedAuthority ga : auth.getAuthorities()) {
            requests.add(GeoXACMLConfig.getRequestCtxBuilderFactory()
                    .getXACMLRoleRequestCtxBuilder((XACMLRole) ga, userName).createRequestCtx());
        }

        List<ResponseCtx> responses = GeoXACMLConfig.getXACMLTransport().evaluateRequestCtxList(
                requests);

        outer: for (int i = 0; i < responses.size(); i++) {
            ResponseCtx response = responses.get(i);
            //XACMLRole role = (XACMLRole) auth.getAuthorities()[i];
            XACMLRole role = null;
            for (AbstractResult result : response.getResults()) {
                if (result.getDecision() != Result.DECISION_PERMIT) {
                    role.setEnabled(false);
                    continue outer;
                }
                role.setEnabled(true);
                setUserProperties(auth, result, role);
            }

        }
        AlreadyPrepared.get().add(auth); // avoid further processing within one thread
    }

    private void setUserProperties(Authentication auth, AbstractResult result, XACMLRole role) {

        if (role.isRoleAttributesProcessed())
            return; // already done

        if (auth.getPrincipal() == null || auth.getPrincipal() instanceof String) {
            role.setRoleAttributesProcessed(true);
            return;
        }
        
        //variable created to get the Id of the obligation and for getAssignments() method
        AbstractObligation obligationId = null;
        Obligation obligationGetAssigment = null;
        
        for (ObligationResult obligation : result.getObligations()) {
            if (XACMLConstants.UserPropertyObligationId.equals(obligationId.getId().toString()))
                setRoleParamsFromUserDetails(auth, obligation, role);
            if (XACMLConstants.RoleConstantObligationId.equals(obligationId.getId().toString()))
                setRoleParamsFromConstants(obligationGetAssigment, role);

        }
        role.setRoleAttributesProcessed(true);
    }

    private void setRoleParamsFromConstants(Obligation obligation, XACMLRole role) {
        for (Attribute attr : obligation.getAssignments()) {
            role.getAttributes().add(attr);
        }
    }

    private void setRoleParamsFromUserDetails(Authentication auth, ObligationResult obligation,
            XACMLRole role) {

        BeanInfo bi = null;
        Obligation obligationGetAssigment = null;
        try {
            bi = Introspector.getBeanInfo(auth.getPrincipal().getClass());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }

        for (Attribute attr : obligationGetAssigment.getAssignments()) {
            String propertyName = ((StringAttribute) attr.getValue()).getValue();
            for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
                if (pd.getName().equals(propertyName)) {
                    Serializable value = null;
                    try {
                        Object tmp = pd.getReadMethod().invoke(auth.getPrincipal(), new Object[0]);
                        if (tmp == null)
                            continue;
                        if (tmp instanceof Serializable == false) {
                            throw new RuntimeException("Role params must be serializable, "
                                    + tmp.getClass() + " is not");
                        }
                        value = (Serializable) tmp;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    // special check for geometries
                    if (value instanceof Geometry) {
                        if (((Geometry) value).getUserData() == null)
                            throw new RuntimeException("Property: " + propertyName
                                    + " : Geometry must have srs name as userdata");
                    }
                    AttributeValue attrValue = createAttributeValueFromObject(value);
                    Attribute xacmlAttr = new Attribute(attr.getId(), null, null, attrValue);
                    role.getAttributes().add(xacmlAttr);
                }
            }
        }

    }

    protected AttributeValue createAttributeValueFromObject(Serializable object) {
        AttributeValue retVal = null;

        if (object instanceof String)
            retVal = new StringAttribute((String) object);
        if (object instanceof URI)
            retVal = new AnyURIAttribute((URI) object);
        if (object instanceof Boolean)
            retVal = ((Boolean) object) ? BooleanAttribute.getTrueInstance() : BooleanAttribute
                    .getFalseInstance();
        if (object instanceof Double)
            retVal = new DoubleAttribute((Double) object);
        if (object instanceof Float)
            retVal = new DoubleAttribute((Float) object);
        if (object instanceof Integer)
            retVal = new IntegerAttribute((Integer) object);
        if (object instanceof Date)
            retVal = new DateTimeAttribute((Date) object);
        if (object instanceof Geometry) {
            Geometry g = (Geometry) object;
            String gmlType = XACMLUtil.getGMLTypeFor(g);
            try {
                retVal = new GeometryAttribute(g, g.getUserData().toString(), null,
                        GMLVersion.Version3, gmlType);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return retVal;
    }

}
