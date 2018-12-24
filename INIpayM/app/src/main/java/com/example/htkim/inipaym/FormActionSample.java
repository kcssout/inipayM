package com.example.htkim.inipaym;

/**
 * Created by ht.kim on 2018-11-07.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

public class FormActionSample extends Activity {

    private WebView sampleWebView;
    private String TAG = "SAMPLE";
    private Handler handler = new Handler();

    private class ChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            FormActionSample.this.setProgress(newProgress * 1000);
        }
    }


    @SuppressLint("JavascriptInterface")
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.content);
        sampleWebView = (WebView) findViewById(R.id.contentView);
        sampleWebView.setWebChromeClient(new ChromeClient());
        sampleWebView.setWebViewClient(new SampleWebView());

        //반드시 javascript 속성을 활성화 시켜야 합니다.
        sampleWebView.getSettings().setJavaScriptEnabled(true);

        //Javascript 인터페이스 선언부 ,myInterface로 선언]
        sampleWebView.addJavascriptInterface(new AndroidBridge(), "myInterface");

        //하단에 버튼을 클릭하면 HTML Page javascript를 호출 할 것입니다.(APP TO WEB)
        Button button = (Button) findViewById(R.id.tstbutton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleWebView.loadUrl("javascript:buttonClick();");
            }
        });


        //기본 페이지
        sampleWebView.loadUrl("file:///android_asset/test2.html");

    }

    //Javascript Interface에 정의되는 클래스
    private class AndroidBridge {

        //웹페이지에서 호출될  임의의 함수를 정의합니다.
        public void callAndroid(final String arg) {
            handler.post(new Runnable() {
                public void run() {
                    Log.d(TAG, "callAndroid(" + arg + ")");
                    Toast.makeText(FormActionSample.this, "자바스크립트에 의해 호출된 함수에서 찍는 값:" + arg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("로딩중입니다. \n잠시만 기다려주세요.");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        return dialog;
    }


    private class SampleWebView extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            showDialog(0);
        }

        public void onPageFinished(WebView view, String url) {
            dismissDialog(0);
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.loadData("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
                    "</head><body>" + "요청실패 : (" + errorCode + ")" + description + "</body></html>", "text/html", "utf-8");
        }
    }
}
