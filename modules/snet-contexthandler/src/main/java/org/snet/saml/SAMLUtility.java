package org.snet.saml;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoGenerator;
import org.opensaml.xml.security.keyinfo.KeyInfoGeneratorFactory;
import org.opensaml.xml.security.keyinfo.KeyInfoGeneratorManager;
import org.opensaml.xml.security.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Element;

/**
 * 
 * 
 */
public class SAMLUtility {
    
    private static BasicCredential signingCredential = null;
    private static BasicX509Credential publicCredential = null;
    private BasicParserPool parserPool = null;
    private static String privateKey_publicKey = null;
    private static PrivateKey privKey = null;
    private static PublicKey publicKey= null;
    
        public SAMLUtility() throws ConfigurationException, NoSuchAlgorithmException, NoSuchProviderException, FileNotFoundException, IOException, InvalidKeySpecException {
            parserPool = new BasicParserPool();
            parserPool.setNamespaceAware(true);
            
            File file = new File("/opt/Netbeans/TRESOR/balana/.ssh/certificate/privkey.der");
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int) file.length()];
            dis.readFully(keyBytes);
            dis.close();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privKey = kf.generatePrivate(spec);
            
            File f = new File("/opt/Netbeans/TRESOR/balana/.ssh/public_key.der");
            fis = new FileInputStream(f);
            dis = new DataInputStream(fis);
            keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            X509EncodedKeySpec spec2 = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf2 = KeyFactory.getInstance("RSA");
            publicKey = kf2.generatePublic(spec2);
	}
    
        public static Signature getSignature() {
            signingCredential = new BasicCredential();
            Signature signature = (Signature) Configuration.getBuilderFactory()
                                        .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                                        .buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            if(privateKey_publicKey == "Private"){
                signingCredential.setPrivateKey(privKey);
            }
            else if (privateKey_publicKey == "Public"){
                signingCredential.setPublicKey(publicKey);
            }
            
            signingCredential.setUsageType(UsageType.SIGNING);

            signature.setSigningCredential(signingCredential);
            //signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            signature.setKeyInfo(signature.getKeyInfo());
            
            return signature;    
        }
        
        public static Signature getSignatureCertificate() throws CertificateException, FileNotFoundException, IOException, org.opensaml.xml.security.SecurityException{
            
            publicCredential = new BasicX509Credential();
            File certificateFile = new File("/opt/Netbeans/TRESOR/balana/.ssh/certificate/cacert.crt");
            
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream fileStream = new FileInputStream(certificateFile);
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fileStream);
            fileStream.close();
            
            publicCredential.setEntityCertificate(certificate);
            
            Signature signature = (Signature) Configuration.getBuilderFactory()
                                        .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                                        .buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            publicCredential. setPrivateKey(privKey);
            publicCredential.setUsageType(UsageType.SIGNING);
            
            signature.setSigningCredential(publicCredential);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            
            SecurityConfiguration secConfiguration = Configuration.getGlobalSecurityConfiguration();
            NamedKeyInfoGeneratorManager namedKeyInfoGeneratorManager = secConfiguration.getKeyInfoGeneratorManager(); 
            KeyInfoGeneratorManager keyInfoGeneratorManager = namedKeyInfoGeneratorManager.getDefaultManager(); 
            KeyInfoGeneratorFactory keyInfoGeneratorFactory = keyInfoGeneratorManager.getFactory(publicCredential); 
            KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance(); 
            KeyInfo keyInfo = keyInfoGenerator.generate(publicCredential);
            
            signature.setKeyInfo(keyInfo);
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
        
        public static void checkSignatureCertificate(XACMLAuthzDecisionQueryTypeImpl SAMLxacmlRequest) throws ValidationException, CertificateException, FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
            
            //Getting the Public Key from the Certificate
            publicCredential = new BasicX509Credential();
            File publicKeyFile = new File("/opt/Netbeans/TRESOR/balana/.ssh/certificate/cacert.crt");
            
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream fileStream = new FileInputStream(publicKeyFile);
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fileStream);
            fileStream.close();

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(certificate.getPublicKey().getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(publicKeySpec);

            publicCredential.setPublicKey(publicKey);

            //check the signature follows SAML standard
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(SAMLxacmlRequest.getSignature()); 

            //check that the signature is correct
            SignatureValidator signatureValidator = new SignatureValidator(publicCredential);
            signatureValidator.validate(SAMLxacmlRequest.getSignature());
        }
        
        
        
        /**
         * Creates a new SAML-XACML Response Signed from a common XACML .xml file or String Response
         * 
         * @param xacmlResponseString - the xml response from the pdp in String format
         * @return The String SAML-XACML Response Signed
         * @throws Exception
         */
        public String makeSAMLxacmlResponse(String xacmlResponseString) throws Exception{
						
		Element xacmlResponseXML = parserPool.parse(new StringReader(xacmlResponseString)).getDocumentElement();
                QName qName= new QName(xacmlResponseXML.getNamespaceURI(), xacmlResponseXML.getLocalName(), XACMLConstants.XACMLCONTEXT_PREFIX);
                                
		ResponseType xacmlResponse = (ResponseType) Configuration.getUnmarshallerFactory()
				.getUnmarshaller(qName)
				.unmarshall(xacmlResponseXML);
		
		Assertion assertion = (Assertion) Configuration.getBuilderFactory().getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
				.buildObject(Assertion.DEFAULT_ELEMENT_NAME);
		
                //privateKey_publicKey = "Private";
                //Signature signature = getSignature();
                
                Signature signature = getSignatureCertificate();
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
		
                return buffer.toString();
        }
	
	public String XACMLRequest2XACMLAuthzDecisionQuery() throws Exception {
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
				SAMLProfileConstants.SAML20XACML30P_NS,
				XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_LOCAL_NAME,
				SAMLProfileConstants.SAML20XACMLPROTOCOL_PREFIX);
		
                privateKey_publicKey = "Private";
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
		
                return buffer.toString();
	}
	
	public String XACMLAuthzDecisionQuery2XACMLRequest(Element SAMLxacmlRequestXML) throws Exception {
		
		UnmarshallerFactory fac = Configuration.getUnmarshallerFactory();
		Unmarshaller unmarshaller = fac.getUnmarshaller(SAMLxacmlRequestXML);
		
		XACMLAuthzDecisionQueryTypeImpl xacmlQuery = (XACMLAuthzDecisionQueryTypeImpl) unmarshaller.unmarshall(SAMLxacmlRequestXML);
                
                /*privateKey_publicKey = "Public";
                Signature signature = getSignature();
                checkSignature(xacmlQuery);*/
                
                checkSignatureCertificate(xacmlQuery);
                
		RequestTypeImpl req = (RequestTypeImpl) xacmlQuery.getRequest();		
		Element xacmlElem = req.getDOM();
		
		TransformerFactory transFac = TransformerFactory.newInstance();
		Transformer trans = transFac.newTransformer();
		StringWriter buffer = new StringWriter();
		trans.transform(new DOMSource(xacmlElem), new StreamResult(buffer));
		
                return buffer.toString();
	}

}
