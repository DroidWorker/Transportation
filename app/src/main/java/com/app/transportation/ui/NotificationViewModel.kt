package com.app.transportation.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.app.transportation.data.NotificationRepository
import com.app.transportation.data.api.NoticeResponce
import com.app.transportation.data.api.OrderDTO
import com.app.transportation.data.api.OrderInfoResponse
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.data.database.entities.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.stream.Collectors.toList

class NotificationViewModel(ctx : Context) {
    private val repository = NotificationRepository(ctx)
    private val c = ctx

    val cachedNotifications = MutableStateFlow(emptyList<Notification>())
    val cachedOrder = MutableStateFlow(emptyList<OrderDTO>())

    private suspend fun getNotification(){
        val answer = repository.getNotice()
        if(answer is NoticeResponce.Success) {
            if (answer.notice[0].type!="empty"){
                var list : ArrayList<Notification> = ArrayList()
                answer.notice.forEach{
                    var description : String = it.dataId
                    val title = when(it.type){
                        "ORDER"-> {
                            description = ""
                            "новая заявка на ваше объявление"
                        }
                        else -> "новое уведомление"
                    }
                    list.add(Notification(it.userId.toLong(), title, description, false))
                }
                cachedNotifications.tryEmit(list.toList())
            }
            else{
                cachedNotifications.tryEmit(emptyList())
            }
            //cachedNotifications.tryEmit(response.notice)
            //response.notice
        }
        else{
            println("nooooooo$answer")
        }
    }

    fun getNotice() = GlobalScope.launch(Dispatchers.IO) {
        getNotification()
    }

    private suspend fun getOrder(id: String){
        val answer = repository.getOrderInfo(id)
        if (answer is OrderInfoResponse.Success){
            println("nooootifOK002")
            cachedOrder.tryEmit(listOf(answer.order))
        }
    }

    fun getOrderInfo(id: String) = GlobalScope.launch(Dispatchers.IO){
        getOrder(id)
    }
}