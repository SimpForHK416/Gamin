package com.example.gamin.MonsterBattler.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.gamin.R

@Composable
fun painterFor(name: String): Painter {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(name.lowercase(), "drawable", context.packageName)

    return if (resId != 0) {
        painterResource(id = resId)
    } else {
        painterResource(id = R.drawable.ic_launcher_foreground)
    }
}