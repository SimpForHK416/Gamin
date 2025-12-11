package com.example.gamin.MonsterBattler.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

// Model dữ liệu điểm số
data class PlayerScore(
    val name: String = "",
    val score: Int = 0,
    val timestamp: Date? = null
)

object LeaderboardManager {
    private val db = FirebaseFirestore.getInstance()

    // Lưu điểm lên Firebase
    fun saveScore(
        gameId: String,
        name: String,
        score: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val scoreData = hashMapOf(
            "name" to name,
            "score" to score,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("leaderboards")
            .document(gameId)
            .collection("scores")
            .add(scoreData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Log.e("Leaderboard", "Error saving score", e)
                onFailure()
            }
    }

    // Tải bảng xếp hạng (Top 10)
    fun getTopScores(
        gameId: String,
        onResult: (List<PlayerScore>) -> Unit
    ) {
        db.collection("leaderboards")
            .document(gameId)
            .collection("scores")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { it.toObject(PlayerScore::class.java) }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}