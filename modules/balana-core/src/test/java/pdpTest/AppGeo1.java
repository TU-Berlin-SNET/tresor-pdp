/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pdpTest;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
public class AppGeo1 
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
        
        balana.getAttributeFactory().addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
    }

    /**
     * Returns a new PDP instance with new XACML policies
     */
    private static void getPDPNewInstance() {

        PDPConfig pdpConfig = balana.getPdpConfig();
        
        //PDP pdp = new PDP(balana.getPdpConfig());
        
        // registering new attribute finder. so default PDPConfig is needed to change
        AttributeFinder attributeFinder = pdpConfig.getAttributeFinder();
        List<AttributeFinderModule> finderModules = attributeFinder.getModules();
        finderModules.add(new SampleAttributeFinderModule());
        attributeFinder.setModules(finderModules);
        
        pdp =  new PDP(new PDPConfig(attributeFinder, pdpConfig.getPolicyFinder(), null, true));
        
    }	
	
    public static void main( String[] args )
    {
    	initBalana();
        // create default instance of Balana
        //balana = Balana.getInstance();
        //balana.getAttributeFactory().addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
        
        GeoXACML.initialize();		
        System.out.println("GeoXACML initialized");
        
    	//getPDPNewInstance();
        PDP pdp = new PDP(balana.getPdpConfig());
        
        
        String req = 
        		" <Request xmlns='urn:oasis:names:tc:xacml:2.0:context:schema:os'"
                            +" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'"
                            +" xsi:schemaLocation='urn:oasis:names:tc:xacml:2.0:context:schema:os http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd'>"
                            + "    <Subject/>"
                                +" <Resource> "
                                    +" <Attribute AttributeId='urn:oasis:names:tc:xacml:1.0:resource:resource-id' DataType='http://www.w3.org/2001/XMLSchema#string'> "
                                           +" <AttributeValue>SampleServer</AttributeValue> "
                                    +" </Attribute>"
                                    +" <Attribute AttributeId='RequestBoundingBox' DataType='urn:ogc:def:dataType:geoxacml:1.0:geometry'> "
                                        +" <AttributeValue> "
                                            +" <Box srsName='http://www.opengis.net/gml/srs/epsg.xml#4326'> "
                                                +" <coord><X>-100.0</X><Y>0.0</Y></coord> "
                                                +" <coord><X>100.0</X><Y>100.0</Y></coord> "
                                            +" </Box> "
                                        +" </AttributeValue> " 
                                    +" </Attribute> "
                                +" </Resource> "
                                +" <Action> "
                                    +" <Attribute AttributeId='ServerAction' DataType='http://www.w3.org/2001/XMLSchema#string'> "
                                        +" <AttributeValue>login</AttributeValue> "
                                    +" </Attribute> "
                                    +" <Attribute AttributeId='ServerAction' DataType='http://www.w3.org/2001/XMLSchema#string'> "
                                        +" <AttributeValue>login</AttributeValue> "
                                    +" </Attribute> "
                                +" </Action> "
                                +" <Environment/> "
                            +" </Request> ";
        
        //pdp.evaluate(req);
        result = pdp.evaluate(req);
        System.out.println("La respuesta es: "+result);
    }
}