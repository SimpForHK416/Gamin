// Vị trí: com/example/gamin/MonsterBattler/data/MonsterDbHelper.kt
package com.example.gamin.MonsterBattler.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

class MonsterDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // ... (companion object giữ nguyên)
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MonsterBattler.db"

        private const val TABLE_MONSTERS = "monsters"
        private const val KEY_NAME = "name"
        private const val KEY_HP = "hp"
        private const val KEY_ATK = "atk"
        private const val KEY_DEF = "def"
        private const val KEY_SPEED = "speed"
        private const val KEY_TYPE = "type"
        private const val KEY_ABILITY = "ability"
        private const val KEY_DESCRIPTION = "description"
    }

    // ... (onCreate giữ nguyên)
    override fun onCreate(db: SQLiteDatabase?) {
        val createTableSql = """
            CREATE TABLE $TABLE_MONSTERS (
                $KEY_NAME TEXT PRIMARY KEY,
                $KEY_HP INTEGER,
                $KEY_ATK INTEGER,
                $KEY_DEF INTEGER,
                $KEY_SPEED INTEGER,
                $KEY_TYPE TEXT,
                $KEY_ABILITY TEXT,
                $KEY_DESCRIPTION TEXT
            )
        """.trimIndent()

        db?.execSQL(createTableSql)
        populateDatabase(db)
    }

    // ... (onUpgrade giữ nguyên)
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MONSTERS")
        onCreate(db)
    }

    // ... (populateDatabase giữ nguyên)
    private fun populateDatabase(db: SQLiteDatabase?) {
        // (Code chèn 3 quái vật...)
        val starters = listOf(
            Monster(
                name = "CRISHY", hp = 45, atk = 50, def = 40, speed = 65, type = "Fire",
                ability = "Bùng Nổ",
                description = "Một con thằn lằn lửa nhỏ. Ngọn lửa trên đuôi nó cháy bùng lên khi nó phấn khích hoặc sẵn sàng chiến đấu."
            ),
            Monster(
                name = "RHINPLINK", hp = 60, atk = 40, def = 65, speed = 35, type = "Leaf",
                ability = "Um Tùm",
                description = "Một sinh vật hiền lành, giống tê giác. Chiếc lá lớn trên lưng nó hấp thụ ánh sáng mặt trời để tạo ra năng lượng."
            ),
            Monster(
                name = "DOREWEE", hp = 55, atk = 45, def = 50, speed = 50, type = "Water",
                ability = "Suối Nguồn",
                description = "Một tinh linh nước hay ngại ngùng. Nó có thể phun ra các bong bóng nước có áp suất cao khi bị đe dọa."
            )
        )
        starters.forEach { monster ->
            val values = ContentValues().apply {
                put(KEY_NAME, monster.name)
                put(KEY_HP, monster.hp)
                put(KEY_ATK, monster.atk)
                put(KEY_DEF, monster.def)
                put(KEY_SPEED, monster.speed)
                put(KEY_TYPE, monster.type)
                put(KEY_ABILITY, monster.ability)
                put(KEY_DESCRIPTION, monster.description)
            }
            db?.insert(TABLE_MONSTERS, null, values)
        }
    }


    /**
     * Hàm để đọc 3 quái vật khởi đầu từ DB
     */
    fun getStartingMonsters(): List<Monster> {
        val monsterList = ArrayList<Monster>()
        val db = this.readableDatabase

        val query = "SELECT * FROM $TABLE_MONSTERS WHERE $KEY_NAME IN (?, ?, ?)"
        val cursor: Cursor? = db.rawQuery(query, arrayOf("CRISHY", "RHINPLINK", "DOREWEE"))

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val nameIndex = cursor.getColumnIndex(KEY_NAME)
                val hpIndex = cursor.getColumnIndex(KEY_HP)
                val atkIndex = cursor.getColumnIndex(KEY_ATK)
                val defIndex = cursor.getColumnIndex(KEY_DEF)
                val speedIndex = cursor.getColumnIndex(KEY_SPEED)
                val typeIndex = cursor.getColumnIndex(KEY_TYPE)
                val abilityIndex = cursor.getColumnIndex(KEY_ABILITY)
                val descIndex = cursor.getColumnIndex(KEY_DESCRIPTION)

                val monster = Monster(
                    name = cursor.getString(nameIndex),
                    hp = cursor.getInt(hpIndex),
                    atk = cursor.getInt(atkIndex),
                    def = cursor.getInt(defIndex),
                    speed = cursor.getInt(speedIndex),
                    type = cursor.getString(typeIndex),
                    ability = cursor.getString(abilityIndex),
                    description = cursor.getString(descIndex)
                )
                monsterList.add(monster)
            } while (cursor.moveToNext())
        }

        cursor?.close()

        // =============================================
        // XÓA DÒNG NÀY ĐI
        // db.close()
        // =============================================

        return monsterList
    }
}