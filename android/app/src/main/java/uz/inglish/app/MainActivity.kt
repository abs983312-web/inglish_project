package uz.inglish.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val battleCode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        battleCode.value = if (savedInstanceState != null) {
            savedInstanceState.getString(BATTLE_CODE_KEY)
        } else {
            battleCodeFrom(intent)
        }
        val container = (application as InglishApp).container
        setContent {
            InglishRoot(
                container = container,
                initialBattleCode = battleCode.value,
                onBattleCodeConsumed = { battleCode.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        battleCodeFrom(intent)?.let { battleCode.value = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BATTLE_CODE_KEY, battleCode.value)
    }

    private fun battleCodeFrom(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "inglish" || data.host != "battle") return null
        return data.lastPathSegment
            ?.filter(Char::isDigit)
            ?.take(6)
            ?.takeIf { it.length == 6 }
    }

    private companion object {
        const val BATTLE_CODE_KEY = "battleCode"
    }
}
