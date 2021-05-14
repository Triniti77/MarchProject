package flow;

import aws.AWSCognitoSession;
import aws.AWSSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import datasource.DataSource;
import graphql.GraphQLQuery;
import okhttp3.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class AddUserToCompany {

    private String userId;

    private AWSSession awsSession;
    DataSource dataSource = DataSource.getDataSource();
    datasource.Properties props;


    public AddUserToCompany() throws IOException {
    }

    @BeforeClass
    public void init() throws IOException {
        props = datasource.Properties.getProperties("rollsoft");
        awsSession = new AWSCognitoSession(props.get("admin_username"), props.get("admin_password"), props.get("admin_clientid"));
        GraphQLQuery.setUri(props.get("graphql_url"));
        GraphQLQuery.setSchemaPath("graphql");
    }

    @Test
    public void addUser() throws IOException {
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());
        ObjectNode user = dataSource.getFakeUserVariables(props.get("samanta_davis_email_template"));
        Response response = query.perform("addUser", user, true);

        Assert.assertEquals(response.code(), 200, "Response Code Assertion");

        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        Assert.assertEquals(jsonNode.get("data").get("addUser").get("user").get("firstName").asText(), user.get("firstName").asText());
        Assert.assertEquals(jsonNode.get("data").get("addUser").get("user").get("lastName").asText(), user.get("lastName").asText());
        Assert.assertEquals(jsonNode.get("data").get("addUser").get("user").get("email").asText().toLowerCase(), user.get("email").asText().toLowerCase());
        userId = jsonNode.get("data").get("addUser").get("user").get("id").asText();
    }

    @Test (dependsOnMethods = {"addUser"})
    public void assignUserToCompany() throws IOException {
        //29
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());

        ObjectNode variables = new ObjectMapper().createObjectNode();
        // {
        // $id: 55656
        // $users: [{ id: 43334 }]
        // }
        variables.put("id","209276");
        variables.putArray("users").addObject().put("id", userId); //

        Response response = query.perform("assignCompanyUser", variables, true);
        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeAssignCompanyUser = jsonNode.get("data").get("assignCompanyUser");
        Assert.assertNotEquals(nodeAssignCompanyUser, null);
        Assert.assertEquals(nodeAssignCompanyUser.get("company").get("id").asText(), variables.get("id").asText());
        Assert.assertEquals(nodeAssignCompanyUser.get("company").get("users").get(0).get("id").asText(), variables.get("users").get(0).get("id").asText());
        String compId = nodeAssignCompanyUser.get("company").get("id").asText();
    }

    @Test (dependsOnMethods = {"addUser", "assignUserToCompany"})
    public void companyGetUsers() throws IOException{
        // 59

        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());

        ObjectNode variables = new ObjectMapper().createObjectNode();

        variables.put("id","209276");


        Response response = query.perform("companyGetUsers", variables, true);
        Assert.assertEquals(response.code(), 200, "Response Code Assertion");
        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        var nodeAssignCompanyUser = jsonNode.get("data").get("companyGetUsers");
        Assert.assertNotEquals(nodeAssignCompanyUser, null);
        Assert.assertEquals(nodeAssignCompanyUser.get("company").get("id").asText(), variables.get("id").asText());

        var arrayUsers = nodeAssignCompanyUser.get("company").get("users");
        var userIdFound = false;
        for (int i=0; i< arrayUsers.size(); i++){
            var user = arrayUsers.get(i);
            var userIdFromArray = user.get("id").asText(); //
            if (userIdFromArray.equals(userId)){
                userIdFound = true;
                break;
            }
        }
        Assert.assertTrue(userIdFound);

    }


    @Test (dependsOnMethods = {"addUser"})
    public void modifyUser() throws IOException{
        // 32
        GraphQLQuery query = new GraphQLQuery(awsSession.getConnector());

        ObjectNode variables = new ObjectMapper().createObjectNode();
        ObjectNode user = dataSource.getFakeUserVariables(props.get("samanta_davis_email_template"));

        variables.put("id", userId);
        variables.put("firstName", user.get("firstName") + "TQ");
        variables.put("lastName", user.get("lastName") + "WQ");

        Response response = query.perform("modifyUser", variables, true);
        Assert.assertEquals(response.code(), 200, "Response Code Assertion");

        String jsonData = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
        Assert.assertEquals(jsonNode.get("data").get("modifyUser").get("user").get("id").asText(), variables.get("id").asText());
        Assert.assertEquals(jsonNode.get("data").get("modifyUser").get("user").get("firstName").asText(), variables.get("firstName").asText());
        Assert.assertEquals(jsonNode.get("data").get("modifyUser").get("user").get("lastName").asText(), variables.get("lastName").asText());
    }
}

// company id 209276
