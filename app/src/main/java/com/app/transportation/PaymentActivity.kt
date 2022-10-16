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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.app.transportation.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import org.w3c.dom.Text
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.DarkThemeMode
import ru.tinkoff.acquiring.sdk.models.enums.CheckType
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.cardio.CameraCardIOScanner
import java.text.SimpleDateFormat
import java.util.*


class PaymentActivity : AppCompatActivity() {
    var summ = 0
    var description = "описание не настроено"
    var clientId : String = "0"
    var clientEmail = "";
    var apptheme = 0//0-light 1-dark

    var paymentMode : Int = 0

    lateinit var descriptionTV : TextView
    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val intent : Intent = intent
        summ = intent.getIntExtra("summ", 1)
        paymentMode = intent.getIntExtra("mode", 0)
        clientId = intent.getIntExtra("id", 0).toString()
        clientEmail = intent.getStringExtra("email").toString()

        when (prefs.getString("appTheme", "system")){
            "system" -> apptheme=0
            "light" -> apptheme=0
            "dark" -> apptheme=1
        }

        descriptionTV = findViewById(R.id.textView4)
        when(paymentMode){
            1->descriptionTV.text = "покупка бизнес аккаунта\n тариф универсал"
            2->descriptionTV.text = "активация платных опций"
        }

        //AcquiringSdk.isDeveloperMode = true
        //AcquiringSdk.isDebug = true
        var paymentOptions = PaymentOptions().setOptions {
            orderOptions { // данные заказа
                val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                orderId = sdf.format(Date())
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
            val tinkoffAcquiring = TinkoffAcquiring(this,"1665760199629DEMO", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv5yse9ka3ZQE0feuGtemYv3IqOlLck8zHUM7lTr0za6lXTszRSXfUO7jMb+L5C7e2QNFs+7sIX2OQJ6a+HG8kr+jwJ4tS3cVsWtd9NXpsU40PE4MeNr5RqiNXjcDxA+L4OsEm/BlyFOEOh2epGyYUd5/iO3OiQFRNicomT2saQYAeqIwuELPs1XpLk9HLx5qPbm8fRrQhjeUD5TLO8b+4yCnObe8vy/BMUwBfq+ieWADIjwWCMp2KTpMGLz48qnaD9kdrYJ0iyHqzb2mkDhdIzkim24A3lWoYitJCBrrB2xM05sm9+OdCI1f7nPNJbl5URHobSwR94IRGT7CJcUjvwIDAQAB") // создание объекта для взаимодействия с SDK и передача данных продавца
            tinkoffAcquiring.openPaymentScreen(this, paymentOptions, 909)
            val pb = findViewById<ProgressBar>(R.id.progressBar3)
            pb.visibility = View.VISIBLE
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==909){
            if(resultCode==Activity.RESULT_OK) {
                when(paymentMode){
                    1 -> {
                        viewModel.setBusinessStatus(clientId, "ACTIVE")
                        findViewById<Button>(R.id.readyButton).text = "готово"
                        findViewById<Button>(R.id.readyButton).setOnClickListener{
                            this.finish()
                        }
                        findViewById<TextView>(R.id.descriptionTV).text = "оплата прошла успешно\nзапрос на активацию бизнес сатуса отправлен"
                    }
                    2 ->{
                        //viewModel.updateOptions(viewModel.lastAdvertAdded) - activate all options for this advert
                        findViewById<Button>(R.id.readyButton).text = "готово"
                        findViewById<Button>(R.id.readyButton).setOnClickListener{
                            this.finish()
                        }
                        findViewById<TextView>(R.id.descriptionTV).text = "оплата прошла успешно\n опции активированы"
                    }
                }
            }
            else if (resultCode == TinkoffAcquiring.RESULT_ERROR){
                Snackbar.make(findViewById(R.id.textView4), "оплата отклонена: "+((data?.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR)) as AcquiringApiException).localizedMessage, Snackbar.LENGTH_LONG).show()
            }
            else{
                Snackbar.make(findViewById(R.id.textView4), "неизвестная ошибка", Snackbar.LENGTH_LONG).show()
            }
            val pb = findViewById<ProgressBar>(R.id.progressBar3)
            pb.visibility = View.GONE
        }
    }

}