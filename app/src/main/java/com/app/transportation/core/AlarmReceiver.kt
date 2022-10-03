package com.app.transportation.core

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.ui.MainViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import androidx.activity.viewModels
import com.app.transportation.ui.NotificationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        var notif : com.app.transportation.data.database.entities.Notification

        val notificationVM : NotificationViewModel = NotificationViewModel(context!!)
        notificationVM.getNotice()
        CoroutineScope(Dispatchers.Main).launch{
            notificationVM.cachedNotifications.collect{
                if (it.isNotEmpty()){
                    notificationVM.getOrderInfo(it.firstOrNull()!!.description)
                    notif = it.first()
                }
            }

            notificationVM.cachedOrder.collect{
                if (it.isNotEmpty()){
                    notif.description=it.first().description

                    val i = Intent(context, MainActivity::class.java)
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    val pendingIntent = PendingIntent.getActivity(context, 0, i, 0)

                    val builder = NotificationCompat.Builder(context!!, "trnsp")
                        .setSmallIcon(R.drawable.ic_done)
                        .setContentTitle(notif.title)
                        .setContentText(notif.description)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)

                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.notify(123, builder.build())
                }
            }
        }
    }
}