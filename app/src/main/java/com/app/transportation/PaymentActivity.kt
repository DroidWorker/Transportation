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


        /*var scriptlight : String = "<html><head><style>table {width: 150px;margin: auto;border-spacing: 7px 11px;}  td {text-align: center;} .radio{text-align: center; background: rgb(222, 222, 222); border-radius: 5px;}  input[type='radio'] { position: relative; height: 10px; width: 10px;-webkit-appearance: none;-moz-appearance: none; appearance: none; outline: none;}   input[type='radio']::before {content: ''; position: absolute; top: 50%; left: 50%; width: 10px; height: 10px; border-radius: 50%; transform: translate(-50%, -50%); background-color: white; border: 2px solid rgb(32, 145, 50);}   input[type='radio']:checked::after { content: ''; position: absolute; top: 50%; left: 50%; width: 9px; height: 9px; border-radius: 50%; background-color: rgb(32,  145, 50); transform: translate(-50%, -50%); visibility: visible;}</style></head><body><form method='POST' action='https://yoomoney.ru/quickpay/confirm.xml'><table width='100%' cellspacing='0' cellpadding='4'><input type='hidden' name='receiver' value='4100117850224340'><input type='hidden' name='formcomment' value='Проект «Железный человек»: реактор холодного ядерного синтеза'><input type='hidden' name='short-dest' value='Проект «Железный человек»: реактор холодного ядерного синтеза'><input type='hidden' name='successURL' value='https://google.com'><input type='hidden' name='quickpay-form' value='donate'><input type='hidden' name='targets' value='транзакция {order_id}'><input type='hidden' name='sum' value='10' data-type='number'><input type='hidden' name='need-fio' value='true'><input type='hidden' name='need-email' value='false'><input type='hidden' name='need-phone' value='true'><input type='hidden' name='need-address' value='false'><tr> <td class='radio'><label><input type='radio' name='paymentType' value='PC'>ЮMoney</label></td></tr><tr> <td  class='radio'><label><input type='radio' name='paymentType' value='AC'>Банковской картой</label></td></tr><tr> <td><input type='submit' value='Перевести' id='button' style='background: rgb(32,145,50); border: 0; border-radius: 5px; color: white;'></td></tr></table></form></body></html>"
        var scriptdark : String = "<html><head><style>table {width: 150px;margin: auto;border-spacing: 7px 11px;}  td {text-align: center;} .radio{text-align: center; background: rgb(222, 222, 222); border-radius: 5px;}  input[type='radio'] { position: relative; height: 10px; width: 10px;-webkit-appearance: none;-moz-appearance: none; appearance: none; outline: none;}   input[type='radio']::before {content: ''; position: absolute; top: 50%; left: 50%; width: 10px; height: 10px; border-radius: 50%; transform: translate(-50%, -50%); background-color: white; border: 2px solid rgb(235,127,0);}   input[type='radio']:checked::after { content: ''; position: absolute; top: 50%; left: 50%; width: 9px; height: 9px; border-radius: 50%; background-color: rgb(235,127,0); transform: translate(-50%, -50%); visibility: visible;}</style></head><body><form method='POST' action='https://yoomoney.ru/quickpay/confirm.xml'><table width='100%' cellspacing='0' cellpadding='4'><input type='hidden' name='receiver' value='4100117850224340'><input type='hidden' name='formcomment' value='Проект «Железный человек»: реактор холодного ядерного синтеза'><input type='hidden' name='short-dest' value='Проект «Железный человек»: реактор холодного ядерного синтеза'><input type='hidden' name='successURL' value='https://google.com'><input type='hidden' name='quickpay-form' value='donate'><input type='hidden' name='targets' value='транзакция {order_id}'><input type='hidden' name='sum' value='10' data-type='number'><input type='hidden' name='need-fio' value='true'><input type='hidden' name='need-email' value='false'><input type='hidden' name='need-phone' value='true'><input type='hidden' name='need-address' value='false'><tr> <td class='radio'><label><input type='radio' name='paymentType' value='PC'>ЮMoney</label></td></tr><tr> <td  class='radio'><label><input type='radio' name='paymentType' value='AC'>Банковской картой</label></td></tr><tr> <td><input type='submit' value='Перевести' id='button' style='background: rgb(235,127,0); border: 0; border-radius: 5px; color: white;'></td></tr></table></form></body></html>"

        val wv : WebView = findViewById(R.id.paymentView)
        wv.settings.javaScriptEnabled = true
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val pb = findViewById<ProgressBar>(R.id.progressBarWV)
                pb.visibility = View.GONE
                if(view.url.equals("https://google.com")){
                    val but = findViewById<Button>(R.id.readyButton)
                    but.isEnabled = true
                }
            }
        }

        wv.loadData(if(apptheme==0) scriptlight else scriptdark, "text/html", null);*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==909){
            descriptionTV.text="success"
        }
    }

}