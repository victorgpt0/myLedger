package com.finance.myledger.data.local

import androidx.room.TypeConverter
import com.finance.myledger.domain.model.Category
import com.finance.myledger.domain.model.Direction
import com.finance.myledger.domain.model.TransactionType

/**
 * Room type converters for domain enums stored as strings.
 * Kept in the data layer so Room stays isolated from domain code that doesn't need converters.
 */
class Converters {
    @TypeConverter
    fun fromDirection(value: Direction): String = value.name

    @TypeConverter
    fun toDirection(value: String): Direction = Direction.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)
}
