package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_REVOKED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_PASSED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_QUIZ_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_QUIZ_RESPONSE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOQuizResponse;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOQuizResponseDAOImpl implements QuizResponseDAO {
	
	@Autowired
	private DBOBasicDao basicDao;	
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	private static final RowMapper<DBOQuizResponse> QUIZ_RESPONSE_ROW_MAPPER = (new DBOQuizResponse()).getTableMapping();
	
	private static final String SELECT_FOR_QUIZ_ID_CORE = " FROM "+TABLE_QUIZ_RESPONSE+" WHERE "+
			COL_QUIZ_RESPONSE_QUIZ_ID+"=:"+COL_QUIZ_RESPONSE_QUIZ_ID;

	private static final String SELECT_FOR_QUIZ_ID_PAGINATED = "SELECT * "+SELECT_FOR_QUIZ_ID_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_FOR_QUIZ_ID_COUNT = "SELECT COUNT(ID) "+SELECT_FOR_QUIZ_ID_CORE;
	
	private static final String SELECT_FOR_QUIZ_ID_AND_USER_CORE = " FROM "+TABLE_QUIZ_RESPONSE+" WHERE "+
			COL_QUIZ_RESPONSE_QUIZ_ID+"=:"+COL_QUIZ_RESPONSE_QUIZ_ID+" AND "+
			COL_QUIZ_RESPONSE_CREATED_BY+"=:"+COL_QUIZ_RESPONSE_CREATED_BY;

	private static final String SELECT_FOR_QUIZ_ID_AND_USER_PAGINATED = "SELECT * "+SELECT_FOR_QUIZ_ID_AND_USER_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_FOR_QUIZ_ID_AND_USER_COUNT = "SELECT COUNT(ID) "+SELECT_FOR_QUIZ_ID_AND_USER_CORE;
	
	private static final String SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE = " FROM "+TABLE_QUIZ_RESPONSE+" WHERE "+
			COL_QUIZ_RESPONSE_QUIZ_ID+"=:"+COL_QUIZ_RESPONSE_QUIZ_ID+" AND "+
			COL_QUIZ_RESPONSE_CREATED_BY+"=:"+COL_QUIZ_RESPONSE_CREATED_BY;

	// select * from QUIZ_RESPONSE where CREATED_BY=? and QUIZ_ID=? order by CREATED_ON desc limit 1
	private static final String SELECT_LAST_RESPONSE_FOR_USER_AND_QUIZ = "SELECT * " +
			SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE + " ORDER BY "+COL_QUIZ_RESPONSE_CREATED_ON+" DESC LIMIT 1";

	private static final String SELECT_RESPONSES_FOR_USER_AND_QUIZ_PAGINATED = "SELECT * "+
			SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE + 
			" LIMIT :" + LIMIT_PARAM_NAME+" OFFSET :"+ OFFSET_PARAM_NAME;

	private static final String SELECT_RESPONSES_FOR_USER_AND_QUIZ_COUNT = "SELECT COUNT(ID) "+SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE;

	@WriteTransaction
	@Override
	public QuizResponse create(QuizResponse dto, PassingRecord passingRecord) throws DatastoreException {
		
		DBOQuizResponse dbo = new DBOQuizResponse();
		
		QuizResponseUtils.copyDtoToDbo(dto, passingRecord, dbo);
		
		dbo.setId(idGenerator.generateNewId(IdType.QUIZ_RESPONSE_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		
		dbo = basicDao.createNew(dbo);
		
		return QuizResponseUtils.copyDboToDto(dbo);
	}

	@Override
	public QuizResponse get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_ID.toLowerCase(), id);
		DBOQuizResponse dbo = basicDao.getObjectByPrimaryKey(DBOQuizResponse.class, param).orElseThrow(()->new NotFoundException("Quiz response not found for: "+id));
		
		return QuizResponseUtils.copyDboToDto(dbo);
	}

	@WriteTransaction
	@Override
	public void delete(Long id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOQuizResponse.class, param);
	}

	@Override
	public List<QuizResponse> getAllResponsesForQuiz(Long quizId, Long limit,
			Long offset) throws DatastoreException {
		List<QuizResponse>  dtos = new ArrayList<QuizResponse>();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(OFFSET_PARAM_NAME, offset);
		List<DBOQuizResponse> dbos = namedJdbcTemplate.query(SELECT_FOR_QUIZ_ID_PAGINATED, param, QUIZ_RESPONSE_ROW_MAPPER);
		for (DBOQuizResponse dbo : dbos) {
			QuizResponse dto = QuizResponseUtils.copyDboToDto(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

	
	@Override
	public long getAllResponsesForQuizCount(Long quizId)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		return namedJdbcTemplate.queryForObject(SELECT_FOR_QUIZ_ID_COUNT, param, Long.class);
	}

	@Override
	public List<QuizResponse> getUserResponsesForQuiz(Long quizId,
			Long principalId, Long limit, Long offset)
			throws DatastoreException {
		List<QuizResponse>  dtos = new ArrayList<QuizResponse>();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(OFFSET_PARAM_NAME, offset);
		List<DBOQuizResponse> dbos = namedJdbcTemplate.query(SELECT_FOR_QUIZ_ID_AND_USER_PAGINATED, param, QUIZ_RESPONSE_ROW_MAPPER);
		for (DBOQuizResponse dbo : dbos) {
			QuizResponse dto = QuizResponseUtils.copyDboToDto(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public long getUserResponsesForQuizCount(Long quizId, Long principalId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		return namedJdbcTemplate.queryForObject(SELECT_FOR_QUIZ_ID_AND_USER_COUNT, param, Long.class);
	}		

	@Override
	public Optional<PassingRecord> getLatestPassingRecord(Long quizId, Long principalId)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		
		DBOQuizResponse dbo;
		
		try {
			dbo = namedJdbcTemplate.queryForObject(SELECT_LAST_RESPONSE_FOR_USER_AND_QUIZ, param, QUIZ_RESPONSE_ROW_MAPPER);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}

		return Optional.of(QuizResponseUtils.extractPassingRecord(dbo));
	}

	@Override
	public List<PassingRecord> getAllPassingRecords(Long quizId, Long principalId, Long limit, Long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(OFFSET_PARAM_NAME, offset);
		try {
			List<DBOQuizResponse> dbos = namedJdbcTemplate.query(SELECT_RESPONSES_FOR_USER_AND_QUIZ_PAGINATED, param, QUIZ_RESPONSE_ROW_MAPPER);
			return dbos.stream().map(QuizResponseUtils::extractPassingRecord).collect(Collectors.toList());
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No quiz results for quiz " + quizId + " and user " + principalId);
		}
	}

	@Override
	public long getAllPassingRecordsCount(Long quizId, Long principalId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		return namedJdbcTemplate.queryForObject(SELECT_RESPONSES_FOR_USER_AND_QUIZ_COUNT, param, Long.class);
	}
	
	@Override
	@WriteTransaction
	public boolean revokeQuizResponse(Long responseId) {
		String sql = "UPDATE " + TABLE_QUIZ_RESPONSE + " SET " 
			+ COL_QUIZ_RESPONSE_REVOKED_ON + " = ?,"
			+ COL_QUIZ_RESPONSE_ETAG + " = UUID()"
			+ " WHERE " + COL_QUIZ_RESPONSE_ID + "=?"
			+ " AND " + COL_QUIZ_RESPONSE_PASSED + " IS TRUE"
			+ " AND " + COL_QUIZ_RESPONSE_REVOKED_ON + " IS NULL";
		
		return namedJdbcTemplate.getJdbcTemplate().update(sql, new Date().getTime(), responseId) > 0;
	}
}
