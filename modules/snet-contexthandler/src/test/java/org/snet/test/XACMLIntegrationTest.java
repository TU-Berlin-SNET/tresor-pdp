package org.snet.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringReader;
import java.util.Scanner;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.xml.parse.BasicParserPool;
import org.snet.contexthandler.ContextHandler;
import org.snet.saml.SAMLConfig;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.attr.AttributeFactory;
import org.wso2.balana.attr.StandardAttributeFactory;
import org.wso2.balana.ctx.ResponseCtx;

public class XACMLIntegrationTest {
	
	String xacmlReqRoot = TestConstants.RequestRoot + "/xacml";
	String xacmlRespRoot = TestConstants.ResponseRoot + "/xacml";
	String samlReqRoot = TestConstants.RequestRoot + "/xacmlsaml";
	String samlRespRoot = TestConstants.ResponseRoot + "/xacmlsaml";
	
	BasicParserPool pp = new BasicParserPool();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		SAMLConfig.InitSAML();
		GeoXACML.initialize();

		AttributeFactory sattr = StandardAttributeFactory.getNewFactory();
		sattr.addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
		Balana.getInstance().setAttributeFactory(sattr);
	}

	@Test
	public void testXACML() {
		doTestLoop(xacmlReqRoot, xacmlRespRoot);
	}
	
	@Test
	public void testSAML() {
		fail("sample files missing");
//		doTestLoop(samlReqRoot, samlRespRoot);
	}
	
	private void doTestLoop(String reqRoot, String respRoot) {
		String policyNr, reqRespNr, policy;
		PDP pdp = null;
		
		ContextHandler cx = new ContextHandler(new BasicParserPool(), pdp);
		
		String reqRespType = (reqRoot.contains("saml")) ? TestConstants.ReqRespTypeSAML : TestConstants.ReqRespTypeXACML;
		String req, expectedResponse, actualResponse;
		
		// loop through policies
		for (int i = 1; i <= TestConstants.PolicyCount ; i++) {
			policyNr = (i < 10) ? "000" + i : "00" + i;			
			policy = TestConstants.PolicyRoot + "/TestPolicy_" + policyNr + ".xml";

			if (new File(policy).isFile()) {
				pdp = TestUtils.getPDPNewInstance(TestConstants.PolicyRoot + "/TestPolicy_" + policyNr + ".xml");
				cx.setPDP(pdp);
			} else {
				continue;
			}

			// max. 10 different cases per policy
			for (int j = 1; j <= 10; j++) {
				reqRespNr = "0" + j;
				// get req and expected response
				req = getReqOrResp(policyNr, reqRespNr, reqRoot, TestConstants.ReqType);
				expectedResponse = getReqOrResp(policyNr, reqRespNr, respRoot, TestConstants.RespType);
				if (req != null && expectedResponse != null) {
					// get actual response
					actualResponse = cx.handle(new StringReader(req));
					
					// evaluate
					if (reqRespType.equals(TestConstants.ReqRespTypeXACML)) {
						assertTrue(isSameResponseXACML(actualResponse, expectedResponse));
					}
					if (reqRespType.equals(TestConstants.ReqRespTypeSAML)) {
						assertTrue(isSameResponseSAML(actualResponse, expectedResponse));
					}
					
				} else {
					break;
				}
			}
		}
	}
	
	private String getReqOrResp(String policyNr, String reqRespNr, String rootPath, String type) {
		File file = new File(rootPath + "/" + type + "_" + policyNr + "_" + reqRespNr + ".xml");
		String s = null;
		if (file.isFile()) {
			try {
				System.out.println(file.getName());
				Scanner sc = new Scanner(file);
				s = sc.useDelimiter("\\A").next();
				sc.close();
			} catch (Exception e) { e.printStackTrace(); }
		}
		
		return s;
	}
	
	private boolean isSameResponseXACML(String actual, String expected) {
		boolean result = false;

		try {
			actual = removeWhitespaces(actual);
			expected = removeWhitespaces(expected);
			
			ResponseCtx actualResponse = ResponseCtx.getInstance(pp.parse(new StringReader(actual)).getDocumentElement());
			ResponseCtx expectedResponse = ResponseCtx.getInstance(pp.parse(new StringReader(expected)).getDocumentElement());
			
			result = TestUtils.isMatchingXACML(actualResponse, expectedResponse);
			
		} catch (Exception e) { e.printStackTrace(); }

		return result;
	}
	
	private boolean isSameResponseSAML(String actual, String expected) {
		boolean result = false;
		
		// TODO implement test
		
		return result;
	}
	
	private String removeWhitespaces(String input) {
		String output = input;
        
        // remove whitespaces and formatting
        Scanner scanner = new Scanner(output);
        output = scanner.useDelimiter("\\A").next()
        		.replaceAll(">\\s*", ">").replaceAll("\\s*<", "<")
        		.replaceAll("\n", "");
		scanner.close();
        
		return output;
	}
    
}
