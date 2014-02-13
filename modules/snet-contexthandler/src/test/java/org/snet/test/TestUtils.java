package org.snet.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import org.opensaml.xml.parse.BasicParserPool;
import org.snet.saml.SAMLConfig;
import org.snet.saml.SAMLUtility;
import org.w3c.dom.Element;

/**
 * Class used for on the fly testing during development, WILL be changed very frequently 
 * @author malik
 */
public class TestUtils {

	public TestUtils() { }
	
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
	
	public static void main(String[] args) throws Exception {
		SAMLConfig.InitSAML();
		String path = "/home/malik/Downloads/TRESOR/Test/requests/";
		File file = new File(path);
		
		for (File f : file.listFiles()) {
			convert2XACMLSAML(f);
		}
		
		System.out.println("finished");
	}

}
