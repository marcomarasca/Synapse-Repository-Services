package org.sagebionetworks.download.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class DownloadListPackageWorker implements MessageDrivenRunner {
	
	static private Logger log = LogManager.getLogger(DownloadListPackageWorker.class);

	private AsynchJobStatusManager asynchJobStatusManager;
	private DownloadListManager downloadListManager;
	private UserManager userManager;

	@Autowired
	public DownloadListPackageWorker(AsynchJobStatusManager asynchJobStatusManager,
			DownloadListManager downloadListManager, UserManager userManager) {
		super();
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.downloadListManager = downloadListManager;
		this.userManager = userManager;
	}



	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try {
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Starting job...");
			DownloadListPackageRequest requestBody = (DownloadListPackageRequest) status.getRequestBody();
			UserInfo userInfo = userManager.getUserInfo(status.getStartedByUserId());
			DownloadListPackageResponse response = downloadListManager.packageFiles(progressCallback, userInfo, requestBody);
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		} catch (Throwable e) {
			log.error("Job failed:", e);
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
	}

}
