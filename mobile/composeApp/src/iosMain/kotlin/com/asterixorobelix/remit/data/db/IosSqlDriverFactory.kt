package com.asterixorobelix.remit.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

class IosSqlDriverFactory : SqlDriverFactory {
    override fun create(): SqlDriver = NativeSqliteDriver(AppDatabase.Schema, "app.db")
}
