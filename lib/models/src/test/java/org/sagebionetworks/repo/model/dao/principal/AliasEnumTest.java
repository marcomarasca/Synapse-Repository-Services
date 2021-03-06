package org.sagebionetworks.repo.model.dao.principal;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;
import org.sagebionetworks.repo.model.principal.AliasEnum;

public class AliasEnumTest {

	@Test
	public void testValidateAliasNull() {
		// Each type should not allow null
		for (AliasEnum ae : AliasEnum.values()) {
			assertThrows(IllegalArgumentException.class, () -> {
				ae.validateAlias(null);
			});
		}
	}

	@Test
	public void testValidatePrincipalNameUser() {
		AliasEnum.USER_NAME
				.validateAlias("1234567890.a-b_cdefghijklmnopqrstuvwxyz");
	}

	@Test
	public void testValidatePrincipalUserSpaces() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_NAME.validateAlias("has spaces");
		});
		assertTrue(ex.getMessage().contains("letters"));
		assertTrue(ex.getMessage().contains("numbers"));
		assertTrue(ex.getMessage().contains("underscore"));
		assertTrue(ex.getMessage().contains("dash"));
		assertTrue(ex.getMessage().contains("dot"));
		assertTrue(ex.getMessage().contains("3 characters long"));
	}

	@Test
	public void testValidatePrincipalUserTooShort() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_NAME.validateAlias("12");
		});
	}

	@Test
	public void testValidatePrincipalUserLongEnough() {
		AliasEnum.USER_NAME.validateAlias("123");
	}

	@Test
	public void testValidatePrincipalUserOtherChars() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_NAME.validateAlias("has!@#$%^&*()otherchars");
		});
	}

	@Test
	public void testValidatePrincipalNameTeam() {
		AliasEnum.TEAM_NAME
				.validateAlias("1234567890.a-b_c defghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP");
	}

	@Test
	public void testValidatePrincipalTeamAt() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.TEAM_NAME.validateAlias("has@chars");
		});
		assertTrue(ex.getMessage().contains("letters"));
		assertTrue(ex.getMessage().contains("numbers"));
		assertTrue(ex.getMessage().contains("underscore"));
		assertTrue(ex.getMessage().contains("dash"));
		assertTrue(ex.getMessage().contains("dot"));
		assertTrue(ex.getMessage().contains("space"));
		assertTrue(ex.getMessage().contains("3 characters long"));
	}

	@Test
	public void testValidatePrincipalTeamTooShort() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.TEAM_NAME.validateAlias("12");
		});
	}

	@Test
	public void testValidatePrincipalTeamLongEnough() {
		AliasEnum.TEAM_NAME.validateAlias("123");
	}
	
	@Test
	public void testValidatePrincipalTeamOtherChars() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.TEAM_NAME.validateAlias("has!@#$%^&*()otherchars");
		});
	}

	@Test
	public void testValidateEmail(){
		AliasEnum.USER_EMAIL.validateAlias("foo.bar@company.com");
	}
	
	@Test
	public void testValidateEmailTopLevelDomain() {
		// PLFM-6743
		// 63 x's
		String email = "foo@company.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
		AliasEnum.USER_EMAIL.validateAlias(email);
		// 64 x's
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_EMAIL.validateAlias(email + "x");
		});
	}
	
	@Test
	public void testValidORCID() {
		AliasEnum.USER_ORCID.validateAlias("https://orcid.org/0000-1111-2222-3333");
	}
	
	// it's valid for the final character to be an "X"
	// http://support.orcid.org/knowledgebase/articles/116780-structure-of-the-orcid-identifier
	@Test
	public void testValidORCIDWithXchecksum() {
		AliasEnum.USER_ORCID.validateAlias("https://orcid.org/0000-1111-2222-333X");
	}
	
	@Test
	public void testORCIDWrongPrefix() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_ORCID.validateAlias("https://foo/0000-1111-2222-3333");
		});
	}
	
	@Test
	public void testORCIDWrongLength() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_ORCID.validateAlias("https://orcid.org/0000-1111-2222");
		});
	}
	
	@Test
	public void testORCIDWrongLength2() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_ORCID.validateAlias("https://orcid.org/0000-1111-222-33");
		});
	}
	
	@Test
	public void testORCIDWrongLength3() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_ORCID.validateAlias("https://orcid.org/0000-1111-");
		});
	}
	
	@Test
	public void testORCIDLetters() {
		assertThrows(IllegalArgumentException.class, () -> {
			AliasEnum.USER_ORCID.validateAlias("https://foo/0000-1111-xxxx-yyyy");
		});
	}
}
