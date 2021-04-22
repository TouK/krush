package pl.touk.krush.base

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

class PostgresContainer(dockerImageName: DockerImageName) : PostgreSQLContainer<PostgresContainer>(dockerImageName)

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseDatabaseTest {

    companion object {
        private val postgresqlContainer = PostgresContainer(DockerImageName.parse("postgres:12"))
            .withDatabaseName("krush")
            .withUsername("krush")
            .withPassword("krush")
            .withReuse(true)
    }

    private lateinit var dataSource: DataSource

    @BeforeAll
    fun connect() {
        postgresqlContainer.start()

        dataSource = PGSimpleDataSource().also {
            it.setURL(postgresqlContainer.jdbcUrl)
            it.user = postgresqlContainer.username
            it.password = postgresqlContainer.password
        }

        Database.connect(dataSource)
        migrate()
    }

    fun migrate() {
        val flyway = Flyway.configure().dataSource(dataSource).load()
        flyway.migrate()
    }

}
