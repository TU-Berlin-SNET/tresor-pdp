package org.snet.contexthandler;

import java.io.Reader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.w3c.dom.Document;

public class PolicyHandler {

	static PolicyHandler policyHandler;
	
	ContextHandler contextHandler;	
	List<Document> policies;
	ParserPool parserPool;
	
	public static PolicyHandler getInstance() {
		if (policyHandler == null)
			policyHandler = new PolicyHandler();
		return policyHandler;
	}
	
	private PolicyHandler() {
		this.contextHandler = ContextHandler.getInstance();
		this.policies = new LinkedList<Document>();
		this.parserPool = new BasicParserPool();
	}
	
	public PolicyHandler(ContextHandler cx, List<Document> policies) {
		this.contextHandler = cx;
		this.policies = policies;
		this.parserPool = new BasicParserPool();
	}
	
	public String handleGet(Reader request) throws Exception {
		StringWriter buffer = new StringWriter();
		
		for (int i = 0; i < this.policies.size(); i++) {
			Document policy = this.policies.get(i);
			buffer.append(i + ": " + transformToString(policy) + System.getProperty("line.separator"));			
		}
		
		return buffer.toString();
	}
	
	public void handlePut(Reader request) throws Exception {
		Document doc = this.parserPool.parse(request);			
		// TODO: add error handling and stuff
		this.addPolicy(doc);
		this.updatePDP();
	}
	
	public String handleDelete(Reader request, int contentlength) throws Exception {
		char[] buffer = new char[contentlength];
		request.read(buffer);
		int index = Integer.parseInt(String.valueOf(buffer));

		// TODO error handling and stuff

		return transformToString(this.deletePolicy(index));
	}
	
	private boolean addPolicy(Document policy) {
		return this.policies.add(policy);
	}
	
	private Document deletePolicy(int index) {
		return this.policies.remove(index);
	}
	
	private String transformToString(Document doc) throws Exception {
		StringWriter buffer = new StringWriter();
		TransformerFactory.newInstance().newTransformer()
			.transform(new DOMSource(doc), new StreamResult(buffer));
		
		return buffer.toString();
	}
	
	private void updatePDP() {
		this.contextHandler.setPDP(Helper.getPDP(this.policies));
	}

}