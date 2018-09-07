package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.principal.AliasEnum;

public class DBOPrincipalAliasTest {
	
	private static DBOPrincipalAlias newDBO(String aliasDisplay, AliasEnum aliasType) {
		DBOPrincipalAlias dbo = new DBOPrincipalAlias();
		dbo.setAliasDisplay(aliasDisplay);
		dbo.setAliasType(aliasType);
		dbo.setAliasUnique(AliasUtils.getUniqueAliasName(aliasDisplay));
		dbo.setEtag("000-111-222");
		dbo.setId(1L);
		dbo.setPrincipalId(100L);
		dbo.setValidated(true);
		return dbo;
	}

	@Test
	public void testTranslatorNotORCID() {
		DBOPrincipalAlias dbo = new DBOPrincipalAlias();
		MigratableTableTranslation<DBOPrincipalAlias, DBOPrincipalAlias> translator = dbo.getTranslator();
		DBOPrincipalAlias backup = newDBO("foo", AliasEnum.TEAM_NAME);
		dbo = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(dbo, backup);
	}

	@Test
	public void testTranslatorORCIDHTTP() {
		DBOPrincipalAlias dbo = new DBOPrincipalAlias();
		MigratableTableTranslation<DBOPrincipalAlias, DBOPrincipalAlias> translator = dbo.getTranslator();
		DBOPrincipalAlias backup = newDBO("http://orcid.org/000-111-222", AliasEnum.USER_ORCID);
		dbo = translator.createDatabaseObjectFromBackup(backup);
		assertEquals("https://orcid.org/000-111-222", dbo.getAliasDisplay());
		assertEquals("httpsorcidorg000111222", dbo.getAliasUnique());
		// everything else should be the same
		backup.setAliasDisplay(dbo.getAliasDisplay());
		backup.setAliasUnique(dbo.getAliasUnique());
		assertEquals(dbo, backup);
	}

	@Test
	public void testTranslatorORCIDHTTPS() {
		DBOPrincipalAlias dbo = new DBOPrincipalAlias();
		MigratableTableTranslation<DBOPrincipalAlias, DBOPrincipalAlias> translator = dbo.getTranslator();
		DBOPrincipalAlias backup = newDBO("https://orcid.org/000-111-222", AliasEnum.USER_ORCID);
		dbo = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(dbo, backup);
	}

}
