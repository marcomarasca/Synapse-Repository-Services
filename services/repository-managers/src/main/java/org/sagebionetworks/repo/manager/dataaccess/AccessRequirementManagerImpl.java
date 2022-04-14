package org.sagebionetworks.repo.manager.dataaccess;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementInfoForUpdate;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.util.jrjc.JRJCHelper;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessRequirementManagerImpl implements AccessRequirementManager {
	public static final Long DEFAULT_LIMIT = 50L;
	public static final Long MAX_LIMIT = 50L;
	public static final Long DEFAULT_OFFSET = 0L;
	public static final Long DEFAULT_EXPIRATION_PERIOD = 0L;
	public static final int MAX_DESCRIPTION_LENGHT = 50;
	
	private AccessRequirementDAO accessRequirementDAO;

	private AuthorizationManager authorizationManager;
	
	private NodeDAO nodeDao;
	
	private NotificationEmailDAO notificationEmailDao;

	private JiraClient jiraClient;

	private ProjectSettingsManager projectSettingsManager;

	private TransactionalMessenger transactionalMessenger;
	
	private AccessControlListDAO aclDao;
	
	@Autowired
	public AccessRequirementManagerImpl(AccessRequirementDAO accessRequirementDAO, AuthorizationManager authorizationManager,
			NodeDAO nodeDao, NotificationEmailDAO notificationEmailDao, JiraClient jiraClient,
			ProjectSettingsManager projectSettingsManager, TransactionalMessenger transactionalMessenger, AccessControlListDAO aclDao) {
		this.accessRequirementDAO = accessRequirementDAO;
		this.authorizationManager = authorizationManager;
		this.nodeDao = nodeDao;
		this.notificationEmailDao = notificationEmailDao;
		this.jiraClient = jiraClient;
		this.projectSettingsManager = projectSettingsManager;
		this.transactionalMessenger = transactionalMessenger;
		this.aclDao = aclDao;
	}

	public static void validateAccessRequirement(AccessRequirement ar) throws InvalidModelException {
		ValidateArgument.required(ar.getAccessType(), "AccessType");
		ValidateArgument.required(ar.getSubjectIds(), "AccessRequirement.subjectIds");
		ValidateArgument.requirement(!ar.getConcreteType().equals(PostMessageContentAccessRequirement.class.getName()),
				"No longer support PostMessageContentAccessRequirement.");
		ValidateArgument.requirement(ar.getDescription() == null || ar.getDescription().length() <= MAX_DESCRIPTION_LENGHT,
				"The AR description can be at most " + MAX_DESCRIPTION_LENGHT + " characters.");
		RestrictableObjectType expecitingObjectType = determineObjectType(ar.getAccessType());
		
		for (RestrictableObjectDescriptor rod : ar.getSubjectIds()) {
			ValidateArgument.requirement(rod.getType().equals(expecitingObjectType),
					"Cannot apply AccessRequirement with AccessType "+ar.getAccessType().name()+" to an object of type "+rod.getType().name());
		}
		
		if (ar instanceof ManagedACTAccessRequirement) {
			ManagedACTAccessRequirement managedAR = (ManagedACTAccessRequirement) ar;
			
			Long expirationPeriod = managedAR.getExpirationPeriod();
			
			if (expirationPeriod != null && !expirationPeriod.equals(DEFAULT_EXPIRATION_PERIOD)) {
				ValidateArgument.requirement(expirationPeriod > DEFAULT_EXPIRATION_PERIOD, "When supplied, the expiration period should be greater than " + DEFAULT_EXPIRATION_PERIOD);
			}
		}
	}

	public static RestrictableObjectType determineObjectType(ACCESS_TYPE accessType) {
		switch(accessType) {
			case DOWNLOAD:
				return RestrictableObjectType.ENTITY;
			case PARTICIPATE:
				return RestrictableObjectType.TEAM;
			default:
				throw new IllegalArgumentException("Not support creating AccessRequirement with AccessType: "+accessType.name());
		}
	}

	public static void populateCreationFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		populateModifiedFields(userInfo, a);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}
	
	private void preventCreateWithinSTSFolder(String entityId) {
		// Can't create an access requirement if the entity lives inside an STS-enabled folder.
		// If the project setting is defined on the current entity, you can still create an access requirement.
		// Creating ARs is only blocked for child entities.
		if (projectSettingsManager.entityIsWithinSTSEnabledFolder(entityId)) {
			throw new IllegalArgumentException("Cannot apply an access requirement to a child of an STS-enabled folder.");
		}
	}

	void signalSubjectId(RestrictableObjectDescriptor rod) {
		if (RestrictableObjectType.ENTITY == rod.getType()) {
			// Send a change message to trigger a snapshot
			EntityType entityType;
			try {
				entityType = nodeDao.getNodeTypeById(rod.getId());
			} catch (NotFoundException e){
				// Do not signal nodes that are deleted
				return;
			}
			if (NodeUtils.isProjectOrFolder(entityType)) {
				transactionalMessenger.sendMessageAfterCommit(rod.getId(), ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
			} else {
				transactionalMessenger.sendMessageAfterCommit(rod.getId(), ObjectType.ENTITY, ChangeType.UPDATE);
			}
		}
		// TODO: Handle team and evaluations here
		return;
	}

	Set<RestrictableObjectDescriptor> findSubjectIdsToSignal(Set<RestrictableObjectDescriptor> currentSubjectIds, Set<RestrictableObjectDescriptor> updatedSubjectIds) {
		// We need to signal the subjectIds in the symmetric difference between currentSubjectIds and updatedSubjectIds
		Set<RestrictableObjectDescriptor> symmetricDiff = new HashSet<>(currentSubjectIds);
		symmetricDiff.removeAll(updatedSubjectIds); // A-B
		Set<RestrictableObjectDescriptor> bMinusA = new HashSet<>(updatedSubjectIds);
		bMinusA.removeAll(currentSubjectIds); // B-A
		symmetricDiff.addAll(bMinusA);
		return symmetricDiff;
	}

	void signalSubjectIds(List<RestrictableObjectDescriptor> currentSubjectIds, List<RestrictableObjectDescriptor> updatedSubjectIds) {
		Set<RestrictableObjectDescriptor> curSubjIds = new HashSet<>(currentSubjectIds);
		Set<RestrictableObjectDescriptor> updSubjIds = new HashSet<>(updatedSubjectIds);
		Set<RestrictableObjectDescriptor> subjectIdsToSignal = findSubjectIdsToSignal(curSubjIds, updSubjIds);
		for (RestrictableObjectDescriptor rod: subjectIdsToSignal) {
			signalSubjectId(rod);
		}
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		validateAccessRequirement(accessRequirement);
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can create an AccessRequirement.");
		}
		populateCreationFields(userInfo, accessRequirement);
		for (RestrictableObjectDescriptor rod : accessRequirement.getSubjectIds()) {
			if (RestrictableObjectType.ENTITY==rod.getType()) {
				preventCreateWithinSTSFolder(rod.getId());
			}
		}
		signalSubjectIds(new ArrayList<RestrictableObjectDescriptor>(), accessRequirement.getSubjectIds()); // Create == empty currentSibjectIds
		
		return (T) accessRequirementDAO.create(setDefaultValues(accessRequirement));
	}

	public static LockAccessRequirement newLockAccessRequirement(UserInfo userInfo, String entityId, String jiraKey) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(jiraKey, "jiraKey");

		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		LockAccessRequirement accessRequirement = new LockAccessRequirement();
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		accessRequirement.setJiraKey(jiraKey);
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirement;
	}

	@WriteTransaction
	@Override
	public LockAccessRequirement createLockAccessRequirement(UserInfo userInfo, String entityId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");

		// check authority
		authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.CREATE).checkAuthorizationOrElseThrow();
		authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();

		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);

		// check whether there is already an access requirement in place
		List<Long> subjectIds = nodeDao.getEntityPathIds(entityId);
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(subjectIds, RestrictableObjectType.ENTITY);
		ValidateArgument.requirement(stats.getRequirementIdSet().isEmpty(), "Entity "+entityId+" is already restricted.");

		preventCreateWithinSTSFolder(entityId);
		signalSubjectId(subjectId);
		
		String emailString = notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		String jiraKey = JRJCHelper.createRestrictIssue(jiraClient,
				userInfo.getId().toString(),
				emailString,
				entityId);

		LockAccessRequirement accessRequirement = newLockAccessRequirement(userInfo, entityId, jiraKey);
		return (LockAccessRequirement) accessRequirementDAO.create(setDefaultValues(accessRequirement));
	}

	@Override
	public AccessRequirement getAccessRequirement(String requirementId) throws DatastoreException, NotFoundException {
		return accessRequirementDAO.get(requirementId);
	}

	@Override
	public List<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo,
			RestrictableObjectDescriptor rod, Long limit, Long offset)
					throws DatastoreException, NotFoundException {
		if (limit == null) {
			limit = DEFAULT_LIMIT;
		}
		if (offset == null) {
			offset = DEFAULT_OFFSET;
		}
		ValidateArgument.requirement(limit >= 1L && limit <= MAX_LIMIT,
				"limit must be between 1 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0L, "offset must be at least 0");
		List<Long> subjectIds = new ArrayList<Long>();
		if (RestrictableObjectType.ENTITY==rod.getType()) {
			subjectIds.addAll(nodeDao.getEntityPathIds(rod.getId()));
		} else {
			subjectIds.add(KeyFactory.stringToKey(rod.getId()));
		}
		return accessRequirementDAO.getAccessRequirementsForSubject(subjectIds, rod.getType(), limit, offset);
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, String accessRequirementId, T toUpdate) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(toUpdate, "toUpdate");
		ValidateArgument.requirement(accessRequirementId.equals(toUpdate.getId().toString()),
			"Update specified ID "+accessRequirementId+" but object contains id: "+toUpdate.getId());
		validateAccessRequirement(toUpdate);

		authorizationManager.canAccess(userInfo, toUpdate.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();

		for (RestrictableObjectDescriptor rod : toUpdate.getSubjectIds()) {
			if (RestrictableObjectType.ENTITY==rod.getType()) {
				preventCreateWithinSTSFolder(rod.getId());
			}
		}
		AccessRequirementInfoForUpdate current = accessRequirementDAO.getForUpdate(accessRequirementId);
		if(!current.getEtag().equals(toUpdate.getEtag())
				|| !current.getCurrentVersion().equals(toUpdate.getVersionNumber())){
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		ValidateArgument.requirement(current.getAccessType().equals(toUpdate.getAccessType()), "Cannot modify AccessType");
		ValidateArgument.requirement(current.getConcreteType().equals(toUpdate.getConcreteType()), "Cannot change "+current.getConcreteType()+" to "+toUpdate.getConcreteType());

		AccessRequirement currentAr = accessRequirementDAO.get(accessRequirementId);
		List<RestrictableObjectDescriptor> currentArSubjectIds = currentAr.getSubjectIds();
		signalSubjectIds(currentArSubjectIds, toUpdate.getSubjectIds());

		toUpdate.setVersionNumber(current.getCurrentVersion()+1);
		populateModifiedFields(userInfo, toUpdate);
		return (T) accessRequirementDAO.update(setDefaultValues(toUpdate));
	}

	@WriteTransaction
	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can delete an AccessRequirement.");
		}
		AccessRequirement ar;
		try {
			ar = accessRequirementDAO.get(accessRequirementId);
		} catch (NotFoundException e) {
			return;
		}
		signalSubjectIds(ar.getSubjectIds(), new ArrayList<RestrictableObjectDescriptor>());
		accessRequirementDAO.delete(accessRequirementId);
	}

	static AccessRequirement setDefaultValues(AccessRequirement ar) {
		if (ar instanceof ManagedACTAccessRequirement) {
			return setDefaultValuesForManagedACTAccessRequirement((ManagedACTAccessRequirement) ar);
		} else if (ar instanceof SelfSignAccessRequirement) {
			return setDefaultValuesForSelfSignAccessRequirement((SelfSignAccessRequirement) ar);
		}
		return ar;
	}

	/**
	 * @param ar
	 * @return
	 */
	public static AccessRequirement setDefaultValuesForSelfSignAccessRequirement(SelfSignAccessRequirement ar) {
		if (ar.getIsCertifiedUserRequired() == null) {
			ar.setIsCertifiedUserRequired(false);
		}
		if (ar.getIsValidatedProfileRequired() == null) {
			ar.setIsValidatedProfileRequired(false);
		}
		return ar;
	}

	/**
	 * @param ar
	 * @return
	 */
	public static AccessRequirement setDefaultValuesForManagedACTAccessRequirement(ManagedACTAccessRequirement ar) {
		if (ar.getIsCertifiedUserRequired() == null) {
			ar.setIsCertifiedUserRequired(false);
		}
		if (ar.getIsValidatedProfileRequired() == null) {
			ar.setIsValidatedProfileRequired(false);
		}
		if (ar.getIsDUCRequired() == null) {
			ar.setIsDUCRequired(false);
		}
		if (ar.getIsIRBApprovalRequired() == null) {
			ar.setIsIRBApprovalRequired(false);
		}
		if (ar.getAreOtherAttachmentsRequired() == null) {
			ar.setAreOtherAttachmentsRequired(false);
		}
		if (ar.getIsIDUPublic() == null) {
			ar.setIsIDUPublic(false);
		}
		if (ar.getExpirationPeriod() == null) {
			ar.setExpirationPeriod(DEFAULT_EXPIRATION_PERIOD);
		}
		if (ar.getIsIDURequired() == null) {
			ar.setIsIDURequired(true);
		}
		return ar;
	}

	@WriteTransaction
	@Override
	public AccessRequirement convertAccessRequirement(UserInfo userInfo, AccessRequirementConversionRequest request) throws NotFoundException, UnauthorizedException, ConflictingUpdateException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "requirementId");
		ValidateArgument.required(request.getEtag(), "etag");
		ValidateArgument.required(request.getCurrentVersion(), "currentVersion");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}

		AccessRequirement current = accessRequirementDAO.getAccessRequirementForUpdate(request.getAccessRequirementId());
		ValidateArgument.requirement(current.getConcreteType().equals(ACTAccessRequirement.class.getName()),
				"Do not support converting AccessRequirement type "+current.getConcreteType());
		if(!current.getEtag().equals(request.getEtag())
				|| !current.getVersionNumber().equals(request.getCurrentVersion())){
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
		}

		ManagedACTAccessRequirement toUpdate = convert((ACTAccessRequirement) current, userInfo.getId().toString());
		return accessRequirementDAO.update(setDefaultValues(toUpdate));
	}

	public static ManagedACTAccessRequirement convert(ACTAccessRequirement current, String modifiedBy) {
		ValidateArgument.required(current, "current");
		ValidateArgument.required(modifiedBy, "modifiedBy");
		ManagedACTAccessRequirement toUpdate = new ManagedACTAccessRequirement();
		toUpdate.setId(current.getId());
		toUpdate.setAccessType(current.getAccessType());
		toUpdate.setCreatedBy(current.getCreatedBy());
		toUpdate.setCreatedOn(current.getCreatedOn());
		toUpdate.setEtag(UUID.randomUUID().toString());
		toUpdate.setModifiedBy(modifiedBy);
		toUpdate.setModifiedOn(new Date());
		toUpdate.setSubjectIds(current.getSubjectIds());
		toUpdate.setVersionNumber(current.getVersionNumber()+1);
		return toUpdate;
	}

	@Override
	public RestrictableObjectDescriptorResponse getSubjects(String accessRequirementId, String nextPageToken){
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		NextPageToken token = new NextPageToken(nextPageToken);
		RestrictableObjectDescriptorResponse response = new RestrictableObjectDescriptorResponse();
		List<RestrictableObjectDescriptor> subjects = accessRequirementDAO.getSubjects(Long.parseLong(accessRequirementId), token.getLimitForQuery(), token.getOffset());
		response.setSubjects(subjects);
		response.setNextPageToken(token.getNextPageTokenForCurrentResults(subjects));
		return response;
	}
	
	@Override
	public AccessControlList getAccessRequirementAcl(UserInfo userInfo, String accessRequirementId) throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		
		String aclArId = getAccessRequirement(accessRequirementId).getId().toString();
		
		return aclDao.get(aclArId, ObjectType.ACCESS_REQUIREMENT);
	}
	
	@Override
	@WriteTransaction
	public AccessControlList createAccessRequirementAcl(UserInfo userInfo, String accessRequirementId, AccessControlList acl) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		AccessRequirementUtils.validateAccessRequirementAcl(acl);
		
		// The only permission check we do is that the user is part of ACT, note that we do not check/require CHANGE_PERMISSIONS 
		// since the ARs do not have any ACL when created
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only an ACT member can assign an ACL to an access requirement.");
		}
		
		String aclArId = getAccessRequirement(accessRequirementId).getId().toString();
		
		acl.setId(aclArId);
		acl.setCreationDate(Date.from(Instant.now()));
		
		aclDao.create(acl, ObjectType.ACCESS_REQUIREMENT);
		
		return aclDao.get(aclArId, ObjectType.ACCESS_REQUIREMENT);
	}
	
	@Override
	@WriteTransaction
	public AccessControlList updateAccessRequirementAcl(UserInfo userInfo, String accessRequirementId, AccessControlList acl)
			throws NotFoundException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		AccessRequirementUtils.validateAccessRequirementAcl(acl);
		
		// The only permission check we do is that the user is part of ACT, note that we do not check/require CHANGE_PERMISSIONS 
		// since the ARs do not have any ACL when created
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only an ACT member can update the ACL of an access requirement.");
		}
		
		String aclArId = getAccessRequirement(accessRequirementId).getId().toString();
		
		acl.setId(aclArId);		
				
		aclDao.update(acl, ObjectType.ACCESS_REQUIREMENT);
		
		return aclDao.get(aclArId, ObjectType.ACCESS_REQUIREMENT);
	}
	
	@Override
	@WriteTransaction
	public void deleteAccessRequirementAcl(UserInfo userInfo, String accessRequirementId) {
		ValidateArgument.required(userInfo, "userInfo");		
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		
		// The only permission check we do is that the user is part of ACT, note that we do not check/require CHANGE_PERMISSIONS 
		// since the ARs do not have any ACL when created
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only an ACT member can delete the ACL of an access requirement.");
		}
		
		String aclArId = getAccessRequirement(accessRequirementId).getId().toString();
		
		aclDao.delete(aclArId, ObjectType.ACCESS_REQUIREMENT);
	}
	
}
