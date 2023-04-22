package com.app.transportation.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.app.transportation.R


class MapDialogFragment : DialogFragment() {
    var catid = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = inflater.inflate(R.layout.map_info_layout, container, false)
        view.findViewById<ImageButton>(R.id.mapInfoClose).setOnClickListener {
            this.dismiss()
        }
        view.findViewById<TextView>(R.id.mapNamePhone).setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Выберите действие")
            builder.setItems(arrayOf("Позвонить", "Написать в мессенджер")) { _, which ->
                val mnp = view.findViewById<TextView>(R.id.mapNamePhone)
                try{
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${mnp.text.toString().split(" ")[1]}"))
                        startActivity(intent)
                    }
                    1 -> {
                        val phoneNumber = mnp.text.toString().split(" ")[1].replace("+", "")
                        val message = "Hello World!" // текст сообщения

                        val url = "https://api.whatsapp.com/send?phone=$phoneNumber"
                        try {
                            val pm = context!!.packageManager
                            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
                            val i = Intent(Intent.ACTION_VIEW)
                            i.data = Uri.parse(url)
                            startActivity(i)
                        } catch (e: PackageManager.NameNotFoundException) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("viber://chat?number=$phoneNumber")
                            startActivity(intent)
                        }
                    }
                }}catch (ex : Exception){
                    Toast.makeText(requireContext(), "ошибка", Toast.LENGTH_SHORT).show()
                }
            }
            builder.show()
        }
        view.findViewById<ImageButton>(R.id.mapInfoClose).setOnClickListener {
            var destinationRes = R.id.creatingOrderFragment
                when (catid) {
                    15, 16, 17, 159, 160 -> destinationRes = R.id.creatingOrderFragment
                    18,19,20, 157, 158 -> destinationRes = R.id.creatingOrderFragment
                    47, 48 ->  destinationRes = R.id.creatingOrderFragment
                    in 49..63, in 64..70, in 126..137, in 71..125 -> destinationRes = R.id.creatingOrderAtFragment
                    45, 44 -> destinationRes = R.id.creatingOrderPFragment
                    25, 26 -> destinationRes = R.id.creatingOrderPFragment
                    9,10,11 -> destinationRes = R.id.creatingOrderPFragment
                    21, 22, 23 -> destinationRes = R.id.creatingOrderRisFragment
                }
            findNavController().navigate(destinationRes, bundleOf("id" to catid))
            this.dismiss()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = arguments
        if (args != null) {
            catid = args.getInt("id")
            val title = args.getString("title")
            val price = args.getString("price")
            val namephone = args.getString("namephone")
            val photo = args.getString("photo")
            if(title!=null) view.findViewById<TextView>(R.id.mapTitle).text = title
            if(price!=null) view.findViewById<TextView>(R.id.mapPrice).text = "ЦЕНА\\n${price} pублей"
            if(namephone!=null) view.findViewById<TextView>(R.id.mapNamePhone).text = namephone
            photo?.let { base64String ->
                try {
                    val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    view.findViewById<ImageView>(R.id.mapPhoto).setImageBitmap(bitmap)
                }
                catch (ex : Exception){
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}