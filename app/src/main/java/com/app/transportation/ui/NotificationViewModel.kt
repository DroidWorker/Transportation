package com.app.transportation.ui

import android.content.Context
import androidx.core.content.edit
import com.app.transportation.data.NotificationRepository
import com.app.transportation.data.api.NoticeResponce
import com.app.transportation.data.api.OrderDTO
import com.app.transportation.data.api.OrderInfoResponse
import com.app.transportation.data.database.entities.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(ctx : Context) {
    private val repository = NotificationRepository(ctx)
    private val c = ctx

    val cachedNotifications = MutableStateFlow(emptyList<Notification>())
    val cachedOrder = MutableStateFlow(emptyList<OrderDTO>())

    private val prefs =  ctx.getSharedPreferences("MainSettings",Context.MODE_PRIVATE)

    private var lastNotificationID: String?
        get() = prefs.getString("lastNotificationID", "0")
        set(value) = prefs.edit { putString("lastNotificationID", value) }

    private suspend fun getNotification(){
        val answ = repository.getNotice()
        (answ as? NoticeResponce.Success)?.let {answer->
            if (answer.notice.entries.firstOrNull()?.value?.type!="empty"){
                var list : ArrayList<Notification> = ArrayList()
                answer.notice.forEach{
                    if (it.key.toInt()>lastNotificationID!!.toInt()) {
                        var description: String = it.value.dataId
                        val title = when (it.value.type) {
                            "ORDER" -> {
                                description = "пользователь ${it.value.userName} оставил заявку"
                                "новая заявка на ваше объявление"
                            }
                            "ADVERT" -> {
                                description = it.value.text
                                "Transportation"
                            }
                            else -> "новое уведомление"
                        }
                        lastNotificationID = it.key
                        list.add(Notification(it.key.toLong(), title, description, false))
                    }
                }
                cachedNotifications.tryEmit(list.toList())
            }
            else{
                cachedNotifications.tryEmit(emptyList())
            }
            //cachedNotifications.tryEmit(response.notice)
            //response.notice
        }
    }

    fun getNotice() = GlobalScope.launch(Dispatchers.IO) {
        getNotification()
    }

    private suspend fun getOrder(id: String){
        val answer = repository.getOrderInfo(id)
        if (answer is OrderInfoResponse.Success){
            cachedOrder.tryEmit(listOf(answer.order))
        }
    }

    fun getOrderInfo(id: String) = GlobalScope.launch(Dispatchers.IO){
        getOrder(id)
    }
}