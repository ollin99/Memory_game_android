package com.example.memorygame

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class GameActivity : AppCompatActivity() {
    private lateinit var gridLayout: GridLayout
    private var rowCount: Int = 0
    private var columnCount: Int = 0
    // Lista med kort i spelet
    private var cards: List<Card> = emptyList()
    // Temporär lista med de kort som klickas på
    private var selectedCards: MutableList<Pair<Card, CardView>> = mutableListOf()

    // Timer variabler
    private var timerHandler: Handler = Handler(Looper.getMainLooper())
    private var timerSeconds: Int = 0
    private lateinit var timerRunnable: Runnable

    private val handler = Handler(Looper.getMainLooper())
    private var flipCheckRunnable: Runnable? = null

    private var gameWon = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        rowCount = intent.getIntExtra("ROW_COUNT", 0)
        columnCount = intent.getIntExtra("COLUMN_COUNT", 0)
        val backButton = findViewById<Button>(R.id.backButton)
        val restartButton = findViewById<Button>(R.id.restartButton)
        backButton.setOnClickListener {
            finish()
        }
        restartButton.setOnClickListener {
            restartGame()
        }
        gridLayout = findViewById(R.id.gridLayout)
        gridLayout.rowCount = rowCount
        gridLayout.columnCount = columnCount
        setupBoard(rowCount, columnCount)
        startTimer()
    }

    //Starta en timer funktion
    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                timerSeconds++
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    //Stoppa en timer funktion
    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    //Denna funktion anropas när användaren vill starta om en nivå eller spela igen
    private fun restartGame() {
        stopTimer()
        stopFlipCheck()
        gameWon = false
        timerSeconds = 0

        selectedCards.clear()
        cards.forEach {
            it.isFaceUp = false
            it.isMatched = false
        }

        gridLayout.removeAllViews()

        cards = generateCards((rowCount * columnCount) / 2)
        setupBoard(rowCount, columnCount)
        startTimer()
        startFlipCheck()
    }

    //Den här funktionen uppdaterar användarens highscore om den nuvarande tiden är lägre än föregående highscore
    private fun saveHighscoreIfBetter(newScore: Int) {
        val prefs = getSharedPreferences("HIGHSCORES", Context.MODE_PRIVATE)
        val levelKey = "${rowCount}x$columnCount"
        val currentHighscore = prefs.getInt(levelKey, Int.MAX_VALUE)
        if (newScore < currentHighscore) {
            prefs.edit().putInt(levelKey, newScore).apply()
        }
    }
    //Den här funktionen är till för att hämta highscore (Anropas när användaen klarar en nivå)
    private fun getHighscore(): Int {
        val prefs = getSharedPreferences("HIGHSCORES", Context.MODE_PRIVATE)
        val levelKey = "${rowCount}x$columnCount"
        return prefs.getInt(levelKey, Int.MAX_VALUE)
    }

    //Den här funktionen skapar själva spelplanen
    private fun setupBoard(rows: Int, columns: Int) {
        val totalCards = rows * columns
        cards = generateCards(totalCards / 2)
        for (card in cards) {
            val cardView = createCardView(card)
            cardView.setOnClickListener {
                handleCardClick(card, cardView)
            }
            gridLayout.addView(cardView)
        }
    }

    //Det här är funktionen som skapar korten i listan och parar ihop de
    private fun generateCards(pairs: Int): List<Card> {
        val cardList = mutableListOf<Card>()
        for (i in 1..pairs) {
            val imageResId = resources.getIdentifier("pixel_icon_$i", "drawable", packageName)
            cardList.add(Card(i, imageResId))
            cardList.add(Card(i, imageResId))
        }
        cardList.shuffle()
        return cardList
    }

    //Den här funktionen används för att skapa korten visuellt
    private fun createCardView(card: Card): View {
        val cardView = CardView(this)
        val cardLayoutParams = GridLayout.LayoutParams().apply {
            width = resources.getDimensionPixelSize(R.dimen.card_fixed_width)
            height = resources.getDimensionPixelSize(R.dimen.card_fixed_height)
            setMargins(8, 8, 8, 8)
        }
        cardView.layoutParams = cardLayoutParams
        val frameLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val imageView = ImageView(this).apply {
            setImageResource(card.imageResId)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.INVISIBLE
        }

        frameLayout.addView(imageView)
        cardView.addView(frameLayout)

        cardView.tag = imageView

        cardView.setBackgroundResource(R.drawable.cardback_cut)

        return cardView
    }

    //Den här loopen är igång hela tiden för att kolla om anvöndaren har vänt upp två kort
    private fun startFlipCheck() {
        if (flipCheckRunnable == null) {
            flipCheckRunnable = Runnable {
                if (selectedCards.size == 2) {
                    checkForMatch()
                }
                flipCheckRunnable?.let {
                    handler.postDelayed(it, 1500)
                }
            }
            handler.post(flipCheckRunnable!!)
        }
    }

    //Denna finktion är till för att stoppa tidigare loop, exempelvis om användaren har klarat nivån
    private fun stopFlipCheck() {
        flipCheckRunnable?.let {
            handler.removeCallbacks(it)
            flipCheckRunnable = null
        }
    }

    override fun onResume() {
        super.onResume()
        startFlipCheck()
    }
    override fun onPause() {
        super.onPause()
        stopFlipCheck()
    }

    //Det här är funktionen som gör att användaren kan klicka på korten och vända på de
    private fun handleCardClick(card: Card, cardView: View) {
        if (card.isFaceUp || card.isMatched) return

        if (selectedCards.size == 2) {
            flipBackCards()
        }
        card.isFaceUp = true
        cardView.setBackgroundResource(R.color.teal_200)
        val imageView = cardView.tag as ImageView
        imageView.visibility = View.VISIBLE
        selectedCards.add(Pair(card, cardView) as Pair<Card, CardView>)
    }

    //Den här funktionen kollar om de två uppåtvända korten matchar eller ej
    private fun checkForMatch() {
        if (selectedCards.size < 2) return
        val (card1, cardView1) = selectedCards[0]
        val (card2, cardView2) = selectedCards[1]
        if (card1.id == card2.id) {
            card1.isMatched = true
            card2.isMatched = true
            cardView1.visibility = View.INVISIBLE
            cardView2.visibility = View.INVISIBLE
            selectedCards.clear()
            if (isGameWon() && !gameWon) {
                gameWon = true
                stopFlipCheck()
                stopTimer()
                showGameWonDialog()
            }
        } else {
            flipBackCards()
        }
    }

    //Om korten inte matchar så anropas denna funktion, för att vända tillbaka de
    private fun flipBackCards() {
        if (selectedCards.size == 2) {
            val (card1, cardView1) = selectedCards[0]
            val (card2, cardView2) = selectedCards[1]
            card1.isFaceUp = false
            card2.isFaceUp = false
            cardView1.setBackgroundResource(R.drawable.cardback_cut)
            cardView2.setBackgroundResource(R.drawable.cardback_cut)
            (cardView1.tag as ImageView).visibility = View.INVISIBLE
            (cardView2.tag as ImageView).visibility = View.INVISIBLE
            selectedCards.clear()
        }
    }

    //Det här är funktionen för alertrutan som kommer upp i slutet av en nivå
    private fun showGameWonDialog() {
        stopTimer()
        saveHighscoreIfBetter(timerSeconds)
        val highscore = getHighscore()

        AlertDialog.Builder(this)
            .setTitle("Grattis!")
            .setMessage("Du klarade nivån! Det tog $timerSeconds sekunder!\nBästa tid: $highscore sekunder.")
            .setPositiveButton("Tillbaka till Menyn") { _, _ ->
                finish()
            }
            .setNegativeButton("Spela igen") { _, _ ->
                restartGame()
            }
            .setCancelable(false)
            .show()
    }

    //Det här är till för att visa när användaren har klarat en nivå
    private fun isGameWon(): Boolean {
        return cards.all { it.isMatched }
    }
}