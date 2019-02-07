package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

import java.util.Objects;

import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;
import org.sagebionetworks.repo.transactions.MandatoryWriteReadCommittedTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;

public class ExponentialBackoffAttemptReporter implements AttemptResultReporter{
	private final UnsuccessfulAttemptLockoutDAO dao;
	private final String attemptKey;

	ExponentialBackoffAttemptReporter(String attemptKey, UnsuccessfulAttemptLockoutDAO dao){
		this.attemptKey = attemptKey;
		this.dao = dao;
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void reportSuccess() {
		dao.removeLockout(attemptKey);
	}

	@MandatoryWriteReadCommittedTransaction
	@Override
	public void reportFailure() {
		long numFailed = dao.incrementNumFailedAttempts(attemptKey);
		long lockDurationMilliseconds = 1 << numFailed;
		dao.setExpiration(attemptKey, lockDurationMilliseconds);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExponentialBackoffAttemptReporter that = (ExponentialBackoffAttemptReporter) o;
		return Objects.equals(dao, that.dao) &&
				Objects.equals(attemptKey, that.attemptKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dao, attemptKey);
	}
}
