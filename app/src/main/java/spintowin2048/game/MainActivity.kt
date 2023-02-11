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
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private val size = 4
    private var turn = 30
    private var score = 0

    private var startRotation = Rotation()
    private var board = Array(size) { ArrayList<CellState>(size)}

    private lateinit var currentRotation: Rotation
    private lateinit var config: Config
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

        initBoard()
        initTimer()
        addNumbers()
        showBoard()
        sensorListener()
    }

    private fun initBoard(){
        for (i in 0 until size) {
            for(j in 0 until size){
                board[i].add(CellState())
            }
        }
    }

    private fun initTimer(){
        val timer = Timer("schedule", true)

        timer.schedule(1000, 700) {
            runOnUiThread { goToMove() }
        }
    }

    private fun sensorListener(){
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object: SensorEventListener{
            override fun onSensorChanged(event: SensorEvent?) {
                val x = ((event?.values!![0] * 90) / 9.81).toInt()
                val y = ((event.values!![1] * 90) / 9.81).toInt()
                val z = ((event.values!![2] * 90) / 9.81).toInt()

                if(startRotation.x == null && startRotation.y == null && startRotation.z == null){
                    startRotation.x = x
                    startRotation.y = y
                    startRotation.z = z
                }

                currentRotation = Rotation(x - startRotation.x!!, y - startRotation.y!!, z - startRotation.z!!)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun goToMove(){
        when(max(abs(currentRotation.x!!), abs(max(abs(currentRotation.y!!), abs(currentRotation.z!!))))){
            abs(currentRotation.x!!) -> {
                if(currentRotation.x!! > turn){
                    moveCell("left")
                }
                if(currentRotation.x!! < turn * -1){
                    moveCell("right")
                }
            }
            abs(currentRotation.y!!) -> {
                if(currentRotation.y!! < turn * -1) {
                    moveCell("up")
                }
            }
            abs(currentRotation.z!!) -> {
                if(currentRotation.z!! < turn * -1) {
                    moveCell("down")
                }
            }
        }
    }

    private fun copyMatrix(before: Array<ArrayList<CellState>>){
        for (i in 0 until size) {
            for (j in 0 until size) {
                before[i].add(board[i][j])
            }
        }
    }

    private fun moveCell(direction: String){
        val beforeMerge = Array(size) { ArrayList<CellState>(size)}
        copyMatrix(beforeMerge)

        for(i in 0 until size){
            if(direction == "left" || direction == "right"){
                val rowWithout = getRowWithoutZero(i)
                if(direction == "right")
                    rowWithout.reverse()

                val rowMerge = mergeCell(rowWithout)
                when(direction){
                    "left" -> replaceRow(i, 0, rowMerge)
                    "right" -> replaceRow(i, size - 1, rowMerge)
                }
            }

            if(direction == "up" || direction == "down"){
                val columnWithout = getColumnWithoutZero(i)
                if(direction == "down")
                    columnWithout.reverse()

                val columnMerge = mergeCell(columnWithout)
                when(direction){
                    "up" -> replaceColumn(i, 0, columnMerge)
                    "down" -> replaceColumn(i, size - 1, columnMerge)
                }
            }
        }
        if(!beforeMerge.contentDeepEquals(board))
            addNumbers()
        showBoard()
    }

    private fun getRowWithoutZero(row: Int): ArrayList<CellState>{
        val list = ArrayList<CellState>()
        for(j in 0 until size){
            if(board[row][j].point > 0) {
                list.add(board[row][j])
                board[row][j] = CellState()
            }
        }
        return list
    }

    private fun getColumnWithoutZero(column: Int): ArrayList<CellState>{
        val list = ArrayList<CellState>()
        for(j in 0 until size){
            if(board[j][column].point > 0) {
                list.add(board[j][column])
                board[j][column] = CellState()
            }
        }
        return list
    }

    private fun mergeCell(cells: ArrayList<CellState>): ArrayList<CellState>{
        var j = 0
        while(j < cells.size - 1) {
            if (cells[j].point == cells[j + 1].point) {
                cells[j].point *= 2
                cells[j].merge = true
                score += cells[j].point
                cells.removeAt(j + 1)
            }
            j++
        }
        return cells
    }

    private fun replaceRow(i: Int, n: Int, row: ArrayList<CellState>){
        for(k in 0 until row.size){
            board[i][abs(n - k)] = row[k]
        }
    }

    private fun replaceColumn(i: Int, n: Int, column: ArrayList<CellState>){
        for(k in 0 until column.size){
            board[abs(n - k)][i] = column[k]
        }
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
            board[x][y].point = arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 4).random()
            board[x][y].merge = true
        }else{
            showToast("Ходов больше нет")
        }
    }

    private fun isEmptyCell(x: Int, y: Int):Boolean{
        return board[x][y].point == 0
    }

    private fun isEmptyBoard():Boolean{
        for (i in 0 until size)
            for(j in 0 until size)
                if(board[i][j].point == 0)
                    return true
        return false
    }

    private fun showToast(msg: String){
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showBoard(){
        binding.board.removeAllViews()
        binding.gameScore.text = "Score: $score"
        for(i in 0 until size){
            for(j in 0 until size){
                val view = inflater.inflate(R.layout.cell, null)

                val cell = view.findViewById<CardView>(R.id.cell)
                val points = view.findViewById<TextView>(R.id.points)

                if(board[i][j].merge)
                    cell.startAnimation(cellAnimation)
                board[i][j].merge=false
                if(board[i][j].point == 0){
                    points.text = ""
                    cell.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
                }
                else{
                    if(board[i][j].point > 4)
                        points.setTextColor(ContextCompat.getColor(this, R.color.white))
                    points.text = board[i][j].point.toString()
                    cell.setCardBackgroundColor(Color.parseColor(config.colors[board[i][j].point]))
                }

                binding.board.addView(view, binding.board.childCount)
            }
        }
    }

    data class CellState(var point: Int = 0, var merge: Boolean = false)

    data class Rotation(var x: Int? = null, var y: Int? = null, var z: Int? = null)
}

