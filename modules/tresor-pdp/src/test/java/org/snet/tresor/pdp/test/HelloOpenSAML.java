package org.snet.tresor.pdp.test;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Statement;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.impl.RequestTypeImpl;
import org.opensaml.xacml.profile.saml.SAMLProfileConstants;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionQueryType;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionStatementType;
import org.opensaml.xacml.profile.saml.impl.XACMLAuthzDecisionQueryTypeImpl;
import org.opensaml.xacml.profile.saml.impl.XACMLAuthzDecisionQueryTypeImplBuilder;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.snet.tresor.pdp.contexthandler.saml.xacml3.XACML3RequestType;
import org.snet.tresor.pdp.contexthandler.saml.xacml3.XACML3ResponseType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class used for on the fly testing during development, WILL be changed very frequently 
 * @author malik
 */
public class HelloOpenSAML {
	
	public static void main(String[] args) throws Exception {
		// prepare opensaml
		SAMLConfig.InitSAML();
		BasicParserPool parserPool = new BasicParserPool();
		parserPool.setNamespaceAware(true);
		
		String xacmlResponseString = "<Response xmlns='urn:oasis:names:tc:xacml:3.0:core:schema:wd-17'>"
				+ "<Result><Decision>Deny</Decision><Status><StatusCode Value='urn:oasis:names:tc:xacml:1.0:status:ok'/>"
				+ "</Status></Result></Response>";				
		Element xacmlResponseXML = parserPool.parse(new StringReader(xacmlResponseString)).getDocumentElement();
		
		ResponseType xacmlResponse = (ResponseType) Configuration.getUnmarshallerFactory()
				.getUnmarshaller(XACML3ResponseType.DEFAULT_ELEMENT_NAME)
				.unmarshall(xacmlResponseXML);
		
		Assertion assertion = (Assertion) Configuration.getBuilderFactory().getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
				.buildObject(Assertion.DEFAULT_ELEMENT_NAME);
		
		Issuer issuer = (Issuer) Configuration.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME)
		.buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue("Omer");				
		
		XACMLAuthzDecisionStatementType xacmlDecisionStatement = (XACMLAuthzDecisionStatementType) Configuration.getBuilderFactory()
				.getBuilder(XACMLAuthzDecisionStatementType.TYPE_NAME_XACML30)
				.buildObject(Statement.DEFAULT_ELEMENT_NAME,
						XACMLAuthzDecisionStatementType.TYPE_NAME_XACML30);
		xacmlDecisionStatement.setResponse(xacmlResponse);
		
		// set needed elements
		assertion.setID("1234");
		assertion.setIssuer(issuer);
		assertion.setIssueInstant(DateTime.now());
		assertion.setVersion(SAMLVersion.VERSION_20);
		assertion.getStatements().add(xacmlDecisionStatement);		
		
		// marshall
		Element assertionXML = Configuration.getMarshallerFactory()
				.getMarshaller(Assertion.DEFAULT_ELEMENT_NAME)
				.marshall(assertion);

		StringWriter buffer = new StringWriter();
		TransformerFactory.newInstance().newTransformer()
			.transform(new DOMSource(assertionXML), new StreamResult(buffer));

		System.out.println(buffer.toString());
		
		XACMLRequest2XACMLAuthzDecisionQuery(parserPool);
	}
	
	private static void XACMLRequest2XACMLAuthzDecisionQuery(ParserPool parserPool) throws Exception {
		// load xacml Request
		Element xacmlRequestXML = parserPool.parse(new FileInputStream(
				new File("/home/malik/workspace/balana/modules/balana-core/src/test/resources/basic/3/requests/request_0001_01.xml"))
				).getDocumentElement();
		RequestType xacmlRequest = (RequestType) Configuration.getUnmarshallerFactory()
				.getUnmarshaller(XACML3RequestType.DEFAULT_ELEMENT_NAME)
				.unmarshall(xacmlRequestXML);
				
		// prepare XacmlAuthzDecisionQueryType
		XACMLAuthzDecisionQueryTypeImplBuilder xacmlDecisionQueryBuilder = (XACMLAuthzDecisionQueryTypeImplBuilder)
				Configuration.getBuilderFactory().getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);
		XACMLAuthzDecisionQueryType xacmlDecisionQuery = xacmlDecisionQueryBuilder.buildObject(
				SAMLProfileConstants.SAML20XACML20P_NS,
				XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_LOCAL_NAME,
				SAMLProfileConstants.SAML20XACMLPROTOCOL_PREFIX);
		
		Issuer issuer = (Issuer) Configuration.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME)
				.buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue("Omer");
		
		// set needed elements
		xacmlDecisionQuery.setID("1234");
		xacmlDecisionQuery.setDestination("localhost");
		xacmlDecisionQuery.setIssuer(issuer);
		xacmlDecisionQuery.setVersion(SAMLVersion.VERSION_20);
		xacmlDecisionQuery.setRequest(xacmlRequest);
		xacmlDecisionQuery.setIssueInstant(DateTime.now());
		
		// marshall back to xml
		Element xacmlDecisionQueryXML = Configuration.getMarshallerFactory()
				.getMarshaller(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20)
				.marshall(xacmlDecisionQuery);
		
		StringWriter buffer = new StringWriter();
		TransformerFactory.newInstance().newTransformer()
			.transform(new DOMSource(xacmlDecisionQueryXML), new StreamResult(buffer));

		System.out.println(buffer.toString());
	
	}
	
	private static void XACMLAuthzDecisionQuery2XACMLRequestTest(ParserPool parserPool) throws Exception {
		
		InputStream in = new FileInputStream(new File("testresources/XACMLAuthzDecisionQuery.xml"));
		Document doc = parserPool.parse(in);
		Element elem = doc.getDocumentElement();
		
		UnmarshallerFactory fac = Configuration.getUnmarshallerFactory();
		Unmarshaller unmarshaller = fac.getUnmarshaller(elem);
		System.out.println("got unmarshaller");
		
		XACMLAuthzDecisionQueryTypeImpl xacmlQuery = (XACMLAuthzDecisionQueryTypeImpl) unmarshaller.unmarshall(elem);
		RequestTypeImpl req = (RequestTypeImpl) xacmlQuery.getRequest();		
		Element xacmlElem = req.getDOM();
		
		TransformerFactory transFac = TransformerFactory.newInstance();
		Transformer trans = transFac.newTransformer();
		StringWriter buffer = new StringWriter();
		trans.transform(new DOMSource(xacmlElem), new StreamResult(buffer));
		
		System.out.println(buffer.toString());
	}

}
