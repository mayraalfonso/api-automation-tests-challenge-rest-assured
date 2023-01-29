import Entities.Booking;
import Entities.BookingDates;
import Entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;


public class BookingTests {
    public static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;
    public static String token = "";

    @BeforeAll
    public static void Setup() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        user = new User(faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8, 10),
                faker.phoneNumber().toString());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        bookingDates = new BookingDates(sdf.format(faker.date().past(1, TimeUnit.DAYS)), sdf.format(faker.date().future(1, TimeUnit.DAYS)));
        booking = new Booking(user.getFirstName(), user.getLastName(),
                (float) faker.number().randomDouble(2, 50, 100000),
                true, bookingDates,
                "");
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest() {
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123");
    }


    @Test // Create booking
    @Order(1)
    public void createBooking_WithValidData_returnOk(){
        request
                .contentType(ContentType.JSON)
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                .and()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON).and().time(lessThan(2000L))
                .extract()
                .jsonPath()
        ;
    }

    @Test // create token
    @Order(2)
    public void createAuthToken(){
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "password123");

        token = request
                .header("ContentType", "application/json") //.contentType(ContentType.JSON)
                .when()
                .body(body)
                .post("/auth")
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .path("token")
        ;
    }

    @Test // Get Booking id list
    @Order(3)
    public void getAllBookingIds_returnOk(){
        Response response = request
                .when()
                .get("/booking")
                .then()
                .extract()
                .response()
                ;

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test // Get booking by id
    @Order(4)
    public void getBookingById_returnOk(){
        request
                .when()
                .get("/booking/" + faker.number().digits(1))
                .then()
                .assertThat()
                .statusCode(200)
        ;
    }

    @Test // Get bookings by user first name
    @Order(5)
    public void getAllBookingsByUserFirstName_BookingExists_returnOk() {
        request
                .when()
                .queryParam("firstName", "Carol")
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));

    }

    @Test // Get bookings by specific price
    @Order(6)
    public void getAllBookingsByPrice_BookingExists_returnOk(){
        request
                .when()
                .queryParam("totalprice", faker.number().digits(4))
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)))
        ;
    }
    @Test
    @Order(7)
    public void updateInformationFromExistingBooking() {
        RestAssured.given().baseUri("https://restful-booker.herokuapp.com/booking")
                .basePath("8")
                .contentType(ContentType.JSON)
                .accept("application/json")
                .auth().preemptive().basic("admin", "password123")
                .body("{\n" +
                        "\"firstname\" : \"James\",\n" +
                        "\"lastname\" : \"Brown\",\n" +
                        "\"totalprice\" : 111,\n" +
                        "\"depositpaid\" : true,\n" +
                        "\"bookingdates\" : {\n" +
                        "    \"checkin\" : \"2018-01-01\",\n" +
                        "    \"checkout\" : \"2019-01-01\"\n" +
                        "},\n" +
                        "\"additionalneeds\" : \"Breakfast\"\n" +
                        "}")
                .when().put()
                .then().statusCode(200);
    }
    @Test
    @Order(8)
    public void updatePartOfInformation_returnOk() {
        RestAssured.given().baseUri("https://restful-booker.herokuapp.com/booking")
                .basePath("3")
                .contentType(ContentType.JSON)
                .accept("application/json")
                .auth().preemptive().basic("admin", "password123")
                .body("{\n" +
                        "\"firstname\" : \"James\",\n" +
                        "\"lastname\" : \"Brown\"\n" +
                        "}")
                .when().patch()
                .then().statusCode(200);
    }

    @Test // Delete booking
    @Order(9)
    public void deleteBookingById_returnOk() {
        request
                .header("Cookie", "token=".concat(token))
                .when()
                .delete("/booking/" + faker.number().digits(2))
                .then()
                .assertThat()
                .statusCode(201);
    }

    @Test // Health check
    @Order(10)
    public void apiIsUpCheck_returnCreated(){
        request
                .when()
                .get("/ping")
                .then()
                .assertThat()
                .statusCode(201)
        ;
    }
}
