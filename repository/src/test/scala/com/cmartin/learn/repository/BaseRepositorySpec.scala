package com.cmartin.learn.repository

import com.cmartin.learn.test.Constants._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.basic.DatabaseConfig
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.duration._

abstract class BaseRepositorySpec extends AsyncFlatSpec with Matchers with BeforeAndAfterEach {
  val config = DatabaseConfig.forConfig[JdbcProfile]("h2_dc")

  implicit def executeFromDb[A](action: DBIO[A]): Future[A] = config.db.run(action)

  val timeout = 5 seconds //TODO refactor test module

  val spainCountry = Country(esCountry._1, esCountry._2)
}
