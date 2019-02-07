package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT;

import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.transactions.MandatoryWriteReadCommittedTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class UnsuccessfulAttemptLockoutDAOImpl implements UnsuccessfulAttemptLockoutDAO {

	private static final String CURRENT_TIMESTAMP_MILLIS = "CURRENT_TIMESTAMP(3)";

	private static final String FROM_TABLE_FILTERED_BY_KEY = " FROM " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " WHERE " + COL_UNSUCCESSFUL_ATTEMPT_KEY + " = ?";

	private static final String SELECT_EXPIRATION_AND_LOCK_ROW = "SELECT CASE WHEN " + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS + " <= "+CURRENT_TIMESTAMP_MILLIS+" THEN null ELSE " + " CAST(UNIX_TIMESTAMP(" + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS + ") * 1000 AS UNSIGNED) END"
			+ FROM_TABLE_FILTERED_BY_KEY
			 + " FOR UPDATE";

	private static final String SELECT_UNSUCCESSFUL_ATTEMPTS = "SELECT " + COL_UNSUCCESSFUL_ATTEMPT_COUNT
			+ FROM_TABLE_FILTERED_BY_KEY;

	private static final String REMOVE_LOCKOUT = "DELETE " + FROM_TABLE_FILTERED_BY_KEY;

	private static final String CREATE_OR_INCREMENT_ATTEMPT_COUNT = "INSERT INTO " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " (" + COL_UNSUCCESSFUL_ATTEMPT_KEY + "," + COL_UNSUCCESSFUL_ATTEMPT_COUNT+","+ COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS +")"
			+ " VALUES (?, 1, "+CURRENT_TIMESTAMP_MILLIS+") ON DUPLICATE KEY UPDATE " + COL_UNSUCCESSFUL_ATTEMPT_COUNT + "=" + COL_UNSUCCESSFUL_ATTEMPT_COUNT + "+1";

	private static final String UPDATE_EXPIRATION = "UPDATE " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT
			+ " SET " + COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS + "="+CURRENT_TIMESTAMP_MILLIS+" + INTERVAL (? * 1000) MICROSECOND" + " WHERE " + COL_UNSUCCESSFUL_ATTEMPT_KEY + "=?";

	@Autowired
	JdbcTemplate jdbcTemplate;

	@MandatoryWriteReadCommittedTransaction
	@Override
	public long incrementNumFailedAttempts(String key) {
		jdbcTemplate.update(CREATE_OR_INCREMENT_ATTEMPT_COUNT, key);
		return getNumFailedAttempts(key);
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public long getNumFailedAttempts(String key){
		return jdbcTemplate.queryForObject(SELECT_UNSUCCESSFUL_ATTEMPTS, Long.class, key);
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void setExpiration(String key, long expirationMillisecondsFromNow) {
		jdbcTemplate.update(UPDATE_EXPIRATION, expirationMillisecondsFromNow, key);
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void removeLockout(String key) {
		jdbcTemplate.update(REMOVE_LOCKOUT, key);
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public Long getUnexpiredLockoutTimestampMillis(String key) {
		try {
			return jdbcTemplate.queryForObject(SELECT_EXPIRATION_AND_LOCK_ROW, Long.class, key);
		}catch (EmptyResultDataAccessException e){
			return null;
		}
	}

	public void truncateTable(){
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT);
	}
}
