package aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.amazonaws.services.gamelift.model.Build;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AWSCognitoSession extends AWSSession {
    private AWSSession.SessionConnector connector;
    private AuthCognitoTokens tokens;
    AWSSession.Builder bld;

    public AWSCognitoSession(String username, String password, String clientId) {
        super();
        bld = new Builder();
        params.put("username", username);
        params.put("password", password);
        params.put("clientId", clientId);
    }

    @Override
    protected AWSSession.Builder builder() {
        return bld;
    }

    @Override
    public AWSSession.SessionConnector getConnector() {
        if (connector == null) {
            connector = bld.build();
        }
        return connector;
    }

    class Builder extends AWSSession.Builder {
        protected Builder() {
            super();
        }

        public SessionConnector build() {
            return new SessionConnector();
        }
    }

    class SessionConnector extends AWSSession.SessionConnector {

        AWSCognitoIdentityProvider awsCognitoIDPClient;

        private SessionConnector() {
            super();
            awsCognitoIDPClient = new AWSCognitoIdentityProviderClient();
            // !TODO! replace region with variable
            awsCognitoIDPClient.setRegion(Region.getRegion(Regions.US_EAST_1));
//            }catch(Exception e){
//                System.out.println(e.getMessage());
//                System.out.println(e.getStackTrace());
//            }
        }

//        protected SessionConnector(Map<String, String> params) {
//            super();
//        }

        @Override
        public AWSSession.SessionConnector authorize() throws IOException {
            return authorize(false);
        }

        public AWSSession.SessionConnector authorize(boolean refresh) throws IOException {
//            String clientId = "2lbq08plvdi7b8h0u9rrdjn9ij";
//            String userPoolId = "us-east-1_lVgMErw47";
            Map<String,String> authParams = new HashMap<>();

            InitiateAuthRequest initialRequest = new InitiateAuthRequest();
            if (!refresh || tokens == null) {
                authParams.put("USERNAME", params.get("username"));
                authParams.put("PASSWORD", params.get("password"));
            }

            initialRequest.withAuthFlow(refresh && tokens != null ?  AuthFlowType.REFRESH_TOKEN_AUTH : AuthFlowType.USER_PASSWORD_AUTH)
                    .withAuthParameters(authParams)
                    .withClientId(params.get("clientId"));

            if (refresh && tokens != null) {
                authParams.put("REFRESH_TOKEN", tokens.getRefreshToken());
                initialRequest.withAuthParameters(authParams);
            }

            InitiateAuthResult initialResponse = awsCognitoIDPClient.initiateAuth(initialRequest);
//        AdminInitiateAuthRequest initialRequest = new AdminInitiateAuthRequest()
//                .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
//                .withAuthParameters(params)
//                .withClientId(clientId)
//                .withUserPoolId(userPoolId); //
//
//        AdminInitiateAuthResult initialResponse = awsCognitoIDPClient.adminInitiateAuth(initialRequest)
            tokens = new AuthCognitoTokens(initialResponse);
            return this;
        }

        @Override
        public AWSSession.SessionConnector refresh() throws IOException {
            return authorize(true);
        }

        @Override
        public AuthTokens getTokens() {
            return tokens;
        }
    }

    protected class TokensIO {
        final static String tokenPath = "./data/";

        public void writeToken(String name, String data) {
            OutputStream os;
            try {
                os = new FileOutputStream(tokenPath + name + ".dat");
                os.write(data.getBytes());
                os.close();
            } catch (IOException e) {
                System.out.println("[ERROR] Error writing token "+name+" with reason: " + e.getMessage());
            }
        }

        public String readToken(String name) {
            InputStream is;
            InputStreamReader reader;
            String fileName;
            long fileSize;
            try {
                fileName = tokenPath + name + ".dat";
                File f = new File(fileName);
                fileSize = f.length();
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                return null;
            }
            try {
                reader = new InputStreamReader(is);
                char[] buf = new char[(int)fileSize];
                reader.read(buf, 0, (int)fileSize);
                return new String(buf);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public class AuthCognitoTokens implements AWSSession.AuthTokens {
        private String method;
        private String accessToken;
        private String refreshToken;
        private String idToken;
        private Long idTokenExpire;

        private AuthCognitoTokens(InitiateAuthResult response) throws IOException {
            method = response.getAuthenticationResult().getTokenType();
            accessToken = response.getAuthenticationResult().getAccessToken();
            refreshToken = response.getAuthenticationResult().getRefreshToken();
            idToken = response.getAuthenticationResult().getIdToken();

            TokensIO tokio = new TokensIO();
            tokio.writeToken("accessToken", accessToken);
            tokio.writeToken("refreshToken", refreshToken);
            tokio.writeToken("idToken", idToken);

            // cut from idToken second part (between . and .); token format: part1.part2.part3
            var idTokenDelimiter = idToken.indexOf(".");
            String idTokenPart2 = new String(Base64.getDecoder().decode(idToken.substring(idTokenDelimiter+1, idToken.indexOf(".", idTokenDelimiter+2))));
            // fromIndex 2 means skip first {}
            JsonNode tokenJson;
            tokenJson = new ObjectMapper().readTree(idTokenPart2);
            idTokenExpire = (long) tokenJson.get("exp").asInt();
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public String getRefreshToken() {
            return refreshToken;
        }

        @Override
        public String getIdToken() {
            if (idTokenExpire - 60 < System.currentTimeMillis()/1000L) {
                throw new Exception.IdTokenExpiredSoonException();
            }
            if (idTokenExpire < System.currentTimeMillis()/1000L) {
                throw new Exception.IdTokenExpiredException();
            }
            return idToken;
        }
    }
}
