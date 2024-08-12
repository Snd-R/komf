package snd.komf.mediaserver.repository

//import org.flywaydb.core.Flyway
//
//class RepositoryModule(private val databaseFile: String) {
//
//    fun migrate() {
//        Flyway(
//            Flyway.configure()
//                .loggers("slf4j")
//                .dataSource("jdbc:sqlite:${databaseFile}", null, null)
//                .locations("classpath:db/migration/sqlite")
//        ).migrate()
//    }
//}