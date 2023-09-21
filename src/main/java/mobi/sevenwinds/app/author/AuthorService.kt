package mobi.sevenwinds.app.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upperCase
import org.joda.time.DateTime

object AuthorService {
    suspend fun addRecord(body: AuthorRequest): AuthorResponse = withContext(Dispatchers.IO) {
        val entity = transaction {
            AuthorEntity.new {
                this.createdAt = DateTime.now()
                this.fullName = body.fullName
            }
        }
        entity.toResponse()
    }

    suspend fun getRecordById(id: EntityID<Int>): AuthorResponse = withContext(Dispatchers.IO) {
        transaction {
            val query = AuthorTable.select { AuthorTable.id eq id }.first()
            val data = AuthorEntity.wrapRow(query)
            return@transaction data.toResponse()
        }
    }

    suspend fun getRecordByFullName(search: String): List<AuthorResponse> = withContext(Dispatchers.IO) {
        transaction {
            val query = AuthorTable
                .select { AuthorTable.fullName.upperCase() like "%${search.toUpperCase()}%" }

            val data = AuthorEntity.wrapRows(query).map { it.toResponse() }
            data
        }
    }
}