package com.cmartin.learn.repository


import java.sql.Date

import slick.jdbc.H2Profile.api._


package object frm {

  object TableNames {
    val airlines = "AIRLINES"
    val airports = "AIRPORTS"
    val countries = "COUNTRIES"
    val fleet = "FLEET"
    val flights = "FLIGHTS"
    val routes = "ROUTES"
  }

  object TypeCodes {
    val AIRBUS_350_900 = "A359"
    val BOEING_787_800 = "B788"
  }

  /*
       A I R C R A F T
   */
  final case class Aircraft(typeCode: String, registration: String, airlineId: Long, id: Option[Long] = None)

  final class Fleet(tag: Tag) extends Table[Aircraft](tag, TableNames.fleet) {
    // This is the primary key column:
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def typeCode = column[String]("TYPE_CODE")

    def registration = column[String]("REGISTRATION")

    def airlineId = column[Long]("AIRLINE_ID")

    def * = (typeCode, registration, airlineId, id.?) <> (Aircraft.tupled, Aircraft.unapply)

    // foreign keys
    def airline = foreignKey("AIRLINE", airlineId, TableQuery[Airlines])(_.id)

  }

  lazy val fleet = TableQuery[Fleet]


  /*
       A I R L I N E
   */
  final case class Airline(name: String, foundationDate: Date, id: Option[Long] = None)

  final class Airlines(tag: Tag) extends Table[Airline](tag, TableNames.airlines) {
    // This is the primary key column:
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("NAME")

    def foundationDate = column[Date]("FOUNDATION_DATE")

    def * = (name, foundationDate, id.?) <> (Airline.tupled, Airline.unapply)
  }

  lazy val airlines = TableQuery[Airlines]


  /*
       C O U N T R Y
   */
  final case class Country(name: String, code: String, id: Option[Long] = None)

  final class Countries(tag: Tag) extends Table[Country](tag, TableNames.countries) {
    // This is the primary key column:
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("NAME")

    def code = column[String]("CODE")

    def * = (name, code, id.?) <> (Country.tupled, Country.unapply)

    def codeIndex = index("code_idx", code, unique = true)
  }

  lazy val countries: TableQuery[Countries] = TableQuery[Countries]


  /*
       A I R P O R T
   */
  final case class Airport(name: String, iataCode: String, icaoCode: String, countryId: Long, id: Option[Long] = None)

  final class Airports(tag: Tag) extends Table[Airport](tag, TableNames.airports) {
    // primary key column:
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("NAME")

    def iataCode = column[String]("IATA_CODE")

    def icaoCode = column[String]("ICAO_CODE")

    // foreign columns:
    def countryId = column[Long]("COUNTRY_ID")

    def * = (name, iataCode, icaoCode, countryId, id.?) <> (Airport.tupled, Airport.unapply)

    // foreign keys
    def country = foreignKey("COUNTRY", countryId, TableQuery[Countries])(_.id)

    // indexes
    def originDestinationIndex = index("iataCode_index", iataCode, unique = true)
  }

  lazy val airports = TableQuery[Airports]


  /*
       R O U T E
   */
  final case class Route(distance: Double, originId: Long, destinationId: Long, id: Option[Long] = None)

  final class Routes(tag: Tag) extends Table[Route](tag, TableNames.routes) {
    // primary key column:
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def distance = column[Double]("DISTANCE")

    // foreign key columns:
    def originId = column[Long]("ORIGIN_ID")

    def destinationId = column[Long]("DESTINATION_ID")

    //def pk = primaryKey("primaryKey", (originId, destinationId))

    def * = (distance, originId, destinationId, id.?) <> (Route.tupled, Route.unapply)

    // foreign keys
    def origin = foreignKey("FK_ORIGIN", originId, TableQuery[Airports])(origin =>
      origin.id, onDelete = ForeignKeyAction.Cascade)

    def destination = foreignKey("FK_DESTINATION", destinationId, TableQuery[Airports])(destination =>
      destination.id, onDelete = ForeignKeyAction.Cascade)

    // compound index
    def originDestinationIndex = index("origin_destination_index", (originId, destinationId), unique = true)
  }

  lazy val routes = TableQuery[Routes]


  /*
     F L I G H T
  */
  final case class Flight(code: String, alias: String, schedDeparture: Date, schedArrival: Date, id: Option[Long] = None)

  final class Flights(tag: Tag) extends Table[Flight](tag, TableNames.flights) {
    // This is the primary key column:
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

    def code = column[String]("CODE")

    def alias = column[String]("ALIAS")

    def schedDeparture = column[Date]("SCHEDULED_DEPARTURE")

    def schedArrival = column[Date]("SCHEDULED_ARRIVAL")

    def * = (code, alias, schedDeparture, schedArrival, id.?) <> (Flight.tupled, Flight.unapply)

    // foreign keys
    //TODO def route = foreignKey("FK_ROUTE")
  }

  lazy val flights = TableQuery[Flights]

}
