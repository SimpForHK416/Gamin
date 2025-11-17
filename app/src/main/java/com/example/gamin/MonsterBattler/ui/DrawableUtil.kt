// Vị trí: com/example/gamin/MonsterBattler/ui/DrawableUtil.kt
package com.example.gamin.MonsterBattler.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.gamin.R

/**
 * Tự động tìm một painter từ thư mục 'drawable' bằng tên của nó.
 * Ví dụ: painterFor("oak") sẽ tìm file "oak.png".
 * Nếu không tìm thấy, nó sẽ trả về một icon placeholder.
 */
@Composable
fun painterFor(name: String): Painter {
    val context = LocalContext.current
    // Tự động tìm ID của resource bằng tên (viết thường)
    val resId = context.resources.getIdentifier(name.lowercase(), "drawable", context.packageName)

    return if (resId != 0) {
        // Nếu tìm thấy, tải ảnh
        painterResource(id = resId)
    } else {
        // Nếu không, dùng ảnh placeholder để không bị crash
        painterResource(id = R.drawable.ic_launcher_foreground)
    }
}