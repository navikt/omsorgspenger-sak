package no.nav.omsorgspenger.sak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class SaksnummerRepository(private val dataSource: DataSource) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentSaksnummer(fødselsnummer: String): String {
        val query = queryOf("SELECT SAKSNUMMER FROM SAKSNUMMER WHERE IDENTITETSNUMMER = ?", fødselsnummer)
        var saksnummer = ""
        // kotlin.UninitializedPropertyAccessException: lateinit property saksnummer has not been initialized

        using(sessionOf(dataSource)) { session ->
            session.run(query.map {
                saksnummer = it.string("SAKSNUMMER")
            }.asSingle)
        }

        if (saksnummer.isNotEmpty()) {
            logger.info("Fann existerande saksnummer")
            return saksnummer
        }

        val tall = queryOf("SELECT NEXTVAL('SEQ_SAKSNUMMER')")
        using(sessionOf(dataSource)) { session ->
            session.run(tall.map {
                saksnummer = it.string(1)
            }.asSingle)
        }

        logger.info("Genererat nytt saksnummer för behov. $saksnummer")
        if (lagreSaksnummer(fødselsnummer, saksnummer) > 0) {
            logger.info("Lagrat saksnummer")
        } else {
            logger.error("Lyckades inte lagra saksnummer")
        }

        return saksnummer
    }

    private fun lagreSaksnummer(fødselsnummer: String, saksnummer: String): Int {
        val query = "INSERT INTO SAKSNUMMER(IDENTITETSNUMMER, SAKSNUMMER) VALUES ($fødselsnummer, $saksnummer)"
        var affectedRows = 0
        using(sessionOf(dataSource)) { session ->
            affectedRows = session.run(queryOf(query).asUpdate)
        }
        return affectedRows
    }

}