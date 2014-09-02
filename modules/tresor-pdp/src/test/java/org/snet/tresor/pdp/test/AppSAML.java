
package org.snet.tresor.pdp.test;

import java.io.ByteArrayInputStream;
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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Statement;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xacml.XACMLConstants;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.impl.RequestTypeImpl;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionStatementType;
import org.opensaml.xacml.profile.saml.impl.XACMLAuthzDecisionQueryTypeImpl;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoGenerator;
import org.opensaml.xml.security.keyinfo.KeyInfoGeneratorFactory;
import org.opensaml.xml.security.keyinfo.KeyInfoGeneratorManager;
import org.opensaml.xml.security.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.impl.SignatureBuilder;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.snet.tresor.pdp.contexthandler.saml.xacml3.XACML3RequestType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Class for extract the XACML request from the SAML messages and for wrapping the XACML response from the PDP
 * into SAML message.
 * ALso for checking the integrity of the Signature or the Certificate on the SAML messages
 * received by the Context Handler.
 * @author zequeira
 */
public class AppSAML {
    
    private static BasicCredential signingCredential = null;
    private static BasicX509Credential publicCredential = null;
    private static BasicX509Credential certificateCredential = null;
    private static BasicParserPool parserPool = null;
    private static String privateKey_publicKey = null;
    private static Boolean certificateInfo = false;
    private static PrivateKey privKeyA = null;
    private static PublicKey publicKeyA= null;
    private static PrivateKey privKeyB = null;
    private static PublicKey publicKeyB= null;
    
    // the logger for tracking the events
    private static Log logger = LogFactory.getLog(AppSAML.class);
    
    // root path for balana directory
    private static String path = null;
    
        /**
        * Constructor where we initiate the parser, and load the Private and Public Key to be used 
        * in the signatures process.
        * @author zequeira
        */
        public AppSAML() throws ConfigurationException, NoSuchAlgorithmException, NoSuchProviderException, FileNotFoundException, IOException, InvalidKeySpecException {
            parserPool = new BasicParserPool();
            parserPool.setNamespaceAware(true);
            
            path = (new File("../..")).getCanonicalPath();
            System.out.println("get ACTUAL Path: " +path);
            
            //File file = new File("/opt/Netbeans/TRESOR/balana/.ssh/A-Key/privkeyA_pk8.der");
            File file = new File(path+"/.ssh/A-Key/privkeyA_pk8.der");
            
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int) file.length()];
            dis.readFully(keyBytes);
            dis.close();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privKeyA = kf.generatePrivate(spec);
            
            //File f = new File("/opt/Netbeans/TRESOR/balana/.ssh/A-Key/pubkeyA.der");
            File f = new File(path+"/.ssh/A-Key/pubkeyA.der");
            fis = new FileInputStream(f);
            dis = new DataInputStream(fis);
            keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            X509EncodedKeySpec spec2 = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf2 = KeyFactory.getInstance("RSA");
            publicKeyA = kf2.generatePublic(spec2);
            
            //file = new File("/opt/Netbeans/TRESOR/balana/.ssh/B-Key/privkeyB_pk8.der");
            file = new File(path+"/.ssh/B-Key/privkeyB_pk8.der");
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
            keyBytes = new byte[(int) file.length()];
            dis.readFully(keyBytes);
            dis.close();
            spec = new PKCS8EncodedKeySpec(keyBytes);
            kf = KeyFactory.getInstance("RSA");
            privKeyB = kf.generatePrivate(spec);
            
            //f = new File("/opt/Netbeans/TRESOR/balana/.ssh/B-Key/pubkeyB.der");
            f = new File(path+"/.ssh/B-Key/pubkeyB.der");
            fis = new FileInputStream(f);
            dis = new DataInputStream(fis);
            keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            spec2 = new X509EncodedKeySpec(keyBytes);
            kf2 = KeyFactory.getInstance("RSA");
            publicKeyB = kf2.generatePublic(spec2);
	}
        
        /**
        * Method to get the signature information to use it in the verification process.
        * Used to get the signature object to sign the XACML response.
        * @return signature
        */
        public static Signature getSignature() throws SecurityException {
            
            //SignatureBuilder signatureBuilder = new SignatureBuilder();
            
            signingCredential = new BasicCredential();
            Signature signature = (Signature) Configuration.getBuilderFactory()
                                        .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                                        .buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            //Signature signatureObject = signatureBuilder.buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            /**
            * Establish whether we use the Public Key to check the signature from the incoming SAML message or
            * the Private Key to sign the response generated from the PDP
            * @return signature
            */
            if(privateKey_publicKey == "PrivateA"){
                signingCredential.setPrivateKey(privKeyA);
            } else if (privateKey_publicKey == "PublicA"){
                signingCredential.setPublicKey(publicKeyA);
            } else if(privateKey_publicKey == "PrivateB"){
                signingCredential.setPrivateKey(privKeyB);
            } else if (privateKey_publicKey == "PublicB"){
                signingCredential.setPublicKey(publicKeyB);
                //signingCredential.setPrivateKey(privKeyB);
                //System.out.println("La Signature es: "+publicKeyB.toString());
            }
            signingCredential.setUsageType(UsageType.SIGNING);

            signature.setSigningCredential(signingCredential);
            //signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            signature.setKeyInfo(signature.getKeyInfo());
            
            /*signatureObject.setSigningCredential(signingCredential);
            signatureObject.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signatureObject.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            
            SecurityConfiguration secConfiguration = Configuration.getGlobalSecurityConfiguration();
            NamedKeyInfoGeneratorManager namedKeyInfoGeneratorManager = secConfiguration.getKeyInfoGeneratorManager(); 
            KeyInfoGeneratorManager keyInfoGeneratorManager = namedKeyInfoGeneratorManager.getDefaultManager(); 
            KeyInfoGeneratorFactory keyInfoGeneratorFactory = keyInfoGeneratorManager.getFactory(signingCredential); 
            KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance(); 
            KeyInfo keyInfo = keyInfoGenerator.generate(signingCredential);
            
            signatureObject.setKeyInfo(signatureObject.getKeyInfo());*/
            
            return signature;
        }
        
        /**
        * Method to set the signature information to use it in the verification process.
        * Used to set the Public key to check the signature's request.
        * @return signature
        */
        public static void setSignature() throws SecurityException {
            
            SignatureBuilder signatureBuilder = new SignatureBuilder();
            
            signingCredential = new BasicCredential();
            Signature signature = (Signature) Configuration.getBuilderFactory()
                                        .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                                        .buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            Signature signatureObject = signatureBuilder.buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            /**
            * Establish whether we use the Public Key to check the signature from the incoming SAML message or
            * the Private Key to sign the response generated from the PDP
            * @return signature
            */
            if(privateKey_publicKey == "PrivateA"){
                signingCredential.setPrivateKey(privKeyA);
            } else if (privateKey_publicKey == "PublicA"){
                signingCredential.setPublicKey(publicKeyA);
            } else if(privateKey_publicKey == "PrivateB"){
                signingCredential.setPrivateKey(privKeyB);
            } else if (privateKey_publicKey == "PublicB"){
                signingCredential.setPublicKey(publicKeyB);
                //signingCredential.setPrivateKey(privKeyB);
                //System.out.println("La Signature es: "+publicKeyB.toString());
            }
            signingCredential.setUsageType(UsageType.SIGNING);

            signature.setSigningCredential(signingCredential);
            //signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            signature.setKeyInfo(signature.getKeyInfo());
        }
        
        /**
        * Method to get the Signature object from a certificate to sign responses
        * @throws java.security.cert.CertificateException
        * @return signature
        */
        public static Signature getSignatureCertificate() throws CertificateException, FileNotFoundException, IOException, org.opensaml.xml.security.SecurityException{
            
            certificateCredential = new BasicX509Credential();
            //File certificateFile = new File("/opt/Netbeans/TRESOR/balana/.ssh/A-Key/cacertA.crt");
            File certificateFile = new File(path+"/.ssh/A-Key/cacertA.crt");
            
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream fileStream = new FileInputStream(certificateFile);
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fileStream);
            fileStream.close();
            
            certificateCredential.setEntityCertificate(certificate);
            
            Signature signature = (Signature) Configuration.getBuilderFactory()
                                        .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                                        .buildObject(Signature.DEFAULT_ELEMENT_NAME);
            
            certificateCredential.setPrivateKey(privKeyA);
            certificateCredential.setUsageType(UsageType.SIGNING);
            
            signature.setSigningCredential(certificateCredential);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            
            SecurityConfiguration secConfiguration = Configuration.getGlobalSecurityConfiguration();
            NamedKeyInfoGeneratorManager namedKeyInfoGeneratorManager = secConfiguration.getKeyInfoGeneratorManager(); 
            KeyInfoGeneratorManager keyInfoGeneratorManager = namedKeyInfoGeneratorManager.getDefaultManager(); 
            KeyInfoGeneratorFactory keyInfoGeneratorFactory = keyInfoGeneratorManager.getFactory(certificateCredential); 
            KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance(); 
            KeyInfo keyInfo = keyInfoGenerator.generate(certificateCredential);
            
            signature.setKeyInfo(keyInfo);
            return signature;
        }
        
        /**
        * Method to check the signature integrity of the SAML message received.
        * @param SAMLxacmlRequest, SAML request
        * @throws org.opensaml.xml.validation.ValidationException, if the signature on the SAML message is not valid
        */
        public static void checkSignature(XACMLAuthzDecisionQueryTypeImpl SAMLxacmlRequest) throws ValidationException {
            
            //check the signature follows SAML standard
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(SAMLxacmlRequest.getSignature());  
            
            //check that the signature is correct
            SignatureValidator sigValidator = new SignatureValidator(signingCredential);
            sigValidator.validate(SAMLxacmlRequest.getSignature());
            logger.info("Signature verification from SAML Request Success!!!");
        }
        
        /**
        * Method to check the signature integrity of the SAML message received.
        * @param SAMLxacmlRequest, SAML request
        * @throws org.opensaml.xml.validation.ValidationException, if the signature on the SAML message is not valid
        */
        public static void checkSignature(XACMLAuthzDecisionQueryTypeImpl SAMLxacmlRequest, Element element) throws ValidationException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, SecurityException {
            
            SignatureValidator signatureValidator = null;
            
            NodeList certificateNodeList = element.getElementsByTagName("ds:X509Certificate");
            if (certificateNodeList.getLength() == 0) {
                certificateInfo = false;
                logger.info("There is no Certificate Info in the Request!!!");
                privateKey_publicKey = "PublicA";
                setSignature();
                signatureValidator = new SignatureValidator(signingCredential);
            } else {
                certificateInfo = true;
                String certificatePart = certificateNodeList.item(0).getFirstChild().getNodeValue();
                InputStream is = new ByteArrayInputStream(Base64.decode(certificatePart));

                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(is);
                
                certificateCredential = new BasicX509Credential();
            
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(certificate.getPublicKey().getEncoded());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                publicKeyA = keyFactory.generatePublic(publicKeySpec);

                certificateCredential.setPublicKey(publicKeyA);
                
                signatureValidator = new SignatureValidator(certificateCredential);
            }
            
            //check the signature follows SAML standard
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(SAMLxacmlRequest.getSignature());
            
            //check that the signature is correct
            signatureValidator.validate(SAMLxacmlRequest.getSignature());
            
            logger.info("Signature verification from SAML Request Success!!!");
        }
        
        /**
        * Method to load the certificate, extract the Public Key from it to use it to check 
        * the signature integrity of the SAML message received.
        * @param SAMLxacmlRequest, SAML request
        * @throws org.opensaml.xml.validation.ValidationException, if the signature on the SAML message is not valid
        */
        public static void checkSignatureCertificate(XACMLAuthzDecisionQueryTypeImpl SAMLxacmlRequest, Element element) throws ValidationException, CertificateException, FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, Exception {
            
            
            NodeList certificateNodeList = element.getElementsByTagName("ds:X509Certificate");
            if (certificateNodeList.getLength() == 0) {
                throw new Exception("Cannot find X509Certificate element");
            }
            String certPart = certificateNodeList.item(0).getFirstChild().getNodeValue();
            System.out.println("The cert Part is: "+certPart);
            //InputStream is = new ByteArrayInputStream(certPart.getBytes());
            InputStream is = new ByteArrayInputStream(Base64.decode(certPart));
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            //Certificate certificate = cf.generateCertificate(is);
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(is);
            
            
            
            
                    
            //Getting the Public Key from the Certificate
            publicCredential = new BasicX509Credential();
            /*File publicKeyFile = new File("/opt/Netbeans/TRESOR/balana/.ssh/certificate/cacert.crt");
            
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream fileStream = new FileInputStream(publicKeyFile);
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fileStream);
            fileStream.close();*/

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(certificate.getPublicKey().getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKeyA = keyFactory.generatePublic(publicKeySpec);

            publicCredential.setPublicKey(publicKeyA);

            //check the signature follows SAML standard
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(SAMLxacmlRequest.getSignature()); 

            //check that the signature is correct
            SignatureValidator signatureValidator = new SignatureValidator(publicCredential);
            signatureValidator.validate(SAMLxacmlRequest.getSignature());
        }
        
        /**
         * Creates a new SAML-XACML response signed from a common XACML String response
         * @param xacmlResponseString, the String response from the pdp
         * @return The String SAML-XACML response signed
         * @throws Exception
         */
        public static String makeSAMLxacmlResponse(String xacmlResponseString) throws Exception{
						
		Element xacmlResponseXML = parserPool.parse(new StringReader(xacmlResponseString)).getDocumentElement();
                QName qName= new QName(xacmlResponseXML.getNamespaceURI(), xacmlResponseXML.getLocalName(), XACMLConstants.XACMLCONTEXT_PREFIX);
                                
		ResponseType xacmlResponse = (ResponseType) Configuration.getUnmarshallerFactory()
				.getUnmarshaller(qName)
				.unmarshall(xacmlResponseXML);
		
		Assertion assertion = (Assertion) Configuration.getBuilderFactory().getBuilder(Assertion.DEFAULT_ELEMENT_NAME)
				.buildObject(Assertion.DEFAULT_ELEMENT_NAME);
                
                Signature signature = null; 
                if (certificateInfo) {
                    signature = getSignatureCertificate();
                } else {
                    privateKey_publicKey = "PrivateB";
                    signature = getSignature();
                }
                assertion.setSignature(signature);
                
                /*privateKey_publicKey = "Private";
                Signature signature = getSignature();
                
                Signature signature = getSignatureCertificate();
                assertion.setSignature(signature);*/
                
		Issuer issuer = (Issuer) Configuration.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME)
		.buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue("Zequeira");				
		
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
        
        /**
        * Method to extract the XACML request from the SAML message.
        * @param SAMLxacmlRequestXML, The SAML message signed with the XACML request on it
        * @return The XACML request to give to the PDP
        */
        public static String XACMLAuthzDecisionQuery2XACMLRequest(Element SAMLxacmlRequestXML) throws Exception {
		
		UnmarshallerFactory fac = Configuration.getUnmarshallerFactory();
		Unmarshaller unmarshaller = fac.getUnmarshaller(SAMLxacmlRequestXML);
		
		XACMLAuthzDecisionQueryTypeImpl xacmlQuery = (XACMLAuthzDecisionQueryTypeImpl) unmarshaller.unmarshall(SAMLxacmlRequestXML);
                
                /*privateKey_publicKey = "Public";
                Signature signature = getSignature();*/
                /*checkSignature(xacmlQuery);
                checkSignatureCertificate(xacmlQuery, SAMLxacmlRequestXML);*/
                
                checkSignature(xacmlQuery, SAMLxacmlRequestXML);
                
		XACML3RequestType req = (XACML3RequestType) xacmlQuery.getRequest();
                // add verification to support both "RequestTypeImpl for 2.0" and "XACML3RequestType for 3.0"
                //RequestTypeImpl req = (RequestTypeImpl) xacmlQuery.getRequest();
		Element xacmlElem = req.getDOM();
		
		TransformerFactory transFac = TransformerFactory.newInstance();
		Transformer trans = transFac.newTransformer();
		StringWriter buffer = new StringWriter();
		trans.transform(new DOMSource(xacmlElem), new StreamResult(buffer));
                    
                return buffer.toString();
	}
        
        public static void main(String[] args) throws Exception {
            
            SAMLConfig.InitSAML();
            
            AppSAML appSAML = new AppSAML();
            logger.info("SAML initiated!!!"); 
            
            privateKey_publicKey = "PrivateA";
            
            File f = new File(".../balana/policies-request/saml-xacml/request_0001_01.xml");
            
            
            System.out.println("get AbsolutePath");
            System.out.println(f.getAbsolutePath());
            System.out.println("get CanonicalPath");
            System.out.println(f.getCanonicalPath());
            
            
            String SAMLxacmlRequest = OpenSAMLutility.
                    XACMLRequest2XACMLAuthzDecisionQuery("/opt/Netbeans/TRESOR/balana/policies-request/saml-xacml/request_0001_01.xml", appSAML.getSignature());
            
            System.out.println("La Request signed es: "+SAMLxacmlRequest);
            
            /*Document doc = parserPool.parse(new StringReader((SAMLxacmlRequest)));
            Element elem = doc.getDocumentElement();
            
            String xacmlRequest = 
                    XACMLAuthzDecisionQuery2XACMLRequest(elem);
            
            System.out.println("La Original Request is: "+xacmlRequest);
            
            String response = new Scanner(new File("/opt/Netbeans/TRESOR/balana/modules/balana-core/src/test/resources/basic/3/responses/response_0001_01.xml")).useDelimiter("\\A").next();
            
            response  = makeSAMLxacmlResponse(response);
            
            System.out.println("La Response is: "+response);*/
            
        }

}
