package graphql;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.MediaType;
import java.io.*;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vimalselvam.graphql.GraphqlTemplate;
import org.testng.Assert;
import org.testng.annotations.Test;

import okhttp3.*;

import static java.time.Instant.now;

public class BaseTest {
    private static final OkHttpClient client = new OkHttpClient();
    private final String graphqlUri = "";
    protected final String queryPath = "/graphql/";
    AuthTokens tokens;


    private Response prepareResponse(String graphqlPayload, boolean requireAuth) throws IOException {
        if (requireAuth) {
            if (tokens == null) {
                tokens = authorize();
            }
        }

        RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), graphqlPayload);
        Request.Builder builder = new Request.Builder().url(graphqlUri)
                .post(body);

        if (requireAuth) {
            try {
                builder.addHeader("Authorization", tokens.getIdToken());
            }
            catch (IdTokenExpiredSoonException e) {
                tokens = refreshAuth();
                builder.addHeader("Authorization", tokens.getIdToken());
            }
            catch (IdTokenExpiredException e) {
                tokens = authorize();
                builder.addHeader("Authorization", tokens.getIdToken());
            }
        } else {
            builder.addHeader("x-api-key", "");
        }

        Request request = builder.build();
        return client.newCall(request).execute();
    }

    private AuthTokens refreshAuth(/*String email, String password,*/) throws IOException {
        return authorize(true);
    }

    private AuthTokens authorize() throws IOException {
        return authorize(false);
    }

    private AuthTokens authorize(/*String email, String password,*/ boolean refresh) throws IOException {
        AWSCognitoIdentityProvider awsCognitoIDPClient = null;
        String clientId = "";
        String userPoolId = "";

        try{
            awsCognitoIDPClient = new AWSCognitoIdentityProviderClient();
            awsCognitoIDPClient.setRegion(Region.getRegion(Regions.US_EAST_1));
        }catch(Exception e){
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }

        Map<String,String> params = new HashMap<String,String>();

        params.put("USERNAME","");
        params.put("PASSWORD","");

        InitiateAuthRequest initialRequest = new InitiateAuthRequest();
        initialRequest.withAuthFlow(refresh && tokens != null ?  AuthFlowType.REFRESH_TOKEN_AUTH : AuthFlowType.USER_PASSWORD_AUTH)
                .withAuthParameters(params)
                .withClientId(clientId);
        if (refresh && tokens != null) {
            Map<String, String> authParams = new HashMap<String, String>() {{
                put("REFRESH_TOKEN", tokens.getRefreshToken());
            }};
            initialRequest.withAuthParameters(authParams);
        }

        InitiateAuthResult initialResponse = awsCognitoIDPClient.initiateAuth(initialRequest);
//        AdminInitiateAuthRequest initialRequest = new AdminInitiateAuthRequest()
//                .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
//                .withAuthParameters(params)
//                .withClientId(clientId)
//                .withUserPoolId(userPoolId); //
//
//        AdminInitiateAuthResult initialResponse = awsCognitoIDPClient.adminInitiateAuth(initialRequest);
        return tokens = new AuthTokens(initialResponse);
//        return this;
    }


    protected Response performGraphqlQuery(String filename, ObjectNode variables, boolean requireAuth) throws IOException {
        InputStream iStream = getGraphqlTestFileStream(filename);
        String graphqlPayload = GraphqlTemplate.parseGraphql(iStream, variables);
        return prepareResponse(graphqlPayload, requireAuth);
    }

    private InputStream getGraphqlTestFileStream(String fileName){
        return TestClass.class.getResourceAsStream(queryPath + fileName + ".graphql");
    }

    private class IdTokenExpiredException extends RuntimeException {}
    private class IdTokenExpiredSoonException extends RuntimeException {}

    private class AuthTokens {
        private /*final*/ int idTokenExpire;

        AuthTokens(InitiateAuthResult result) throws IOException {
            accessToken = result.getAuthenticationResult().getAccessToken();
            refreshToken = result.getAuthenticationResult().getRefreshToken();
            idToken = result.getAuthenticationResult().getIdToken();

            // get expiration time
            // cut from idToken second part (between . and .); token format: part1.part2.part3
            var idTokenDelimiter = idToken.indexOf(".");
            String idTokenPart2 = new String(Base64.getDecoder().decode(idToken.substring(idTokenDelimiter+1, idToken.indexOf(".", idTokenDelimiter+2))));
            // fromIndex 2 means skip first {}
            JsonNode tokenJson = null;
            tokenJson = new ObjectMapper().readTree(idTokenPart2);
            idTokenExpire = tokenJson.get("exp").asInt();
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public String getIdToken() {
            if (idTokenExpire - 60 < System.currentTimeMillis()/1000L) {
                throw new IdTokenExpiredSoonException();
            }
            if (idTokenExpire < System.currentTimeMillis()/1000L) {
                throw new IdTokenExpiredException();
            }
            return idToken;
        }

        private final String accessToken;
        private final String refreshToken;
        private final String idToken;
    }
}
