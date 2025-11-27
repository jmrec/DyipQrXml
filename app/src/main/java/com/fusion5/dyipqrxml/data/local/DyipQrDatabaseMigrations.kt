package com.fusion5.dyipqrxml.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        db.beginTransaction()
        try {
            db.execSQL(
                """
                CREATE TABLE Users_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    email TEXT NOT NULL,
                    password_hash TEXT NOT NULL,
                    CHECK (updated_at >= created_at),
                    UNIQUE (email)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO Users_new (id, created_at, updated_at, first_name, last_name, email, password_hash)
                SELECT id,
                       created_fixed,
                       CASE
                           WHEN updated_at >= created_fixed THEN updated_at
                           ELSE created_fixed
                       END,
                       COALESCE(first_name, ''),
                       COALESCE(last_name, ''),
                       COALESCE(email, ''),
                       COALESCE(password_hash, '')
                FROM (
                    SELECT *, COALESCE(created_at, datetime('now')) AS created_fixed FROM Users
                ) AS legacy_users
                """.trimIndent()
            )
            db.execSQL("DROP TABLE Users")
            db.execSQL("ALTER TABLE Users_new RENAME TO Users")
            db.execSQL("CREATE UNIQUE INDEX uk_email ON Users(email)")

            db.execSQL(
                """
                CREATE TABLE Terminals_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    CHECK (updated_at >= created_at),
                    CHECK (latitude >= -90 AND latitude <= 90),
                    CHECK (longitude >= -180 AND longitude <= 180)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO Terminals_new (id, created_at, updated_at, name, description, latitude, longitude)
                SELECT id,
                       created_fixed,
                       CASE
                           WHEN updated_at >= created_fixed THEN updated_at
                           ELSE created_fixed
                       END,
                       COALESCE(name, ''),
                       COALESCE(description, ''),
                       CASE
                           WHEN latitude IS NULL THEN 0
                           WHEN latitude < -90 THEN -90
                           WHEN latitude > 90 THEN 90
                           ELSE latitude
                       END,
                       CASE
                           WHEN longitude IS NULL THEN 0
                           WHEN longitude < -180 THEN -180
                           WHEN longitude > 180 THEN 180
                           ELSE longitude
                       END
                FROM (
                    SELECT *, COALESCE(created_at, datetime('now')) AS created_fixed FROM Terminals
                ) AS legacy_terminals
                """.trimIndent()
            )
            db.execSQL("DROP TABLE Terminals")
            db.execSQL("ALTER TABLE Terminals_new RENAME TO Terminals")
            db.execSQL("CREATE INDEX idx_name ON Terminals(name)")
            db.execSQL("CREATE UNIQUE INDEX uk_coordinates ON Terminals(latitude, longitude)")

            db.execSQL(
                """
                CREATE TABLE Routes_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    start_terminal_id INTEGER NOT NULL,
                    end_terminal_id INTEGER NOT NULL,
                    route_code TEXT NOT NULL,
                    fare REAL NOT NULL,
                    estimated_travel_time_in_seconds INTEGER,
                    frequency INTEGER NOT NULL DEFAULT 0,
                    route_geojson TEXT NOT NULL,
                    CHECK (updated_at >= created_at),
                    CHECK (fare >= 0),
                    CHECK (frequency >= 0),
                    CHECK (estimated_travel_time_in_seconds >= 0),
                    FOREIGN KEY (start_terminal_id) REFERENCES Terminals(id) ON DELETE RESTRICT ON UPDATE CASCADE,
                    FOREIGN KEY (end_terminal_id) REFERENCES Terminals(id) ON DELETE RESTRICT ON UPDATE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO Routes_new (id, created_at, updated_at, start_terminal_id, end_terminal_id, route_code, fare, estimated_travel_time_in_seconds, frequency, route_geojson)
                SELECT id,
                       created_fixed,
                       CASE
                           WHEN updated_at >= created_fixed THEN updated_at
                           ELSE created_fixed
                       END,
                       start_terminal_id,
                       end_terminal_id,
                       COALESCE(route_code, ''),
                       CASE
                           WHEN fare IS NULL OR fare < 0 THEN 0
                           ELSE fare
                       END,
                       CASE
                           WHEN estimated_travel_time_in_seconds IS NULL THEN NULL
                           WHEN estimated_travel_time_in_seconds < 0 THEN 0
                           ELSE estimated_travel_time_in_seconds
                       END,
                       CASE
                           WHEN frequency IS NULL OR frequency < 0 THEN 0
                           ELSE frequency
                       END,
                       COALESCE(route_geojson, '{}')
                FROM (
                    SELECT *, COALESCE(created_at, datetime('now')) AS created_fixed FROM Routes
                ) AS legacy_routes
                """.trimIndent()
            )
            db.execSQL("DROP TABLE Routes")
            db.execSQL("ALTER TABLE Routes_new RENAME TO Routes")
            db.execSQL("CREATE INDEX idx_start_terminal_id ON Routes(start_terminal_id)")
            db.execSQL("CREATE INDEX idx_end_terminal_id ON Routes(end_terminal_id)")
            db.execSQL("CREATE UNIQUE INDEX uk_terminal_routes ON Routes(start_terminal_id, end_terminal_id, route_code)")

            db.execSQL(
                """
                CREATE TABLE Favorites_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    user_id INTEGER NOT NULL,
                    route_id INTEGER NOT NULL,
                    CHECK (updated_at >= created_at),
                    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
                    FOREIGN KEY (route_id) REFERENCES Routes(id) ON DELETE CASCADE ON UPDATE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO Favorites_new (id, created_at, updated_at, user_id, route_id)
                SELECT id,
                       created_fixed,
                       CASE
                           WHEN updated_at >= created_fixed THEN updated_at
                           ELSE created_fixed
                       END,
                       user_id,
                       route_id
                FROM (
                    SELECT *, COALESCE(created_at, datetime('now')) AS created_fixed FROM Favorites
                ) AS legacy_favorites
                """.trimIndent()
            )
            db.execSQL("DROP TABLE Favorites")
            db.execSQL("ALTER TABLE Favorites_new RENAME TO Favorites")
            db.execSQL("CREATE INDEX idx_favorites_user_id ON Favorites(user_id)")
            db.execSQL("CREATE INDEX idx_favorites_route_id ON Favorites(route_id)")
            db.execSQL("CREATE UNIQUE INDEX uk_favorites_unique_route ON Favorites(user_id, route_id)")

            db.execSQL(
                """
                CREATE TABLE ScanHistories_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
                    user_id INTEGER NOT NULL,
                    content TEXT,
                    CHECK (updated_at >= created_at),
                    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO ScanHistories_new (id, created_at, updated_at, user_id, content)
                SELECT id,
                       created_fixed,
                       CASE
                           WHEN updated_at >= created_fixed THEN updated_at
                           ELSE created_fixed
                       END,
                       user_id,
                       content
                FROM (
                    SELECT *, COALESCE(created_at, datetime('now')) AS created_fixed FROM ScanHistories
                ) AS legacy_scan_histories
                """.trimIndent()
            )
            db.execSQL("DROP TABLE ScanHistories")
            db.execSQL("ALTER TABLE ScanHistories_new RENAME TO ScanHistories")
            db.execSQL("CREATE INDEX idx_scan_histories_user_id ON ScanHistories(user_id)")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }
}

