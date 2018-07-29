package org.hypergo.explore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.HashMap;
import org.hypergo.explore.R;

public class MainActivity extends Activity {
  
  private RelativeLayout rl;
  private FrameLayout fl;
  
  private WebView wv;
  private ImageButton ib;
  private Button btn_go, btn_del;
  private EditText et;
  private ProgressBar pb;
  
  private String state = "NOTHING"; //NOTHING为无加载，LOADING为正在加载，FINISH为完成加载
  private String url;
  private HashMap<String, String> titles = new HashMap<String, String>();
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    this.rl = (RelativeLayout) findViewById(R.id.titlebar_layout);
    this.fl = (FrameLayout) findViewById(R.id.video_player);
    
    this.wv = (WebView) findViewById(R.id.wv);
    this.ib = (ImageButton) findViewById(R.id.icon);
    this.btn_go = (Button) findViewById(R.id.go);
    this.btn_del = (Button) findViewById(R.id.del);
    this.et = (EditText) findViewById(R.id.et);
    this.pb = (ProgressBar) findViewById(R.id.pb);
    
    this.et.setOnFocusChangeListener(new FocusChangeListener());
    this.et.setOnEditorActionListener(new EditorActionListener());
    
    this.btn_go.setOnClickListener(new OnClickListener());
    this.btn_del.setOnClickListener(new OnClickListener());
    
    //初始化标题栏
    this.initTitleBarLayout(rl);
    
    //初始化webView
    this.initWebView(wv);
    
    this.wv.loadUrl("https://m.baidu.com");
    
  }
  
  private void initWebView(WebView wv) {
    WebSettings webSettings = wv.getSettings();
    
    wv.setWebViewClient(new MyWebViewClient());
    
    wv.setWebChromeClient(new MyWebChromeClient());
    
    //设置开启http和https混合模式, 防止无法加载http或https图片
    if (Build.VERSION.SDK_INT >= 21) {
      webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }
    
    //设置多窗口
    webSettings.supportMultipleWindows();
    
    //设置支持JS
    webSettings.setJavaScriptEnabled(true);
    
    //设置支持JS打开新窗口
    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
    
    //设置支持插件
    //webSettings.setPluginsEnabled(true);
    
    //设置此属性，可任意比例缩放
    webSettings.setUseWideViewPort(true);
    webSettings.setLoadWithOverviewMode(true);
    
    //使页面可自由缩放
    webSettings.setBuiltInZoomControls(true);
    webSettings.setSupportZoom(true);
    
    //隐藏放大放小按钮
    webSettings.setDisplayZoomControls(false);
    
    //自动加载图片
    webSettings.setLoadsImagesAutomatically(true);//最后加载图片
    webSettings.setBlockNetworkImage(false);
    
    //设置图片适应屏幕尺寸
    webSettings.setUseWideViewPort(false);
    
    //支持访问文件
    webSettings.setAllowFileAccess(true);
    
    //开启缓存
    webSettings.setAppCacheEnabled(true);
    webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
    webSettings.setDatabaseEnabled(true);
    webSettings.setDomStorageEnabled(true);
    
    //隐藏滚动条
    //wv.setVerticalScrollBarEnabled(false);
    wv.setHorizontalScrollBarEnabled(false);
    
    //设置支持获得焦点
    wv.requestFocusFromTouch();
  }
  
  private void initTitleBarLayout(RelativeLayout rl) {
    //使imageButton显示在最前面
    this.ib.bringToFront();
    
    //重新绘制布局
    rl.requestLayout();
    rl.invalidate();
  }
  
  private void setFavicon(Bitmap icon) {
    if (icon == null) {
      this.ib.setImageDrawable(getResources().getDrawable(R.drawable.round));
    }
    else{
      this.ib.setImageBitmap(icon);
    }
  }
  
  //尝试修复残缺的url
  private String fixUrl(String url) {
    String https = ("https://" + url).toString();
    String http = ("http://" + url).toString();
    
    if (Patterns.WEB_URL.matcher(url).matches()) {
      //为了避免无法访问未备案的网站，所以不能直接返回猜测URL
      if (url.startsWith("http://") || url.startsWith("https://")) {
        return url;
      } else {
        if (Patterns.WEB_URL.matcher(https).matches()) {
          return https;
        } else if (Patterns.WEB_URL.matcher(http).matches()) {
          return http;
        } else {
          return URLUtil.guessUrl(url); //猜测URL
        }
      }
    } else {
      return "https://m.baidu.com/s?word=" + url;
    }
  }
 
  //按下返回键返回上一页
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_BACK) && this.wv.canGoBack()) {
      String url = this.wv.getUrl().toString();
      
      this.wv.goBack();
      
      //如果遇到迷之无法返回上一页
      if (this.wv.getUrl().toString().equals(url)) {
        this.wv.goBack();
      }
      
      return true;
    }
    
    return super.onKeyDown(keyCode, event);
  }
  
  
  
  class MyWebViewClient extends WebViewClient {
    
    @Override
    public boolean shouldOverrideUrlLoading(WebView wv, String url) {
      if (url == null) {
        return false;
      }
      
      try {
        if ((!url.startsWith("http://")) && (!url.startsWith("https://"))) {
          //拉起APP
          Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
          MainActivity.this.startActivity(intent);
          
          return true;
        } else {
          //处理http和https开头的url
          wv.loadUrl(url);
          return true;
        }
      } catch (Exception e) {
        //没有这个APP
        return true;
      }
    }
    
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);
      
      MainActivity.this.setFavicon(favicon);
      
      //记录当前网站URL用于网站后退标题设置
      MainActivity.this.url = url;
    }
    
    @Override
    public void onPageFinished(WebView view, String url) {
      String title = MainActivity.this.titles.get(url);
      
      //在输入框没有焦点时设置网站标题
      if (!TextUtils.isEmpty(title) && !MainActivity.this.et.hasFocus()) {
        MainActivity.this.et.setText(title);
      }
    }
  }
  
  class MyWebChromeClient extends WebChromeClient {
    
    @Override
    public void onReceivedTitle(WebView view, String title) {
      super.onReceivedTitle(view, title);
      
      //记录网站标题
      if (TextUtils.isEmpty(title)) {
        title = "";
      }
      
      MainActivity.this.titles.put(MainActivity.this.url, title);
    }
    
    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
      super.onReceivedIcon(view, icon);
      
      MainActivity.this.setFavicon(icon);
    }
    
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
      if (newProgress == 100) {
        MainActivity.this.state = "FINISH";
        MainActivity.this.et.setText("");
        MainActivity.this.btn_go.setText("Go!");
        MainActivity.this.btn_go.setTextSize(15);
        MainActivity.this.pb.setVisibility(View.INVISIBLE); //加载完网页进度条消失
      } else {
        MainActivity.this.state = "LOADING";
        MainActivity.this.et.setText("正在载入...");
        MainActivity.this.btn_go.setText("×");
        MainActivity.this.btn_go.setTextSize(20);
        MainActivity.this.pb.setVisibility(View.VISIBLE); //开始加载网页时显示进度条
        MainActivity.this.pb.setProgress(newProgress); //设置进度值
      }
    }
  }
  
  
  
  class OnClickListener implements View.OnClickListener {
  
    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.del: //清空输入框
          MainActivity.this.et.setText("");
        break;
        
        case R.id.go: //GO
          String etUrl = MainActivity.this.et.getText().toString();
          String wvUrl = MainActivity.this.wv.getUrl().toString();
          String title = MainActivity.this.wv.getTitle().toString();
          String state = MainActivity.this.state;
          
          //如果网站正在加载
          if (state.equals("LOADING")) {
            MainActivity.this.wv.loadUrl(""); //停止加载
            MainActivity.this.et.setText("about:blank");
            MainActivity.this.state = "NOTHING"; //设置状态为无加载
          //如果输入框内容不为空且不等于网站标题且不等于网站URL
          } else if (!TextUtils.isEmpty(etUrl) && !title.equals(etUrl) && !etUrl.equals(wvUrl)) {
            MainActivity.this.wv.loadUrl(MainActivity.this.fixUrl(etUrl));
          } else {
            
          }
          
          //使输入框失去焦点
          MainActivity.this.et.setFocusable(false);
          MainActivity.this.et.setFocusableInTouchMode(false);
          MainActivity.this.et.clearFocus();
          
          //强制关闭软键盘
          InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(MainActivity.this.getWindow().getDecorView().getWindowToken(), 0);
          
          //重新允许输入框获得焦点
          MainActivity.this.et.setFocusable(true);
          MainActivity.this.et.setFocusableInTouchMode(true);
        break;
      }
    }
  
  }
  
  class FocusChangeListener implements View.OnFocusChangeListener {
    
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      
      if (hasFocus) {
        //获得焦点即显示删除按钮
        MainActivity.this.btn_del.setVisibility(View.VISIBLE);
        ((EditText) v).setPadding(
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 31, MainActivity.this.getResources().getDisplayMetrics()),
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, MainActivity.this.getResources().getDisplayMetrics()),
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, MainActivity.this.getResources().getDisplayMetrics()),
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, MainActivity.this.getResources().getDisplayMetrics())
        );
        
        //显示网站URL
        ((EditText) v).setText(MainActivity.this.wv.getUrl());
        
        //获得焦点时即选中全文
        Spannable content = ((EditText) v).getText();
        Selection.selectAll(content);
        
        ((EditText) v).setFocusable(true);
        ((EditText) v).setFocusableInTouchMode(true);
        ((EditText) v).requestFocus();
        
        //强制拉起软键盘，防止因为setPadding()而导致软键盘不会自动拉起
        InputMethodManager imm = (InputMethodManager) ((EditText) v).getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(((EditText) v), 0);
      } else {
        
        //失去焦点则隐藏删除按钮
        MainActivity.this.btn_del.setVisibility(View.INVISIBLE);
        ((EditText) v).setPadding(
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 31, MainActivity.this.getResources().getDisplayMetrics()),
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, MainActivity.this.getResources().getDisplayMetrics()),
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, MainActivity.this.getResources().getDisplayMetrics()),
          (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, MainActivity.this.getResources().getDisplayMetrics())
        );
        
        //显示网站标题
        ((EditText) v).setText(MainActivity.this.wv.getTitle().toString());
      }
    }
  }
  
  class EditorActionListener implements TextView.OnEditorActionListener {
    
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      if (
        actionId == EditorInfo.IME_ACTION_SEND ||
        actionId == EditorInfo.IME_ACTION_DONE ||
        (
          event != null &&
          event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
          event.getAction() == KeyEvent.ACTION_DOWN
        )
        ) {
        String url = MainActivity.this.et.getText().toString();
        
        //如果输入框内容不为空
        if (!TextUtils.isEmpty(url)) {
          MainActivity.this.wv.loadUrl(MainActivity.this.fixUrl(url));
        }
        
        //使输入框失去焦点
        MainActivity.this.et.setFocusable(false);
        MainActivity.this.et.setFocusableInTouchMode(false);
        MainActivity.this.et.clearFocus();
        
        //强制关闭软键盘
        InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(MainActivity.this.getWindow().getDecorView().getWindowToken(), 0);
        
        //重新允许输入框获得焦点
        MainActivity.this.et.setFocusable(true);
        MainActivity.this.et.setFocusableInTouchMode(true);
      }
      
      return false;
    }
  }
  
}
