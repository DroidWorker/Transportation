package com.app.transportation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.data.database.entities.Notification
import com.app.transportation.databinding.FragmentNotificationsBinding
import com.app.transportation.ui.adapters.NotificationsAdapter

class NotificationsFragment : Fragment() {

    private var binding: FragmentNotificationsBinding? = null
    private val b get() = binding!!

    private val adapter by lazy { NotificationsAdapter() }

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

        applyInitialData()

        applyAdapters()

        applyListeners()
    }

    private fun applyInitialData() {
        //b.name.text = "Николай"
        //b.telNumber.text = "+7 (495) 510-55-55"
    }

    private fun applyAdapters() {
        b.notificationsRV.adapter = adapter

        adapter.submitList(
            listOf(
                Notification(0, "Заголовок уведомления", "Текст уведомления", true),
                Notification(1, "Заголовок уведомления", "Текст уведомления", false)
            )
        )
    }

    private fun applyListeners() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.notificationsRV.adapter = null
        binding = null
    }

}