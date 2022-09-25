package com.app.transportation

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import org.w3c.dom.Text
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.DarkThemeMode
import ru.tinkoff.acquiring.sdk.models.enums.CheckType
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.cardio.CameraCardIOScanner


class PaymentActivity : AppCompatActivity() {
    var summ = 0
    var description = "описание не настроено"
    var clientId : String = "0"
    var clientEmail = "";
    var apptheme = 0//0-light 1-dark

    var descriptionTV : TextView = TextView(this)
    private val prefs: SharedPreferences by inject(named("MainSettings"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val intent : Intent = intent
        summ = intent.getIntExtra("summ", 1)

        when (prefs.getString("appTheme", "system")){
            "system" -> apptheme=0
            "light" -> apptheme=0
            "dark" -> apptheme=1
        }

        descriptionTV = findViewById<TextView>(R.id.descriptionTV)
        descriptionTV.text = "покупка бизнес аккаунта\n тариф: базовый"

        AcquiringSdk.isDeveloperMode = true
        AcquiringSdk.isDebug = true
        var paymentOptions = PaymentOptions().setOptions {
            orderOptions { // данные заказа
                orderId = summ.toString()
                amount = Money.ofCoins(summ.toLong())
                title = "Покупка в приложении"
                description = description
                recurrentPayment = false
            }
            customerOptions { // данные покупателя
                customerKey = clientId
                email = clientEmail
                checkType = CheckType.NO.toString()
            }
            featuresOptions { // настройки визуального отображения и функций экрана оплаты
                useSecureKeyboard = true
                localizationSource = AsdkSource(Language.RU)
                handleCardListErrorInSdk = true
                cameraCardScanner = CameraCardIOScanner()
                darkThemeMode = if (apptheme==0) DarkThemeMode.DISABLED else DarkThemeMode.ENABLED
            }
        }

        val pay = findViewById<Button>(R.id.readyButton);
        pay.setOnClickListener(View.OnClickListener {
            val tinkoffAcquiring = TinkoffAcquiring(this,"TERMINAL_KEY", "PUBLIC_KEY") // создание объекта для взаимодействия с SDK и передача данных продавца
            tinkoffAcquiring.openPaymentScreen(this, paymentOptions, 909)
            val pb = findViewById<ProgressBar>(R.id.progressBar3)
            pb.visibility = View.VISIBLE
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==909){
            descriptionTV.text="success"
        }
    }

}