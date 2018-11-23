package com.cmartin.learn

import slick.jdbc.H2Profile.api._

package object poc {

  trait BaseEntity[K] {
    val id: K
  }

  abstract class LongBaseEntity extends BaseEntity[Option[Long]]

  final case class User(name: String, id: Option[Long] = None) extends LongBaseEntity

  final case class Message(text: String, userId: Long = 0, id: Option[Long] = None) extends LongBaseEntity

  abstract class BaseTable[E <: LongBaseEntity](tag: Tag, tableName: String) extends Table[E](tag, tableName) {
    // primary key column:
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  }

  final class UserTable(tag: Tag) extends BaseTable[User](tag, "USERS") {
    // property columns:
    def name = column[String]("NAME")

    def * = (name, id.?) <> (User.tupled, User.unapply)
  }

  final class MessageTable(tag: Tag) extends BaseTable[Message](tag, "MESSAGES") {
    // property columns:
    def name = column[String]("NAME")

    // foreign columns:
    def userId = column[Long]("USER_ID")

    def * = (name, userId, id.?) <> (Message.tupled, Message.unapply)
  }

  abstract class BaseRepository[E <: LongBaseEntity, T <: BaseTable[E]](db: Database) {
    protected val entities: TableQuery[T]

    def findById(id: Long) = entities.filter(_.id === id).result

    def count() = db.run(entities.length.result)
  }

  class UserRepository(implicit db: Database) extends BaseRepository[User, UserTable](db) {
    protected lazy val entities = TableQuery[UserTable]
  }

  class MessageRepository(implicit db: Database) extends BaseRepository[Message, MessageTable](db) {
    protected lazy val entities = TableQuery[MessageTable]
  }

}