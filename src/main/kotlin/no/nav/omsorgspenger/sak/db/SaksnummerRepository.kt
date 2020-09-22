package no.nav.omsorgspenger.sak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.math.BigInteger
import javax.sql.DataSource

internal class SaksnummerRepository(private val dataSource: DataSource) {

    fun hentSaksnummer(fødselsnummer: String): String {
        val query = queryOf("SELECT saksnummer FROM saksnummer WHERE identitetsnummer = ? LIMIT 1", fødselsnummer)
        lateinit var saksnummer: String

        using(sessionOf(dataSource)) { session ->
            session.run(query.map {
                saksnummer = it.string("saksnummer")
            }.asSingle)
        }

        if(saksnummer.isNullOrEmpty()) {
            val tall = queryOf("SELECT nextval('SEQ_SAKSNUMMER')")
                    .map { saksnummer = it.string("saksnummer") }
                    .asSingle as BigInteger
            saksnummer = java.lang.Long.toString(tall.toLong()).toUpperCase().replace("O", "o").replace("I", "i");
        }

        return saksnummer

    }


}