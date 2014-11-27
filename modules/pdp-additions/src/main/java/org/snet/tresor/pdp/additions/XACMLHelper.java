package org.snet.tresor.pdp.additions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.finder.impl.FinderConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.Balana;
import org.wso2.balana.DOMHelper;
import org.wso2.balana.ParsingException;
import org.wso2.balana.Policy;
import org.wso2.balana.PolicySet;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;
import org.xml.sax.SAXException;

/**
 * XACMLHelper class containing miscellaneous helper methods
 * @author malik
 */
public class XACMLHelper {
	private static final Logger log = LoggerFactory.getLogger(XACMLHelper.class);

	/**
	 * Retrieve subjectID from given context
	 * @param ctx the EvaluationCtx
	 * @return subjectID or null
	 */
	public static String getSubjectID(EvaluationCtx ctx) {
		return getAttributeAsString(FinderConstants.STRING_DATATYPE_URI, FinderConstants.SUBJECT_ID_URI,
				null, FinderConstants.SUBJECT_CATEGORY_URI, ctx);
	}

	/**
	 * Retrieve deviceID from given context
	 * @param ctx the EvaluationCtx
	 * @return deviceID or null
	 */
	public static String getDeviceID(EvaluationCtx ctx) {
		return getAttributeAsString(FinderConstants.STRING_DATATYPE_URI, FinderConstants.DEVICE_ID_URI,
				null, FinderConstants.SUBJECT_CATEGORY_URI, ctx);
	}

	/**
	 * Retrieve clientID from given context
	 * @param ctx the EvaluationCtx
	 * @return clientID or null
	 */
	public static String getClientID(EvaluationCtx ctx) {
		return getAttributeAsString(FinderConstants.STRING_DATATYPE_URI, FinderConstants.CLIENT_ID_URI,
				null, FinderConstants.SUBJECT_CATEGORY_URI, ctx);
	}

	/**
	 * Retrieve serviceID from given context
	 * @param ctx the EvaluationCtx
	 * @return serviceID or null
	 */
	public static String getServiceID(EvaluationCtx ctx) {
		return getAttributeAsString(FinderConstants.STRING_DATATYPE_URI, FinderConstants.SERVICE_ID_URI,
				null, FinderConstants.RESOURCE_CATEGORY_URI, ctx);
	}

	/**
	 * Searches in given EvaluationContext for an attribute
	 * @param attributeType type of attribute to look for
	 * @param attributeId id of attribute to look for
	 * @param issuer issuer of attribute to look for
	 * @param category category of attribute to look for
	 * @param context context in which to look
	 * @return a string representation of the value or null
	 */
	public static String getAttributeAsString(URI attributeType, URI attributeId,	String issuer, URI category, EvaluationCtx context) {
		String value = null;
		BagAttribute bag = (BagAttribute) context.getAttribute(attributeType, attributeId, issuer, category).getAttributeValue();

		if (!bag.isEmpty()) {
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

		if (value != null) {
			try {
				AttributeValue attr = Balana.getInstance().getAttributeFactory().createValue(type, value);
				List<AttributeValue> coll = new ArrayList<AttributeValue>(1);
				coll.add(attr);

				return new BagAttribute(type, coll);
			} catch (UnknownIdentifierException e) {
				log.warn("Unknown DataType {}", type, e);
			} catch (ParsingException e) {
				log.warn("Failed to create attribute from given String", e);
			}
		}

		return BagAttribute.createEmptyBag(type);
	}

	public static BagAttribute makeBagAttribute(URI type, String[] values) {

		if (values != null) {
			try {
				Set<AttributeValue> set = new HashSet<AttributeValue>();
				for (int i = 0; i < values.length; i++)
					set.add(Balana.getInstance().getAttributeFactory().createValue(type, values[i]));
				return new BagAttribute(type, set);
			} catch (UnknownIdentifierException e) {
				log.warn("Unknown DataType {}", type, e);
			} catch (ParsingException e) {
				log.warn("Failed to create attribute from given String", e);
			}
		}

		return BagAttribute.createEmptyBag(type);
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

        InputStream stream = null;
        try {
        	stream = new ByteArrayInputStream(xml.getBytes());
        	return db.parse(stream);
        } finally {
        	try { stream.close(); }
        	catch (Exception e) { log.debug("Failed to close stream resource", e); }
        }
    }

}
