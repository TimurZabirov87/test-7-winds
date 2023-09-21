package mobi.sevenwinds.common

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ResponseBodyExtractionOptions
import io.restassured.specification.RequestSpecification
import mobi.sevenwinds.app.author.AuthorRequest
import mobi.sevenwinds.app.author.AuthorResponse

fun RequestSpecification.auth(token: String): RequestSpecification = this
    .header("Authorization", "Bearer $token")

fun <T> RequestSpecification.jsonBody(body: T): RequestSpecification = this
    .body(body)
    .contentType(ContentType.JSON)

inline fun <reified T> ResponseBodyExtractionOptions.toResponse(): T {
    return this.`as`(T::class.java)
}

fun RequestSpecification.When(): RequestSpecification {
    return this.`when`()
}

fun addAuthor(record: AuthorRequest): AuthorResponse {
    RestAssured.given()
        .jsonBody(record)
        .post("/author/add")
        .toResponse<AuthorResponse>().let { response ->
            return response
        }
}