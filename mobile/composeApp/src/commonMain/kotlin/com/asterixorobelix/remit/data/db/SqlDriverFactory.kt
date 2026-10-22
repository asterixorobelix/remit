package com.asterixorobelix.remit.data.db

import app.cash.sqldelight.db.SqlDriver

interface SqlDriverFactory {
    fun create(): SqlDriver
}
