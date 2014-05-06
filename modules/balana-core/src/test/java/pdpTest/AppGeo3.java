/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pdpTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.config.GeoXACML;

/**
 *
 * @author zequeira
 */
public class AppGeo3 
{
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
        
        //balana.getAttributeFactory().addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
    }	
	
    public static void main( String[] args ) throws FileNotFoundException
    {
    	initBalana();
        
        GeoXACML.initialize();		
        System.out.println("GeoXACML initialized");
        
        PDP pdp = new PDP(balana.getPdpConfig());
        
        Date timeDate = new Date();
        String unixTime = String.valueOf(timeDate.getTime()/1000);
        System.out.println("The string is: "+unixTime);
        
        
        //Policy for this request
        //<string>/opt/NetBeans/TRESOR/WSO2-Balana-Basic_OK/policies-request/geoxacml-policies/policy-3-actions.xml</string>
        
        String request = new Scanner(new File("/opt/Netbeans/TRESOR/balana/policies-request/missing attributes/geo_time-Request.xml")).useDelimiter("\\A").next();
        //String request = new Scanner(new File("/opt/Netbeans/TRESOR/Test_Request.xml")).useDelimiter("\\A").next();
        //String request = new Scanner(new File("/opt/Netbeans/TRESOR/Request_Date_3.0.xml")).useDelimiter("\\A").next();
        
        result = pdp.evaluate(request);
        System.out.println("La respuesta es: "+result);
    }
}