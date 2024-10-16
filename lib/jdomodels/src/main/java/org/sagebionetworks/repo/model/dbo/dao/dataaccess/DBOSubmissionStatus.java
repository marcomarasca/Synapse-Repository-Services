package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DATA_ACCESS_SUBMISSION_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_STATUS;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSubmissionStatus implements MigratableDatabaseObject<DBOSubmissionStatus, DBOSubmissionStatus>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("submisionId", COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID, true).withIsBackupId(true),
			new FieldColumn("createdBy", COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY),
			new FieldColumn("createdOn", COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_ON),
			new FieldColumn("modifiedBy", COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY),
			new FieldColumn("modifiedOn", COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON),
			new FieldColumn("state", COL_DATA_ACCESS_SUBMISSION_STATUS_STATE),
			new FieldColumn("reason", COL_DATA_ACCESS_SUBMISSION_STATUS_REASON)
		};

	private Long submisionId;
	private Long createdBy;
	private Long createdOn;
	private Long modifiedBy;
	private Long modifiedOn;
	private String state;
	private byte[] reason;

	@Override
	public String toString() {
		return "DBOSubmissionStatus [submisionId=" + submisionId + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", state=" + state
				+ ", reason=" + reason + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(reason);
		result = prime * result + Objects.hash(createdBy, createdOn, modifiedBy, modifiedOn, state, submisionId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOSubmissionStatus other = (DBOSubmissionStatus) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modifiedOn, other.modifiedOn)
				&& Arrays.equals(reason, other.reason) && Objects.equals(state, other.state)
				&& Objects.equals(submisionId, other.submisionId);
	}

	public Long getSubmisionId() {
		return submisionId;
	}

	public void setSubmisionId(Long submisionId) {
		this.submisionId = submisionId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public byte[] getReason() {
		return reason;
	}

	public void setReason(byte[] reason) {
		this.reason = reason;
	}

	@Override
	public TableMapping<DBOSubmissionStatus> getTableMapping() {
		return new TableMapping<DBOSubmissionStatus>(){

			@Override
			public DBOSubmissionStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSubmissionStatus dbo = new DBOSubmissionStatus();
				dbo.setSubmisionId(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID));
				dbo.setCreatedBy(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_ON));
				dbo.setModifiedBy(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY));
				dbo.setModifiedOn(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON));
				dbo.setState(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE));
				Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_STATUS_REASON);
				if (!rs.wasNull()) {
					dbo.setReason(blob.getBytes(1, (int) blob.length()));
				}
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DATA_ACCESS_SUBMISSION_STATUS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DATA_ACCESS_SUBMISSION_STATUS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSubmissionStatus> getDBOClass() {
				return DBOSubmissionStatus.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_SUBMISSION_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOSubmissionStatus, DBOSubmissionStatus> getTranslator() {
		return new BasicMigratableTableTranslation<DBOSubmissionStatus>();
	}

	@Override
	public Class<? extends DBOSubmissionStatus> getBackupClass() {
		return DBOSubmissionStatus.class;
	}

	@Override
	public Class<? extends DBOSubmissionStatus> getDatabaseObjectClass() {
		return DBOSubmissionStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
