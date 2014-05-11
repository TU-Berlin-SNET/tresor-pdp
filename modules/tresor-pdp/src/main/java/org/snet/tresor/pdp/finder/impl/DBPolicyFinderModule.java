package org.snet.tresor.pdp.finder.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.snet.tresor.pdp.contexthandler.Helper;
import org.snet.tresor.pdp.policystore.DBPolicyStoreManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.MatchResult;
import org.wso2.balana.Policy;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.PolicyFinderResult;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

public class DBPolicyFinderModule extends PolicyFinderModule{

	private ParserPool parser;
	private PolicyFinder finder;
	private DBPolicyStoreManager policyStore;	
	private static Log log = LogFactory.getLog(FileBasedPolicyFinderModule.class);
	
	public DBPolicyFinderModule(DBPolicyStoreManager policyStore) {
		this.policyStore = policyStore;
		this.parser = new BasicParserPool();
	}

	@Override
	public void init(PolicyFinder finder) {
		this.finder = finder;
	}
	
    @Override
    public boolean isRequestSupported() {
        return true;
    }
	
	@Override
	public PolicyFinderResult findPolicy(EvaluationCtx context) {
		String domain = getAttributeAsString(Helper.DATATYPE_STRING_URI, Helper.ID_DOMAIN_URI, null, Helper.CATEGORY_SUBJECT_URI, context);
		String service = getAttributeAsString(Helper.DATATYPE_STRING_URI, Helper.ID_SERVICE_URI, null, Helper.CATEGORY_RESOURCE_URI, context);
		
		String policyString = this.policyStore.getPolicy(domain, service);
		AbstractPolicy policy = loadPolicy(policyString, this.finder);
		
		if (policy != null) {
			MatchResult match = policy.match(context);
			
			if (match.getResult() == MatchResult.INDETERMINATE)
				return new PolicyFinderResult(match.getStatus());
			
			if (match.getResult() == MatchResult.MATCH) {
				return new PolicyFinderResult(policy);
			}
			
		}
		
		if (log.isDebugEnabled())
			log.debug("No matching XACML policy found");
		
		return new PolicyFinderResult();
	}
	
    /**
     * Private helper that tries to load the given file-based policy, and
     * returns null if any error occurs.
     *
     * @param policyString policy as string
     * @param finder policy finder
     * @return  <code>AbstractPolicy</code>
     */
    private AbstractPolicy loadPolicy(String policyString, PolicyFinder finder) {

        AbstractPolicy policy = null;
        Reader reader = null;

        try {
        	reader = new StringReader(policyString);       
            Document doc = this.parser.parse(reader);            

            // handle the policy, if it's a known type
            Element root = doc.getDocumentElement();
//            String name = DOMHelper.getLocalName(root);   // in our case we only have policies and NO policysets

//            if (name.equals("Policy")) {
            policy = Policy.getInstance(root);
//            } else if (name.equals("PolicySet")) {
//                policy = PolicySet.getInstance(root, finder);
//            }
        } catch (Exception e) {
            // just only logs
            log.error("Fail to load policy : " + policyString , e);
        } finally {
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Error while closing input reader");
                }                
            }
        }

        return policy;
    }
	
	/**
	 * Searches in given EvaluationContext for an attribute
	 * @param attributeType, type of attribute to look for
	 * @param attributeId, id of attribute to look for
	 * @param issuer, issuer of attribute to look for
	 * @param category, category of attribute to look for
	 * @param context, context in which to look
	 * @return a string representation of the value or null
	 */
	private String getAttributeAsString(URI attributeType, URI attributeId,	String issuer, URI category, EvaluationCtx context) {
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
}
