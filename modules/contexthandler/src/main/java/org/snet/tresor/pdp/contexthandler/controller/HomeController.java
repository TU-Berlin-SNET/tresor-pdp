package org.snet.tresor.pdp.contexthandler.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class for the home document API
 */
@RestController
@RequestMapping("/")
public class HomeController {

	public static final String HOME_DISCOVERY_XML = "<resources xmlns='http://ietf.org/ns/home-documents'	xmlns:atom='http://www.w3.org/2005/Atom'>"
													+ "<resource rel='http://docs.oasis-open.org/ns/xacml/relation/pdp'>"
													+ "<atom:link href='/pdp' />"
													+ "</resource></resources>";

	@RequestMapping(method = RequestMethod.GET, produces="application/xml")
	public String getHomeDocument() {
		return HOME_DISCOVERY_XML;
	}

}
