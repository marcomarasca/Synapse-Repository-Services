package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.DockerRegistryEventUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DockerManagerImplAutowiredTest {

	private static final String SERVICE = "docker.synapse.org";
	private static final String TYPE = "repository";
	private static final String TAG = "lastest";
	private static final String DIGEST = "sha256:10010101";
	private static final String MEDIA_TYPE = DockerManagerImpl.MANIFEST_MEDIA_TYPE;
	
	private String repositoryPath;
	
	@Autowired
	private DockerManager dockerManager;
	
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DockerNodeDao dockerNodeDao;

	@Autowired
	private OIDCTokenManager oidcTokenManager;
	
	private UserInfo adminUserInfo;
	private String projectId;
	
	@BeforeEach
	public void setUp() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		Project project = new Project();
		project.setName("project" + RandomStringUtils.randomAlphanumeric(10));
		projectId = entityManager.createEntity(adminUserInfo, project, null);
		repositoryPath = projectId+"/path";
	}
	
	@AfterEach
	public void tearDown() throws Exception {
		entityManager.deleteEntity(adminUserInfo, projectId);
	}

	@Test
	public void testAuthorizeDockerAccess() {
		// test to see if we can push to the project.  Answer should be yes!
		List<String> scope = new ArrayList<String>();
		scope.add(TYPE+":"+repositoryPath+":push");
		String accessToken = oidcTokenManager.createInternalTotalAccessToken(adminUserInfo.getId());
		
		DockerAuthorizationToken token = dockerManager.authorizeDockerAccess(adminUserInfo, accessToken, SERVICE, scope);
		assertNotNull(token.getToken());
	}
	
	@Test
	public void testDockerRegistryNotification() {
		assertNull(dockerNodeDao.getEntityIdForRepositoryName(SERVICE+"/"+repositoryPath));

		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(
						RegistryEventAction.push, SERVICE, adminUserInfo.getId(), repositoryPath, TAG, DIGEST, MEDIA_TYPE);
		dockerManager.dockerRegistryNotification(events);

		String createdEntityId = dockerNodeDao.getEntityIdForRepositoryName(SERVICE+"/"+repositoryPath);
		assertNotNull(createdEntityId);

		PaginatedResults<DockerCommit> pgs = 
				dockerManager.listDockerTags(adminUserInfo, createdEntityId,
						DockerCommitSortBy.TAG, /*ascending*/true, 10, 0);
		
		assertEquals(1L, pgs.getTotalNumberOfResults());
		assertEquals(1L, pgs.getResults().size());
		DockerCommit commit = pgs.getResults().get(0);
		assertNotNull(commit.getCreatedOn());
		assertEquals(TAG, commit.getTag());
		assertEquals(DIGEST, commit.getDigest());
	}
	
	@Test
	public void testAddDockerCommitToUnmanagedRespository() throws Exception {
		DockerRepository unmanagedRepo = new DockerRepository();
		unmanagedRepo.setId("111");
 		unmanagedRepo.setRepositoryName("repo/name");
 		unmanagedRepo.setParentId(projectId);
		String entityId = entityManager.createEntity(adminUserInfo, unmanagedRepo, null);
		DockerCommit commit = new DockerCommit();
		commit.setDigest(DIGEST);
		commit.setTag(TAG);
		
		// method under test
		dockerManager.addDockerCommitToUnmanagedRespository(adminUserInfo, entityId, commit);
		
		PaginatedResults<DockerCommit> pgs = 
				dockerManager.listDockerTags(adminUserInfo, entityId,
						DockerCommitSortBy.TAG, /*ascending*/true, 10, 0);
		
		assertEquals(1L, pgs.getTotalNumberOfResults());
		assertEquals(1L, pgs.getResults().size());
		DockerCommit retrievedCommit = pgs.getResults().get(0);
		assertNotNull(retrievedCommit.getCreatedOn());
		assertEquals(TAG, retrievedCommit.getTag());
		assertEquals(DIGEST, retrievedCommit.getDigest());
	}
	
	@Test
	public void testGetEntityIdForRepositoryName() {
		
		String repoName = SERVICE+"/"+repositoryPath;
		
		assertThrows(NotFoundException.class, () -> {			
			dockerManager.getEntityIdForRepositoryName(adminUserInfo, repoName);
		});
		
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(
						RegistryEventAction.push, SERVICE, adminUserInfo.getId(), repositoryPath, TAG, DIGEST, MEDIA_TYPE);
		
		dockerManager.dockerRegistryNotification(events);
		
		// Call under test
		EntityId result = dockerManager.getEntityIdForRepositoryName(adminUserInfo, repoName);
		
		assertNotNull(result.getId());
		
	}

}
