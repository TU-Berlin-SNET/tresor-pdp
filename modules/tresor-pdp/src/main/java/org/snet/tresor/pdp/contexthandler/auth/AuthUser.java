package org.snet.tresor.pdp.contexthandler.auth;

/**
 * Interface for authenticated users
 * @author malik
 *
 */
public interface AuthUser {

	/**
	 * @return the name, e.g. username
	 */
	public String getName();
	
	/**
	 * @return domain this user belongs to
	 */
	public String getDomain();	
	
	/**
	 * @param action the action the user wants to do, e.g. httpmethod
	 * @param resource the resource the user tries to act upon, e.g. the requestURI
	 * @return whether the user is authorized to do the given action on the given resource
	 */
	public boolean isAuthorizedTo(String action, String resource);
	
}
