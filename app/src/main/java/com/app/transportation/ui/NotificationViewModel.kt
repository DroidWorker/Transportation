package com.app.transportation.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.app.transportation.data.NotificationRepository
import com.app.transportation.data.api.NoticeResponce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class NotificationViewModel(ctx : Context) {
    private val repository = NotificationRepository(ctx)
    private val c = ctx

    private suspend fun getNotification(){
        val answer = repository.getNotice()
        if(answer is NoticeResponce.Success) {
            println("yeeeeeees"+answer.notice)
            Toast.makeText(c, "notification katched", Toast.LENGTH_SHORT).show()
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
}