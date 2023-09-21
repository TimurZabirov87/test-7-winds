package mobi.sevenwinds.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorResponse
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.authorId = if (body.authorId != null) EntityID(body.authorId, AuthorTable) else null
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        val year = param.year

        // Execute a query without applying pagination to get the total count
        val totalCountQuery = BudgetTable
            .select { BudgetTable.year eq year }
        val totalByType = mutableMapOf<String, Int>()
        var totalCount = 0

        transaction {
            for (row in totalCountQuery) {
                val type = row[BudgetTable.type].name
                totalByType[type] = totalByType.getOrDefault(type, 0) + row[BudgetTable.amount]
            }
            totalCount = totalCountQuery.count()
        }


        // Execute the paginated query
        val query = BudgetTable
            .join(AuthorTable, JoinType.LEFT, BudgetTable.authorId, AuthorTable.id)
            .slice(
                BudgetTable.year,
                BudgetTable.month,
                BudgetTable.amount,
                BudgetTable.type,
                AuthorTable.id,
                AuthorTable.fullName,
                AuthorTable.createdAt
            )
            .select { BudgetTable.year eq year }
            .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)

        if (param.limit > 0) {
            query.limit(param.limit, param.offset)
        }

        if (param.search != null) {
            val search = "%${param.search.toLowerCase()}%"
            query.andWhere { AuthorTable.fullName.lowerCase() like search }
        }

        val items = mutableListOf<BudgetResponse>()

        transaction {
            for (row in query) {
                val author = if (row[AuthorTable.fullName] != null) {
                    AuthorResponse(row[AuthorTable.id].value, row[AuthorTable.fullName], row[AuthorTable.createdAt].toLDT())
                } else {
                    null
                }

                val budget = BudgetResponse(
                    row[BudgetTable.year],
                    row[BudgetTable.month],
                    row[BudgetTable.amount],
                    row[BudgetTable.type],
                    author
                )

                items.add(budget)
            }
        }

        BudgetYearStatsResponse(totalCount, totalByType, items)
    }

}