package com.sap.cloud.security.xsuaa.tokenflows;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sap.cloud.security.xsuaa.client.ClientCredentials;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenResponse;
import com.sap.cloud.security.xsuaa.client.OAuth2ServiceEndpointsProvider;
import com.sap.cloud.security.xsuaa.client.OAuth2ServiceException;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenService;
import com.sap.cloud.security.xsuaa.client.XsuaaDefaultEndpoints;

@RunWith(MockitoJUnitRunner.class)
public class RefreshTokenFlowTest {

	@Mock
	private OAuth2TokenService mockTokenService;

	private ClientCredentials clientCredentials;
	private RefreshTokenFlow cut;

	private static final String JWT_ACCESS_TOKEN = "4bfad399ca10490da95c2b5eb4451d53";
	private static final String REFRESH_TOKEN = "99e2cecfa54f4957a782f07168915b69-r";
	private OAuth2ServiceEndpointsProvider endpointsProvider;

	@Before
	public void setup() {
		this.clientCredentials = new ClientCredentials("clientId", "clientSecret");
		this.endpointsProvider = new XsuaaDefaultEndpoints(TestConstants.xsuaaBaseUri);
		this.cut = new RefreshTokenFlow(mockTokenService, endpointsProvider, clientCredentials);

	}

	@Test
	public void constructor_throwsOnNullValues() {
		assertThatThrownBy(() -> {
			new RefreshTokenFlow(null, endpointsProvider, clientCredentials);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("OAuth2TokenService");

		assertThatThrownBy(() -> {
			new RefreshTokenFlow(mockTokenService, null, clientCredentials);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("OAuth2ServiceEndpointsProvider");

		assertThatThrownBy(() -> {
			new RefreshTokenFlow(mockTokenService, endpointsProvider, null);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("ClientCredentials");
	}

	@Test
	public void execute_throwsIfMandatoryFieldsNotSet() {
		assertThatThrownBy(() -> {
			cut.execute();
		}).isInstanceOf(IllegalStateException.class).hasMessageContaining("Refresh token not set");
	}

	@Test
	public void execute() throws TokenFlowException, OAuth2ServiceException {
		OAuth2TokenResponse accessToken = new OAuth2TokenResponse(JWT_ACCESS_TOKEN, 441231, null);

		Mockito.when(mockTokenService
				.retrieveAccessTokenViaRefreshToken(eq(TestConstants.tokenEndpointUri), eq(clientCredentials),
						eq(REFRESH_TOKEN), isNull()))
				.thenReturn(accessToken);

		OAuth2TokenResponse jwt = cut.refreshToken(REFRESH_TOKEN)
				.execute();

		assertThat(jwt.getAccessToken(), is(accessToken.getAccessToken()));
	}

	@Test
	public void execute_throwsIfServiceRaisesException() throws OAuth2ServiceException {
		Mockito.when(mockTokenService
				.retrieveAccessTokenViaRefreshToken(eq(TestConstants.tokenEndpointUri), eq(clientCredentials),
						eq(REFRESH_TOKEN), isNull()))
				.thenThrow(new OAuth2ServiceException("exception executed REST call"));

		assertThatThrownBy(() -> {
			cut.refreshToken(REFRESH_TOKEN)
					.execute();
		}).isInstanceOf(TokenFlowException.class)
				.hasMessageContaining(
						"Error refreshing token with grant_type 'refresh_token': exception executed REST call");
	}

}
