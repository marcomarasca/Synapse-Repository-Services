package org.sagebionetworks.repo.model.dbo.statistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOProjectStorageUsage implements DatabaseObject<DBOProjectStorageUsage> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("projectId", SqlConstants.COL_PROJECT_STORAGE_USAGE_PROJECT_ID, true),
		new FieldColumn("storageLocationId", SqlConstants.COL_PROJECT_STORAGE_USAGE_LOCATION_ID, true),
		new FieldColumn("updatedOn", SqlConstants.COL_PROJECT_STORAGE_USAGE_UPDATED_ON),
		new FieldColumn("size", SqlConstants.COL_PROJECT_STORAGE_USAGE_SIZE)
	};
	
	private static final TableMapping<DBOProjectStorageUsage> TABLE_MAPPING = new TableMapping<DBOProjectStorageUsage>() {

		@Override
		public DBOProjectStorageUsage mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DBOProjectStorageUsage()
				.setProjectId(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_USAGE_PROJECT_ID))
				.setStorageLocationId(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_USAGE_LOCATION_ID))
				.setUpdatedOn(new Date(rs.getTimestamp(SqlConstants.COL_PROJECT_STORAGE_USAGE_UPDATED_ON).getTime()))
				.setSize(rs.getLong(SqlConstants.COL_PROJECT_STORAGE_USAGE_SIZE));
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_PROJECT_STORAGE_USAGE;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_PROJECT_STORAGE_USAGE;
		}

		@Override
		public Class<? extends DBOProjectStorageUsage> getDBOClass() {
			return DBOProjectStorageUsage.class;
		}
	};
		
	private Long projectId;
	private Long storageLocationId;
	private Date updatedOn;
	private Long size;
	
	public Long getProjectId() {
		return projectId;
	}
	
	public DBOProjectStorageUsage setProjectId(Long projectId) {
		this.projectId = projectId;
		return this;
	}
	
	public Long getStorageLocationId() {
		return storageLocationId;
	}
	
	public DBOProjectStorageUsage setStorageLocationId(Long storageLocationId) {
		this.storageLocationId = storageLocationId;
		return this;
	}
	
	public Date getUpdatedOn() {
		return updatedOn;
	}
	
	public DBOProjectStorageUsage setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
		return this;
	}

	public Long getSize() {
		return size;
	}
	
	public DBOProjectStorageUsage setSize(Long size) {
		this.size = size;
		return this;
	}

	@Override
	public TableMapping<DBOProjectStorageUsage> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public int hashCode() {
		return Objects.hash(projectId, size, storageLocationId, updatedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOProjectStorageUsage)) {
			return false;
		}
		DBOProjectStorageUsage other = (DBOProjectStorageUsage) obj;
		return Objects.equals(projectId, other.projectId) && Objects.equals(size, other.size)
				&& Objects.equals(storageLocationId, other.storageLocationId) && Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return String.format("DBOProjectStorageUsage [projectId=%s, storageLocationId=%s, updatedOn=%s, size=%s]", projectId,
				storageLocationId, updatedOn, size);
	}

}
