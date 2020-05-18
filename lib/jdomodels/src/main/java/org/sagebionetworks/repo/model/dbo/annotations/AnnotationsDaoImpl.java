package org.sagebionetworks.repo.model.dbo.annotations;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATIONS_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ANNOTATIONS;

import java.sql.ResultSet;
import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AnnotationsDaoImpl implements AnnotationsDao {
		
	// @formatter:off
	
	private static final String SQL_OBJECT_WHERE_CLAUSE = " WHERE "
			+ COL_ANNOTATIONS_OBJECT_ID + " = ? AND "
			+ COL_ANNOTATIONS_OBJECT_VERSION + " = ? AND "
			+ COL_ANNOTATIONS_OBJECT_TYPE + " = ?";

	private static final String SQL_SET_ANNOTATIONS = "INSERT INTO " + TABLE_ANNOTATIONS 
			+ "(" 
			+ COL_ANNOTATIONS_ID + ", " 
			+ COL_ANNOTATIONS_ETAG + ", "
			+ COL_ANNOTATIONS_CREATED_ON + ", "
			+ COL_ANNOTATIONS_MODIFIED_ON + ", "
			+ COL_ANNOTATIONS_OBJECT_TYPE + ", "
			+ COL_ANNOTATIONS_OBJECT_ID + ", "
			+ COL_ANNOTATIONS_OBJECT_VERSION + ", "
			+ COL_ANNOTATIONS_OBJECT_ANNOTATIONS
			+ ") VALUES (?, UUID(), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), ?, ?, ?, ?) "
			+ "ON DUPLICATE KEY UPDATE "
			+ COL_ANNOTATIONS_ETAG + " = UUID(), "
			+ COL_ANNOTATIONS_MODIFIED_ON + " = CURRENT_TIMESTAMP(), "
			+ COL_ANNOTATIONS_OBJECT_ANNOTATIONS + " = ?";
	
	private static final String SQL_GET_ANNOTATIONS_FOR_OBJECT = "SELECT "
			+ COL_ANNOTATIONS_OBJECT_ID + ", "
			+ COL_ANNOTATIONS_OBJECT_ANNOTATIONS
			+ " FROM " + TABLE_ANNOTATIONS 
			+ SQL_OBJECT_WHERE_CLAUSE;
	
	private static final String SQL_GET_ID_FOR_UPDATE = "SELECT "
			+ COL_ANNOTATIONS_ID
			+ " FROM " + TABLE_ANNOTATIONS
			+ SQL_OBJECT_WHERE_CLAUSE
			+ " FOR UPDATE";
	
	private static final String SQL_TRUNCATE = "DELETE FROM " + TABLE_ANNOTATIONS;
	
	// @formatter:on
	
	private static final RowMapper<Annotations> ROW_MAPPER = (ResultSet rs, int index) -> {
		
		String jsonAnnotations = rs.getString(COL_ANNOTATIONS_OBJECT_ANNOTATIONS);		
		Annotations annotations;
		
		try {
			annotations = EntityFactory.createEntityFromJSONString(jsonAnnotations, Annotations.class);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException("Cannot load annotations", e);
		}
		
		annotations.setId(KeyFactory.keyToString(rs.getLong(COL_ANNOTATIONS_OBJECT_ID)));
		
		return annotations;
	};
	
	
	private JdbcTemplate jdbcTemplate;
	private IdGenerator idGenerator;

	@Autowired
	public AnnotationsDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}

	@Override
	public Optional<Annotations> getAnnotations(ObjectType objectType, IdAndVersion idAndVersion) {
		validateTypeAndId(objectType, idAndVersion);
		
		final Long id = idAndVersion.getId();
		final Long version = idAndVersion.getVersion().get();
		
		try {
			Annotations annotations = jdbcTemplate.queryForObject(SQL_GET_ANNOTATIONS_FOR_OBJECT, ROW_MAPPER, id, version, objectType.name());
			return Optional.of(annotations);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public Annotations setAnnotations(ObjectType objectType, IdAndVersion idAndVersion, Annotations annotations) {
		AnnotationsV2Utils.validateAnnotations(annotations);
		
		final String jsonAnnotations;
		
		try {
			jsonAnnotations = AnnotationsV2Utils.toJSONStringForStorage(annotations);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException("Cannot parse annotations", e);
		}
		
		// Don't store empty annotations
		if (jsonAnnotations == null) {
			return annotations;
		}
		
		final Long id = getAnnotationsId(objectType, idAndVersion)
				.orElse(idGenerator.generateNewId(IdType.ANNOTATIONS_ID));
		
		final Long objectId = idAndVersion.getId();
		final Long objectVersion = idAndVersion.getVersion().get();
		
		jdbcTemplate.update(SQL_SET_ANNOTATIONS, (pss) -> {
			int index = 1;
			
			// For create fields
			pss.setLong(index++, id);
			pss.setString(index++, objectType.name());
			pss.setLong(index++, objectId);
			pss.setLong(index++, objectVersion);
			pss.setString(index++, jsonAnnotations);
			
			// For update fields
			pss.setString(index, jsonAnnotations);
			
		});
		
		return annotations;
	}	

	@Override
	public Optional<Long> getAnnotationsId(ObjectType objectType, IdAndVersion idAndVersion) {
		validateTypeAndId(objectType, idAndVersion);

		final Long objectId = idAndVersion.getId();
		final Long objectVersion = idAndVersion.getVersion().get();
		
		try {
			Long id = jdbcTemplate.queryForObject(SQL_GET_ID_FOR_UPDATE, Long.class, objectId, objectVersion, objectType.name());
			return Optional.of(id);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update(SQL_TRUNCATE);
	}
	
	private void validateTypeAndId(ObjectType objectType, IdAndVersion idAndVersion) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.requirement(idAndVersion.getVersion().isPresent(), "idAndVersion.version");
	}


}
