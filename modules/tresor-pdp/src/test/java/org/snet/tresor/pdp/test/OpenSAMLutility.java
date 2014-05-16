package org.snet.tresor.pdp.test;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Statement;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xacml.XACMLConstants;
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
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.validation.ValidationException;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.w3c.dom.Element;

/**
 * Class used for on the fly testing during development, WILL be changed very frequently 
 * @author malik
 */
public class OpenSAMLutility {
    
    private static BasicCredential signingCredential = null;
    
        public static Signature getSignature() throws NoSuchAlgorithmException, NoSuchProviderException{
            
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            kpg.initialize(1024, random);
            KeyPair kp = kpg.genKeyPair();
            Key publicKey = kp.getPublic();
            Key privateKey = kp.getPrivate();
            
            signingCredential = new BasicCredential();  
            signingCredential.setPublicKey(kp.getPublic());  
            signingCredential.setPrivateKey(kp.getPrivate());
            signingCredential.setUsageType(UsageType.SIGNING);
            

            Signature signature = (Signature) Configuration.getBuilderFactory()
                                    .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                                    .buildObject(Signature.DEFAULT_ELEMENT_NAME);

            signature.setSigningCredential(signingCredential);
            //signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            signature.setKeyInfo(signature.getKeyInfo());
            
            System.out.println("La Signature es: "+publicKey.toString());
            
            return signature;    
        }
        
        public static void checkSignature(XACMLAuthzDecisionQueryTypeImpl SAMLxacmlRequest) throws ValidationException {
            
            //check the signature follows SAML standard
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(SAMLxacmlRequest.getSignature());  
            
            //check that the signature is correct
            SignatureValidator sigValidator = new SignatureValidator(signingCredential);
            sigValidator.validate(SAMLxacmlRequest.getSignature());
        }
        
        
        
        /**
         * Creates a new SAML-XACML Response Signed from a common XACML .xml file or String Response
         * 
         * @return The String SAML-XACML Response
         * @throws Exception
         */
        public static String makeSAMLxacmlResponse() throws Exception{
        
                BasicParserPool parserPool = new BasicParserPool();
		parserPool.setNamespaceAware(true);
		/*
                String xacmlResponseString = "<Response xmlns='urn:oasis:names:tc:xacml:3.0:core:schema:wd-17'>"
				+ "<Result><Decision>Deny</Decision><Status><StatusCode Value='urn:oasis:names:tc:xacml:1.0:status:ok'/>"
				+ "</Status></Result></Response>";				
		Element xacmlResponseXML = parserPool.parse(new StringReader(xacmlResponseString)).getDocumentElement();
                */
                // load xacml Response
		Element xacmlResponseXML = parserPool.parse(new FileInputStream(
				new File("/opt/Netbeans/TRESOR/saml20-xacml20-Response.xml"))
				).getDocumentElement();
                
                QName qName= new QName(xacmlResponseXML.getNamespaceURI(), xacmlResponseXML.getLocalName(), XACMLConstants.XACMLCONTEXT_PREFIX);
                                
		ResponseType xacmlResponse = (ResponseType) Configuration.getUnmarshallerFactory()
				.getUnmarshaller(qName)
				.unmarshall(xacmlResponseXML);
		
		Assertion assertion = (Assertion) Configuration.getBuilderFactory().getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
				.buildObject(Assertion.DEFAULT_ELEMENT_NAME);
		
                Signature signature = getSignature();
                assertion.setSignature(signature);
                
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
                
                Signer.signObject(signature);
                
		StringWriter buffer = new StringWriter();
		TransformerFactory.newInstance().newTransformer()
			.transform(new DOMSource(assertionXML), new StreamResult(buffer));

		System.out.println(buffer.toString());
                return buffer.toString();
        }
	
	private static String XACMLRequest2XACMLAuthzDecisionQuery() throws Exception {
                BasicParserPool parserPool = new BasicParserPool();
		parserPool.setNamespaceAware(true);
                
		// load xacml Request
		Element xacmlRequestXML = parserPool.parse(new FileInputStream(
				new File("/opt/Netbeans/TRESOR/OpenSAML/geoxacml-request-coordinate.xml"))
				).getDocumentElement();
                
                QName qName= new QName(xacmlRequestXML.getNamespaceURI(), xacmlRequestXML.getLocalName(), XACMLConstants.XACMLCONTEXT_PREFIX);
                
		RequestType xacmlRequest = (RequestType) Configuration.getUnmarshallerFactory()
				.getUnmarshaller(qName)
				.unmarshall(xacmlRequestXML);
				
		// prepare XacmlAuthzDecisionQueryType
		XACMLAuthzDecisionQueryTypeImplBuilder xacmlDecisionQueryBuilder = (XACMLAuthzDecisionQueryTypeImplBuilder)
				Configuration.getBuilderFactory().getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);
		XACMLAuthzDecisionQueryType xacmlDecisionQuery = xacmlDecisionQueryBuilder.buildObject(
				SAMLProfileConstants.SAML20XACML20P_NS,
				XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_LOCAL_NAME,
				SAMLProfileConstants.SAML20XACMLPROTOCOL_PREFIX);
		
                Signature signature = getSignature();
                xacmlDecisionQuery.setSignature(signature);
                
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
                
                Signer.signObject(signature);
                
		StringWriter buffer = new StringWriter();
		TransformerFactory.newInstance().newTransformer()
			.transform(new DOMSource(xacmlDecisionQueryXML), new StreamResult(buffer));
                
		System.out.println("Request: "+buffer.toString());
                return buffer.toString();
	}
	
	private static void XACMLAuthzDecisionQuery2XACMLRequestTest(String SAMLxacmlRequest) throws Exception {
                BasicParserPool parserPool = new BasicParserPool();
		parserPool.setNamespaceAware(true);
                
                Element SAMLxacmlRequestXML = parserPool.parse(new StringReader(SAMLxacmlRequest)).getDocumentElement();
		
		UnmarshallerFactory fac = Configuration.getUnmarshallerFactory();
		Unmarshaller unmarshaller = fac.getUnmarshaller(SAMLxacmlRequestXML);
		System.out.println("got unmarshaller");
		
		XACMLAuthzDecisionQueryTypeImpl xacmlQuery = (XACMLAuthzDecisionQueryTypeImpl) unmarshaller.unmarshall(SAMLxacmlRequestXML);
                
                checkSignature(xacmlQuery);
                
		RequestTypeImpl req = (RequestTypeImpl) xacmlQuery.getRequest();		
		Element xacmlElem = req.getDOM();
		
		TransformerFactory transFac = TransformerFactory.newInstance();
		Transformer trans = transFac.newTransformer();
		StringWriter buffer = new StringWriter();
		trans.transform(new DOMSource(xacmlElem), new StreamResult(buffer));
		
		System.out.println(buffer.toString());
	}
        
        public static void main(String[] args) throws Exception {
		// prepare opensaml
		SAMLConfig.InitSAML();
                
                String SAMLxacmlResponse = makeSAMLxacmlResponse();
		
		
		String SAMLxacmlRequest = XACMLRequest2XACMLAuthzDecisionQuery();
                //XACMLAuthzDecisionQueryType SAMLxacmlRequest = XACMLRequest2XACMLAuthzDecisionQuery();
                
                XACMLAuthzDecisionQuery2XACMLRequestTest(SAMLxacmlRequest);
                //checkSignature(SAMLxacmlRequest);
	}

}
