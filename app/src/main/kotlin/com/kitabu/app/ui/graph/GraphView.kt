package com.kitabu.app.ui.graph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import kotlin.math.*
import kotlin.random.Random

class GraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Node(val id: Int, val label: String, val color: Int, var x: Float = 0f, var y: Float = 0f)
    data class Edge(val fromId: Int, val toId: Int)

    var onNodeClick: ((Int) -> Unit)? = null

    private val nodes = mutableListOf<Node>()
    private val edges = mutableListOf<Edge>()

    private val nodePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edgePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF444466.toInt(); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFA0A0C0.toInt(); textSize = 26f; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val glowPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
    }
    private val bgPaint = Paint().apply { color = 0xFF111118.toInt() }

    private var scale = 1f
    private var transX = 0f; private var transY = 0f
    private val matrix = Matrix()

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            transX -= dx; transY -= dy; invalidate(); return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val inv = Matrix().also { matrix.invert(it) }
            val pts = floatArrayOf(e.x, e.y).also { inv.mapPoints(it) }
            nodes.firstOrNull { n -> hypot((n.x - pts[0]).toDouble(), (n.y - pts[1]).toDouble()) < NODE_R + 16 }
                ?.let { onNodeClick?.invoke(it.id) }
            return true
        }
    })
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            scale = (scale * d.scaleFactor).coerceIn(0.2f, 5f); invalidate(); return true
        }
    })

    fun setData(newNodes: List<Node>, newEdges: List<Edge>) {
        nodes.clear(); edges.clear()
        nodes.addAll(newNodes); edges.addAll(newEdges)
        post { layout() }
    }

    private fun layout() {
        if (nodes.isEmpty()) return
        val rnd = Random(42)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.35f
        // Initial circular layout for better initial positioning
        nodes.forEachIndexed { i, it ->
            val angle = (2.0 * Math.PI * i / nodes.size)
            it.x = cx + (radius * cos(angle)).toFloat()
            it.y = cy + (radius * sin(angle)).toFloat()
        }
        // Run more iterations for larger graphs
        val iterations = maxOf(150, nodes.size * 10)
        repeat(iterations.coerceAtMost(300)) { tick() }
        invalidate()
    }

    private fun tick() {
        val map = nodes.associateBy { it.id }
        // Repulsion between all nodes (O(n^2))
        nodes.forEach { a -> nodes.forEach { b ->
            if (a.id == b.id) return@forEach
            val dx = a.x - b.x; val dy = a.y - b.y
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
            val f = 8000f / (d * d)
            a.x += dx / d * f; a.y += dy / d * f
        }}
        // Attraction along edges
        edges.forEach { e ->
            val a = map[e.fromId] ?: return@forEach; val b = map[e.toId] ?: return@forEach
            val dx = b.x - a.x; val dy = b.y - a.y
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
            val idealDist = 180f
            val f = (d - idealDist) * 0.008f
            a.x += dx * f; a.y += dy * f; b.x -= dx * f; b.y -= dy * f
        }
        // Gravity towards center
        val cx = width / 2f; val cy = height / 2f
        nodes.forEach { it.x += (cx - it.x) * 0.01f; it.y += (cy - it.y) * 0.01f }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPaint(bgPaint)
        matrix.reset()
        matrix.postScale(scale, scale, width / 2f, height / 2f)
        matrix.postTranslate(transX, transY)
        canvas.save(); canvas.concat(matrix)
        val map = nodes.associateBy { it.id }
        // Draw edges
        edges.forEach { e ->
            val a = map[e.fromId] ?: return@forEach; val b = map[e.toId] ?: return@forEach
            edgePaint.color = 0xFF444466.toInt()
            canvas.drawLine(a.x, a.y, b.x, b.y, edgePaint)
        }
        // Draw nodes
        nodes.forEach { n ->
            // Glow
            glowPaint.color = (n.color and 0x00FFFFFF) or 0x44000000
            canvas.drawCircle(n.x, n.y, NODE_R * 2f, glowPaint)
            // Node circle
            nodePaint.color = n.color
            canvas.drawCircle(n.x, n.y, NODE_R, nodePaint)
            // Border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = 0x66FFFFFF.toInt()
            }
            canvas.drawCircle(n.x, n.y, NODE_R, borderPaint)
            // Label
            val label = n.label.take(16)
            canvas.drawText(label, n.x, n.y + NODE_R + 36f, labelPaint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e); gestureDetector.onTouchEvent(e); return true
    }

    companion object { private const val NODE_R = 26f }
}
