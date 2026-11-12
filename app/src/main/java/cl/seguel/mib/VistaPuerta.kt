package cl.seguel.mib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

// Dibujo propio: la puerta del prototipo, presentada como entrada a la agencia MIB.
class VistaPuerta(contexto: Context) : View(contexto) {
    var abierta = false
    var confirmada = false
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marco = RectF(17f, 15f, 93f, 135f)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        contentDescription = if (!confirmada) "Puerta sin estado confirmado" else if (abierta) "Puerta abierta" else "Puerta cerrada"
        val escala = height / 150f
        canvas.save()
        canvas.translate(width / 2f - 55 * escala, 0f)
        canvas.scale(escala, escala)
        pincel.color = if (confirmada) Color.rgb(185, 244, 102) else Color.rgb(130, 143, 124)
        pincel.style = Paint.Style.STROKE; pincel.strokeWidth = 2f
        canvas.drawRoundRect(marco, 7f, 7f, pincel)
        pincel.style = Paint.Style.FILL
        pincel.color = Color.rgb(12, 20, 13)
        canvas.drawRect(23f, 21f, 87f, 129f, pincel)
        pincel.color = if (confirmada) Color.rgb(185, 244, 102) else Color.rgb(130, 143, 124)
        if (abierta && confirmada) {
            canvas.drawRect(24f, 22f, 48f, 128f, pincel)
        } else {
            pincel.style = Paint.Style.STROKE
            canvas.drawRect(25f, 23f, 85f, 127f, pincel)
            pincel.style = Paint.Style.FILL
            canvas.drawCircle(76f, 80f, 3f, pincel)
        }
        pincel.textSize = 13f; pincel.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("MIB", 42f, 57f, pincel)
        pincel.strokeWidth = 1f
        canvas.drawLine(2f, 136f, 108f, 136f, pincel)
        canvas.restore()
    }
}
