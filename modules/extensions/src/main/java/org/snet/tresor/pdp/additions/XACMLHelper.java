package org.snet.tresor.pdp.additions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.*;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.IntegerAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * XACMLHelper class containing miscellaneous helper methods
 */
public class XACMLHelper {
	private static final Logger log = LoggerFactory.getLogger(XACMLHelper.class);

    public static final URI ATTRIBUTE_ID_SUBJECT = URI.create("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
    public static final URI ATTRIBUTE_ID_DEVICE = URI.create("org:snet:tresor:attribute:device-id");
    public static final URI ATTRIBUTE_ID_CLIENT = URI.create("http://schemas.cloud-tresor.com/request/2014/09/tresor-organization-uuid");
    public static final URI ATTRIBUTE_ID_SERVICE = URI.create("http://schemas.cloud-tresor.com/request/2014/04/service-uuid");

    public static final URI CATEGORY_SUBJECT = URI.create(XACMLConstants.SUBJECT_CATEGORY);
    public static final URI CATEGORY_RESOURCE = URI.create(XACMLConstants.RESOURCE_CATEGORY);
    public static final URI CATEGORY_ENVIRONMENT = URI.create(XACMLConstants.ENT_CATEGORY);

    public static final URI DATATYPE_STRING = URI.create(StringAttribute.identifier);
    public static final URI DATATYPE_INT = URI.create(IntegerAttribute.identifier);

	/**
	 * Retrieve subjectID from given context
	 * @param ctx the EvaluationCtx
	 * @return subjectID or null
	 */
	public static String getSubjectId(EvaluationCtx ctx) {
		return getAttributeAsString(DATATYPE_STRING, ATTRIBUTE_ID_SUBJECT,
				null, CATEGORY_SUBJECT, ctx);
	}

	/**
	 * Retrieve deviceID from given context
	 * @param ctx the EvaluationCtx
	 * @return deviceID or null
	 */
	public static String getDeviceId(EvaluationCtx ctx) {
		return getAttributeAsString(DATATYPE_STRING, ATTRIBUTE_ID_DEVICE,
				null, CATEGORY_SUBJECT, ctx);
	}

	/**
	 * Retrieve clientID from given context
	 * @param ctx the EvaluationCtx
	 * @return clientID or null
	 */
	public static String getClientId(EvaluationCtx ctx) {
		return getAttributeAsString(DATATYPE_STRING, ATTRIBUTE_ID_CLIENT,
				null, CATEGORY_SUBJECT, ctx);
	}

	/**
	 * Retrieve serviceID from given context
	 * @param ctx the EvaluationCtx
	 * @return serviceID or null
	 */
	public static String getServiceId(EvaluationCtx ctx) {
		return getAttributeAsString(DATATYPE_STRING, ATTRIBUTE_ID_SERVICE,
				null, CATEGORY_RESOURCE, ctx);
	}

	/**
	 * Search in given EvaluationContext for attribute, return one value as string
	 * @param attributeType type of attribute to look for
	 * @param attributeId id of attribute to look for
	 * @param issuer issuer of attribute to look for
	 * @param category category of attribute to look for
	 * @param context context in which to look
	 * @return a string representation of a value or null
	 */
	public static String getAttributeAsString(URI attributeType, URI attributeId, String issuer, URI category, EvaluationCtx context) {
		String value = null;
		BagAttribute bag = (BagAttribute) context.getAttribute(attributeType, attributeId, issuer, category).getAttributeValue();

        if (bag != null && !bag.isEmpty()) {
			AttributeValue val;
			Iterator it = bag.iterator();
			while (it.hasNext()) {
				val = (AttributeValue) it.next();
				if (!val.isBag() && val.getType().equals(attributeType)) {
					value = val.encode();
					break;
				}
			}
		}

		return value;
	}

	/**
	 * Create Attribute and return wrapped in a BagAttribute
	 * @param type the attributeType
	 * @param value the attributeValue as string
	 * @return BagAttribute containing attributeValue or empty BagAttribute
	 */
	public static BagAttribute makeBagAttribute(URI type, String value) {
        return XACMLHelper.makeBagAttribute(type, new String[]{ value });
	}

	public static BagAttribute makeBagAttribute(URI type, String[] values) {
		if (values != null) {
			AttributeValue attributeValue;
			List<AttributeValue> attributeValueList = new ArrayList<AttributeValue>();

			for (int i = 0; i < values.length; i++) {
				attributeValue = XACMLHelper.makeValue(type, values[i]);

				if (attributeValue != null)
					attributeValueList.add(attributeValue);
			}

			return new BagAttribute(type, attributeValueList);
		}

		return BagAttribute.createEmptyBag(type);
	}

	public static Attribute makeAttribute(URI id, URI type, AttributeValue attributeValue, int xacmlVersion) {
		if (id == null || type == null || attributeValue == null)
			return null;

		return new Attribute(id, null, null, attributeValue, xacmlVersion);
	}

	public static Attribute makeAttribute(URI id, URI type, String value, int xacmlVersion) {
		AttributeValue attributeValue = XACMLHelper.makeValue(type, value);
		return makeAttribute(id, type, attributeValue, xacmlVersion);
	}

	public static AttributeValue makeValue(URI type, String value) {
        if (type != null && value != null) {
            try {
                return Balana.getInstance().getAttributeFactory().createValue(type, value);
            } catch (UnknownIdentifierException e) {
                log.warn("Failed to parse attributeValue. Unknown DataType {}", type, e);
            } catch (ParsingException e) {
                log.warn("Failed to parse attributeValue of type {} from given String {}", type, value, e);
            }
        }

		return null;
	}

    /**
     * Tries to load given string into a corresponding policy OR policySet
     * @param policyString containing the policy or policySet as String
     * @param finder the PolicyFinder
     * @return a policy or policySet
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws ParsingException
     */
    public static AbstractPolicy loadPolicyOrPolicySet(String policyString, PolicyFinder finder) throws ParserConfigurationException, SAXException, IOException, ParsingException {
        Document doc = XACMLHelper.parseXML(policyString);
        Element root = doc.getDocumentElement();
        String name = DOMHelper.getLocalName(root);

        AbstractPolicy policy = null;
        if (name.equals("Policy"))
            policy = Policy.getInstance(root);

        if (name.equals("PolicySet"))
            policy = PolicySet.getInstance(root, finder);

        return policy;
    }

	/**
	 * Parses and loads given Policy
	 * @param policyString containing a policy
	 * @return AbstractPolicy instance
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParsingException
	 */
	public static AbstractPolicy loadPolicy(String policyString) throws ParserConfigurationException, SAXException, IOException, ParsingException {
		Document doc = XACMLHelper.parseXML(policyString);
		return Policy.getInstance(doc.getDocumentElement());
	}

	/**
	 * Parses and loads given PolicySet
	 * @param policyString containing a policySet
	 * @param finder the policyfinder
	 * @return AbstractPolicy instance
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParsingException
	 */
	public static AbstractPolicy loadPolicySet(String policyString, PolicyFinder finder) throws ParserConfigurationException, SAXException, IOException, ParsingException {
		Document doc = XACMLHelper.parseXML(policyString);
		return PolicySet.getInstance(doc.getDocumentElement(), finder);
	}

    /**
     * Parse given xml string into a Document instance
     * @param xml
     * @return the parsed xml Document instance
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document parseXML(String xml) throws ParserConfigurationException, SAXException, IOException {
        // create the factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        // create a builder based on the factory & try to load the policy
        DocumentBuilder db = factory.newDocumentBuilder();

		try (InputStream stream = new ByteArrayInputStream(xml.getBytes())) {
			return db.parse(stream);
		}
    }

}
