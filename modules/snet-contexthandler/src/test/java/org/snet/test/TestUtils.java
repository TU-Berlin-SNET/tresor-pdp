package org.snet.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wso2.balana.Balana;
import org.wso2.balana.ObligationResult;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.ResponseCtx;
import org.wso2.balana.ctx.xacml3.Result;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.xacml3.Advice;
import org.wso2.balana.xacml3.Attributes;

/**
 * Class which provides several methods used in testing
 * @author malik
 */
public class TestUtils {
	
    /**
     * Returns a new PDP instance with new XACML policies
     *
     * @param policyPath, path to folder (or file) containing policies
     * @return a  PDP instance
     */
    public static PDP getPDPNewInstance(String policyPath){

        PolicyFinder finder= new PolicyFinder();

        Set<String> policies = getPolicies(policyPath);
        
        FileBasedPolicyFinderModule testPolicyFinderModule = new FileBasedPolicyFinderModule(policies);
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(testPolicyFinderModule);
        finder.setModules(policyModules);

        Balana balana = Balana.getInstance();
        PDPConfig pdpConfig = balana.getPdpConfig();
        pdpConfig = new PDPConfig(pdpConfig.getAttributeFinder(), finder,
                                                            pdpConfig.getResourceFinder(), true);
        return new PDP(pdpConfig);
    }
    
    private static Set<String> getPolicies(String policyPath) {
    	File file = new File(policyPath);
    	Set<String> policies = new HashSet<String>();
    	
    	if (file.isDirectory()) {
    		for (File f : file.listFiles()) {
    			policies.addAll(getPolicies(f.getAbsolutePath()));
    		}
    	}
    	
    	if (file.isFile()) {
    		policies.add(file.getAbsolutePath());
    	}
    	
    	return policies;
    }
    
    /**
     * Checks matching of result that got from PDP and expected response from a file.
     * (taken from org.wso2.balana), (only XACML)
     *
     * @param resultResponse  result that got from PDP
     * @param expectedResponse  expected response from a file
     * @return True/False
     */
    public static boolean isMatchingXACML(ResponseCtx resultResponse, ResponseCtx expectedResponse) {

        Set<AbstractResult> results = resultResponse.getResults();
        Set<AbstractResult> expectedResults = expectedResponse.getResults();

        boolean finalResult = false;

        for(AbstractResult result : results){

            boolean match = false;

            int decision = result.getDecision();

            String status =  result.getStatus().encode();

            List<String> advices = new ArrayList <String>();
            if( result.getAdvices() != null){
                for(Advice advice : result.getAdvices()){
                    advices.add(advice.encode());
                }
            }

            List<String> obligations = new ArrayList <String>();
            if(result.getObligations() != null){
                for(ObligationResult obligationResult : result.getObligations()){
                    obligations.add(obligationResult.encode());
                }
            }

            List<String> attributesList = new ArrayList <String>();

            if(result instanceof Result){
                Result xacml3Result = (Result) result;
                if(xacml3Result.getAttributes() != null){
                    for(Attributes attributesElement : xacml3Result.getAttributes()){
                        attributesList.add(attributesElement.encode());
                    }
                }
            }

            for(AbstractResult expectedResult : expectedResults){

                int decisionExpected = expectedResult.getDecision();
                if(decision == 4 || decision == 5 || decision == 6){
                    decision = 2;
                }
                if(decision != decisionExpected){
                    continue;
                }

                String statusExpected = expectedResult.getStatus().encode();

                if(!processResult(statusExpected).equals(processResult(status))){
                    continue;
                }

                List<String> advicesExpected = new ArrayList <String>();
                if(expectedResult.getAdvices() != null){
                    for(Advice advice : expectedResult.getAdvices()){
                        advicesExpected.add(advice.encode());
                    }
                }

                if(advices.size() != advicesExpected.size()){
                    continue;
                }

                if(advices.size() > 0){
                    boolean adviceContains = false;
                    for(String advice : advices){
                        if(!advicesExpected.contains(advice)){
                            adviceContains = false;
                            break;
                        } else {
                            adviceContains = true;
                        }
                    }

                    if(!adviceContains){
                        continue;
                    }
                }

                List<String> obligationsExpected = new ArrayList <String>();
                if(expectedResult.getObligations() != null){
                    for(ObligationResult obligationResult : expectedResult.getObligations()){
                        obligationsExpected.add(obligationResult.encode());
                    }
                }

                if(obligations.size() != obligationsExpected.size()){
                    continue;
                }

                if(obligations.size() > 0){
                    boolean obligationContains = false;
                    for(String obligation : obligations){
                        if(!obligationsExpected.contains(obligation)){
                            obligationContains = false;
                            break;
                        } else {
                            obligationContains = true;
                        }
                    }

                    if(!obligationContains){
                        continue;
                    }
                }

                // if only XACML 3.0. result
                if(expectedResult instanceof Result){

                    Result xacml3Result = (Result) expectedResult;
                    List<String> attributesExpected = new ArrayList <String>();

                    if(xacml3Result.getAttributes() != null){
                        for(Attributes  attributes : xacml3Result.getAttributes()){
                            attributesExpected.add(attributes.encode());
                        }
                    }

                    if(attributesList.size() != attributesExpected.size()){
                        continue;
                    }

                    if(attributesList.size() > 0){
                        boolean attributeContains = false;
                        for(String attribute : attributesList){
                            if(!attributesExpected.contains(attribute)){
                                attributeContains = false;
                                break;
                            } else {
                                attributeContains = true;
                            }
                        }

                        if(!attributeContains){
                            continue;
                        }
                    }
                }
                match = true;
                break;
            }

            if(match){
                finalResult = true;
            } else {
                finalResult = false;
                break;
            }
        }

//        if(finalResult){
//            log.info("Test is Passed........!!!   " +
//                    "Result received from the PDP is matched with expected result");
//        } else {
//            log.info("Test is Failed........!!!     " +
//                    "Result received from the PDP is NOT match with expected result");
//        }
        return finalResult;
    }
    
    /**
     * This would remove the StatusMessage from the response. Because StatusMessage depends
     * on the how you have defined it with the PDP, Therefore we can not compare it with
     * conformance tests. (taken from org.wso2.balana)
     *
     * @param response  XACML response String
     * @return XACML response String with out StatusMessage
     */
    private static String processResult(String response){

        if(response.contains("StatusMessage")){
            response = response.substring(0, response.indexOf("<StatusMessage>")) +
                    response.substring(response.indexOf("</Status>"));
        }

        return response;
    }

}
