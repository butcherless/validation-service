package com.cmartin.learn.poc

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.cmartin.learn.domain.Model.Airport2
import com.cmartin.learn.domain.Model.Country
import io.getquill.Delete
import io.getquill.EntityQuery
import io.getquill.Insert
import io.getquill.Literal
import io.getquill.NamingStrategy
import io.getquill.PostgresAsyncContext
import io.getquill.PostgresDialect
import io.getquill.PostgresJdbcContext
import io.getquill.Update
import io.getquill.context.Context
import io.getquill.idiom.Idiom
import io.getquill.monad.IOMonad

object Abstractions {

  trait BaseEntity[I] {
    val id: I
  }

  // Domain Port
  trait BaseRepository[F[_], E] {
    def insert(e: E): F[E]
    def update(e: E): F[E]
    def delete(e: E): F[E]
  }

  abstract class LongEntity(id: Long) extends BaseEntity[Long]
  abstract class UuidEntity(id: UUID) extends BaseEntity[UUID]
}

object Implementations {
  import Abstractions._

  /* C L A S S E S */

  trait Converter {
    def toModel[A, B](a: A): B
  }
  case class CountryDbo(name: String, code: String, id: Long = 0L) extends LongEntity(id)
  object CountryDbo {
    def fromCountry(country: Country): CountryDbo =
      CountryDbo(
        name = country.name,
        code = country.code
      )

    def toModel(dbo: CountryDbo): Country =
      Country(
        name = dbo.name,
        code = dbo.code
      )

    def update(dbo: CountryDbo, country: Country) =
      dbo.copy(name = country.name, code = country.code)
  }

  case class AirportDbo(
      name: String,
      iataCode: String,
      icaoCode: String,
      countryId: Long,
      id: Long = 0L
  ) extends LongEntity(id)

  object AirportDbo {
    def from(airport: Airport2, countryId: Long): AirportDbo =
      AirportDbo(
        name = airport.name,
        iataCode = airport.iataCode,
        icaoCode = airport.icaoCode,
        countryId = countryId
      )
    def toModel(dbo: AirportDbo, c: CountryDbo): Airport2 =
      Airport2(
        name = dbo.name,
        iataCode = dbo.iataCode,
        icaoCode = dbo.icaoCode,
        country = CountryDbo.toModel(c)
      )

    def update(dbo: AirportDbo, airport: Airport2, countryId: Long) =
      dbo.copy(
        name = airport.name,
        iataCode = airport.iataCode,
        icaoCode = airport.icaoCode,
        countryId
      )
  }

  case class RepositoryException(m: String) extends RuntimeException(m)

  /* C O N T E X T S */

  trait CommonContext[I <: Idiom, N <: NamingStrategy] { this: Context[I, N] =>
    def findCountryByCodeQuery(code: String) =
      quote {
        query[CountryDbo]
          .filter(c => c.code == lift(code))
      }
  }

  trait CountryDboContext[I <: Idiom, N <: NamingStrategy] extends CommonContext[I, N] {
    this: Context[I, N] =>

    def insertQuery(dbo: CountryDbo) =
      quote {
        query[CountryDbo]
          .insert(lift(dbo))
      }

    def updateQuery(dbo: CountryDbo) =
      quote {
        query[CountryDbo]
          .filter(c => c.id == lift(dbo.id))
          .update(lift(CountryDbo(dbo.name, dbo.code, dbo.id)))
      }

    def deleteQuery(dbo: CountryDbo) =
      quote {
        query[CountryDbo]
          .filter(c => c.id == lift(dbo.id))
          .delete
      }

  }

  trait AirportDboContext[I <: Idiom, N <: NamingStrategy] extends CommonContext[I, N] {
    this: Context[I, N] =>
    def insertQuery(dbo: AirportDbo) =
      quote {
        query[AirportDbo]
          .insert(lift(dbo))
      }

    def findByCountryCodeQuery(code: String) =
      quote {
        for {
          country <- query[CountryDbo]
            .filter(c => c.code == lift(code))
          airports <- query[AirportDbo]
            .join(airport => airport.countryId == country.id)

        } yield (airports, country)
      }

  }

  /* Domain Port
     TODO refactor: Future => zio.Task
   */
  trait CountryRepository extends BaseRepository[Future, Country] {
    def findByCode(code: String): Future[Country]
  }

  /* Domain Port
     TODO refactor: Future => zio.Task
   */
  trait AirportRepository extends BaseRepository[Future, Airport2] {
    def findByCountryCode(code: String): Future[Seq[Airport2]]
  }

  class AirportPostgresRepository(configPrefix: String)(implicit ec: ExecutionContext)
      extends CommonPostgresRepository(configPrefix)
      with AirportDboContext[PostgresDialect, Literal]
      with AirportRepository {

    override def insert(airport: Airport2): Future[Airport2] = {
      val program = for {
        countries <- runIO(findCountryByCodeQuery(airport.country.code))
        country   <- checkHeadElement(countries, s"country code not found: ${airport.country.code}")
        dbo       <- IO.successful(AirportDbo.from(airport, country.id))
        _         <- runIO(insertQuery(dbo))
      } yield airport

      performIO(program)
    }

    override def update(e: Airport2): Future[Airport2] = ???

    override def delete(e: Airport2): Future[Airport2] = ???

    override def findByCountryCode(code: String): Future[Seq[Airport2]] = {
      val program = for {
        dbos <- runIO(findByCountryCodeQuery(code))
        airports <- IO.successful(
          dbos.map(tuple => AirportDbo.toModel(tuple._1, tuple._2))
        )
      } yield airports

      performIO(program)
    }

  }

  class CommonPostgresRepository(configPrefix: String)
      extends PostgresAsyncContext[Literal](Literal, configPrefix) {
    def checkHeadElement[T](seq: Seq[T], error: String) = {
      seq.headOption
        .fold(
          IO.failed[T](RepositoryException(error))
        )(e => IO.successful(e))
    }

  }

  import scala.language.experimental.macros

  /* AbstractContext: Query DSL for common operations via macro implmentations */
  trait EntityContext extends IOMonad {
    this: Context[_, _] =>

    def insertQuery[T](entity: T): Quoted[Insert[T]] = macro CommonOpsMacro.insert[T]

    def updateQuery[T](entity: T): Quoted[Update[T]] = macro CommonOpsMacro.update[T]

    def deleteQuery[T](entity: T): Quoted[Delete[T]] = macro CommonOpsMacro.delete[T]

    def findCountryByCodeQuery(code: String) =
      quote {
        query[CountryDbo]
          .filter(c => c.code == lift(code))
      }

    def findAirportByIataCodeQuery(code: String) =
      quote {
        query[AirportDbo]
          .filter(a => a.iataCode == lift(code))
      }

    def findAirportByCountryCodeQuery(code: String) =
      quote {
        for {
          c  <- query[CountryDbo] if (c.code == lift(code))
          as <- query[AirportDbo] if (as.countryId == c.id)
        } yield (as, c)
      }

    def checkHeadElement[T](seq: Seq[T], error: String) = {
      seq.headOption
        .fold(
          IO.failed[T](RepositoryException(error))
        )(e => IO.successful(e))
    }
  }

  class CountryDboMacroRepo(configPrefix: String)(implicit ec: ExecutionContext)
      extends CountryRepository {

    val ctx = new PostgresAsyncContext(Literal, configPrefix) with EntityContext
    import ctx._

    override def findByCode(code: String): Future[Country] = ???

    def insert(country: Country): Future[Country] = {
      val dbo = CountryDbo.fromCountry(country)
      val program =
        for {
          _ <- runIO(insertQuery(dbo))
        } yield country

      performIO(program)
    }

    def update(country: Country): Future[Country] = {
      val program =
        for {
          dbos <- runIO(findCountryByCodeQuery(country.code))
          dbo  <- checkHeadElement(dbos, s"country code not found: ${country.code}}")
          _    <- runIO(updateQuery(CountryDbo.update(dbo, country)))
        } yield country

      performIO(program)
    }

    def delete(country: Country): Future[Country] = {
      val program = for {
        dbos <- runIO(findCountryByCodeQuery(country.code))
        dbo  <- checkHeadElement(dbos, s"country code not found: ${country.code})")
        _    <- runIO(deleteQuery(dbo))
      } yield country

      performIO(program)
    }

  }

  class AirportDboMacroRepo(configPrefix: String)(implicit ec: ExecutionContext)
      extends AirportRepository {

    // TODO refactor to common class
    val ctx = new PostgresAsyncContext(Literal, configPrefix) with EntityContext
    import ctx._

    override def insert(airport: Airport2): Future[Airport2] = {
      val program = for {
        countries <- runIO(findCountryByCodeQuery(airport.country.code))
        country   <- checkHeadElement(countries, s"country code not found: ${airport.country.code})")
        dbo       <- IO.successful(AirportDbo.from(airport, country.id))
        _         <- runIO(insertQuery(dbo))
      } yield airport

      performIO(program)
    }

    /* countries.head is guaranteed by the restriction of the
       Country foreign key in the Airport table
     */
    override def update(airport: Airport2): Future[Airport2] = {
      val program = for {
        dbo       <- findUniqueAirportByIataCode(airport.iataCode)
        countries <- runIO(findCountryByCodeQuery(airport.country.code))
        _         <- runIO(updateQuery(AirportDbo.update(dbo, airport, countries.head.id)))
      } yield airport

      performIO(program)
    }

    override def delete(airport: Airport2): Future[Airport2] = {
      val program = for {
        dbo <- findUniqueAirportByIataCode(airport.iataCode)
        _   <- runIO(deleteQuery(dbo))
      } yield airport

      performIO(program)
    }

    private def findUniqueAirportByIataCode(code: String) = {
      for {
        dbos <- runIO(findAirportByIataCodeQuery(code))
        dbo  <- checkHeadElement(dbos, s"airport code not found: $code)")
      } yield dbo
    }

    override def findByCountryCode(code: String): Future[Seq[Airport2]] = {
      val program = for {
        tupleDbos <- runIO(findAirportByCountryCodeQuery(code))
        airports <- IO.successful(
          tupleDbos
            .map(airport_country => AirportDbo.toModel(airport_country._1, airport_country._2))
        )
      } yield airports

      performIO(program)
    }

  }

  // FOR REMOVING

  class CountryPostgresRepository(configPrefix: String)(implicit ec: ExecutionContext)
      extends CommonPostgresRepository(configPrefix)
      with CountryDboContext[PostgresDialect, Literal]
      with CountryRepository {

    override def insert(country: Country): Future[Country] = {
      val dbo = CountryDbo.fromCountry(country)
      val program =
        for {
          _ <- runIO(insertQuery(dbo))
        } yield country

      performIO(program)
    }

    override def update(country: Country): Future[Country] = {
      val program =
        for {
          dbos <- runIO(findCountryByCodeQuery(country.code))
          dbo  <- checkHeadElement(dbos, s"country code not found: ${country.code}}")
          _    <- runIO(updateQuery(dbo))
        } yield country

      performIO(program)
    }

    override def delete(country: Country): Future[Country] = {
      val program =
        for {
          dbos <- runIO(findCountryByCodeQuery(country.code))
          dbo  <- checkHeadElement(dbos, s"country code not found: ${country.code}}")
          _    <- runIO(deleteQuery(dbo))
        } yield country

      performIO(program)
    }

    /* cases:
       - Dbolist is empty => Entity not found error
       - DboList has a single Entity => database index constraint
     */
    override def findByCode(code: String): Future[Country] = {
      val program = for {
        dbos    <- runIO(findCountryByCodeQuery(code))
        dbo     <- checkHeadElement(dbos, s"country code not found: $code")
        country <- IO.successful(CountryDbo.toModel(dbo))
      } yield country

      performIO(program)
    }

  }

}
