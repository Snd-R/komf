package snd.komf.mediaserver.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Path

actual class DriverFactory(private val databaseFile: Path) {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver =
            JdbcSqliteDriver(
                url = "jdbc:sqlite:${databaseFile}",
                schema = Database.Schema,
            )
        return driver
    }
}