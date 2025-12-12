package com.example.gamin.MonsterBattler.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

class MonsterDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 15
        private const val DATABASE_NAME = "MonsterBattler.db"

        private const val TABLE_MONSTERS = "monsters"
        private const val KEY_NAME = "name"

        private const val TABLE_SKILLS = "skills"
        private const val KEY_OWNER_NAME = "owner_name"
        private const val KEY_SKILL_NAME = "skill_name"
        private const val KEY_SKILL_TYPE = "skill_type"
        private const val KEY_POWER = "power"
        private const val KEY_PP = "pp"
        private const val KEY_SKILL_DESC = "skill_desc"

        private const val TABLE_BUFFS = "buffs"
        private const val KEY_BUFF_NAME = "buff_name"
        private const val KEY_BUFF_DESC = "buff_desc"
        private const val KEY_BUFF_TARGET_TYPE = "target_type"
        private const val KEY_BUFF_EFFECT = "effect_type"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_MONSTERS ($KEY_NAME TEXT PRIMARY KEY, hp INTEGER, atk INTEGER, def INTEGER, speed INTEGER, type TEXT, ability TEXT, description TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_SKILLS (id INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_OWNER_NAME TEXT, $KEY_SKILL_NAME TEXT, $KEY_SKILL_TYPE TEXT, $KEY_POWER INTEGER, $KEY_PP INTEGER, $KEY_SKILL_DESC TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_BUFFS (id INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_BUFF_NAME TEXT, $KEY_BUFF_DESC TEXT, $KEY_BUFF_TARGET_TYPE TEXT, $KEY_BUFF_EFFECT TEXT)")

        populateDatabase(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_BUFFS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SKILLS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MONSTERS")
        onCreate(db)
    }

    private fun populateDatabase(db: SQLiteDatabase?) {
        db?.beginTransaction()
        try {
            val allMonsters = listOf(
                Monster("CRISHY", 60, 30, 40, 65, "Fire", "Bùng Nổ", "Thằn lằn lửa."),
                Monster("RHINPLINK", 75, 25, 65, 35, "Leaf", "Um Tùm", "Tê giác cỏ."),
                Monster("DOREWEE", 65, 28, 50, 50, "Water", "Suối Nguồn", "Tinh linh nước."),

                Monster("CONFLEVOUR", 80, 50, 55, 75, "Fire", "Bùng Nổ", "Evo Crishy."),
                Monster("RHITAIN", 100, 40, 90, 45, "Leaf", "Um Tùm", "Evo Rhinplink."),
                Monster("DOPERAMI", 90, 45, 65, 60, "Water", "Suối Nguồn", "Evo Dorewee."),

                Monster("FLORAMONA_1", 35, 20, 20, 60, "Leaf", "Phấn Hoa", "Hoa nhỏ."),
                Monster("FLORAMONA_2", 55, 28, 30, 70, "Leaf", "Gai Nhọn", "Hoa gai."),
                Monster("FLORAMONA_3", 80, 32, 45, 80, "Leaf", "Nữ Hoàng", "Hoa chúa."),

                Monster("OCTOKINETUS", 60, 30, 35, 60, "Water", "Xúc Tu", "Bạch tuộc."),
                Monster("STORMANTA", 65, 28, 30, 85, "Water", "Lướt Sóng", "Cá đuối."),
                Monster("KOCOMB", 70, 22, 55, 30, "Water", "Gai Cứng", "Gai góc."),
                Monster("MUNCHILL", 55, 30, 40, 40, "Water", "Điềm Tĩnh", "Băng giá."),

                Monster("ORICORIO", 50, 28, 20, 85, "Fire", "Vũ Điệu", "Chim lửa."),
                Monster("GREXCLUB", 60, 30, 40, 55, "Fire", "Nhiệt Huyết", "Khủng long."),
                Monster("CHUB", 85, 22, 30, 30, "Fire", "Mỡ Dày", "Mập mạp.")
            )

            allMonsters.forEach { m ->
                val values = ContentValues().apply {
                    put(KEY_NAME, m.name)
                    put("hp", m.hp)
                    put("atk", m.atk)
                    put("def", m.def)
                    put("speed", m.speed)
                    put("type", m.type)
                    put("ability", m.ability)
                    put("description", m.description)
                }
                db?.insert(TABLE_MONSTERS, null, values)
            }

            insertSkillsFor(db, "CRISHY", listOf(
                Skill("Nóng Giận", "Fire", 0, 10, 10, "Tăng Tấn Công 3 lượt."),
                Skill("Cào", "Normal", 12, 15, 15, "Cào mạnh."),
                Skill("Đốm Lửa", "Fire", 15, 10, 10, "Bắn lửa."),
                Skill("Ném Đá", "Leaf", 14, 10, 10, "Ném đá.")
            ))

            insertSkillsFor(db, "RHINPLINK", listOf(
                Skill("Quang Hợp", "Leaf", 0, 5, 5, "Hồi 30 HP."),
                Skill("Húc", "Normal", 13, 15, 15, "Húc đầu."),
                Skill("Lá Bay", "Leaf", 14, 10, 10, "Phóng lá."),
                Skill("Bùn Lầy", "Water", 12, 10, 10, "Ném bùn.")
            ))

            insertSkillsFor(db, "DOREWEE", listOf(
                Skill("Vỏ Cứng", "Water", 0, 10, 10, "Tăng Thủ 3 lượt."),
                Skill("Đập", "Normal", 12, 15, 15, "Đuôi đập."),
                Skill("Bong Bóng", "Water", 14, 10, 10, "Bắn bong bóng."),
                Skill("Hơi Nóng", "Fire", 13, 10, 10, "Thổi hơi nóng.")
            ))

            val fireSkills = listOf(Skill("Cào","Normal",10,10,10,""), Skill("Lửa","Fire",15,10,10,""), Skill("Buff Công","Fire",0,10,10,""), Skill("Đá","Leaf",12,10,10,""))
            listOf("CONFLEVOUR", "ORICORIO", "GREXCLUB", "CHUB").forEach { insertSkillsFor(db, it, fireSkills) }

            val leafSkills = listOf(Skill("Húc","Normal",10,10,10,""), Skill("Lá","Leaf",15,10,10,""), Skill("Hồi Máu","Leaf",0,10,10,""), Skill("Bùn","Water",12,10,10,""))
            listOf("RHITAIN", "FLORAMONA_1", "FLORAMONA_2", "FLORAMONA_3").forEach { insertSkillsFor(db, it, leafSkills) }

            val waterSkills = listOf(Skill("Đập","Normal",10,10,10,""), Skill("Nước","Water",15,10,10,""), Skill("Giáp","Water",0,10,10,""), Skill("Hơi","Fire",12,10,10,""))
            listOf("DOPERAMI", "OCTOKINETUS", "KOCOMB", "STORMANTA", "MUNCHILL").forEach { insertSkillsFor(db, it, waterSkills) }

            val buffs = listOf(
                Buff("Búa Choáng", "10% cơ hội gây CHOÁNG khi dùng kỹ năng bất kỳ.", "Any", "STUN"),
                Buff("Tinh Thể Lửa", "10% cơ hội gây BỎNG (Mất 10 HP/lượt) khi dùng chiêu Lửa.", "Fire", "BURN"),
                Buff("Nước Axit", "10% cơ hội phá giáp (Giảm 1/2 Thủ) khi dùng chiêu Nước.", "Water", "BREAK_DEF"),
                Buff("Dây Leo Gai", "10% cơ hội làm yếu (Giảm 1/2 Công) khi dùng chiêu Lá.", "Leaf", "WEAKEN")
            )
            buffs.forEach { buff ->
                val values = ContentValues().apply {
                    put(KEY_BUFF_NAME, buff.name)
                    put(KEY_BUFF_DESC, buff.description)
                    put(KEY_BUFF_TARGET_TYPE, buff.targetType)
                    put(KEY_BUFF_EFFECT, buff.effectType)
                }
                db?.insert(TABLE_BUFFS, null, values)
            }

            db?.setTransactionSuccessful()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.endTransaction()
        }
    }

    private fun insertSkillsFor(db: SQLiteDatabase?, ownerName: String, skills: List<Skill>) {
        skills.forEach { s ->
            val values = ContentValues().apply {
                put(KEY_OWNER_NAME, ownerName)
                put(KEY_SKILL_NAME, s.name)
                put(KEY_SKILL_TYPE, s.type)
                put(KEY_POWER, s.power)
                put(KEY_PP, s.maxPp)
                put(KEY_SKILL_DESC, s.description)
            }
            db?.insert(TABLE_SKILLS, null, values)
        }
    }

    fun getRandomOneBuff(): Buff? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_BUFFS ORDER BY RANDOM() LIMIT 1", null)
        var buff: Buff? = null
        if (cursor.moveToFirst()) {
            buff = Buff(
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUFF_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUFF_DESC)),
                targetType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUFF_TARGET_TYPE)),
                effectType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUFF_EFFECT))
            )
        }
        cursor.close()
        return buff
    }

    fun getStartingMonsters(): List<Monster> {
        val list = ArrayList<Monster>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MONSTERS WHERE $KEY_NAME IN ('CRISHY','RHINPLINK','DOREWEE')", null)
        if (cursor.moveToFirst()) { do { list.add(cursorToMonster(cursor)) } while (cursor.moveToNext()) }
        cursor.close()
        return list
    }

    fun getAllMonsters(): List<Monster> {
        val list = ArrayList<Monster>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MONSTERS", null)
        if (cursor.moveToFirst()) { do { list.add(cursorToMonster(cursor)) } while (cursor.moveToNext()) }
        cursor.close()
        return list
    }

    fun getMonsterByName(name: String): Monster? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MONSTERS WHERE $KEY_NAME = ?", arrayOf(name))
        var monster: Monster? = null
        if (cursor.moveToFirst()) { monster = cursorToMonster(cursor) }
        cursor.close()
        return monster
    }

    fun getSkillsForMonster(monsterName: String): List<Skill> {
        val list = ArrayList<Skill>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SKILLS WHERE $KEY_OWNER_NAME = ?", arrayOf(monsterName))
        if (cursor.moveToFirst()) {
            do {
                val skill = Skill(
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SKILL_NAME)),
                    type = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SKILL_TYPE)),
                    power = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_POWER)),
                    maxPp = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PP)),
                    currentPp = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PP)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SKILL_DESC))
                )
                list.add(skill)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    private fun cursorToMonster(cursor: Cursor): Monster {
        return Monster(
            name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
            hp = cursor.getInt(cursor.getColumnIndexOrThrow("hp")),
            atk = cursor.getInt(cursor.getColumnIndexOrThrow("atk")),
            def = cursor.getInt(cursor.getColumnIndexOrThrow("def")),
            speed = cursor.getInt(cursor.getColumnIndexOrThrow("speed")),
            type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
            ability = cursor.getString(cursor.getColumnIndexOrThrow("ability")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
        )
    }
}