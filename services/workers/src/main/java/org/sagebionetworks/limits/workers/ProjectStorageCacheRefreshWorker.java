package org.sagebionetworks.limits.workers;

import org.sagebionetworks.repo.manager.limits.ProjectStorageLimitManager;
import org.sagebionetworks.repo.model.limits.ProjectStorageCacheUpdate;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class ProjectStorageCacheRefreshWorker implements TypedMessageDrivenRunner<ProjectStorageCacheUpdate> {
	
	private ProjectStorageLimitManager manager;
	
	public ProjectStorageCacheRefreshWorker(ProjectStorageLimitManager manager) {
		this.manager = manager;
	}

	@Override
	public Class<ProjectStorageCacheUpdate> getObjectClass() {
		return ProjectStorageCacheUpdate.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, ProjectStorageCacheUpdate convertedMessage)
			throws RecoverableMessageException, Exception {
		manager.refreshProjectStorageUsage(Long.valueOf(convertedMessage.getObjectId()));
	}
}
