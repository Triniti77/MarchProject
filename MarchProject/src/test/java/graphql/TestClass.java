package graphql;

import java.io.*;
import java.util.ArrayList;

import aws.AWSCognitoSession;
import aws.AWSSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import datasource.DataSource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import okhttp3.*;
// x-api-key: da2-wpubjrjtsjc4do4ojuv5v2pem4

public class TestClass extends BaseTest{

    private AWSSession awsSession;
    DataSource dataSource = DataSource.getDataSource();
    datasource.Properties props;


    public TestClass() throws IOException {
    }

    @BeforeClass
    public void init() throws IOException {
        props = datasource.Properties.getProperties("rollsoft");
        awsSession = new AWSCognitoSession(props.get("admin_username"), props.get("admin_password"), props.get("admin_clientid"));
        GraphQLQuery.setUri(props.get("graphql_url"));
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

        ObjectNode variables = dataSource.getFakeUserVariables();
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
    public void testAdminUserGetByEmail() throws IOException{
        //65
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());

        ObjectNode variables = dataSource.getRealAdminUserVariables();
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
    // еще нужно доделать
    public void testOperatorUserGetByEmail() throws IOException{
        //65
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());

        ObjectNode user = dataSource.getRealOperatorUserVariables();
//        performGraphqlQueryAuth("userGetByEmail", user);
//        Response response = performGraphqlQueryAuth("userGetByEmail", user);
        Response response = query.perform("userGetByEmail", user, true);


//        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeGetUserByEmail = jsonNode.get("data").get("userGetByEmail");
        Assert.assertNotEquals(nodeGetUserByEmail, null);
        Assert.assertEquals(nodeGetUserByEmail.get("user").get("id").asText(), user.get("id").asText());
        Assert.assertEquals(nodeGetUserByEmail.get("user").get("email").asText(), user.get("email").asText());
    }

    @Test
    public void testCompanyGetByUser() throws IOException{
        //60 получаем компанию по id пользователя
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());
        ObjectNode user = dataSource.getRealAdminUserVariables();
        ObjectNode variables = new ObjectMapper().createObjectNode();
        variables.putArray("users").addObject().put("id", user.get("id")); //

        Response response = query.perform("companyGetByUser", variables, true);
        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeGetUserByEmail = jsonNode.get("data").get("companyGetByUser");
        Assert.assertNotEquals(nodeGetUserByEmail, null);
        Assert.assertEquals(nodeGetUserByEmail.get("user").get("id").asText(), user.get("id").asText());
        companyId = nodeGetUserByEmail.get("user").get("companies").get(0).get("id").asText();
    }

    String companyId;

    @Test (dependsOnMethods = {"testCompanyGetByUser"})
    public void testCompanyGetById() throws IOException{
        //56
        // Достать id компании, сохранить его в переменную
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());
       // ObjectNode user = dataSource.getRealAdminUserVariables();
        ObjectNode company = new ObjectMapper().createObjectNode();
        company.put("id", companyId);
        // variables.putArray("users").addObject().put("id", user.get("id"));
        Response response = query.perform("companyGetById", company, true);

        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeGetUserByEmail = jsonNode.get("data").get("companyGetById");
        Assert.assertNotEquals(nodeGetUserByEmail, null);
        Assert.assertEquals(nodeGetUserByEmail.get("company").get("id").asText(), company.get("id").asText());
        countryId = nodeGetUserByEmail.get("company").get("country").asText();
        // с строчки 161 нужно достать country его id
    }

    String countryId;


    @Test (dependsOnMethods = {"testCompanyGetByUser","testCompanyGetById"})
    public void testGetCountryOperators() throws IOException{
        //76
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());
        ObjectNode country = new ObjectMapper().createObjectNode();
        country.put("country", countryId);
        Response response = query.perform("getCountryOperators", country, true);

        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeGetCountryOperators = jsonNode.get("data").get("getCountryOperators");
        JsonNode arrayUsers = nodeGetCountryOperators.get("users");
        var userIdFound = false;
        for (int i=0; i< arrayUsers.size(); i++){
            var user = arrayUsers.get(i);
            var userId = user.get("id").asText(); //
            if (userId.equals("1085")){
                userIdFound = true;
                break;
            }
        }

        Assert.assertTrue(userIdFound);
    }

    @Test
    public void testGetOperatorCountries() throws IOException{
        //77
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());
        ObjectNode user = dataSource.getRealOperatorUserVariables();
        // {
        //    id: ID
        // }
        ObjectNode variables = new ObjectMapper().createObjectNode(); // {}
        variables.put("id", user.get("id").asText()); // { id: 453564 }
        Response response = query.perform("getOperatorCountries", variables, true);
        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeGetCountryOperators = jsonNode.get("data").get("getOperatorCountries");
        JsonNode arrayCountries = nodeGetCountryOperators.get("countries");
        var countriesNameFound = false;
        for (int i=0; i< arrayCountries.size(); i++){
            var country = arrayCountries.get(i);
            var countryName = country.asText();
            if (countryName.equals("XXF")){
                countriesNameFound = true;
                break;
            }
        }
        Assert.assertTrue(countriesNameFound);
    }
}
