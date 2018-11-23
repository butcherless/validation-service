package com.cmartin.learn.repository

import java.time.{LocalDate, LocalTime}

import com.cmartin.learn.repository.frm._
import com.cmartin.learn.repository.spec.BaseRepository
import slick.jdbc.H2Profile.api._
import slick.lifted.TableQuery


package object implementation {


  class AircraftRepository(implicit db: Database) extends BaseRepository[Aircraft, Fleet](db) {
    lazy val entities = TableQuery[Fleet]

    def findByRegistration(registration: String) =
      db.run(entities.filter(_.registration === registration).result)

    def insert(aircraft: Aircraft) =
      db.run(entityReturningId += aircraft)
  }

  class AirlineRepository(implicit db: Database) extends BaseRepository[Airline, Airlines](db) {
    lazy val entities = TableQuery[Airlines]

    def insert(airline: Airline) =
      db.run(entityReturningId += airline)
  }


  class AirportRepository(implicit db: Database) extends BaseRepository[Airport, Airports](db) {
    lazy val entities = TableQuery[Airports]

    def insert(airport: Airport) =
      db.run(entityReturningId += airport)

    def findByCountryCode(code: String) = {
      val query = for {
        airport <- entities
        country <- airport.country if country.code === code
      } yield airport

      db.run(query.result.headOption)
    }
  }

  class CountryRepository(implicit db: Database) extends BaseRepository[Country, Countries](db) {
    lazy val entities = TableQuery[Countries]

  def insert(country:Country) =
    db.run(entityReturningId += country)


  def findByCode(code: String) = {
          db.run(entities.filter(_.code === code).result.headOption)
        }

  /*
    def findByCodeQuery(code: String) = {
      entities.filter(_.code === code) //.result.headOption
    }

    def insertAction(name: String, code: String) =
      entityReturningId += Country(name, code)
      */
  }

  /*
  object FlightRepository extends BaseRepository[Flight, Flights] {
    lazy val entities = TableQuery[Flights]

    def insertAction(code: String, alias: String, departure: LocalTime, arrival: LocalTime, airlineId: Long, routeId: Long) =
      entityReturningId += Flight(code, alias, departure, arrival, airlineId, routeId)

    def findByCode(code: String) = entities.filter(_.code === code).result

    def findByOrigin(origin: String) = {
      val query = for {
        flight <- entities
        route <- flight.route
        airport <- route.origin if airport.iataCode === origin
      } yield flight

      query.result
    }

  }

  object JourneyRepository extends BaseRepository[Journey, Journeys] {
    lazy val entities = TableQuery[Journeys]

    def insertAction(departureDate: LocalTime, arrivalDate: LocalTime, flightId: Long, aircraftId: Long) =
      entityReturningId += Journey(departureDate, arrivalDate, flightId, aircraftId)
  }


  object RouteRepository extends BaseRepository[Route, Routes] {
    lazy val entities = TableQuery[Routes]

    def insertAction(distance: Double, originId: Long, destinationId: Long) =
      entityReturningId += Route(distance, originId, destinationId)

    def findDestinationsByOrigin(iataCode: String) = {
      val query = for {
        route <- entities
        airport <- route.destination
        origin <- route.origin if origin.iataCode === iataCode
      } yield airport

      query.result
    }

  }
*/
}