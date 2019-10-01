package org.sagebionetworks.repo.manager.ses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESJsonRecipient;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class EmailQuarantineBounceProviderTest {

	@Mock
	private SESJsonNotificationDetails mockDetails;
	
	@Mock
	private SESJsonRecipient mockRecipient;
	
	@InjectMocks
	private EmailQuarantineBounceProvider provider;
	
	private String messageId = "000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000";
	
	@Test
	public void testGetSupportedType() {
		assertEquals(SESNotificationType.BOUNCE, provider.getSupportedType());
	}
	
	@Test
	public void getQuarantinedEmailsWithEmptySubtype() {
		
		String recipientEmail = "recipient1@test.com";
		QuarantineReason reason = QuarantineReason.TRANSIENT_BOUNCE;
		Optional<String> subType = Optional.empty();
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getSubType()).thenReturn(subType);
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId)
				.withExpirationTimeout(EmailQuarantineBounceProvider.RETRY_TIMEOUT);
			
		expected.add(recipientEmail);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void getQuarantinedEmailsWithUnknownSubtype() {
		
		String recipientEmail = "recipient1@test.com";
		QuarantineReason reason = QuarantineReason.TRANSIENT_BOUNCE;
		Optional<String> subType = Optional.of("Unknown");
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getSubType()).thenReturn(subType);
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId)
				.withExpirationTimeout(EmailQuarantineBounceProvider.RETRY_TIMEOUT);
		
		expected.add(recipientEmail);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void getQuarantinedEmailsWithUndeterminedSubtype() {
		
		String recipientEmail = "recipient1@test.com";
		QuarantineReason reason = QuarantineReason.TRANSIENT_BOUNCE;
		Optional<String> subType = Optional.of("Undetermined");
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getSubType()).thenReturn(subType);
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId)
				.withExpirationTimeout(EmailQuarantineBounceProvider.RETRY_TIMEOUT);
		
		expected.add(recipientEmail);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void getQuarantinedEmailsWithPermanentSubtype() {
		
		String recipientEmail = "recipient1@test.com";
		QuarantineReason reason = QuarantineReason.PERMANENT_BOUNCE;
		Optional<String> subType = Optional.of("Permanent");
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getSubType()).thenReturn(subType);
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId);
		
		expected.add(recipientEmail);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void getQuarantinedEmailsWithTransientSubtype() {
		
		String recipientEmail = "recipient1@test.com";
		QuarantineReason reason = QuarantineReason.TRANSIENT_BOUNCE;
		Optional<String> subType = Optional.of("Transient");
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getSubType()).thenReturn(subType);
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId)
				.withExpirationTimeout(EmailQuarantineBounceProvider.RETRY_TIMEOUT);
		
		expected.add(recipientEmail);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void getQuarantinedEmailsWithReasonDetails() {
		
		String recipientEmail = "recipient1@test.com";
		QuarantineReason reason = QuarantineReason.TRANSIENT_BOUNCE;
		Optional<String> subType = Optional.of("Transient");
		Optional<String> reasonDetails = Optional.of("MailboxFull");
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getSubType()).thenReturn(subType);
		when(mockDetails.getReason()).thenReturn(reasonDetails);
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withReasonDetails(reasonDetails.get().toUpperCase())
				.withSesMessageId(messageId)
				.withExpirationTimeout(EmailQuarantineBounceProvider.RETRY_TIMEOUT);
		
		expected.add(recipientEmail);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void getQuarantinedEmailsWithEmptyEmailAddress() {
		
		String recipientEmail = null;
		QuarantineReason reason = QuarantineReason.PERMANENT_BOUNCE;
		Optional<String> subType = Optional.of("Permanent");
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getSubType()).thenReturn(subType);
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}
	
}