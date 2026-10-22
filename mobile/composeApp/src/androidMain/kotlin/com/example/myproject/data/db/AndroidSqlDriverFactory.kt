package com.example.myproject.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidSqlDriverFactory(private val context: Context) : SqlDriverFactory {
    override fun create(): SqlDriver = AndroidSqliteDriver(AppDatabase.Schema, context, "app.db")
}
