package com.app.transportation.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.PaymentActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.data.database.entities.Notification
import com.app.transportation.databinding.FragmentNotificationsBinding
import com.app.transportation.databinding.PopupMessageBinding
import com.app.transportation.ui.adapters.NotificationsAdapter
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named


class NotificationsFragment : Fragment() {

    private var binding: FragmentNotificationsBinding? = null
    private val b get() = binding!!

    private val adapter by lazy { NotificationsAdapter() }

    private val viewModel by activityViewModels<MainViewModel>()

    //private val id by lazy { arguments?.getLong("id") ?: 0L }

    var adverts : List<Advert> = emptyList()
    var ReadyToloadflag = false

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private val userId : String? = prefs.getString("myId", null).takeIf { it != "" }

    private var userEmail = "";

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

        viewModel.profileFlow.collectWithLifecycle(viewLifecycleOwner){
            if (it?.bussiness?.contains("ACTIVE") == false){
                userEmail = it?.email.toString()
                val popup = PopupWindow(requireContext()).apply {
                    val menuB = PopupMessageBinding.inflate(layoutInflater)
                    contentView = menuB.root
                    menuB.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

                    setBackgroundDrawable(
                        ContextCompat.getDrawable(requireContext(), R.drawable.menu_background)
                    )

                    menuB.pmButton.setOnClickListener{
                        if (userEmail.isEmpty() || userId?.toInt()==0)
                            return@setOnClickListener
                        val intent = Intent(activity, PaymentActivity::class.java)
                        intent.putExtra("summ", 35000)
                        intent.putExtra("mode", 1)
                        intent.putExtra("id", userId?.toInt())
                        intent.putExtra("email", userEmail)
                        startActivity(intent)
                    }
                    isOutsideTouchable = true
                    setTouchInterceptor { v, event ->
                        v.performClick()
                        return@setTouchInterceptor if (event.action == MotionEvent.ACTION_OUTSIDE) {
                            dismiss()
                            true
                        } else
                            false
                    }
                    showAtLocation(b.parentLayout, Gravity.CENTER, 0, 0)
                }
            }
        }

        applyAdapters()

        applyListeners()
    }

    private fun applyAdapters() {
        b.notificationsRV.adapter = adapter
        viewModel.cachedAdvertsSF.collectWithLifecycle(viewLifecycleOwner){
            adverts = it
            if (it.isNotEmpty()&&ReadyToloadflag)
                viewModel.getNotice()
            else
                ReadyToloadflag = true
        }
        viewModel.cachedNotifications.collectWithLifecycle(viewLifecycleOwner){ it ->
            val notifications : ArrayList<Notification> = ArrayList()
            it.forEach{ entryNotificarion->
                when(entryNotificarion.value.type)
                {
                    "ORDER"-> {
                        notifications.add(
                            Notification(
                                entryNotificarion.key.toLong(),
                                "Новое уведомление",
                                entryNotificarion.value.text,
                                entryNotificarion.key.toInt() <= lastCheckedNotificationID!!.toInt()
                            )
                        )
                        if (entryNotificarion.key.toInt()>lastCheckedNotificationID!!.toInt())
                            lastCheckedNotificationID = entryNotificarion.key
                    }
                    "ADVERT"->{
                        notifications.add(
                            Notification(
                                entryNotificarion.key.toLong(),
                                "Новое уведомление!",
                                entryNotificarion.value.text,
                                entryNotificarion.key.toInt() <= lastCheckedNotificationID!!.toInt()
                            )
                        )
                        if (entryNotificarion.key.toInt()>lastCheckedNotificationID!!.toInt())
                            lastCheckedNotificationID = entryNotificarion.key
                    }
                }
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
        b.setOpen.setOnClickListener(View.OnClickListener {
            val notificationSettingsIntent = Intent()
            notificationSettingsIntent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            notificationSettingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationSettingsIntent.putExtra(
                    "android.provider.extra.APP_PACKAGE",
                    requireActivity().packageName
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationSettingsIntent.putExtra(
                    "app_package",
                    requireActivity().packageName
                )
                notificationSettingsIntent.putExtra(
                    "app_uid",
                    requireActivity().applicationInfo.uid
                )
            }
            requireActivity().startActivityForResult(
                notificationSettingsIntent,
                9009
            )
        })
        b.modeOpen.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent()
                val packageName = requireActivity().packageName
                val pm = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isIgnoringBatteryOptimizations(packageName)) intent.action =
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS else {
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
                }
                requireActivity().startActivity(intent)
            }

        })
        b.floatingActionButton.bringToFront()
        b.floatingActionButton.setOnClickListener(View.OnClickListener {
            b.textView.visibility = View.VISIBLE
            b.setOpen.visibility = View.VISIBLE
            b.modeOpen.visibility = View.VISIBLE
            b.floatingActionButton.visibility = View.GONE
        })}

    override fun onDestroyView() {
        super.onDestroyView()
        b.notificationsRV.adapter = null
        binding = null
    }

}