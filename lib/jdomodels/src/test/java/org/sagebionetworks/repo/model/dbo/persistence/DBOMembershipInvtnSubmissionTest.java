package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.dbo.dao.MembershipInvitationUtils.unzip;
import static org.sagebionetworks.repo.model.dbo.dao.MembershipInvitationUtils.zip;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.MembershipInvitationUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipInvtnSubmissionTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
		
	private List<Long> toDelete = null;
	private List<Long> teamToDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, params);
			}
		}
		if(dboBasicDao != null && teamToDelete != null){
			for(Long id: teamToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
		teamToDelete = new LinkedList<Long>();
	}
	
	public static DBOMembershipInvtnSubmission newMembershipInvtnSubmission(
			IdGenerator idGenerator, 
			DBOBasicDao dboBasicDao) {
		DBOMembershipInvtnSubmission invitation = new DBOMembershipInvtnSubmission();
		invitation.setId(idGenerator.generateNewId(IdType.MEMBERSHIP_INVITATION_ID));
		invitation.setEtag(DBOMembershipInvtnSubmission.defaultEtag);
		invitation.setCreatedOn(System.currentTimeMillis());
		invitation.setExpiresOn(System.currentTimeMillis());
		DBOTeam team = DBOTeamTest.newTeam();
		team = dboBasicDao.createNew(team);
		invitation.setTeamId(team.getId());
		invitation.setProperties((new String("abcdefg")).getBytes());
		Long invitee = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		invitation.setInviteeId(invitee);
		return invitation;
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException {
		DBOMembershipInvtnSubmission invitation = newMembershipInvtnSubmission(idGenerator, dboBasicDao);
		// Make sure we can create it
		DBOMembershipInvtnSubmission clone = dboBasicDao.createNew(invitation);
		toDelete.add(invitation.getId());
		teamToDelete.add(invitation.getTeamId());
		assertNotNull(clone);
		assertEquals(invitation, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", invitation.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, params);
		assertNotNull(clone);
		assertEquals(invitation, clone);
		
		// Make sure we can update it.
		clone.setProperties(new byte[] { (byte)1 });
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOMembershipInvtnSubmission clone2 = dboBasicDao.getObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, params);
		assertEquals(clone, clone2);
	}

	private static MembershipInvitation createMembershipInvtnSubmission(Date createdOn) {
		//It's easiest to create a DBO object by first creating a DTO object and then converting it
		MembershipInvitation dto = new MembershipInvitation();
		dto.setId("101");
		dto.setCreatedOn(createdOn);
		dto.setExpiresOn(null);
		dto.setInviteeId("987");
		dto.setTeamId("456");
		dto.setCreatedBy("123");
		dto.setMessage("foo");
		return dto;
	}

	@Test
	public void testTranslator() throws Exception {
		MembershipInvitation dto = createMembershipInvtnSubmission(new Date());
		DBOMembershipInvtnSubmission dbo = new DBOMembershipInvtnSubmission();
		MembershipInvitationUtils.copyDtoToDbo(dto, dbo);
		// now do the round trip
		DBOMembershipInvtnSubmission backup = dbo.getTranslator().createBackupFromDatabaseObject(dbo);
		DBOMembershipInvtnSubmission actual = dbo.getTranslator().createDatabaseObjectFromBackup(backup);
		DBOMembershipInvtnSubmission expected = backup;
		expected.setEtag(DBOMembershipInvtnSubmission.defaultEtag);
		assertEquals(expected, actual);
		assertEquals(dto, MembershipInvitationUtils.copyDboToDto(dbo));
	}

	@Test
	public void testTranslatorRefactor() throws IOException {
		DBOMembershipInvtnSubmission backup = new DBOMembershipInvtnSubmission();
		String oldProperties = "<MembershipInvtnSubmission/>";
		backup.setProperties(zip(oldProperties.getBytes()));

		// Method under test
		DBOMembershipInvtnSubmission translated = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		String expectedProperties = "<MembershipInvitation/>";
		String translatedProperties = new String(unzip(translated.getProperties()));
		assertEquals(expectedProperties, translatedProperties);
		assertNotEquals(oldProperties, translatedProperties);
	}
}