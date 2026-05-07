package duckpond.buriedincode.bookcatalogue.cache

import java.nio.file.Path
import java.sql.DriverManager
import java.sql.Timestamp
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

data class SQLiteCache(private val path: Path, private val expiry: Duration? = null) {
  private val databaseUrl: String = "jdbc:sqlite:$path"

  init {
    this.createTable()
  }

  private fun createTable() {
    val query =
      """
      CREATE TABLE IF NOT EXISTS queries (
        url TEXT PRIMARY KEY,
        response TEXT NOT NULL,
        created_at DATETIME NOT NULL
      );
      """
        .trimIndent()
    DriverManager.getConnection(this.databaseUrl).use { it.createStatement().use { it.execute(query) } }
  }

  fun select(url: String): CacheData? {
    val query =
      if (this.expiry == null) {
        """
        SELECT url, response, created_at
        FROM queries
        WHERE url = ?;
        """
          .trimIndent()
      } else {
        """
        SELECT url, response, created_at
        FROM queries
        WHERE url = ? AND created_at > ?;
        """
          .trimIndent()
      }
    DriverManager.getConnection(this.databaseUrl).use {
      it.prepareStatement(query).use {
        it.setString(1, url)
        if (this.expiry != null) {
          it.setTimestamp(2, Timestamp.from((Clock.System.now() - this.expiry).toJavaInstant()))
        }
        it.executeQuery().use {
          if (!it.next()) {
            return null
          }
          return CacheData(
            url = it.getString("url"),
            response = it.getString("response"),
            createdAt = it.getTimestamp("created_at").toInstant().toKotlinInstant(),
          )
        }
      }
    }
  }

  @OptIn(ExperimentalTime::class)
  fun upsert(url: String, response: String, createdAt: Instant = Clock.System.now()) =
    upsert(data = CacheData(url = url, response = response, createdAt = createdAt))

  @OptIn(ExperimentalTime::class)
  fun upsert(data: CacheData) {
    val query =
      """
      INSERT INTO queries (url, response, created_at)
      VALUES (?, ?, ?)
      ON CONFLICT(url) DO UPDATE SET
        response = excluded.response,
        created_at = excluded.created_at;
      """
        .trimIndent()
    DriverManager.getConnection(this.databaseUrl).use {
      it.prepareStatement(query).use {
        it.setString(1, data.url)
        it.setString(2, data.response)
        it.setTimestamp(3, Timestamp.from(data.createdAt.toJavaInstant()))
        it.executeUpdate()
      }
    }
  }

  fun delete(url: String) {
    val query = "DELETE FROM queries WHERE url = ?;"
    DriverManager.getConnection(this.databaseUrl).use {
      it.prepareStatement(query).use {
        it.setString(1, url)
        it.executeUpdate()
      }
    }
  }
}
