package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockoutException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AuthenticationManagerImplAutowiredTest {
	
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserManager userManager;

	Long createdUserId = null;

	LoginRequest loginRequest;
	
	@BeforeEach
	public void setup(){
		if(createdUserId == null) {
			NewUser newUser = new NewUser();
			newUser.setUserName("someUsername");
			newUser.setEmail("myTestEmail@testEmailthingy.com");

			createdUserId = userManager.createUser(newUser);

			String password = UUID.randomUUID().toString();
			loginRequest = new LoginRequest();
			loginRequest.setUsername(newUser.getUserName());
			loginRequest.setPassword(password);
			authenticationManager.setPassword(createdUserId, password);


			assertNotNull(authenticationManager.login(loginRequest,  null));
		}
	}

	@AfterEach
	public void tearDown(){
		if(createdUserId != null) {
			userManager.deletePrincipal(new UserInfo(true, 42L), createdUserId);
		}
	}

	@Test
	public void testLoginWrongPassword_Lockout() throws InterruptedException {
		String wrongPassword = "hunter2";
		loginRequest.setPassword(wrongPassword);
		try {
			for (int i = 0; i < 10; i++) {
				try {
					authenticationManager.login(loginRequest, null);
					Thread.sleep(10);
					fail("expected exception to be throw for wrong password");
				} catch (UnauthenticatedException e) {
					//expected
				}
			}
		} catch (UnsuccessfulLoginLockoutException e){
			//expected. return early to avoid fail condition
			return;
		}

		fail("expected UnsuccessfulLoginLockoutException to be thrown after many failed login attempts");
	}
}
