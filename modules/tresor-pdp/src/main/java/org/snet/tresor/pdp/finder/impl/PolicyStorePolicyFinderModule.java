package org.snet.tresor.pdp.finder.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.policystore.PolicyStoreManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.MatchResult;
import org.wso2.balana.Policy;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.PolicyFinderResult;

/**
 * PolicyFinderModule which uses the PolicyStoreManager interface to retrieve
 * policies from the Policy Store
 * @author malik
 */
public class PolicyStorePolicyFinderModule extends PolicyFinderModule{
	private static Log log = LogFactory.getLog(PolicyStorePolicyFinderModule.class);
	
	private ParserPool parser;
	private PolicyFinder finder;
	private PolicyStoreManager policyStoreManager;	
	
	public PolicyStorePolicyFinderModule() {
//		this.policyStoreManager = TresorPDP.getInstance().getPolicyStoreManager();
	}
	
	public PolicyStorePolicyFinderModule(PolicyStoreManager policyStore) {
		this.policyStoreManager = policyStore;
		this.parser = new BasicParserPool();
	}

	@Override
	public void init(PolicyFinder finder) {
		this.finder = finder;
		this.policyStoreManager = TresorPDP.getInstance().getPolicyStoreManager();
	}
	
    @Override
    public boolean isRequestSupported() {
        return true;
    }
	
	@Override
	public PolicyFinderResult findPolicy(EvaluationCtx context) {
		String domain = Helper.getAttributeAsString(FinderConstants.DATATYPE_STRING_URI, 
													FinderConstants.ID_DOMAIN_URI, null, 
													FinderConstants.CATEGORY_SUBJECT_URI, 
													context);
		
		String service = Helper.getAttributeAsString(FinderConstants.DATATYPE_STRING_URI, 
													 FinderConstants.ID_SERVICE_URI, null, 
													 FinderConstants.CATEGORY_RESOURCE_URI, 
													 context);
		
		String policyString = this.policyStoreManager.getPolicy(domain, service);
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
	
}
