package graphql;

import aws.AWSCognitoSession;
import aws.AWSSession;
import aws.Exception;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vimalselvam.graphql.GraphqlTemplate;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;

public class GraphQLQuery {
    AWSSession.SessionConnector awsSession;
    AWSSession.AuthTokens tokens;
    private static final OkHttpClient client = new OkHttpClient();

    static private String graphqlUrl;
    static private String queryPath;

    public GraphQLQuery(AWSSession.SessionConnector sess) {
        awsSession = sess;
    }

    static public void setUri(String url) {
        graphqlUrl = url;
    }

    // Set path relative to resources or absolute path;
    static public void setSchemaPath(String path) {
        queryPath = path;
    }

    Response perform(String method, ObjectNode variables, boolean requireAuth) throws IOException {
        InputStream iStream = getGraphqlTestFileStream(method);
        String graphqlPayload = GraphqlTemplate.parseGraphql(iStream, variables);
        return prepareResponse(graphqlPayload, requireAuth);
    }

    private Response prepareResponse(String graphqlPayload, boolean requireAuth) throws IOException {
        if (requireAuth) {
            if (tokens == null) {
                tokens = awsSession.authorize().getTokens();
            }
        }

        RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), graphqlPayload);
        Request.Builder builder = new Request.Builder().url(graphqlUrl)
                .post(body);

        if (requireAuth) {
            try {
                builder.addHeader("Authorization", tokens.getIdToken());
            }
            catch (Exception.IdTokenExpiredSoonException e) {
                tokens = awsSession.authorize().getTokens();
                builder.addHeader("Authorization", tokens.getIdToken());
            }
            catch (Exception.IdTokenExpiredException e) {
                tokens = awsSession.refresh().getTokens();
                builder.addHeader("Authorization", tokens.getIdToken());
            }
        } else {
            builder.addHeader("x-api-key", "da2-wpubjrjtsjc4do4ojuv5v2pem4");
        }

        Request request = builder.build();
        return client.newCall(request).execute();

    }

    private InputStream getGraphqlTestFileStream(String fileName) {
//        if (queryPath.indexOf(0) == '/') {
//            throw new IOException("");
//        }
        if (queryPath.charAt(queryPath.length()-1) != '/')
            queryPath += '/';
        if (queryPath.charAt(0) != '/')
            queryPath = '/' + queryPath;
        return GraphQLQuery.class.getResourceAsStream(queryPath + fileName + ".graphql");
    }

}
