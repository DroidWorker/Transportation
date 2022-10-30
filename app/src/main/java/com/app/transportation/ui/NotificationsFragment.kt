package com.app.transportation.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.data.database.entities.Notification
import com.app.transportation.databinding.FragmentNotificationsBinding
import com.app.transportation.ui.adapters.NotificationsAdapter
import org.koin.android.ext.android.inject
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class NotificationsFragment : Fragment() {

    private var binding: FragmentNotificationsBinding? = null
    private val b get() = binding!!

    private val adapter by lazy { NotificationsAdapter() }

    private val viewModel by activityViewModels<MainViewModel>()

    private val id by lazy { arguments?.getLong("id") ?: 0L }

    var adverts : List<Advert> = emptyList()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private var lastCheckedNotificationID: String?
        get() = prefs.getString("lastCheckedNotificationID", "0")
        set(value) = prefs.edit { putString("lastCheckedNotificationID", value) }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Уведомления"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        viewModel.getMyAdverts()
        //applyInitialData()

        applyAdapters()

        applyListeners()
    }

    private fun applyAdapters() {
        b.notificationsRV.adapter = adapter
        viewModel.cachedAdvertsSF.collectWithLifecycle(viewLifecycleOwner){
            adverts = it
            if (it.isNotEmpty())
                viewModel.getNotice()
        }
        viewModel.cachedNotifications.collectWithLifecycle(viewLifecycleOwner){ it ->
            val notifications : ArrayList<Notification> = ArrayList()
            it.forEach{ entryNotificarion->
                notifications.add(Notification(entryNotificarion.key.toLong(), entryNotificarion.value.type, "пользователь ${entryNotificarion.value.userId} оставил отклик на ваше объявление ${((adverts.find { it.id.toString() == entryNotificarion.value.dataId})?.title)==null?:"-удалено-"}", entryNotificarion.key.toInt()<=lastCheckedNotificationID!!.toInt()))
                lastCheckedNotificationID = entryNotificarion.key
            }

            if (notifications.isEmpty())
                notifications.add(Notification(0, "Уведомлений нет", "", false))

            adapter.submitList(notifications.toList())
        }
    }

    private fun applyListeners() {
        adapter.onItemClick = {
            viewModel.deleteNotification(it.toString())
            adapter.currentList.toMutableList().let {  list->
                list.remove(list.find { not -> not.id.toInt() == it })
                adapter.submitList(list)
                viewModel.getMyAdverts()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.notificationsRV.adapter = null
        binding = null
    }

}