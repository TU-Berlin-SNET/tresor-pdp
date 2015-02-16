package org.snet.tresor.pdp.contexthandler;

import org.slf4j.MDC;

/**
 * Class containing some constants and methods related to logging
 */
public class LogHelper {

	/**
	 * MDC-key for the category
	 */
	public static final String CATEGORY = "category";

	/**
	 * MDC-key for the client-id
	 */
	public static final String CLIENT_ID = "client-id";

	/**
	 * MDC-key for the subject-id
	 */
	public static final String SUBJECT_ID = "subject-id";

	public static void putMDCs(String clientId, String subjectId) {
		MDC.put(CLIENT_ID, clientId);
		MDC.put(SUBJECT_ID, subjectId);
	}

	public static void putMDCs(String category, String clientId, String subjectId) {
		MDC.put(CATEGORY, category);
		MDC.put(CLIENT_ID, clientId);
		MDC.put(SUBJECT_ID, subjectId);
	}

}
