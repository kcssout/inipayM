package com.example.htkim.inipaym;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import ch.boye.httpclientandroidlib.util.EntityUtils;

//htkim 12/17/2018
public class Activity_Payment extends Activity {

    private WebView payWebView;
    private String TAG = this.getClass().getSimpleName();

    private static final int DIALOG_PROGRESS_WEBVIEW = 0;
    private static final int DIALOG_PROGRESS_MESSAGE = 1;
    private static final int DIALOG_ISP = 2;
    private static final int DIALOG_CARDAPP = 3;
    private static String DIALOG_CARDNM = "";
    private static String httpPayResult = "";
    private AlertDialog alertIsp;
    private JSONObject objJSON;
    private Handler payHandler;
    private Map<String,Object> qparam;

    private static final String ENTRY_URL= "sample url"; // 

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.content);

        qparam = new HashMap<String,Object>();

        String myJSONData="";
        try {
            objJSON = new JSONObject(myJSONData);
            Log.d(TAG, ENTRY_URL+objJSON.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        payWebView = (WebView)findViewById(R.id.contentView);
        payWebView.getSettings().setJavaScriptEnabled(true);
        payWebView.getSettings().setSavePassword(false);

//        Third party cookies 사용의 차단으로 안심클릭 카드 결제 시, 보안 키보드를 불러오지 못 하는 이슈
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(payWebView, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }
        LinearLayout layout = (LinearLayout)findViewById(R.id.tstLinearLayout);
        layout.setVisibility(View.GONE);

        //기본 페이지
        payWebView.setWebViewClient(new payWebView());
        payWebView.loadUrl(ENTRY_URL+objJSON.toString());

        Intent intent = getIntent();
        Uri intentData = intent.getData();

        if ( intentData == null ) {
            Log.d(TAG, " 널");
        } else {
            Log.d(TAG, intentData.toString()+ " OK");
        }

        findViewById(R.id.imgb_back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(payWebView.canGoBack()) {
                    payWebView.goBack();
                } else {
                    onBackPressed();
                }
            }
        });

    }

    public String httpPay(final String url_req,final String P_TID) {
        final ProgressDialog asyncDialog = new ProgressDialog(
                Activity_Payment.this);

        new AsyncTask<Object, Object, String>(){

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                asyncDialog.setMessage("결제 진행 중입니다..");
                asyncDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFD4D9D0")));

                asyncDialog.setIndeterminate(false);
                // show dialog
                asyncDialog.show();
            }

            protected String doInBackground(Object... params) {
                // Create a new HttpClient and Post Header
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(url_req);

                try {
                    // 아래처럼 적절히 응용해서 데이터형식을 넣으시고
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("P_TID", P_TID));
                    nameValuePairs.add(new BasicNameValuePair("P_MID", "INIpayTest"));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    //HTTP Post 요청 실행
                    HttpResponse response = httpclient.execute(httppost);

                    if(200 == response.getStatusLine().getStatusCode()){
                        String qString = EntityUtils.toString(response.getEntity());
                        if(qString != null){
                            Uri uri = Uri.parse(URLDecoder.decode("http://dummy/?" + qString, "UTF-8"));
                            if (uri != null) {
                                for(String key: uri.getQueryParameterNames()) {
                                    qparam.put(key,uri.getQueryParameter(key));
//                                    System.out.println("key=[" + key + "], value=[" + uri.getQueryParameter(key) + "]");
                                }
                            }
                        }
                    }
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                    Toast.makeText(Activity_Payment.this, "통신이 원할하지 않습니다.",Toast.LENGTH_SHORT);
                    // TODO Auto-generated catch block
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(Activity_Payment.this, "통신이 원할하지 않습니다.",Toast.LENGTH_SHORT);
                    // TODO Auto-generated catch block
                }
                return "";
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                httpPayResult = result;
                asyncDialog.dismiss();
                Log.d(TAG, "결과=> "+ qparam.get("P_STATUS").toString() );
                payHandler.sendEmptyMessage(Integer.parseInt(qparam.get("P_STATUS").toString()));
            }
        }.execute();
        return httpPayResult;
    }


    @Override public void onBackPressed() {
        if(payWebView.canGoBack()) {
            payWebView.goBack();

        } else {
            super.onBackPressed();
        }
    }

    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }


    @SuppressWarnings("unused")
    private AlertDialog getCardInstallAlertDialog(final String coCardNm){

        final Hashtable<String, String> cardNm = new Hashtable<String, String>();
        cardNm.put("HYUNDAE", "현대 앱카드");
        cardNm.put("SAMSUNG", "삼성 앱카드");
        cardNm.put("LOTTE",   "롯데 앱카드");
        cardNm.put("SHINHAN", "신한 앱카드");
        cardNm.put("KB", 	  "국민 앱카드");
        cardNm.put("HANASK",  "하나SK 통합안심클릭");
        //cardNm.put("SHINHAN_SMART",  "Smart 신한앱");

        final Hashtable<String, String> cardInstallUrl = new Hashtable<String, String>();
        cardInstallUrl.put("HYUNDAE", "market://details?id=com.hyundaicard.appcard");
        cardInstallUrl.put("SAMSUNG", "market://details?id=kr.co.samsungcard.mpocket");
        cardInstallUrl.put("LOTTE",   "market://details?id=com.lotte.lottesmartpay");
        cardInstallUrl.put("LOTTEAPPCARD",   "market://details?id=com.lcacApp");
        cardInstallUrl.put("SHINHAN", "market://details?id=com.shcard.smartpay");
        cardInstallUrl.put("KB", 	  "market://details?id=com.kbcard.cxh.appcard");
        cardInstallUrl.put("HANASK",  "market://details?id=com.ilk.visa3d");
        //cardInstallUrl.put("SHINHAN_SMART",  "market://details?id=com.shcard.smartpay");//여기 수정 필요!!2014.04.01

        AlertDialog alertCardApp =  new AlertDialog.Builder(Activity_Payment.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("알림")
                .setMessage( cardNm.get(coCardNm) + " 어플리케이션이 설치되어 있지 않습니다. \n설치를 눌러 진행 해 주십시요.\n취소를 누르면 결제가 취소 됩니다.")
                .setPositiveButton("설치", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String installUrl = cardInstallUrl.get(coCardNm);
                        Uri uri = Uri.parse(installUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        Log.d("<INIPAYMOBILE>","Call : "+uri.toString());
                        try{
                            startActivity(intent);
                        }catch (ActivityNotFoundException anfe) {
                            Toast.makeText(Activity_Payment.this, cardNm.get(coCardNm) + "설치 url이 올바르지 않습니다" , Toast.LENGTH_SHORT).show();
                        }
                        //finish();
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(Activity_Payment.this, "(-1)결제를 취소 하셨습니다." , Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .create();

        return alertCardApp;

    }//end getCardInstallAlertDialog

    private void showResult(String result) {
        AlertDialog.Builder mDialog = new AlertDialog.Builder(Activity_Payment.this, android.R.style.Theme_Holo_Light_Dialog);
        mDialog.setMessage("결제 " + result + "하였습니다.")

                .setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                finish();
                            }
                        });
        AlertDialog alert = mDialog.create();
        alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alert.setTitle("결제 확인");
        alert.show();
    }



    protected Dialog onCreateDialog(int id) {//ShowDialog


        switch(id){

            case DIALOG_PROGRESS_WEBVIEW:
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage("로딩중입니다. \n잠시만 기다려주세요.");
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                return dialog;

            case DIALOG_PROGRESS_MESSAGE:
                break;


            case DIALOG_ISP:

                alertIsp =  new AlertDialog.Builder(Activity_Payment.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("알림")
                        .setMessage("모바일 ISP 어플리케이션이 설치되어 있지 않습니다. \n설치를 눌러 진행 해 주십시요.\n취소를 누르면 결제가 취소 됩니다.")
                        .setPositiveButton("설치", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String ispUrl = "http://mobile.vpay.co.kr/jsp/MISP/andown.jsp";
                                payWebView.loadUrl(ispUrl);
                                finish();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Toast.makeText(Activity_Payment.this, "(-1)결제를 취소 하셨습니다." , Toast.LENGTH_SHORT).show();
                                finish();
                            }

                        })
                        .create();

                return alertIsp;

            case DIALOG_CARDAPP :
                return getCardInstallAlertDialog(DIALOG_CARDNM);

        }//end switch

        return super.onCreateDialog(id);

    }//end onCreateDialog



    private class payWebView extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG," deprecation");
            return checkPayUrl(url, view);
        }
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Log.d(TAG," nnn");
            return checkPayUrl(url, view);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            showDialog(0);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            // TODO Auto-generated method stub
            super.onLoadResource(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            dismissDialog(0);
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.loadData("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
                    "</head><body>"+"요청실패 : ("+errorCode+")" + description+"</body></html>", "text/html", "utf-8");
        }
    }

    public boolean checkPayUrl(String url,WebView view) {
        Log.d(TAG, "Enter url : "+url);
	    	/*
	    	 * URL별로 분기가 필요합니다. 어플리케이션을 로딩하는것과
	    	 * WEB PAGE를 로딩하는것을 분리 하여 처리해야 합니다.
	    	 * 만일 가맹점 특정 어플 URL이 들어온다면
	    	 * 조건을 더 추가하여 처리해 주십시요.
	    	 */
        String next = "https://mobile.inicis.com/smart/testmall/next_url_test.php?";
        String P_STATUS="";
        String P_REQ_URL="";
        String P_TID ="";
        String P_MID  ="";
        if(url.startsWith(next)){
            Uri uri = Uri.parse(url);
            try {
                P_STATUS = uri.getQueryParameter("P_STATUS");
                P_REQ_URL = uri.getQueryParameter("P_REQ_URL");
                P_TID = uri.getQueryParameter("P_TID");
//                P_MID = uri.getQueryParameter("P_MID");
                Log.d(TAG, "P_STATUS 결과값 => " + P_STATUS + " req_url : "+ P_REQ_URL+ " P_TID : "+ P_TID+ " P_MID : "+ P_MID);
                if("00".equals(P_STATUS)){
                    httpPay(P_REQ_URL,P_TID);
                    payHandler = new Handler(){
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            switch(msg.what){
                                case 00:
                                    showResult("완료");
                                    break;
                                default:
                                    showResult("실패");
                                    Log.d(TAG, qparam.toString());
                                    break;
                            }
                        }
                    };
                    qparam.clear();
                    return true;
                }else{
                    showResult("실패");
                    return false;
                }
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }


        if( !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("javascript:") )
        {
            Intent intent;

            try{
                Log.d("<INIPAYMOBILE>", "intent url : " + url);
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                Log.d("<INIPAYMOBILE>", "intent getDataString : " + intent.getDataString());
                Log.d("<INIPAYMOBILE>", "intent getPackage : " + intent.getPackage() );

                if(intent.getPackage() == null){
                    Log.d(TAG, "ISP 설치 요망");
                }
            } catch (URISyntaxException ex) {
                Log.e("<INIPAYMOBILE>", "URI syntax error : " + url + ":" + ex.getMessage());
                return false;
            }

            Uri uri = Uri.parse(intent.getDataString());
            intent = new Intent(Intent.ACTION_VIEW, uri);



            try{

                startActivity(intent);

	    			/*가맹점의 사정에 따라 현재 화면을 종료하지 않아도 됩니다.
	    			    삼성카드 기타 안심클릭에서는 종료되면 안되기 때문에
	    			    조건을 걸어 종료하도록 하였습니다.*/
                if( url.startsWith("ispmobile://"))
                {
                    Log.d(TAG, "엔드 ==> " +url.toString());
//                        finish();
                }

            }catch(ActivityNotFoundException e)
            {
                Log.e("INIPAYMOBILE", "INIPAYMOBILE, ActivityNotFoundException INPUT >> " + url);
                Log.e("INIPAYMOBILE", "INIPAYMOBILE, uri.getScheme()" + intent.getDataString());

                //ISP
                if( url.startsWith("ispmobile://"))
                {
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_ISP);
                    return false;
                }

                //현대앱카드
                else if( intent.getDataString().startsWith("hdcardappcardansimclick://"))
                {
                    DIALOG_CARDNM = "HYUNDAE";
                    Log.e("INIPAYMOBILE", "INIPAYMOBILE, 현대앱카드설치 ");
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_CARDAPP);
                    return false;
                }

                //신한앱카드
                else if( intent.getDataString().startsWith("shinhan-sr-ansimclick://"))
                {
                    DIALOG_CARDNM = "SHINHAN";
                    Log.e("INIPAYMOBILE", "INIPAYMOBILE, 신한카드앱설치 ");
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_CARDAPP);
                    return false;
                }

                //삼성앱카드
                else if( intent.getDataString().startsWith("mpocket.online.ansimclick://"))
                {
                    DIALOG_CARDNM = "SAMSUNG";
                    Log.e("INIPAYMOBILE", "INIPAYMOBILE, 삼성카드앱설치 ");
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_CARDAPP);
                    return false;
                }

                //롯데 모바일결제
                else if( intent.getDataString().startsWith("lottesmartpay://"))
                {
                    DIALOG_CARDNM = "LOTTE";
                    Log.e("INIPAYMOBILE", "INIPAYMOBILE, 롯데모바일결제 설치 ");
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_CARDAPP);
                    return false;
                }
                //롯데앱카드(간편결제)
                else if(intent.getDataString().startsWith("lotteappcard://"))
                {
                    DIALOG_CARDNM = "LOTTEAPPCARD";
                    Log.e("INIPAYMOBILE", "INIPAYMOBILE, 롯데앱카드 설치 ");
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_CARDAPP);
                    return false;
                }

                //KB앱카드
                else if( intent.getDataString().startsWith("kb-acp://"))
                {
                    DIALOG_CARDNM = "KB";
                    Log.e("INIPAYMOBILE", "INIPAYMOBILE, KB카드앱설치 ");
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_CARDAPP);
                    return false;
                }

                //하나SK카드 통합안심클릭앱
                else if( intent.getDataString().startsWith("hanaansim://"))
                {
                    DIALOG_CARDNM = "HANASK";
                    Log.e("INIPAYMOBILE", "INIPAYMOBILE, 하나카드앱설치 ");
                    view.loadData("<html><body></body></html>", "text/html", "euc-kr");
                    showDialog(DIALOG_CARDAPP);
                    return false;
                }

	    			/*
	    			//신한카드 SMART신한 앱
	    			else if( intent.getDataString().startsWith("smshinhanansimclick://"))
	    			{
	    				DIALOG_CARDNM = "SHINHAN_SMART";
	    				Log.e("INIPAYMOBILE", "INIPAYMOBILE, Smart신한앱설치");
	    				view.loadData("<html><body></body></html>", "text/html", "euc-kr");
	    				showDialog(DIALOG_CARDAPP);
				        return false;
	    			}
	    			*/

                /**
                 > 현대카드 안심클릭 droidxantivirusweb://
                 - 백신앱 : Droid-x 안드로이이드백신 - NSHC
                 - package name : net.nshc.droidxantivirus
                 - 특이사항 : 백신 설치 유무는 체크를 하고, 없을때 구글마켓으로 이동한다는 이벤트는 있지만, 구글마켓으로 이동되지는 않음
                 - 처리로직 : intent.getDataString()로 하여 droidxantivirusweb 값이 오면 현대카드 백신앱으로 인식하여
                 하드코딩된 마켓 URL로 이동하도록 한다.
                 */

                //현대카드 백신앱
                else if( intent.getDataString().startsWith("droidxantivirusweb"))
                {
                    /*************************************************************************************/
                    Log.d("<INIPAYMOBILE>", "ActivityNotFoundException, droidxantivirusweb 문자열로 인입될시 마켓으로 이동되는 예외 처리: " );
                    /*************************************************************************************/

                    Intent hydVIntent = new Intent(Intent.ACTION_VIEW);
                    hydVIntent.setData(Uri.parse("market://search?q=net.nshc.droidxantivirus"));
                    startActivity(hydVIntent);

                }


                //INTENT:// 인입될시 예외 처리
                else if( url.startsWith("intent://"))
                {

                    /**

                     > 삼성카드 안심클릭
                     - 백신앱 : 웹백신 - 인프라웨어 테크놀러지
                     - package name : kr.co.shiftworks.vguardweb
                     - 특이사항 : INTENT:// 인입될시 정상적 호출

                     > 신한카드 안심클릭
                     - 백신앱 : TouchEn mVaccine for Web - 라온시큐어(주)
                     - package name : com.TouchEn.mVaccine.webs
                     - 특이사항 : INTENT:// 인입될시 정상적 호출

                     > 농협카드 안심클릭
                     - 백신앱 : V3 Mobile Plus 2.0
                     - package name : com.ahnlab.v3mobileplus
                     - 특이사항 : 백신 설치 버튼이 있으며, 백신 설치 버튼 클릭시 정상적으로 마켓으로 이동하며, 백신이 없어도 결제가 진행이 됨

                     > 외환카드 안심클릭
                     - 백신앱 : TouchEn mVaccine for Web - 라온시큐어(주)
                     - package name : com.TouchEn.mVaccine.webs
                     - 특이사항 : INTENT:// 인입될시 정상적 호출

                     > 씨티카드 안심클릭
                     - 백신앱 : TouchEn mVaccine for Web - 라온시큐어(주)
                     - package name : com.TouchEn.mVaccine.webs
                     - 특이사항 : INTENT:// 인입될시 정상적 호출

                     > 하나SK카드 안심클릭
                     - 백신앱 : V3 Mobile Plus 2.0
                     - package name : com.ahnlab.v3mobileplus
                     - 특이사항 : 백신 설치 버튼이 있으며, 백신 설치 버튼 클릭시 정상적으로 마켓으로 이동하며, 백신이 없어도 결제가 진행이 됨

                     > 하나카드 안심클릭
                     - 백신앱 : V3 Mobile Plus 2.0
                     - package name : com.ahnlab.v3mobileplus
                     - 특이사항 : 백신 설치 버튼이 있으며, 백신 설치 버튼 클릭시 정상적으로 마켓으로 이동하며, 백신이 없어도 결제가 진행이 됨

                     > 롯데카드
                     - 백신이 설치되어 있지 않아도, 결제페이지로 이동

                     */

                    /*************************************************************************************/
                    Log.d("<INIPAYMOBILE>", "Custom URL (intent://) 로 인입될시 마켓으로 이동되는 예외 처리: " );
                    /*************************************************************************************/

                    try {

                        Intent excepIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        String packageNm = excepIntent.getPackage();

                        Log.d("<INIPAYMOBILE>", "excepIntent getPackage : " + packageNm );

                        excepIntent = new Intent(Intent.ACTION_VIEW);
							/*
								가맹점별로 원하시는 방식으로 사용하시면 됩니다.
								market URL
								market://search?q="+packageNm => packageNm을 검색어로 마켓 검색 페이지 이동
								market://search?q=pname:"+packageNm => packageNm을 패키지로 갖는 앱 검색 페이지 이동
								market://details?id="+packageNm => packageNm 에 해당하는 앱 상세 페이지로 이동
							*/
                        excepIntent.setData(Uri.parse("market://search?q="+packageNm));

                        startActivity(excepIntent);

                    } catch (URISyntaxException e1) {
                        Log.e("<INIPAYMOBILE>", "INTENT:// 인입될시 예외 처리  오류 : " + e1 );
                    }

                }
            }

        }
        else
        {
//            view.loadUrl(url);
            return false;
        }
        return true;

    }
}
