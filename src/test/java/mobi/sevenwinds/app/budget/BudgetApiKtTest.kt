package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.BudgetRecord
import mobi.sevenwinds.app.BudgetTable
import mobi.sevenwinds.app.BudgetType
import mobi.sevenwinds.app.BudgetYearStatsResponse
import mobi.sevenwinds.app.author.AuthorRequest
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.addAuthor
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход, null))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход, null))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testBudgetPaginationWithAuthor() {

        val authorId1 = addAuthor(AuthorRequest("Warren Buffet")).id
        val authorId2 = addAuthor(AuthorRequest("Mikluho Maklay")).id
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход, authorId1))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход, authorId1))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход, authorId1))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход, authorId2))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход, authorId2))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход, null))

        RestAssured.given()
            .queryParam("limit", 2)
            .queryParam("offset", 1)
            .queryParam("search", "arre")
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(2, response.items.size)
                Assert.assertEquals("Warren Buffet", response.items[0].author?.fullName)
                Assert.assertEquals("Warren Buffet", response.items[1].author?.fullName)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход, null))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход, null))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход, null))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход, null))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                Assert.assertEquals(record, response)
            }
    }
}