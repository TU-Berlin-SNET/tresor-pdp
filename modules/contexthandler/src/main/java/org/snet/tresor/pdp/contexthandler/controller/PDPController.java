package org.snet.tresor.pdp.contexthandler.controller;

import javax.inject.Inject;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.snet.tresor.pdp.contexthandler.LogHelper;
import org.snet.tresor.pdp.contexthandler.saml.SAMLHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ParsingException;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.ctx.EvaluationCtxFactory;
import org.wso2.balana.ctx.RequestCtxFactory;
import org.wso2.balana.ctx.ResponseCtx;

/**
 * Controller class for the PDP API
 */
@RestController
@RequestMapping("/pdp")
public class PDPController {
	private static final Logger log = LoggerFactory.getLogger(PDPController.class);

	private PDP pdp;
	private PDPConfig pdpConfig;
	private RequestCtxFactory reqFactory;
	private EvaluationCtxFactory evalFactory;

	@Inject
	public PDPController(PDP pdp, PDPConfig config, RequestCtxFactory reqFac, EvaluationCtxFactory evalFac) {
		this.pdp = pdp;
		this.pdpConfig = config;
		this.reqFactory = reqFac;
		this.evalFactory = evalFac;
	}

	@RequestMapping(method = RequestMethod.POST, consumes="application/xacml+xml", produces="application/xacml+xml")
	public ResponseEntity<String> getXACMLDecision(@RequestBody String req) {
        MDC.clear();
		MDC.put(LogHelper.CATEGORY, "XACML");
		log.trace("New XACML decision request");

		try {
			String result = this.processXACMLDecisionRequest(req);
			return new ResponseEntity<String>(result, HttpStatus.OK);
		} catch (ParsingException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
		}
	}

	@RequestMapping(method = RequestMethod.POST, consumes="application/samlassertion+xml", produces="application/samlassertion+xml")
	public ResponseEntity<String> getXACMLSAMLDecision(@RequestBody String req) {
        MDC.clear();
        MDC.put(LogHelper.CATEGORY, "XACMLSAML");
		log.trace("New XACMLSAML decision request");

		try {
			SAMLHandler samlHandler = new SAMLHandler();
			Document samlDoc = XACMLHelper.parseXML(req);
			log.debug("Parsed SAML document, now extracting xacml request inside");

			Element xacmlElem = samlHandler.handleRequest(samlDoc.getDocumentElement());
			log.debug("Extracted xacml request from saml document");

			String result = this.processXACMLDecisionRequest(xacmlElem);
			String response = samlHandler.handleResponse(result);

			return new ResponseEntity<String>(response, HttpStatus.OK);
		} catch (ParsingException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
		} catch (Exception e) {
			log.error("Failed to process request. Unexpected Error", e);
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String processXACMLDecisionRequest(String xacmlRequest) throws ParsingException {
		EvaluationCtx evalCtx = this.evalFactory.getEvaluationCtx(this.reqFactory.getRequestCtx(xacmlRequest),
				this.pdpConfig);
		log.debug("Retrieved EvaluationContext for xacml decision request STRING successfully");
		return this.processXACMLDecisionRequest(evalCtx);
	}

	private String processXACMLDecisionRequest(Element xacmlDoc) throws ParsingException {
		EvaluationCtx evalCtx = this.evalFactory.getEvaluationCtx(this.reqFactory.getRequestCtx(xacmlDoc),
				this.pdpConfig);
		log.debug("Retrieved EvaluationContext for xacml decision request ELEMENT successfully");
		return this.processXACMLDecisionRequest(evalCtx);
	}

	private String processXACMLDecisionRequest(EvaluationCtx evalCtx) {
		String subjectId = XACMLHelper.getSubjectId(evalCtx);
		String clientId = XACMLHelper.getClientId(evalCtx);
		String serviceId = XACMLHelper.getServiceId(evalCtx);

		// escape the backslash for logging purposes
		subjectId = (subjectId != null) ?  subjectId.replace("\\", "\\\\") : subjectId;
		LogHelper.putMDCs(clientId, subjectId);

		ResponseCtx result = this.pdp.evaluate(evalCtx);

        int decisionNumber;
		for (AbstractResult r : result.getResults()) {
            // AbstractResult.DECISIONS array has only 4 values, r.getDecision can return higher values
            // but everything above 3 is INDETERMINATE=2 anyway
            decisionNumber = (r.getDecision() <= 3) ? r.getDecision() : 2;
            log.info("Decision for subject {} to access service {} of client {} is {}",
                    subjectId, serviceId, clientId, AbstractResult.DECISIONS[decisionNumber]);
        }

		return result.encode();
	}

}
