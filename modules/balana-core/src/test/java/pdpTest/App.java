/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pdpTest;
/** ************************************************* 
package pdpTest.pdpTest;*/

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.wso2.balana.Balana;
import org.wso2.balana.ConfigurationStore;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

/**
 * Hello world!
 *
 */
public class App 
{
    
	static Balana balana;
	static PDP pdp;
        static String policyLocation;
        static String configFileLocation;
        static String result;
        
	private static void initBalana() {

        try {
            // using file based policy repository. so set the policy location as system property
            policyLocation = (new File(".")).getCanonicalPath() + File.separator + "src/test/resources/basic/3/policies";
            //policyLocation = (new File(".")).getCanonicalPath() + File.separator + "resources";
            //policyLocation = (new File(".")).getCanonicalPath() + File.separator + "src/test/resources";
            configFileLocation = "/opt/NetBeans/TRESOR/WSO2-Balana/modules/balana-core/src/main/resources/config-2.xml";
            
            System.setProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY, policyLocation);
            //System.setProperty(ConfigurationStore.PDP_CONFIG_PROPERTY, configFileLocation);
            
            System.out.println("Location_1rst: "+policyLocation);
        } 
        catch (IOException e) {
            System.err.println("Can not locate policy repository");
        }
        
        // create default instance of Balana
        System.out.println("Location: "+policyLocation);
        balana = Balana.getInstance();
       
    }

    /**
     * Returns a new PDP instance with new XACML policies
     */
    private static void getPDPNewInstance() {

        PDPConfig pdpConfig = balana.getPdpConfig();

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
    	getPDPNewInstance();
       
        //Policy for this request
        //<string>/opt/NetBeans/TRESOR/WSO2-Balana/modules/balana-core/src/test/resources/basic/3/policies/TestPolicy_0001.xml</string>
        
        String req =
                
  "<Request xmlns='urn:oasis:names:tc:xacml:3.0:core:schema:wd-17' ReturnPolicyIdList='false' CombinedDecision='false'>"
+"   <Attributes Category='urn:oasis:names:tc:xacml:1.0:subject-category:access-subject' >"
+"     <Attribute IncludeInResult='false' AttributeId='urn:oasis:names:tc:xacml:1.0:subject:subject-id'>"
+"      <AttributeValue DataType='http://www.w3.org/2001/XMLSchema#string'>bob</AttributeValue>"
+"	</Attribute>"
+"  </Attributes>"
+"  <Attributes Category='urn:oasis:names:tc:xacml:3.0:attribute-category:resource'>"
+"    <Attribute IncludeInResult='false' AttributeId='urn:oasis:names:tc:xacml:1.0:resource:resource-id'>"
+"      <AttributeValue DataType='http://www.w3.org/2001/XMLSchema#string'>foo/foo1</AttributeValue>"
+"    </Attribute>"
+"  </Attributes>"
+"  <Attributes Category='urn:oasis:names:tc:xacml:3.0:attribute-category:action'>"
+"    <Attribute IncludeInResult='false' AttributeId='urn:oasis:names:tc:xacml:1.0:action:action-id'>"
+"      <AttributeValue DataType='http://www.w3.org/2001/XMLSchema#string'>bar1</AttributeValue>"
+"    </Attribute>"
+"  </Attributes>"
+"</Request>";
        
        result = pdp.evaluate(req);
        System.out.println("La respuesta es: "+result);
    }
}

