package datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;

import java.io.IOException;

public class DataSource {

    protected Faker faker = new Faker();
    private ObjectNode variablesFakeUser;
    private ObjectNode variablesRealAdminUser;
    private ObjectNode variablesRealOperatorUser;

    static private DataSource instance;
    Properties props;

    private DataSource() throws IOException {
        props = Properties.getProperties("rollsoft");
    }

    public static DataSource getDataSource( ) throws IOException {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public ObjectNode getFakeUserVariables() {

        if (variablesFakeUser == null) {
            variablesFakeUser = new ObjectMapper().createObjectNode();

            variablesFakeUser.put("firstName", faker.name().firstName());
            variablesFakeUser.put("lastName", faker.name().lastName());
            variablesFakeUser.put("address", faker.address().fullAddress());
            variablesFakeUser.put("email", "rollsoft-qatest-" + faker.lorem().characters(10) + "@mailinator.com");
            variablesFakeUser.put("phone", faker.phoneNumber().cellPhone());
        }

        return variablesFakeUser;
    }

    public ObjectNode getRealAdminUserVariables() {

        if (variablesRealAdminUser == null) {
            variablesRealAdminUser = new ObjectMapper().createObjectNode();

            variablesRealAdminUser.put("firstName", props.get("admin_firstname"));
            variablesRealAdminUser.put("lastName", props.get("admin_lastname"));
            variablesRealAdminUser.put("id", props.get("admin_userid"));
            variablesRealAdminUser.put("email", props.get("admin_email"));

        }

        return variablesRealAdminUser;
    }

    public ObjectNode getRealOperatorUserVariables() {

        if (variablesRealOperatorUser == null) {
            variablesRealOperatorUser = new ObjectMapper().createObjectNode();

            variablesRealOperatorUser.put("firstName", props.get("operator_firstname"));
            variablesRealOperatorUser.put("lastName", props.get("operator_lastname"));
            variablesRealOperatorUser.put("id", props.get("operator_id"));
            variablesRealOperatorUser.put("email", props.get("operator_email"));
        }

        return variablesRealOperatorUser; // { "firstName": "...", "lastName": "...", "id": 1234, "email": "..." }
    }

    public ObjectNode getFakeUserVariables(String emailTemplate) {

        if (variablesFakeUser == null) {
            variablesFakeUser = new ObjectMapper().createObjectNode();

            variablesFakeUser.put("firstName", faker.name().firstName());
            variablesFakeUser.put("lastName", faker.name().lastName());
            variablesFakeUser.put("address", faker.address().fullAddress());
            variablesFakeUser.put("phone", faker.phoneNumber().cellPhone());

            // anyemail+danny-smith-zne@gmail.com
            String subst = variablesFakeUser.get("firstName").asText().toLowerCase() + "-"
                    + variablesFakeUser.get("lastName").asText().toLowerCase() + "-"
                    + faker.lorem().characters(3);
            variablesFakeUser.put("email", String.format(emailTemplate, subst));
        }

        return variablesFakeUser;
    }

}
