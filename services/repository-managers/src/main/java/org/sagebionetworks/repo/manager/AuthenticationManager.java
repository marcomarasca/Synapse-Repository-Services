package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.web.NotFoundException;


public interface AuthenticationManager {	
	/**
	 * Set a user's password without any authorization checks
	 */
	public void setPassword(Long principalId, String password);

	/**
	 * Change a user's password after checking the validity of the request by checking the user's old password
	 * @return id of the user whose password has been changed
	 */
	public long changePassword(ChangePasswordInterface changePasswordWithOldPassword);

	/**
	 * Gets the user's secret key
	 */
	public String getSecretKey(Long principalId) throws NotFoundException;
	
	/**
	 * Replaces the user's secret key with a new one
	 */
	public void changeSecretKey(Long principalId);
	
	/**
	 * Creates a token tha can be used to reset a user's password
	 */
	public PasswordResetSignedToken createPasswordResetToken(long principalId) throws NotFoundException;
	
	/**
	 * Returns whether the user has accepted the terms of use
	 */
	public boolean hasUserAcceptedTermsOfUse(Long id) throws NotFoundException;

	/**
	 * Sets whether the user has accepted or rejected the terms of use
	 */
	public void setTermsOfUseAcceptance(Long principalId, Boolean acceptance);

	/**
	 * Log user in using information from the LoginRequest
	 * 
	 * @param request
	 * @param tokenIssuer
	 * @return
	 */
	public LoginResponse login(LoginRequest request, String tokenIssuer);
	
	/**
	 * Get the date/time the user was last logged in
	 * 
	 * @param userInfo
	 * @return
	 */
	public AuthenticatedOn getAuthenticatedOn(UserInfo userInfo);
	
	/**
	 * Bypass password check and just create a login response for the user.
	 * 
	 * @param principalId
	 * @param tokenIssuer
	 * @return
	 */
	public LoginResponse loginWithNoPasswordCheck(long principalId, String tokenIssuer);
}
