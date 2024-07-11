package ch.zu.chrimametro.ui

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ch.zu.chrimametro.MainActivity
import ch.zu.chrimametro.R
import java.lang.reflect.Modifier
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainWidget : GlanceAppWidget() {

    val dailyBudget = 60.5
    val salaryDay = 25

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // create your AppWidget here
            Content(context)
        }
    }

    @Composable
    fun Content(context: Context) {
        val currentDate = LocalDate.now()
        Row(
            modifier = GlanceModifier.fillMaxSize().background(Color.White)
                .padding(vertical = 8.dp)
                .clickable {
                    context.startActivity(
                        Intent(
                            context,
                            MainActivity::class.java
                        ).apply {
                            flags = FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    (daysToSalaryDay(currentDate) * dailyBudget).toString() + " Fr",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(end = 16.dp)
                )
                Text(
                    daysToSalaryDay(currentDate).toString(),
                    style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color.Gray))
                )
            }
            Image(
                provider = ImageProvider(R.drawable.ic_wallet),
                contentDescription = "Image Icon",
                modifier = GlanceModifier.fillMaxSize().size(28.dp)
            )
        }
    }

    private fun daysToSalaryDay(currentDate: LocalDate): Int {
        val targetDate = if (currentDate.dayOfMonth >= 25) {
            currentDate.plusMonths(1).withDayOfMonth(25)
        } else {
            currentDate.withDayOfMonth(25)
        }
        return ChronoUnit.DAYS.between(currentDate, targetDate).toInt()
    }
}