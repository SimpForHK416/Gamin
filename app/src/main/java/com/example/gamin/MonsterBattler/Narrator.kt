package com.example.gamin.MonsterBattler

object Narrator {

    val introDialogue = listOf(
        "Chào mừng đến với thế giới của Monster Battler!",
        "Đây là một vùng đất... nơi những sinh vật đáng kinh ngạc, được gọi là Quái Vật, tự do lang thang.",
        "Ta là Giáo sư, người nghiên cứu những sinh vật tuyệt vời này...",
        "Hành trình của ngươi sắp bắt đầu. Hãy chuẩn bị tinh thần cho một thử thách thực sự!"
    )

    // =============================================
    // THÊM MỚI: Lấy lời thoại khi bắt đầu trận đấu
    // =============================================
    fun getBattleStartDialogue(enemyName: String): String {
        return "Ta là $enemyName! Ngươi không có cửa thắng đâu!"
    }

    // Lấy lời thoại khi monster người chơi hết máu (sau này dùng)
    fun getDefeatDialogue(): String {
        return "Ôi không! Monster của bạn đã gục ngã..."
    }
}