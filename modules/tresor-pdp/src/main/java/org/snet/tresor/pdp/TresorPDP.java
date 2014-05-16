package org.snet.tresor.pdp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.balana.PDP;

public class TresorPDP {
	private static Log logger = LogFactory.getLog(PDP.class);
	private static TresorPDP INSTANCE;
	private PDP pdp;
		
	public static TresorPDP getInstance() {
		if (INSTANCE == null)
			INSTANCE = new TresorPDP();
		return INSTANCE;
	}
	
	private TresorPDP() {
		// TODO: implement TresorPDP
		// get properties
		// construct configuration
		// construct pdp
		// do any necesarry additional work
	} 
	
	
}
