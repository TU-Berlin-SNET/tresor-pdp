package org.snet.tresor.pdp.contexthandler.servlet;

public class ServletConstants {
	
	public static final String HTTP_GET = "GET";
	public static final String HTTP_POST = "POST";
	public static final String HTTP_PUT = "PUT";
	public static final String HTTP_DELETE = "DELETE";
	
	public static final String CHARSET_UTF8 = "UTF-8";
	
	public static final String CONTENTTYPE_XML = "application/xml";
	public static final String CONTENTTYPE_JSON = "application/json";
	
	public static final String CONTENTTYPE_TEXTHTML = "text/html";
	public static final String CONTENTTYPE_TEXTPLAIN = "text/plain";
        
	public static final String CONTENTTYPE_XACML = "application/xacml+xml";
	public static final String CONTENTTYPE_XACMLSAML = "application/samlassertion+xml";	
	
	public static final String HEADER_ACCEPT = "Accept";
	public static final String HEADER_AUTHORIZATION = "Authorization";
		
	public static final String HOME_DISCOVERY_XML = "<resources xmlns='http://ietf.org/ns/home-documents'	xmlns:atom='http://www.w3.org/2005/Atom'>"
											+ "<resource rel='http://docs.oasis-open.org/ns/xacml/relation/pdp'>"
											+ "<atom:link href='/pdp' />"
											+ "</resource></resources>";
	
	public static final int SC_UNPROCESSABLE_ENTITY = 422;
	
}
