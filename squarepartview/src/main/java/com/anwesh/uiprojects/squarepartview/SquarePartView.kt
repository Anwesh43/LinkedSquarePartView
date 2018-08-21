package com.anwesh.uiprojects.squarepartview

/**
 * Created by anweshmishra on 21/08/18.
 */
import android.app.Activity
import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Context
import android.graphics.Color

val nodes : Int = 4

fun Canvas.drawSquarePartNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (nodes + 1)
    val deg : Float = 360f / (nodes)
    val size : Float = gap / 2
    paint.color = Color.parseColor("#4CAF50")
    save()
    translate(gap * i + gap / 2 + gap * scale, h / 2)
    rotate(deg * i)
    drawLine(-size/2, size/2, size/2, size / 2, paint)
    restore()
}

class SquarePartView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += 0.1f * this.dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1 - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {
        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class SquarePartNode(var i : Int, val state : State = State()) {
        private var next : SquarePartNode? = null
        private var prev : SquarePartNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes -1) {
                next = SquarePartNode(i)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSquarePartNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : SquarePartNode {
            var curr : SquarePartNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LinkedSquarePart(var i : Int) {

        private var curr : SquarePartNode = SquarePartNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer (var view : SquarePartView) {

        private val animator : Animator = Animator(view)

        private val linkedSquarePart : LinkedSquarePart = LinkedSquarePart(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            linkedSquarePart.draw(canvas, paint)
            animator.animate {
                linkedSquarePart.update{i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            linkedSquarePart.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : SquarePartView {
            val view : SquarePartView = SquarePartView(activity)
            activity.setContentView(view)
            return view
        }
    }
}