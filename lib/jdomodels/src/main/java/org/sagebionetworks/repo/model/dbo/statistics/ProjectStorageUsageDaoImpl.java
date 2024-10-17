package org.sagebionetworks.repo.model.dbo.statistics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STORAGE_USAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_USAGE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_USAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_USAGE_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_USAGE_SIZE;

@Repository
public class ProjectStorageUsageDaoImpl implements ProjectStorageUsageDao {

	private JdbcTemplate jdbcTemplate;
	
	public ProjectStorageUsageDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	// For testing
	void truncateAll() {
		jdbcTemplate.update("TRANCATE TABLE " + TABLE_PROJECT_STORAGE_USAGE);
	}

}
