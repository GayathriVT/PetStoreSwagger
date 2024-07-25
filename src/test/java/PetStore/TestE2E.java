package PetStore;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;
import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import java.io.FileReader;
import java.io.IOException;


public class TestE2E {

    public static RequestSpecification requestSpecification;
    public static Response response;
    public static ValidatableResponse validate;
    public static int id;
    public static JSONObject jsonObject;


    String BASE_URL = "https://petstore.swagger.io/";
    String  BASE_PATH = "v2/pet/";


    // Test 1:
    @Test(priority = 0)
    @Description("Get Request for reading all available pets")

    public void readAllAvailablePets() {

        requestSpecification = given();
        requestSpecification.baseUri(BASE_URL);
        requestSpecification.basePath(BASE_PATH );
        response = requestSpecification.when().get("findByStatus?status=available");
        validate = response.then().log().all();
        validate.statusCode(200);
        Assert.assertEquals(response.getStatusCode(), 200);
        List<String> statuses = response.jsonPath().getList("status");
        for (String status : statuses) {
            Assert.assertEquals(status, "available");
        }
    }

    //Test 2:
    @Test(priority = 1)
    @Description("Post request to add a new pet and extract the id")

    public void addNewPet() throws IOException, ParseException {

        JSONParser jsonParser = new JSONParser();
        jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/test/resources/PetToUpload.json"));

        requestSpecification = given();
        requestSpecification.baseUri(BASE_URL);
        requestSpecification.basePath(BASE_PATH);
        requestSpecification.contentType(ContentType.JSON);
        requestSpecification.body(jsonObject);

        response = requestSpecification.when().post();
        String responsestring = response.asString();
        System.out.println(responsestring);

        validate = response.then().log().all();
        validate.statusCode(200);

        id = response.then().extract().path("id");
        Assert.assertNotNull(id);
        System.out.println("Pet Id is " + id + " created");
    }

    //Test 3:
    @Test(priority = 2)
    @Description("Get request to find the pet by id")

    public void findPetbyId()
    {
        requestSpecification.basePath("v2/pet/" + id);
        response = requestSpecification.when().get();
        validate = response.then().log().all();
        validate.statusCode(200);

        int responseId = response.then().extract().path("id");
        Assert.assertEquals(responseId,id);

        String name = response.then().extract().path("name");
        Assert.assertEquals(name,"Loki");

        System.out.println("Pet Id " + responseId + " is found");
    }

    //Test 4:
    @Test(priority = 3)
    @Description("Put request for updating the existing pet details. Set the status to sold for the newly added pet")

    public void sellNewPet() {

        jsonObject.put("id",id);
        jsonObject.put("status","sold");

        requestSpecification.basePath(BASE_PATH);
        requestSpecification.body(jsonObject);
        response = requestSpecification.when().put();
        validate = response.then().log().all();
        validate.statusCode(200);

        String status = response.then().extract().path("status");
        Assert.assertEquals(status,"sold");
        System.out.println("Pet " + id +" status is changed to " + status);
    }

    //Test 5:
    @Test(priority = 4)
    @Description("Delete request to delete the sold pet from the list - Positive TC")

    public void deleteSoldPet()
    {
        requestSpecification.basePath((BASE_PATH) + id);
        response = requestSpecification.when().delete();
        validate = response.then().log().all();
        validate.statusCode(200);

        String message = response.then().extract().path("message");
        int no = Integer.parseInt(message);
        Assert.assertEquals(no,id);
        System.out.println("Pet " + id +" is sold and deleted from the list");
    }

    //Test 6:
    @Test(priority = 5)
    @Description("Delete request to delete already deleted pet, operation fails with Error code 404 - Negative TC")

    public void deleteSoldPetAgain_NegativeTC()
    {
        requestSpecification.basePath((BASE_PATH) + id);
        response = requestSpecification.when().delete();
        validate = response.then().log().all();
        validate.statusCode(404);
        assertEquals(String.valueOf(response.getStatusCode()).startsWith("40"),true, "value of response code is" + response.getStatusCode());
        System.out.println("Pet " + id +" is already deleted");
    }

    //Test 7:
    @Test(priority = 6)
    @Description("Get request to find pet by the deleted id - already deleted pet should not exist - Negative TC")

    public void findDeletedPet_NegativeTC()
    {
        requestSpecification.basePath((BASE_PATH) + id);
        response = requestSpecification.when().get();
        validate = response.then().log().all();
        validate.statusCode(404);

        assertEquals(String.valueOf(response.getStatusCode()).startsWith("40"),true, "value of response code is" + response.getStatusCode());
        String message = response.then().extract().path("message");
        Assert.assertEquals(message, "Pet not found");
    }
}


