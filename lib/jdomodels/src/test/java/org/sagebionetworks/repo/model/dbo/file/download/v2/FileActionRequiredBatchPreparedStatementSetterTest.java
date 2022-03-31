package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;


@ExtendWith(MockitoExtension.class)
public class FileActionRequiredBatchPreparedStatementSetterTest {
	
	@Mock
	private PreparedStatement mockPreparedStatement;
	
	private static final long FILE_ID = 101;
	private static final long AR_ID = 202;
	private static final long BENEFACTOR_ID = 303;

	@Test
	void testPreparedStatementSetter_MeetAccessRequirement() throws Exception {
		FileActionRequired[] actions = new FileActionRequired[1];
		
		MeetAccessRequirement action = new MeetAccessRequirement();
		action.setAccessRequirementId(AR_ID);
		actions[0] = (new FileActionRequired()).withFileId(FILE_ID).withAction(action);
		
		FileActionRequiredBatchPreparedStatementSetter pss = new FileActionRequiredBatchPreparedStatementSetter(actions);
		
		// method under test
		pss.setValues(mockPreparedStatement, 0);
		
		verify(mockPreparedStatement).setLong(1, FILE_ID);
		verify(mockPreparedStatement).setString(2, "ACCESS_REQUIREMENT");
		verify(mockPreparedStatement).setLong(3, AR_ID);
		
		
		// method under test
		assertEquals(1, pss.getBatchSize());
	}

	@Test
	void testPreparedStatementSetter_RequestDownload() throws Exception {
		FileActionRequired[] actions = new FileActionRequired[1];
		
		RequestDownload action = new RequestDownload();
		action.setBenefactorId(BENEFACTOR_ID);
		actions[0] = (new FileActionRequired()).withFileId(FILE_ID).withAction(action);
		
		FileActionRequiredBatchPreparedStatementSetter pss = new FileActionRequiredBatchPreparedStatementSetter(actions);
		
		// method under test
		pss.setValues(mockPreparedStatement, 0);
		
		verify(mockPreparedStatement).setLong(1, FILE_ID);
		verify(mockPreparedStatement).setString(2, "DOWNLOAD_PERMISSION");
		verify(mockPreparedStatement).setLong(3, BENEFACTOR_ID);
		
		
		// method under test
		assertEquals(1, pss.getBatchSize());
	}

	@Test
	void testPreparedStatementSetter_RequestDownload_MissingBenefactor() throws Exception {
		FileActionRequired[] actions = new FileActionRequired[1];
		
		RequestDownload action = new RequestDownload();
		actions[0] = (new FileActionRequired()).withFileId(FILE_ID).withAction(action);
		
		FileActionRequiredBatchPreparedStatementSetter pss = new FileActionRequiredBatchPreparedStatementSetter(actions);
		
		// method under test
		assertEquals(0, pss.getBatchSize());
	}

}
