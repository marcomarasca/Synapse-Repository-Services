package org.sagebionetworks.repo.manager.config;

import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.v2.AwsCrdentialPoviderV2;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.evaluation.dbo.SubmissionFileHandleDBO;
import org.sagebionetworks.repo.manager.agent.AgentClientProvider;
import org.sagebionetworks.repo.manager.authentication.TotpManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.RowMapperSupplier;
import org.sagebionetworks.repo.manager.file.scanner.SerializedFieldRowMapperSupplier;
import org.sagebionetworks.repo.manager.file.scanner.tables.TableFileHandleScanner;
import org.sagebionetworks.repo.manager.oauth.GoogleOAuth2Provider;
import org.sagebionetworks.repo.manager.oauth.OAuthProviderBinding;
import org.sagebionetworks.repo.manager.oauth.OIDCConfig;
import org.sagebionetworks.repo.manager.oauth.OrcidOAuth2Provider;
import org.sagebionetworks.repo.manager.oauth.claimprovider.OIDCClaimProvider;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.webhook.WebhookMessageDispatcher;
import org.sagebionetworks.repo.model.agent.AgentType;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBORequest;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionUtils;
import org.sagebionetworks.repo.model.dbo.form.DBOFormData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmissionFile;
import org.sagebionetworks.repo.model.dbo.wikiV2.V2DBOWikiAttachmentReservation;
import org.sagebionetworks.repo.model.dbo.wikiV2.V2DBOWikiMarkdown;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphoreImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsRequest;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

@Configuration
public class ManagerConfiguration {

	private static final String VELOCITY_RESOURCE_LOADERS = "classpath,file";
	private static final String VELOCITY_PARAM_CLASSPATH_LOADER_CLASS = "classpath.resource.loader.class";
	private static final String VELOCITY_PARAM_FILE_LOADER_CLASS = "file.resource.loader.class";
	private static final String VELOCITY_PARAM_RUNTIME_REFERENCES_STRICT = "runtime.references.strict";

	/**
	 * @return The velocity engine instance that can be used within the managers
	 */
	@Bean
	public VelocityEngine velocityEngine() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, VELOCITY_RESOURCE_LOADERS);
		engine.setProperty(VELOCITY_PARAM_CLASSPATH_LOADER_CLASS, ClasspathResourceLoader.class.getName());
		engine.setProperty(VELOCITY_PARAM_FILE_LOADER_CLASS, FileResourceLoader.class.getName());
		engine.setProperty(VELOCITY_PARAM_RUNTIME_REFERENCES_STRICT, true);
		return engine;
	}

	/**
	 * 
	 * @return A general purpose JSON object mapper configured to not fail on
	 *         unkonwn properties and with the Java time module enabled
	 */
	@Bean
	public ObjectMapper jsonObjectMapper() {
		return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.registerModule(new JavaTimeModule());
	}

	@Bean
	public Map<FileHandleAssociateType, FileHandleAssociationProvider> fileHandleAssociationProviderMap(
			List<FileHandleAssociationProvider> providers) {
		return providers.stream().collect(Collectors.toMap(p -> p.getAssociateType(), Function.identity()));
	}

	@Bean
	public Map<FileHandleAssociateType, FileHandleAssociationScanner> fileHandleAssociationScannerMap(
			NamedParameterJdbcTemplate jdbcTemplate, TableEntityManager tableManager) {
		Map<FileHandleAssociateType, FileHandleAssociationScanner> scannerMap = new HashMap<>();

		scannerMap.put(FileHandleAssociateType.TableEntity, tableEntityFileScanner(tableManager));

		scannerMap.put(FileHandleAssociateType.FileEntity, fileEntityFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.SubmissionAttachment, evaluationSubmissionFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.FormData, formFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.MessageAttachment, messageFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.TeamAttachment, teamFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.UserProfileAttachment, userProfileFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.VerificationSubmission, verificationFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.WikiMarkdown, wikiMarkdownFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.WikiAttachment, wikiAttachmentFileScanner(jdbcTemplate));

		scannerMap.put(FileHandleAssociateType.AccessRequirementAttachment, accessRequirementFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.DataAccessRequestAttachment, accessRequestFileScanner(jdbcTemplate));
		scannerMap.put(FileHandleAssociateType.DataAccessSubmissionAttachment,
				accessSubmissionFileScanner(jdbcTemplate));

		return scannerMap;
	}

	@Bean
	public FileHandleAssociationScanner tableEntityFileScanner(TableEntityManager tableManager) {
		// Note: for configuration consistency this bean is not annotated with the
		// @Service annotation (e.g. will not be auto-scanned) but we
		// configure it here as a public bean
		return new TableFileHandleScanner(tableManager);
	}

	@Bean
	public FileHandleAssociationScanner fileEntityFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBORevision().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner formFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOFormData().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner messageFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOMessageContent().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner teamFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOTeam().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner userProfileFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOUserProfile().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner verificationFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate,
				new DBOVerificationSubmissionFile().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner wikiMarkdownFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		// Note: the wiki might also contain attachments, those are stored in the
		// serialized field of the wiki but also in a dedicated table
		// that is actually scanned with the scanner provided by the dedicated
		// wikiAttachmentFileScanner
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new V2DBOWikiMarkdown().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner wikiAttachmentFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		// Note: This table contains all the attachments of a wiki plus the wiki id
		// itself
		return new BasicFileHandleAssociationScanner(jdbcTemplate,
				new V2DBOWikiAttachmentReservation().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner evaluationSubmissionFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, new SubmissionFileHandleDBO().getTableMapping());
	}

	@Bean
	public FileHandleAssociationScanner accessRequirementFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		RowMapperSupplier rowMapperSupplier = new SerializedFieldRowMapperSupplier<>(
				AccessRequirementUtils::readSerializedField, AccessRequirementUtils::extractAllFileHandleIds);

		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOAccessRequirementRevision().getTableMapping(),
				DEFAULT_BATCH_SIZE, rowMapperSupplier);
	}

	@Bean
	public FileHandleAssociationScanner accessRequestFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		RowMapperSupplier rowMapperSupplier = new SerializedFieldRowMapperSupplier<>(RequestUtils::readSerializedField,
				RequestUtils::extractAllFileHandleIds);

		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBORequest().getTableMapping(),
				DEFAULT_BATCH_SIZE, rowMapperSupplier);
	}

	@Bean
	public FileHandleAssociationScanner accessSubmissionFileScanner(NamedParameterJdbcTemplate jdbcTemplate) {
		RowMapperSupplier rowMapperSupplier = new SerializedFieldRowMapperSupplier<>(
				SubmissionUtils::readSerializedField, SubmissionUtils::extractAllFileHandleIds);

		return new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOSubmission().getTableMapping(),
				DEFAULT_BATCH_SIZE, rowMapperSupplier);
	}

	@Bean
	public Map<OAuthProvider, OAuthProviderBinding> oauthProvidersBindingMap(StackConfiguration config,
			SimpleHttpClient client) {
		return Map.of(OAuthProvider.GOOGLE_OAUTH_2_0, googleOAuthProvider(config, client), OAuthProvider.ORCID,
				orcidOAuthProvider(config, client));
	}

	@Bean
	public GoogleOAuth2Provider googleOAuthProvider(StackConfiguration config, SimpleHttpClient client) {
		return new GoogleOAuth2Provider(config.getOAuth2GoogleClientId(), config.getOAuth2GoogleClientSecret(),
				new OIDCConfig(client, config.getOAuth2GoogleDiscoveryDocument()));
	}

	@Bean
	public OrcidOAuth2Provider orcidOAuthProvider(StackConfiguration config, SimpleHttpClient client) {
		return new OrcidOAuth2Provider(config.getOAuth2ORCIDClientId(), config.getOAuth2ORCIDClientSecret(),
				new OIDCConfig(client, config.getOAuth2ORCIDDiscoveryDocument()));
	}

	@Bean
	public TotpManager totpManager() {
		SecretGenerator secretGenerator = new DefaultSecretGenerator(TotpManager.SECRET_LENGHT);

		TimeProvider timeProvider = new SystemTimeProvider();

		CodeGenerator codeGenerator = new DefaultCodeGenerator(TotpManager.HASH_ALG, TotpManager.DIGITS_COUNT);

		DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
		codeVerifier.setTimePeriod(TotpManager.PERIOD);

		RecoveryCodeGenerator recoveryCodesGenerator = new RecoveryCodeGenerator();

		return new TotpManager(secretGenerator, codeVerifier, recoveryCodesGenerator);
	}

	@Bean
	public Map<OIDCClaimName, OIDCClaimProvider> claimProviders(List<OIDCClaimProvider> providerList) {
		return providerList.stream().collect(Collectors.toMap(OIDCClaimProvider::getName, Function.identity()));
	}

	@Bean
	public WriteReadSemaphore getWriteReadSemaphore(StackConfiguration config, CountingSemaphore countingSemaphore) {
		return new WriteReadSemaphoreImpl(countingSemaphore, config.getWriteReadSemaphoreRunnerMaxReaders());
	}

	@Bean
	public ExecutorService cachedThreadPool() {
		return Executors.newCachedThreadPool();
	}

	@Bean
	@Primary
	public HttpClient defaultHttpClient() {
		return HttpClient.newBuilder().connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
				.followRedirects(Redirect.NORMAL).build();
	}

	@Bean
	public HttpClient webhookHttpClient() {
		return HttpClient.newBuilder().connectTimeout(WebhookMessageDispatcher.REQUEST_TIMEOUT)
				.followRedirects(Redirect.NEVER).build();
	}

	@Bean
	public AwsCredentialsProvider createAwsCredentialProviderV2() {
		return AwsCrdentialPoviderV2.createCredentialProvider();
	}

	@Bean
	public BedrockAgentRuntimeAsyncClientBuilder createBedrockAgentRuntimeAsyncClientBuilder() {
		return BedrockAgentRuntimeAsyncClient.builder().region(Region.US_EAST_1)
				.httpClientBuilder(NettyNioAsyncHttpClient.builder().readTimeout(Duration.ofMinutes(2)));
	}

	@Bean
	public BedrockAgentRuntimeAsyncClient defaultBedrockAgentRuntimeAsyncClient(
			AwsCredentialsProvider credentialProvider, BedrockAgentRuntimeAsyncClientBuilder builder) {
		// This client uses the stack's credentials.
		return builder.credentialsProvider(credentialProvider).build();
	}

	@Bean
	public BedrockAgentRuntimeAsyncClient customBedrockAgentRuntimeAsyncClient(
			AwsCredentialsProvider credentialProvider, BedrockAgentRuntimeAsyncClientBuilder builder,
			StackConfiguration config) {
		String rollSessionName = new StringJoiner("-").add(config.getStack()).add(config.getStackInstance())
				.add(UUID.randomUUID().toString()).toString();
		// The sts client uses the stack's credentials to assume the role.
		StsClient stsClient = StsClient.builder().region(Region.US_EAST_1).credentialsProvider(credentialProvider)
				.build();
		// The provider will renew the STS credentials provided from assuming the role.
		StsAssumeRoleCredentialsProvider p = StsAssumeRoleCredentialsProvider.builder()
				.refreshRequest(AssumeRoleRequest.builder().roleArn(config.getCrossAccountBedrockRoleArn())
						.roleSessionName(rollSessionName).durationSeconds(60 * 60).build())
				.stsClient(stsClient).build();

		return builder.credentialsProvider(p).build();
	}

	@Bean
	public AgentClientProvider createAgentClientProvider(
			BedrockAgentRuntimeAsyncClient defaultBedrockAgentRuntimeAsyncClient,
			BedrockAgentRuntimeAsyncClient customBedrockAgentRuntimeAsyncClient) {
		return new AgentClientProvider(Map.of(AgentType.BASELINE, defaultBedrockAgentRuntimeAsyncClient,
				AgentType.CUSTOM, customBedrockAgentRuntimeAsyncClient));
	}

	@Bean
	public BedrockAgentClient createBedrockAgentClient(AwsCredentialsProvider credentialProvider) {
		return BedrockAgentClient.builder().credentialsProvider(credentialProvider).region(Region.US_EAST_1).build();
	}

	@Bean
	public String stackBedrockAgentId(BedrockAgentClient bedrockAgentClient, StackConfiguration stackConfig) {
		String agentName = new StringJoiner("-").add(stackConfig.getStack()).add(stackConfig.getStackInstance())
				.add("agent").toString();

		return bedrockAgentClient.listAgentsPaginator(ListAgentsRequest.builder().build()).stream()
				.flatMap(a -> a.agentSummaries().stream()).filter(a -> agentName.equals(a.agentName()))
				.map(a -> a.agentId()).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Could not find a bedrock agent named: " + agentName));

	}

}
