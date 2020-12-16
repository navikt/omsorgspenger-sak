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
import no.nav.omsorgspenger.sak.pdl.Identitetsnummer
import no.nav.omsorgspenger.sak.incFannHistoriskSak

internal class SaksnummerRepository(
        private val dataSource: DataSource
) : HealthCheck {

    internal fun hentSaksnummerEllerLagNytt(
        gjeldendeIdentitetsnummer: Identitetsnummer,
        historiskeIdentitetsnummer: Set<Identitetsnummer>): String {

        val saksnummerForGjeldendeIdentitetsnummer = hentSaksnummer(gjeldendeIdentitetsnummer)

        if (saksnummerForGjeldendeIdentitetsnummer != null) {
            logger.info("Fant eksisterende saksnummer på gjeldende identitetsnummer. ($saksnummerForGjeldendeIdentitetsnummer)")
            incHentSaksnummer()
            return saksnummerForGjeldendeIdentitetsnummer
        }

        val saksnummerForHistoriskeIdentitetsnummer = historiskeIdentitetsnummer.mapNotNull {
            hentSaksnummer(it)
        }.toSet().also { require(it.size in 0..1) }.firstOrNull()

        if (saksnummerForHistoriskeIdentitetsnummer != null) {
            logger.info("Fant eksisterende saksnummer på blant historiske identitetsnummer. ($saksnummerForHistoriskeIdentitetsnummer)")
            lagreSaksnummer(gjeldendeIdentitetsnummer, saksnummerForHistoriskeIdentitetsnummer)
            incFannHistoriskSak()
            return saksnummerForHistoriskeIdentitetsnummer
        }

        val nyttSaksnummer = generereSaksnummer()
        lagreSaksnummer(gjeldendeIdentitetsnummer, nyttSaksnummer)
        logger.info("Opprettet nytt saksnummer. ($nyttSaksnummer)")
        return nyttSaksnummer
    }

    internal fun hentSaksnummer(identitetsnummer: Set<Identitetsnummer>): String? {
        var saksnummer: String? = null

        identitetsnummer.forEach { ident ->
            saksnummer = hentSaksnummer(ident)
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
        saksnummer = "$SAKSNUMMER_PREFIX${i.toLong().toString(36)}"

        logger.info("Generert nytt saksnummer för behov: $saksnummer")
        incNyttSaksnummer()
        return saksnummer
    }

    private fun lagreSaksnummer(identitetsnummer: Identitetsnummer, saksnummer: String) {
        val query = "INSERT INTO SAKSNUMMER(IDENTITETSNUMMER, SAKSNUMMER) VALUES ('$identitetsnummer', '$saksnummer')"

        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(query).asUpdate)
        }.let { affectedRows ->
            if (affectedRows>0) logger.info("Lagrat nytt saksnummer")
            else {
                incPostgresFeil()
                throw IllegalStateException("Lyckades inte lagra saksnummer")
            }
        }
    }

    private fun hentSaksnummer(identitetsnummer: Identitetsnummer) : String? {
        val query = queryOf(HENT_SAKSNUMMER_QUERY, identitetsnummer)
        return using(sessionOf(dataSource)) { session ->
            session.run(
                query.map {
                    it.string("SAKSNUMMER")
                }.asSingle
            )
        }
    }

    override suspend fun check() = kotlin.runCatching {
        using(sessionOf(dataSource)) { session ->
            session.run(healthQuery)
        }
    }.fold(
            onSuccess = { Healthy("SaksnummerRepository", "OK") },
            onFailure = { UnHealthy("SaksnummerRepository", "Feil: ${it.message}") }
    )

    private companion object {
        private const val SAKSNUMMER_PREFIX = "OP"
        private const val HENT_SAKSNUMMER_QUERY = "SELECT SAKSNUMMER FROM SAKSNUMMER WHERE IDENTITETSNUMMER = ?"
        private val logger = LoggerFactory.getLogger(SaksnummerRepository::class.java)
        private val healthQuery = queryOf("SELECT 1").asExecute
    }
}
