package com.cmartin.learn.repository

import com.cmartin.learn.repository.slick3.{Airlines, Countries}
import com.cmartin.learn.test.Constants
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await

abstract class EntitySpec extends FlatSpec with Matchers with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit var db: Database = _

  val schemaActionList = List(
    TableQuery[Countries],
    TableQuery[Airlines]
  )

  def createSchema() = {
    db.run(DBIO.sequence(schemaActionList.map(_.schema.create)))
  }

  override def beforeEach() = {
    db = Database.forConfig("h2mem")
    Await.result(createSchema(), Constants.waitTimeout)
  }

  override def afterEach() = {
    db.close
  }
}
