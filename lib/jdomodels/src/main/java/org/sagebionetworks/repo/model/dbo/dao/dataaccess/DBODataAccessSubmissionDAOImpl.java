package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBODataAccessSubmissionDAOImpl implements DataAccessSubmissionDAO{

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String SQL_GET_STATUS_FOR_USER = "SELECT "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_REASON
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "
				+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR+"."+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_SUBMISSION_ID
			+ " AND "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND ("+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_CREATED_BY+" = ?"
				+ " OR "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID+" = ?)"
			+ "LIMIT 1";

	private static final String SQL_GET_STATUS_BY_ID = " SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+" = ?";

	private static final String SQL_UPDATE_SUBMISSION_ETAG = "UPDATE "+TABLE_DATA_ACCESS_SUBMISSION
			+ " SET "+COL_DATA_ACCESS_SUBMISSION_ETAG+" = ?"
			+ " WHERE "+COL_DATA_ACCESS_SUBMISSION_ID+" = ?";

	private static final String SQL_UPDATE_STATUS = "UPDATE "+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+" SET "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = ?, "
			+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY+" = ?, "
			+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+" = ?, "
			+COL_DATA_ACCESS_SUBMISSION_STATUS_REASON+" = ?"
			+ " WHERE "+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+" = ?";

	private static final String SQL_GET_SUBMISSION_BY_ID = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID+" = ?";

	private static final String SQL_GET_SUBMISSION_FOR_UPDATE = SQL_GET_SUBMISSION_BY_ID + " FOR UPDATE";

	private static final String SQL_HAS_STATE = "SELECT COUNT(*)"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+", "
				+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR+"."+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_SUBMISSION_ID
			+ " AND "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND ("+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_CREATED_BY+" = ?"
				+ " OR "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID+" = ?)";

	private static final String SQL_DELETE = "DELETE FROM "+TABLE_DATA_ACCESS_SUBMISSION
			+" WHERE "+COL_DATA_ACCESS_SUBMISSION_ID+" = ?";

	private static final RowMapper<DataAccessSubmissionStatus> STATUS_MAPPER = new RowMapper<DataAccessSubmissionStatus>(){
		@Override
		public DataAccessSubmissionStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DataAccessSubmissionStatus status = new DataAccessSubmissionStatus();
			status.setSubmissionId(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID));
			status.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
			status.setState(DataAccessSubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE)));
			Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_STATUS_REASON);
			if (!rs.wasNull()) {
				try {
					status.setRejectedReason(new String(blob.getBytes(1, (int) blob.length()), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException();
				}
			}
			return status;
		}
	};

	private static final RowMapper<DataAccessSubmission> SUBMISSION_MAPPER = new RowMapper<DataAccessSubmission>(){

		@Override
		public DataAccessSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED);
				DataAccessSubmission submission = (DataAccessSubmission)JDOSecondaryPropertyUtils.decompressedObject(blob.getBytes(1, (int) blob.length()));
				submission.setId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ID));
				submission.setAccessRequirementId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID));
				submission.setDataAccessRequestId(rs.getString(COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID));
				submission.setSubmittedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_CREATED_BY));
				submission.setSubmittedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_CREATED_ON)));
				submission.setEtag(rs.getString(COL_DATA_ACCESS_SUBMISSION_ETAG));
				submission.setState(DataAccessSubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE)));
				Blob reason = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_STATUS_REASON);
				if (!rs.wasNull()) {
					submission.setRejectedReason(new String(reason.getBytes(1, (int) reason.length()), "UTF-8"));
				}
				submission.setModifiedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY));
				submission.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
				return submission;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	@Override
	public DataAccessSubmissionStatus getStatus(String accessRequirementId, String userId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_STATUS_FOR_USER, STATUS_MAPPER, accessRequirementId, userId, userId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessSubmission updateStatus(String submissionId, DataAccessSubmissionState newState, String reason,
			String userId, Long timestamp, String etag) {
		jdbcTemplate.update(SQL_UPDATE_STATUS, newState.toString(), userId, timestamp, reason, submissionId);
		jdbcTemplate.update(SQL_UPDATE_SUBMISSION_ETAG, etag, submissionId);
		return getSubmission(submissionId);
	}

	@Override
	public DataAccessSubmission getSubmission(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_SUBMISSION_BY_ID, SUBMISSION_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessSubmissionStatus create(DataAccessSubmission toCreate) {
		DBODataAccessSubmission dboSubmission = new DBODataAccessSubmission();
		DataAccessSubmissionUtils.copyDtoToDbo(toCreate, dboSubmission);
		List<DBODataAccessSubmissionAccessor> accessors = DataAccessSubmissionUtils.getDBOAccessors(toCreate);
		DBODataAccessSubmissionStatus status = DataAccessSubmissionUtils.getDBOStatus(toCreate);
		basicDao.createNew(dboSubmission);
		basicDao.createBatch(accessors);
		basicDao.createNew(status);
		return getSubmissionStatus(toCreate.getId());
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessSubmissionStatus cancel(String submissionId, String userId, Long timestamp, String etag) {
		jdbcTemplate.update(SQL_UPDATE_STATUS, DataAccessSubmissionState.CANCELLED.toString(), userId, timestamp, null, submissionId);
		jdbcTemplate.update(SQL_UPDATE_SUBMISSION_ETAG, etag, submissionId);
		return getSubmissionStatus(submissionId);
	}

	private DataAccessSubmissionStatus getSubmissionStatus(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_STATUS_BY_ID, STATUS_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public boolean hasSubmissionWithState(String userId, String accessRequirementId, DataAccessSubmissionState state) {
		Integer count = jdbcTemplate.queryForObject(SQL_HAS_STATE, Integer.class, state.toString(), accessRequirementId, userId, userId);
		return !count.equals(0);
	}

	@MandatoryWriteTransaction
	@Override
	public DataAccessSubmission getForUpdate(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_SUBMISSION_FOR_UPDATE, SUBMISSION_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
	}
}