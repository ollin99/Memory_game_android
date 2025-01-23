package com.example.memorygame

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showHighscores()
        findViewById<Button>(R.id.btn_easy).setOnClickListener {
            startGameActivity(2,4)
        }
        findViewById<Button>(R.id.btn_medium).setOnClickListener {
            startGameActivity(3,4)
        }
        findViewById<Button>(R.id.btn_hard).setOnClickListener {
            startGameActivity(4,4)
        }
    }

    //Den här funktionen används för att skicka vald nivå till GameActivity
    private fun startGameActivity(rowCount: Int, columnCount: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("ROW_COUNT", rowCount)
        intent.putExtra("COLUMN_COUNT", columnCount)
        startActivity(intent)
    }

    //Den här funktionen är till för att visa användarens highscore i menyn
    private fun showHighscores() {
        val prefs = getSharedPreferences("HIGHSCORES", Context.MODE_PRIVATE)
        val easyHighscore = prefs.getInt("2x4", Int.MAX_VALUE)
        val mediumHighscore = prefs.getInt("3x4", Int.MAX_VALUE)
        val hardHighscore = prefs.getInt("4x4", Int.MAX_VALUE)

        findViewById<TextView>(R.id.highscore_easy).text =
            "Lätt nivå: ${if (easyHighscore == Int.MAX_VALUE) "-" else "$easyHighscore sek"}"
        findViewById<TextView>(R.id.highscore_medium).text =
            "Mellan nivå: ${if (mediumHighscore == Int.MAX_VALUE) "-" else "$mediumHighscore sek"}"
        findViewById<TextView>(R.id.highscore_hard).text =
            "Svår nivå: ${if (hardHighscore == Int.MAX_VALUE) "-" else "$hardHighscore sek"}"
    }

    override fun onResume() {
        super.onResume()
        showHighscores()
    }

}