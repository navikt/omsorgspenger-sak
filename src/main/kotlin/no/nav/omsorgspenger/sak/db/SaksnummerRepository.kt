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
import no.nav.omsorgspenger.client.pdl.Identitetsnummer

internal class SaksnummerRepository(
        private val dataSource: DataSource
) : HealthCheck {

    private val logger = LoggerFactory.getLogger(SaksnummerRepository::class.java)
    private val healthQuery = queryOf("SELECT 1").asExecute

    companion object {
        private const val HENT_SAKSNUMMER_QUERY = "SELECT SAKSNUMMER FROM SAKSNUMMER WHERE IDENTITETSNUMMER = ?"
    }

    internal fun hentSaksnummerEllerLagNytt(
            identitetsnummer: Identitetsnummer,
            historiskeIdenter: Set<Identitetsnummer>): String {

        var saksnummer = ""

        historiskeIdenter.plus(identitetsnummer).forEach { ident ->
            val query = queryOf(HENT_SAKSNUMMER_QUERY, ident)
            using(sessionOf(dataSource)) { session ->
                session.run(
                        query.map {
                            saksnummer = it.string("SAKSNUMMER")
                        }.asSingle
                )
            }
            if(saksnummer.isNotEmpty()) {
                logger.info("Fann existerande saksnummer")
                if(identitetsnummer == ident) {
                    incHentSaksnummer()
                } else {
                    lagreSaksnummer(identitetsnummer, saksnummer)
                }
                return saksnummer
            }
        }

        return saksnummer
    }

    internal fun hentSaksnummer(identitetsnummer: Set<Identitetsnummer>): String? {
        var saksnummer: String? = null

        identitetsnummer.forEach { ident ->
            val query = queryOf(HENT_SAKSNUMMER_QUERY, ident)
            using(sessionOf(dataSource)) { session ->
                session.run(
                        query.map {
                            saksnummer = it.string("SAKSNUMMER")
                        }.asSingle
                )
            }
            if(!saksnummer.isNullOrEmpty()) return saksnummer
        }

        return saksnummer
    }

    private fun generereSaksnummer(): String {
        lateinit var saksnummer: String
        val sequence = queryOf("SELECT NEXTVAL('SEQ_SAKSNUMMER')")
        using(sessionOf(dataSource)) { session ->
            session.run(
                    sequence.map {
                        saksnummer = it.string(1)
                    }.asSingle
            )
        }

        val i = BigInteger.valueOf(saksnummer.toLong())
        saksnummer = i.toLong().toString(36)

        logger.info("Generert nytt saksnummer för behov: $saksnummer")
        incNyttSaksnummer()
        return saksnummer
    }

    private fun lagreSaksnummer(identitetsnummer: Identitetsnummer, saksnummer: String): Boolean {
        val query = "INSERT INTO SAKSNUMMER(IDENTITETSNUMMER, SAKSNUMMER) VALUES ('$identitetsnummer', '$saksnummer')"
        var affectedRows = 0
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(query).asUpdate)
        }.let { affectedRows = it }

        if(affectedRows>0) {
            logger.info("Lagrat nytt saksnummer")
        } else {
            logger.error("Lyckades inte lagra saksnummer").also { incPostgresFeil() }
        }

        return affectedRows > 0
    }

    override suspend fun check() = kotlin.runCatching {
        using(sessionOf(dataSource)) { session ->
            session.run(healthQuery)
        }
    }.fold(
            onSuccess = { Healthy("SaksnummerRepository", "OK") },
            onFailure = { UnHealthy("SaksnummerRepository", "Feil: ${it.message}") }
    )
}
