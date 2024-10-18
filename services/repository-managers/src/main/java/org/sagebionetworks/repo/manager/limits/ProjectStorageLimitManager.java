package org.sagebionetworks.repo.manager.limits;

import java.time.Duration;
import java.util.Map;

import org.sagebionetworks.repo.model.dbo.limits.ProjectStorageUsageCacheDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Service;

@Service
public class ProjectStorageLimitManager {
	
	private static final Duration MAX_PROJECT_UPDATE_FREQUENCY = Duration.ofMinutes(5);
	
	private ProjectStorageUsageCacheDao storageUsageDao;
	
	private TableIndexDAO replicationDao;
	
	private Clock clock;
	
	public ProjectStorageLimitManager(ProjectStorageUsageCacheDao storageUsageDao, TableIndexDAO replicationDao, Clock clock) {
		this.storageUsageDao = storageUsageDao;
		this.replicationDao = replicationDao;
		this.clock = clock;
	}
	
	@WriteTransaction
	public void refreshProjectStorageUsage(long projectId) {
		if (storageUsageDao.isUpdatedOnAfter(projectId, clock.now().toInstant().minus(MAX_PROJECT_UPDATE_FREQUENCY))) {
			return;
		}
		
		Map<String, Long> storageUsageMap = replicationDao.getProjectStorageUsageData(projectId);
	
		storageUsageDao.setStorageUsageMap(projectId, storageUsageMap);
	}

}
