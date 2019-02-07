package org.sagebionetworks.repo.manager;

import java.util.Collections;
import java.util.Date;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.unsuccessfulattemptlockout.AttemptResultReporter;
import org.sagebionetworks.repo.manager.unsuccessfulattemptlockout.UnsuccessfulAttemptLockout;
import org.sagebionetworks.repo.manager.unsuccessfulattemptlockout.UnsuccessfulAttemptLockoutException;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.transactions.RequiresNewReadCommitted;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthenticationManagerUtilImpl implements AuthenticationManagerUtil {
	public static final String LOGIN_FAIL_ATTEMPT_METRIC_UNIT = "Count";

	public static final double LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE = 1.0;

	public static final String LOGIN_FAIL_ATTEMPT_METRIC_NAME = "LoginFailAttemptExceedLimit";


	public static final String UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX = "login-";


	static final long REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD = 11;


	@Autowired
	AuthenticationDAO authDAO;

	@Autowired
	UnsuccessfulAttemptLockout unsuccessfulAttemptLockout;

	@Autowired
	private Consumer consumer;


	private void logAttemptAfterAccountIsLocked(long principalId) {
		ProfileData loginFailAttemptExceedLimit = new ProfileData();
		loginFailAttemptExceedLimit.setNamespace(this.getClass().getName());
		loginFailAttemptExceedLimit.setName(LOGIN_FAIL_ATTEMPT_METRIC_NAME);
		loginFailAttemptExceedLimit.setValue(LOGIN_FAIL_ATTEMPT_METRIC_DEFAULT_VALUE);
		loginFailAttemptExceedLimit.setUnit(LOGIN_FAIL_ATTEMPT_METRIC_UNIT);
		loginFailAttemptExceedLimit.setTimestamp(new Date());
		loginFailAttemptExceedLimit.setDimension(Collections.singletonMap("UserId", ""+principalId));
		consumer.addProfileData(loginFailAttemptExceedLimit);
	}

	/**
	 * Check username, password combination
	 *
	 * @param principalId
	 * @param password
	 */
	@Override
	public boolean checkPassword(Long principalId, String password) {
		byte[] salt = authDAO.getPasswordSalt(principalId);
		String passHash = PBKDF2Utils.hashPassword(password, salt);
		return authDAO.checkUserCredentials(principalId, passHash);
	}

	@Override
	@RequiresNewReadCommitted
	public boolean authenticateWithLock(Long principalId, String password){
		AttemptResultReporter loginAttemptReporter;
		try {
			loginAttemptReporter = unsuccessfulAttemptLockout.checkIsLockedOut(UNSUCCESSFUL_LOGIN_ATTEMPT_KEY_PREFIX + principalId.toString());
		} catch (UnsuccessfulAttemptLockoutException e){
			//log to cloudwatch and rethrow exception if too many consecutive unsuccessful logins.
			if (e.getNumFailedAttempts() >= REPORT_UNSUCCESSFUL_LOGIN_GREATER_OR_EQUAL_THRESHOLD){
				logAttemptAfterAccountIsLocked(principalId);
			}
			throw e;
		}

		boolean credentialsCorrect = checkPassword(principalId, password);
		// check credentials and report success/failure of check
		if(credentialsCorrect) {
			loginAttemptReporter.reportSuccess();
		} else {
			loginAttemptReporter.reportFailure();
		}

		return credentialsCorrect;
	}
}
