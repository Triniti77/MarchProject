package graphql;

import java.io.*;
import java.util.Properties;

import aws.AWSCognitoSession;
import aws.AWSSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import okhttp3.*;
// x-api-key: da2-wpubjrjtsjc4do4ojuv5v2pem4

public class TestClass extends BaseTest{

    private AWSSession awsSession;
    DataSourse dataSourse = new DataSourse();

    public TestClass() {
    }

    @BeforeClass
    public void init() {
        Properties prop = new Properties();
        try {
            prop.load(TestClass.class.getClassLoader().getResourceAsStream("rollsoft.properties"));
        } catch (IOException e) {
            System.out.println("Cannot read properties");
            System.exit(1);
        }
        awsSession = new AWSCognitoSession(prop.getProperty("admin_username"), prop.getProperty("admin_password"), prop.getProperty("admin_clientid"));
        GraphQLQuery.setUri(prop.getProperty("graphql_url"));
        GraphQLQuery.setSchemaPath("graphql");
    }

    protected Response performGraphqlQueryAuth(String filename, ObjectNode variables) throws IOException {
        return performGraphqlQuery(filename, variables, true);
    }

    protected Response performGraphqlQueryNoauth(String filename, ObjectNode variables) throws IOException {
        return performGraphqlQuery(filename, variables, false);
    }


    @Test
    public void testCountriesGetActive() throws IOException {
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());
        Response response = query.perform("countriesGetActive", null, false);
//        Response response = performGraphqlQueryNoauth("countriesGetActive", null);

        Assert.assertEquals(response.code(), 200, "Response Code Assertion");

        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        Assert.assertEquals(jsonNode.get("data").get("countriesGetActive").get(0).get("__typename").asText(), "Country");
    }

    @Test
    public void testAddUser() throws IOException {
        //66 userIsRegistered(user: UserEmailInput): Int @aws_api_key @aws_cognito_user_pools

        // Create a variables to pass to the graphql query
//        ObjectNode variables = new ObjectMapper().createObjectNode();
//        variables.put("firstName", faker.name().firstName());
//        variables.put("lastName", faker.name().lastName());
//        variables.put("address", faker.address().fullAddress());
//        variables.put("email", "rollsoft-qatest-" + faker.lorem().characters(10) + "@mailinator.com");
//        variables.put("phone", faker.phoneNumber().cellPhone());

        ObjectNode variables = dataSourse.getFakeUserVariables();
        Response response = performGraphqlQueryNoauth("addUser",variables);

        Assert.assertEquals(response.code(), 200, "Response Code Assertion");

        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        Assert.assertEquals(jsonNode.get("data").get("addUser").get("user").get("firstName").asText(), variables.get("firstName").asText());
        Assert.assertEquals(jsonNode.get("data").get("addUser").get("user").get("lastName").asText(), variables.get("lastName").asText());
        Assert.assertEquals(jsonNode.get("data").get("addUser").get("user").get("email").asText(), variables.get("email").asText());
        Assert.assertEquals(jsonNode.get("data").get("addUser").get("user").get("lastLogin").asInt(), 0);

    }

    @Test
    public void testUserGetByEmail() throws IOException{
        //65
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());

        ObjectNode variables = dataSourse.getRealAdminUserVariables();
//        performGraphqlQueryAuth("userGetByEmail", variables);
//        Response response = performGraphqlQueryAuth("userGetByEmail", variables);
        Response response = query.perform("userGetByEmail", variables, true);


//        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeGetUserByEmail = jsonNode.get("data").get("userGetByEmail");
        Assert.assertNotEquals(nodeGetUserByEmail, null);
        Assert.assertEquals(nodeGetUserByEmail.get("user").get("id").asText(), variables.get("id").asText());
        Assert.assertEquals(nodeGetUserByEmail.get("user").get("email").asText(), variables.get("email").asText());
    }

    @Test
    public void testCompanyGetByEmail() throws IOException{
        //60 получаем компанию по id пользователя



    }








// https://stackoverflow.com/questions/36840244/how-do-i-authenticate-against-an-aws-cognito-user-pool
    // https://stackoverflow.com/questions/51162584/aws-cognito-sign-in-with-java-sdk-for-desktop-application



//    @Test
//    public void testGraphqlWithFile() throws IOException {
//        // Read a graphql file
//        File file = new File("src/test/resources" + queryPath);
//
//        // Create a variables to pass to the graphql query
//        ObjectNode variables = new ObjectMapper().createObjectNode();
//        variables.put("name", "Pikachu");
//
//        // Now parse the graphql file to a request payload string
//        String graphqlPayload = GraphqlTemplate.parseGraphql(file, variables);
//
//        // Build and trigger the request
//        Response response = prepareResponse(graphqlPayload);
//
//        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
//
//        String jsonData = response.body().string();
//        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
//        Assert.assertEquals(jsonNode.get("data").get("pokemon").get("name").asText(), "Pikachu");
//    }

//    @Test
//    public void testGraphqlWithNoVariables() throws IOException {
//        // Read a graphql file
//        File file = new File("src/test/resources/graphql/pokemon-with-no-variable.graphql");
//
//        // Now parse the graphql file to a request payload string
//        String graphqlPayload = GraphqlTemplate.parseGraphql(file, null);
//
//        // Build and trigger the request
//        Response response = prepareResponse(graphqlPayload);
//
//        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
//
//        String jsonData = response.body().string();
//        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
//        Assert.assertEquals(jsonNode.get("data").get("pokemon").get("name").asText(), "Pikachu");
//    }
}
