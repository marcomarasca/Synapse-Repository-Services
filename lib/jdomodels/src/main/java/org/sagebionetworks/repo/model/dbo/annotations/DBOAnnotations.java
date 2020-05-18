package org.sagebionetworks.repo.model.dbo.annotations;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ANNOTATIONS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAnnotations implements MigratableDatabaseObject<DBOAnnotations, DBOAnnotations> {

	private static final MigratableTableTranslation<DBOAnnotations, DBOAnnotations> TRANSLATOR = new BasicMigratableTableTranslation<>();

	private static final FieldColumn[] FIELD_COLUMNS = new FieldColumn[] {
			new FieldColumn("id", COL_ANNOTATIONS_ID, true).withIsBackupId(true),
			new FieldColumn("etag", COL_ANNOTATIONS_ETAG).withIsEtag(true),
			new FieldColumn("createdOn", COL_ANNOTATIONS_CREATED_ON),
			new FieldColumn("modifiedOn", COL_ANNOTATIONS_MODIFIED_ON),
			new FieldColumn("objectType", COL_ANNOTATIONS_OBJECT_TYPE),
			new FieldColumn("objectId", COL_ANNOTATIONS_OBJECT_ID),
			new FieldColumn("objectVersion", COL_ANNOTATIONS_OBJECT_VERSION),
			new FieldColumn("objectAnnotations", COL_ANNOTATIONS_OBJECT_ANNOTATIONS) };

	private static final TableMapping<DBOAnnotations> TABLE_MAPPER = new TableMapping<DBOAnnotations>() {

		@Override
		public DBOAnnotations mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOAnnotations dbo = new DBOAnnotations();
			dbo.setId(rs.getLong(COL_ANNOTATIONS_ID));
			dbo.setEtag(rs.getString(COL_ANNOTATIONS_ETAG));
			dbo.setCreatedOn(rs.getTimestamp(COL_ANNOTATIONS_CREATED_ON));
			dbo.setModifiedOn(rs.getTimestamp(COL_ANNOTATIONS_MODIFIED_ON));
			dbo.setObjectId(rs.getLong(COL_ANNOTATIONS_OBJECT_ID));
			dbo.setObjectType(rs.getString(COL_ANNOTATIONS_OBJECT_TYPE));
			dbo.setObjectVersion(rs.getLong(COL_ANNOTATIONS_OBJECT_VERSION));
			dbo.setObjectAnnotations(rs.getString(COL_ANNOTATIONS_OBJECT_ANNOTATIONS));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_ANNOTATIONS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_ANNOTATIONS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELD_COLUMNS;
		}

		@Override
		public Class<? extends DBOAnnotations> getDBOClass() {
			return DBOAnnotations.class;
		}

	};

	private Long id;
	// Note: The etag is used purely for migration purposes and will not be included
	// in the Annotations DTO
	private String etag;
	private Timestamp createdOn;
	private Timestamp modifiedOn;
	private String objectType;
	private Long objectId;
	private Long objectVersion;
	private String objectAnnotations;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public Long getObjectVersion() {
		return objectVersion;
	}

	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
	}

	public String getObjectAnnotations() {
		return objectAnnotations;
	}

	public void setObjectAnnotations(String objectAnnotations) {
		this.objectAnnotations = objectAnnotations;
	}

	@Override
	public TableMapping<DBOAnnotations> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ANNOTATIONS;
	}

	@Override
	public MigratableTableTranslation<DBOAnnotations, DBOAnnotations> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOAnnotations> getBackupClass() {
		return DBOAnnotations.class;
	}

	@Override
	public Class<? extends DBOAnnotations> getDatabaseObjectClass() {
		return DBOAnnotations.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, etag, id, modifiedOn, objectAnnotations, objectId, objectType, objectVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOAnnotations other = (DBOAnnotations) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(objectAnnotations, other.objectAnnotations)
				&& Objects.equals(objectId, other.objectId) && Objects.equals(objectType, other.objectType)
				&& Objects.equals(objectVersion, other.objectVersion);
	}

}
