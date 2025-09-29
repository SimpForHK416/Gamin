package com.example.gamin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gamin.ui.theme.GaminTheme

class GameIntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title") ?: ""
        val rules = intent.getStringExtra("rules") ?: ""
        val image = intent.getIntExtra("image", 0)
        val targetClass = intent.getSerializableExtra("targetClass") as? Class<*>

        Log.d("DEBUG", "Target class = $targetClass")
        Log.d("DEBUG", "Title = $title")

        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameIntroScreen(
                        title = title,
                        rules = rules,
                        imageRes = image,
                        targetClass = targetClass
                    )
                }
            }
        }
    }
}

@Composable
fun GameIntroScreen(
    title: String,
    rules: String,
    imageRes: Int,
    targetClass: Class<*>?
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageRes != 0) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = rules, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                Log.d("DEBUG", "PVP button clicked")
                if (targetClass != null) {
                    val intent = Intent(context, targetClass)
                    intent.putExtra("mode", "PVP")
                    context.startActivity(intent)
                    Log.d("DEBUG", "PVP Activity started successfully")
                } else {
                    Log.e("DEBUG", "Target class is null")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Chơi với người (PVP)")
        }

        Button(
            onClick = {
                Log.d("DEBUG", "PVE button clicked")
                if (targetClass != null) {
                    val intent = Intent(context, targetClass)
                    intent.putExtra("mode", "PVE")
                    context.startActivity(intent)
                    Log.d("DEBUG", "PVE Activity started successfully")
                } else {
                    Log.e("DEBUG", "Target class is null")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Chơi với máy (PVE)")
        }

        if (targetClass == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Lỗi: Không thể khởi động game",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}