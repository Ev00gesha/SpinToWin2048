package spintowin2048.game

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlin.random.Random
import spintowin2048.game.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private val size = 4
    private var turn = 30
    private var score = 0
    private var X = 0
    private var Y = 0
    private lateinit var currentRotation: CurrentRotation
    private lateinit var config: Config
    private var startRotation = StartRotation(null, null)

    private var board = Array(size) {IntArray(size)}

    private lateinit var manager: SensorManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var inflater: LayoutInflater
    private lateinit var cellAnimation: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        cellAnimation = AnimationUtils.loadAnimation(this, R.anim.cell_animation)
        config = Config()

        setContentView(binding.root)

        initTimer()
        addNumbers()
        showBoard()
        sensorListener()
    }

    private fun initTimer(){
        val timer = Timer("schedule", true);

        timer.schedule(1000, 700) {
            runOnUiThread { goToMove() }
        }
    }

    private fun goToMove(){
        when(max(abs(currentRotation.x), abs(max(abs(currentRotation.y), abs(currentRotation.z))))){
            abs(currentRotation.x) -> {
                if(currentRotation.x > turn){
                    moveCell("left")
                }
                if(currentRotation.x < turn * -1){
                    moveCell("right")
                }
            }
            abs(currentRotation.y) -> {
                if(currentRotation.y < turn * -1) {
                    moveCell("up")
                }
            }
            abs(currentRotation.z) -> {
                if(currentRotation.z < turn * -1) {
                    moveCell("down")
                }
            }
        }
    }

    private fun getRowWithoutZero(row: Int): ArrayList<Int>{
        var list = ArrayList<Int>()
        for(j in 0 until size){
            if(board[row][j] > 0) {
                list.add(board[row][j])
                board[row][j] = 0
            }
        }
        return list
    }

    private fun getColumnWithoutZero(column: Int): ArrayList<Int>{
        var list = ArrayList<Int>()
        for(j in 0 until size){
            if(board[j][column] > 0) {
                list.add(board[j][column])
                board[j][column] = 0
            }
        }
        return list
    }

    private fun mergeCell(cells: ArrayList<Int>): ArrayList<Int>{
        var j = 0
        while(j < cells.size - 1) {
            if (cells[j] == cells[j + 1]) {
                cells[j] *= 2
                score += cells[j]
                cells.removeAt(j + 1)
            }
            j++
        }
        return cells
    }

    private fun moveCell(direction: String){
        when(direction){
            "left" -> {
                for (i in 0 until size) {
                    var row = getRowWithoutZero(i)

                    row = mergeCell(row)

                    for(k in 0 until row.size) {
                        board[i][k] = row[k]
                    }
                }
            }
            "right" -> {
                for (i in 0 until size) {
                    var row = getRowWithoutZero(i)
                    row.reverse()

                    row = mergeCell(row)

                    for(k in 0 until row.size) {
                        board[i][size - k - 1] = row[k]
                    }
                }
            }
            "up" -> {
                for (i in 0 until size) {
                    var column = getColumnWithoutZero(i)

                    column = mergeCell(column)

                    for (k in 0 until column.size){
                        board[k][i] = column[k]
                    }
                }
            }
            "down" -> {
                for (i in 0 until size) {
                    var column = getColumnWithoutZero(i)
                    column.reverse()

                    column = mergeCell(column)

                    for (k in 0 until column.size){
                        board[size - k - 1][i] = column[k]
                    }
                }
            }
        }
        addNumbers()
        showBoard()
    }

    private fun addNumbers(){
        if(isEmptyBoard()) {
            var x = 0
            var y = 0

            do {
                val place = Random(System.nanoTime()).nextInt(size * size)
                x = place / 4
                y = place % 4
            } while (!isEmptyCell(x, y))
            X = x
            Y = y
            board[x][y] = arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 4).random()
        }else{
            showToast("Ходов больше нет")
        }
    }

    private fun isEmptyCell(x: Int, y: Int):Boolean{
        return board[x][y] == 0
    }

    private fun isEmptyBoard():Boolean{
        for (i in 0 until size)
            for(j in 0 until size)
                if(board[i][j] == 0)
                    return true
        return false
    }

    private fun sensorListener(){
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object: SensorEventListener{
            override fun onSensorChanged(event: SensorEvent?) {
                var x = ((event?.values!![0] * 90) / 9.81).toInt()
                var y = ((event?.values!![1] * 90) / 9.81).toInt()
                var z = ((event?.values!![2] * 90) / 9.81).toInt()

                if(startRotation.x == null && startRotation.y == null && startRotation.z == null){
                    startRotation.x = x
                    startRotation.y = y
                    startRotation.z = z
                }

                currentRotation = CurrentRotation(x - startRotation.x!!, y - startRotation.y!!, z - startRotation.z!!)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun showToast(msg: String){
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }

    data class StartRotation(var x: Int? = null, var y: Int? = null, var z: Int? = null)

    data class CurrentRotation(var x: Int, var y: Int, var z: Int)

    private fun showBoard(){
        binding.board.removeAllViews()
        binding.gameScore.text = "Score: $score"
        for(i in 0 until size){
            for(j in 0 until size){
                val view = inflater.inflate(R.layout.cell, null)

                val cell = view.findViewById<CardView>(R.id.cell)
                val points = view.findViewById<TextView>(R.id.points)

                if(i == X && j == Y)
                    cell.startAnimation(cellAnimation)

                if(board[i][j] == 0){
                    points.text = ""
                    cell.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
                }
                else{
                    if(board[i][j] > 4)
                        points.setTextColor(ContextCompat.getColor(this, R.color.white))
                    points.text = board[i][j].toString()
                    cell.setCardBackgroundColor(Color.parseColor(config.colors[board[i][j]]))
                }

                binding.board!!.addView(view, binding.board!!.childCount)
            }
        }
    }
}

