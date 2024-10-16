package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalOidcBinding;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserManager {
	
	/**
	 * Get the User and UserGroup information for the given user ID.  
	 * 
	 * @param principalId the ID of the user of interest
	 */
	UserInfo getUserInfo(Long principalId) throws NotFoundException;
	
	/**
	 * Creates a new user
	 * 
	 * @return The ID of the user
	 */
	long createUser(NewUser user);
	
	UserInfo createOrGetTestUser(UserInfo adminUserInfo, NewUser user, String password, boolean signTermsOfService) throws NotFoundException;
	
	UserInfo createOrGetTestUser(UserInfo adminUserInfo, NewUser user) throws NotFoundException;
	
	/**
	 * Delete a principal by ID
	 * 
	 * For testing purposes only
	 */
	void deletePrincipal(UserInfo adminUserInfo, Long principalId) throws NotFoundException;

	/**
	 * Get all non-individual user groups, including Public.
	 */
	Collection<UserGroup> getGroups() throws DatastoreException;

	/**
	 * Get non-individual user groups (including Public) in range
	 **/
	List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException;
	
	
	/**
	 * Principals can have many aliases including a username, multiple email addresses, and OpenIds.
	 * This method will look a user by any of the aliases.
	 * @param alias
	 * @return
	 */
	PrincipalAlias lookupUserByUsernameOrEmail(String alias);
	
	PrincipalAlias lookupUserByAliasType(AliasType type, String alias);

	PrincipalAlias bindAlias(String aliasName, AliasType type, Long principalId);
	
	void unbindAlias(String aliasName, AliasType type, Long principalId);
	
	/**
	 * Get the distinct principal IDs of users for a given list of principal
	 * aliases. If a given alias is a team name then the results will include
	 * the principal ID of each user in the team. If a given alias is a user
	 * name then the results will include the principal ID of that user.
	 * 
	 * @param aliases
	 *            List of aliases that can include both user names and team
	 *            names.
	 * @param limit Limit the number of results.
	 */
	Set<String> getDistinctUserIdsForAliases(Collection<String> aliases, Long limit, Long offset);
	
	/**
	 * Lookup the oidc binding of the user that is bound to the given {@link OAuthProvider}, subject pair
	 * 
	 * @param provider
	 * @param subject
	 * @return An optional containing the id of the user bound to the given subject of the {@link OAuthProvider}
	 */
	Optional<PrincipalOidcBinding> lookupOidcBindingBySubject(OAuthProvider provider, String subject);
	
	/**
	 * Binds the given alias to the subject of the given {@link OAuthProvider}
	 * @param principalAlias
	 * @param provider
	 * @param subject
	 */
	PrincipalOidcBinding bindUserToOidcSubject(PrincipalAlias principalAlias, OAuthProvider provider, String subject);
	
	/**
	 * Updates the alias referenced by the oidc binding
	 * @param binding
	 * @param principalAlias
	 */
	void setOidcBindingAlias(PrincipalOidcBinding binding, PrincipalAlias principalAlias);
	
	/**
	 * Deletes the oidc binding with the given id
	 * 
	 * @param userId
	 */
	void deleteOidcBinding(Long bindingId);
	
	/**
	 * Removes all the oidc bindings for the given user
	 * 
	 * @param userId
	 */
	void clearOidcBindings(Long userId);

	/**
	 * Clear all user
	 */
	void truncateAll();


	
	
}
