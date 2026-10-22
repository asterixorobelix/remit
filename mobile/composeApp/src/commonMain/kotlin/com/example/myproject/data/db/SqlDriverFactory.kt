package com.example.myproject.data.db

import app.cash.sqldelight.db.SqlDriver

interface SqlDriverFactory {
    fun create(): SqlDriver
}
