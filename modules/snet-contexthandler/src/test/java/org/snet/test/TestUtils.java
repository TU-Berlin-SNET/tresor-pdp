package org.snet.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opensaml.xml.parse.BasicParserPool;
import org.snet.saml.SAMLConfig;
import org.snet.saml.SAMLUtility;
import org.w3c.dom.Element;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.TestConstants;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

/**
 * Class used for on the fly testing during development, WILL be changed very frequently 
 * @author malik
 */
public class TestUtils {

	public TestUtils() { }
	
	public static void main(String[] args) throws Exception {
		SAMLConfig.InitSAML();
		String path = "/home/malik/Downloads/TRESOR/Test/requests/";
//		String path = "/home/malik/Downloads/TRESOR/Test/responses/xacml";
		File file = new File(path);
		
		for (File f : file.listFiles()) {
			if (f.isFile()) {
				convert2XACMLSAML(f);
//				convert2XACMLSAMLResponse(f);
			}				
		}
		
		System.out.println("finished");
	}

	public static void convert2XACMLSAML(File file) throws Exception {
		BasicParserPool pp = new BasicParserPool();
		Element elem = pp.parse(new FileInputStream(file)).getDocumentElement();
		String xAuthz = SAMLUtility.XACMLRequest2XACMLAuthzDecisionQuery(elem);
		
		File out = new File(file.getAbsolutePath() + "xacmlAuthz");
		out.createNewFile();
		PrintWriter writer = new PrintWriter(out);
		writer.write(xAuthz);
		writer.flush();
		writer.close();		
	}
	
	private static void convert2XACMLSAMLResponse(File file) throws Exception {
		BasicParserPool pp = new BasicParserPool();
		Element elem = pp.parse(new FileInputStream(file)).getDocumentElement();
		
		StringWriter buffer = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(new DOMSource(elem), new StreamResult(buffer));
		
		SAMLUtility samlUtil = new SAMLUtility();
		String samlResp = samlUtil.makeSAMLxacmlResponse(buffer.toString());
		
		File out = new File(file.getAbsolutePath() + "xacmlAuthz");
		out.createNewFile();
		PrintWriter writer = new PrintWriter(out);
		writer.write(samlResp);
		writer.flush();
		writer.close();
	}
	
    /**
     * Returns a new PDP instance with new XACML policies
     *
     * @param policyPath, path to folder (or file) containing policies
     * @return a  PDP instance
     */
    public static PDP getPDPNewInstance(String policyPath){

        PolicyFinder finder= new PolicyFinder();

        Set<String> policies = getPolicies(policyPath);
        
        FileBasedPolicyFinderModule testPolicyFinderModule = new FileBasedPolicyFinderModule(policies);
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(testPolicyFinderModule);
        finder.setModules(policyModules);

        Balana balana = Balana.getInstance();
        PDPConfig pdpConfig = balana.getPdpConfig();
        pdpConfig = new PDPConfig(pdpConfig.getAttributeFinder(), finder,
                                                            pdpConfig.getResourceFinder(), true);
        return new PDP(pdpConfig);

    }
    
    private static Set<String> getPolicies(String policyPath) {
    	File file = new File(policyPath);
    	Set<String> policies = new HashSet<String>();
    	
    	if (file.isDirectory()) {
    		for (File f : file.listFiles()) {
    			policies.addAll(getPolicies(f.getAbsolutePath()));
    		}
    	}
    	
    	if (file.isFile()) {
    		policies.add(file.getAbsolutePath());
    	}
    	
    	return policies;
    }

}
