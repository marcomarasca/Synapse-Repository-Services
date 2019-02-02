package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class UnsuccessfulAttemptLockoutDAOImpl implements UnsuccessfulAttemptLockoutDAO{

	public static final String FROM_TABLE_FILTERED_BY_KEY = " FROM " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " WHERE " + COL_UNSUCCESSFUL_ATTEMPT_KEY + " = ?";

	public static final String SELECT_ROW_EXPIRATION_AND_LOCK = "SELECT " + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC
			+ FROM_TABLE_FILTERED_BY_KEY
			 + " FOR UPDATE";

	public static final String REMOVE_LOCKOUT = "DELETE " + FROM_TABLE_FILTERED_BY_KEY;

	public static final String CREATE_OR_INCREMENT_ATTEMPT_COUNT = "INSERT INTO " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " (" + COL_UNSUCCESSFUL_ATTEMPT_KEY + "," + COL_UNSUCCESSFUL_ATTEMPT_COUNT+","+COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC+")"
			+ "VALUES (?, 1, NULL) ON DUPLICATE UPDATE " + COL_UNSUCCESSFUL_ATTEMPT_COUNT + "=" + COL_UNSUCCESSFUL_ATTEMPT_COUNT + "+1";

	public static final String UPDATE_EXPIRATION = "UPDATE " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " SET " + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_SEC + "=?" + " WHERE " + COL_UNSUCCESSFUL_ATTEMPT_KEY + " = ?";

	@Autowired
	JdbcTemplate namedParameterJdbcTemplate;

	@Override
	public long incrementNumFailedAttempts(String key) {
		return 0;
	}

	@Override
	public long setExpiration(String key, long expirationInSeconds) {
		return 0;
	}

	@Override
	public void removeLockout(String key) {

	}

	@Override
	public Long isLockedOut(String key) {
		return null;
	}
}
