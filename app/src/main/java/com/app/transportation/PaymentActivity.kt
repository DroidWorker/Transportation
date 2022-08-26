package com.app.transportation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity


class PaymentActivity : AppCompatActivity() {
    var summ = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val intent : Intent = intent
        summ = intent.getIntExtra("summ", 1)

        var script : String = "<html><body ><div align='center'><iframe src='https://yoomoney.ru/quickpay/button-widget?targets=%D0%9E%D0%BF%D0%BB%D0%B0%D1%82%D0%B0%20%20%D1%83%D1%81%D0%BB%D1%83%D0%B3&default-sum=2&button-text=12&yoomoney-payment-type=on&button-size=m&button-color=orange&fio=on&phone=on&successURL=&quickpay=small&account=4100117850224340&' width='184' height='36' frameborder='0' allowtransparency='true' scrolling='no'></iframe></div><div> </div><div align='center'><iframe src='https://yoomoney.ru/quickpay/button-widget?targets=%D0%9E%D0%BF%D0%BB%D0%B0%D1%82%D0%B0%20%20%D1%83%D1%81%D0%BB%D1%83%D0%B3&default-sum=2&button-text=12&any-card-payment-type=on&button-size=m&button-color=orange&fio=on&phone=on&successURL=&quickpay=small&account=4100117850224340&' width='184' height='36' frameborder='0' allowtransparency='true' scrolling='no'></iframe></div></body></html>"

        val wv : WebView = findViewById(R.id.paymentView)
        wv.settings.javaScriptEnabled = true
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val pb = findViewById<ProgressBar>(R.id.progressBarWV)
                pb.visibility = View.GONE
            }
        }
        wv.loadData(script, "text/html", null);
    }

    public fun onReadyClicked(view : View){
        this.finish()
    }

}