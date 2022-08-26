package com.app.transportation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.database.entities.Notification
import com.app.transportation.databinding.FragmentNotificationsBinding
import com.app.transportation.ui.adapters.NotificationsAdapter

class NotificationsFragment : Fragment() {

    private var binding: FragmentNotificationsBinding? = null
    private val b get() = binding!!

    private val adapter by lazy { NotificationsAdapter() }

    private val viewModel by activityViewModels<MainViewModel>()

    private val id by lazy { arguments?.getLong("id") ?: 0L }


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

        viewModel.getNotice()
        //applyInitialData()

        applyAdapters()

        applyListeners()
    }

    private fun applyInitialData() {
        //b.name.text = "Николай"
        //b.telNumber.text = "+7 (495) 510-55-55"
    }

    private fun applyAdapters() {
        b.notificationsRV.adapter = adapter
        viewModel.cachedNotifications.collectWithLifecycle(viewLifecycleOwner){ it ->
            val notifications : ArrayList<Notification> = ArrayList()
            it.forEach{
                notifications.add(Notification(it.userId.toLong(), it.type, "ользователь ${it.userId} оставил отклик на объявление ${it.dataId}", false))
            }

            if (notifications.isEmpty())
                notifications.add(Notification(1, "Уведомлений нет", "", false))

            adapter.submitList(notifications.toList())
        }
    }

    private fun applyListeners() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.notificationsRV.adapter = null
        binding = null
    }

}