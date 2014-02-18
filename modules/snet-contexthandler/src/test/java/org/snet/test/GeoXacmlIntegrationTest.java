package org.snet.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.xml.parse.BasicParserPool;
import org.snet.contexthandler.ContextHandler;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 *	Basic tests for evaluating geoxacml integration
 */
public class GeoXacmlIntegrationTest {
	
	String policy1 = "/home/malik/Downloads/TRESOR/Test/policies/geoxacml/policy-1-time-stamp.xml";
	String policy2 = "/home/malik/Downloads/TRESOR/Test/policies/geoxacml/policy-2-coordinates.xml";
	String policy3 = "/home/malik/Downloads/TRESOR/Test/policies/geoxacml/policy-3-actions.xml";
	
	static String requestPath = "/home/malik/Downloads/TRESOR/Test/requests/geoxacml/";
	static String responsePath = "/home/malik/Downloads/TRESOR/Test/responses/geoxacml/";
	
	static File[] reqs = new File(requestPath).listFiles();
	static File[] resps = new File(responsePath).listFiles();
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		GeoXACML.initialize();
		Balana.getInstance().getAttributeFactory()
			.addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());

		Arrays.sort(reqs);
		Arrays.sort(resps);		
	}

	@Test
	public void testPolicy1() {
		PDP pdp = TestUtils.getPDPNewInstance(policy1);
		ContextHandler cx = new ContextHandler(new BasicParserPool(), pdp);
		Scanner scanner;
		
		for (int i = 0; i < 3; i++) {
			try {
				scanner = new Scanner(resps[i]).useDelimiter("\\A");
				String actualResp = cx.handle(new FileReader(reqs[i]));
				String expectedResp = scanner.next();				
				assertTrue(actualResp.equals(expectedResp));
				scanner.close();
			} catch (Exception e) { fail("Exception thrown"); }			
		}
	}
	
	@Test
	public void testPolicy2() {
		PDP pdp = TestUtils.getPDPNewInstance(policy2);
		ContextHandler cx = new ContextHandler(new BasicParserPool(), pdp);
		Scanner scanner;
		
		for (int i = 3; i < 5; i++) {
			try {
				scanner = new Scanner(resps[i]).useDelimiter("\\A");
				String actualResp = cx.handle(new FileReader(reqs[i]));
				String expectedResp = scanner.next();				
				assertTrue(actualResp.equals(expectedResp));
				scanner.close();
			} catch (Exception e) { fail("Exception thrown"); }			
		}
	}
	
	@Test
	public void testPolicy3() {
		PDP pdp = TestUtils.getPDPNewInstance(policy3);
		ContextHandler cx = new ContextHandler(new BasicParserPool(), pdp);
		Scanner scanner;
		
		for (int i = 5; i < reqs.length; i++) {
			try {
				scanner = new Scanner(resps[i]).useDelimiter("\\A");
				String actualResp = cx.handle(new FileReader(reqs[i]));
				String expectedResp = scanner.next();				
				assertTrue(actualResp.equals(expectedResp));
				scanner.close();
			} catch (Exception e) { fail("Exception thrown"); }			
		}
	}

}
