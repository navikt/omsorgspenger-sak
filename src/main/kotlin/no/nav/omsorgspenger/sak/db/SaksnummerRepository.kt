package no.nav.omsorgspenger.sak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.omsorgspenger.sak.incHentSaksnummer
import no.nav.omsorgspenger.sak.incNyttSaksnummer
import no.nav.omsorgspenger.sak.incPostgresFeil
import org.slf4j.LoggerFactory
import java.math.BigInteger
import javax.sql.DataSource

internal class SaksnummerRepository(
    private val dataSource: DataSource) : HealthCheck {

    private val logger = LoggerFactory.getLogger(SaksnummerRepository::class.java)
    private val healthQuery = queryOf("SELECT 1").asExecute

    internal fun hentSaksnummer(fødselsnummer: String): String {
        val query = queryOf("SELECT SAKSNUMMER FROM SAKSNUMMER WHERE IDENTITETSNUMMER = ?", fødselsnummer)
        var saksnummer = ""

        using(sessionOf(dataSource)) { session ->
            session.run(query.map {
                saksnummer = it.string("SAKSNUMMER")
            }.asSingle)
        }

        if (saksnummer.isNotEmpty()) {
            logger.info("Fann existerande saksnummer")
            incHentSaksnummer()
            return saksnummer
        }

        val sequence = queryOf("SELECT NEXTVAL('SEQ_SAKSNUMMER')")
        using(sessionOf(dataSource)) { session ->
            session.run(sequence.map {
                saksnummer = it.string(1)
            }.asSingle)
        }

        val i = BigInteger.valueOf(saksnummer.toLong())
        saksnummer = i.toLong().toString(36)

        logger.info("Genererat nytt saksnummer för behov: $saksnummer")
        incNyttSaksnummer()

        if (lagreSaksnummer(fødselsnummer, saksnummer) > 0) {
            logger.info("Lagrat nytt saksnummer")
        } else {
            logger.error("Lyckades inte lagra saksnummer")
            incPostgresFeil()
        }

        return saksnummer
    }

    private fun lagreSaksnummer(fødselsnummer: String, saksnummer: String): Int {
        val query = "INSERT INTO SAKSNUMMER(IDENTITETSNUMMER, SAKSNUMMER) VALUES ('$fødselsnummer', '$saksnummer')"
        var affectedRows = 0
        using(sessionOf(dataSource)) { session ->
            affectedRows = session.run(queryOf(query).asUpdate)
        }
        return affectedRows
    }

    override suspend fun check() = kotlin.runCatching {
        using(sessionOf(dataSource)) { session ->
            session.run(healthQuery)
        }
    }.fold(
        onSuccess = { Healthy("SaksnummerRepository", "OK")},
        onFailure = { UnHealthy("SaksnummerRepository", "Feil: ${it.message}")}
    )
}