package com.q.dartsync

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.launch

class PlayerDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_detail)

        val playerName = intent.getStringExtra("PLAYER_NAME") ?: "Oyuncu"

        // UI Bileşenleri
        val tvPlayerName = findViewById<TextView>(R.id.tvDetailPlayerName)
        val tvWins = findViewById<TextView>(R.id.tvDetailWins)
        val tvLosses = findViewById<TextView>(R.id.tvDetailLosses)
        val tvPPD = findViewById<TextView>(R.id.tvDetailPPD) // XML'de PPD için alanın olmalı
        val pieChart = findViewById<PieChart>(R.id.pieChart)

        tvPlayerName.text = "$playerName Karnesi"

        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@PlayerDetailActivity).gameResultDao()

            // 1. Temal Galibiyet/Mağlubiyet Verileri
            val wins = dao.getWinCount(playerName)
            val totalMatches = dao.getTotalMatchCount(playerName)
            val losses = totalMatches - wins

            // 2. PPD Hesaplama Mantığı
            val totalDarts = dao.getTotalDartsByPlayer(playerName)

            // Oyuncunun tüm maçlarda topladığı toplam puanı hesaplayalım
            // Formül: (Toplam Maç Sayısı * 501) - (Kalan Puanların Toplamı)
            val sumRemaining = dao.getSumOfRemainingP1(playerName) + dao.getSumOfRemainingP2(playerName)
            val totalPointsScored = (totalMatches * 501) - sumRemaining

            // Points Per Dart (PPD)
            val ppd = if (totalDarts > 0) totalPointsScored.toDouble() / totalDarts else 0.0

            // 3. UI Güncelleme
            tvWins.text = "Galibiyet: $wins"
            tvLosses.text = "Mağlubiyet: $losses"
            tvPPD?.text = "Ortalama PPD: ${String.format("%.2f", ppd)}"

            // 4. Grafiği Kur
            setupPieChart(pieChart, wins, losses, totalMatches)
        }
    }

    private fun setupPieChart(chart: PieChart, wins: Int, losses: Int, total: Int) {
        chart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)

            holeRadius = 58f
            transparentCircleRadius = 62f

            setDrawCenterText(true)
            val winRate = if (total > 0) (wins.toDouble() / total * 100).toInt() else 0
            centerText = "%$winRate\nKazanma"
            setCenterTextSize(18f)
            setCenterTextColor(Color.WHITE)

            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)
        }

        val entries = ArrayList<PieEntry>()
        if (wins > 0) entries.add(PieEntry(wins.toFloat(), "Galibiyet"))
        if (losses > 0) entries.add(PieEntry(losses.toFloat(), "Mağlubiyet"))

        val colors = ArrayList<Int>()
        if (wins > 0) colors.add(Color.parseColor("#00D26A")) // Neon Yeşil
        if (losses > 0) colors.add(Color.parseColor("#E94560")) // Neon Kırmızı

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 4f
            selectionShift = 7f
            setColors(colors)
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLineColor = Color.WHITE
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }

        chart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(chart))
            setValueTextSize(12f)
            setValueTextColor(Color.WHITE)
        }
        chart.invalidate()
    }
}