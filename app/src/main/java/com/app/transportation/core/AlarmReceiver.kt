package com.app.transportation.core

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.ui.NotificationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        var notif : com.app.transportation.data.database.entities.Notification

        val notificationVM = NotificationViewModel(context!!)
        notificationVM.getNotice()
        CoroutineScope(Dispatchers.Main).launch{
            notificationVM.cachedNotifications.collect{
                if (it.isNotEmpty()){
                    notificationVM.getOrderInfo(it.firstOrNull()!!.description)
                    notif = it.first()
                    val i = Intent(context, MainActivity::class.java)
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    var pendingIntent = PendingIntent.getActivity(context, 0, i, 0)
                    pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.getActivity(
                            context,
                            0,
                            i,
                            PendingIntent.FLAG_MUTABLE
                        )
                    } else {
                        PendingIntent.getActivity(
                            context,
                            0,
                            i,
                            PendingIntent.FLAG_ONE_SHOT
                        )
                    }

                    val builder = NotificationCompat.Builder(context!!, "trnsp")
                        .setSmallIcon(R.drawable.ic_notif)
                        .setLargeIcon(
                            BitmapFactory.decodeResource(context.resources,
                            R.drawable.logo))
                        .setColor(Color.argb(255, 235, 127, 0))//(Color.argb(R.color.primary_color.alpha, R.color.primary_color.red, R.color.primary_color.green, R.color.primary_color.blue))
                        .setContentTitle("Transportation")
                        .setContentText(notif.description)
                        .setAutoCancel(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(pendingIntent)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(notif.description))

                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.notify(123, builder.build())
                }
            }

            notificationVM.cachedOrder.collect{
                if (it.isNotEmpty()){
                    notif.description=it.first().description


                }
            }
        }
    }
}