package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Manager for the file handle association scanner jobs, takes care of starting the driver job and processing the association ranges
 */
public interface FileHandleAssociationScannerJobManager {
	
	/**
	 * Process a range of ids for a given type of association according to the given request specification.
	 * 
	 * @param request The scan range request
	 * @throws RecoverableMessageException If the request failed but can be retried on a later time
	 * return The total number of scanned records that contained file handles
	 * @throws NotFoundException If the job referenced by the given requets does not exist
	 */
	int processScanRangeRequest(FileHandleAssociationScanRangeRequest request) throws RecoverableMessageException, NotFoundException;
	
	/**
	 * @param jobId The id of the job
	 * @return True if all the scan range requests were completed for the job with the given id
	 */
	boolean isScanJobCompleted(long jobId);
	
	/**
	 * @param idlePeriod The idle period
	 * @return True if a scan job does not exist or if it hasn't been updated for the given amount of days
	 */
	boolean isScanJobIdle(int daysNum);
	
	/**
	 * Start a file handle association scan job and dispatch SQS messages to be processed by the {@link #processScanRangeRequest(FileHandleAssociationScanRangeRequest)}
	 */
	void startScanJob();
	

}
