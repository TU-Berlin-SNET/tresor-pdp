/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.snet.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;
import org.snet.contexthandler.ContextHandler;
import org.snet.saml.SAMLConfig;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

/**
 *
 * @author zequeira
 */
public class SystemTest {
    
    static Balana balana;
    static PDP pdp;
    static String result;
    
    private static void initBalana() {

        try {
            // using file based policy repository. so set the policy location as system property
            String policyLocation = (new File(".")).getCanonicalPath() + File.separator + "resources";
            System.setProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY, policyLocation);
        } 
        catch (IOException e) {
            System.err.println("Can not locate policy repository");
        }
        
        // create default instance of Balana
        balana = Balana.getInstance();
        
        balana.getAttributeFactory().addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
    }
    
    public static void main( String[] args ) throws FileNotFoundException, UnsupportedEncodingException, ConfigurationException
    {
    	initBalana();
        GeoXACML.initialize();
        SAMLConfig.InitSAML();
        pdp = new PDP(balana.getPdpConfig());
        
        //Policy for this request
        //<string>/opt/NetBeans/TRESOR/WSO2-Balana/policies-request/geoxacml-policies/policy-2-coordinates.xml</string>
        String request = new Scanner(new File("/opt/Netbeans/TRESOR/Signed_Request_OK.xml")).useDelimiter("\\A").next();
        
        ContextHandler contextHandler = new ContextHandler(new BasicParserPool(), pdp);
        
        InputStream requestStream = new ByteArrayInputStream(request.getBytes("UTF-8"));
        
        String response = contextHandler.handle(requestStream); 
       
        System.out.println("La respuesta es: "+response);
    }
    
}
