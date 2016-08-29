package com.denny.demo;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BulletSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Xml;
import android.webkit.WebView;
import android.widget.TextView;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;

public class MainActivity extends AppCompatActivity implements ContentHandler {

    strictfp
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.tv);
//        WebView wv = (WebView) findViewById(R.id.wv);
//        startActivity();
        String htmlString =
                "<font color='#ff0000'> 颜     5&nbsp;&nbsp;&nbsp;3色</font><br/>" +
                        "<a href='http://www.baidu.com'>  链接</a><>br/>" +
                        "<big>大字体   </big><br/>"+
                        "<small>小 字体</small>"+
                        "<b> 加  粗</b><br>"+
                        "<i>斜\n 体</i><br/>" +
                        "<h1>标题一</h1>" +
                        "<h2>标题二</h2>" +
                        "<h3>标题三</h3>" +
                        "<h4>标题四</h4>" +
                        "<img src='ic_launcher'/>" +
                        "<blockquote>引用</blockquote>" +
                        "<div>块</div>" +
                        "<u>下划线</u><br/>" +
                        "<sup>上标</sup>正常字体<sub>下标</sub><br/>" +
                        "<u><b><font color='@holo_blue_light'><sup><sup>组</sup>合</sup><big>样式</big><sub>字<sub>体</sub></sub></font></b></u>";
        Html.ImageGetter getter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                System.out.println(source);
                int id = getResources().getIdentifier(source,"mipmap",getPackageName());
                Drawable drawable = getResources().getDrawable(id);
                //必须设置手动设置drawable大小，否则无法显示
                drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
                return drawable;
            }
        };
//        TypefaceSpan span
//        Color.getHtmlColor()
        Html.TagHandler tagHandler = new Html.TagHandler() {
            int start = -1;
            int end = -1;
            @Override
            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                if(opening){
                    if(tag.equals("custom")) {
                        start = output.length();
                    }
                }else{
                    if(tag.equals("custom")&&start!=-1){
                        end = output.length();
                        BulletSpan bullet = new BulletSpan(10);
                        output.setSpan(bullet,start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        };
//        Spannable.SPAN_PARAGRAPH
//        "<br/><custom>自定义标签</custom>"
        Spanned
        textView.setText( Html.toHtml(Html.fromHtml(htmlString,getter,tagHandler)));
//        wv.loadData(Html.toHtml(Html.fromHtml(htmlString)),"text/html", "utf-8");
//        System.out.println(Html.escapeHtml(Html.toHtml(Html.fromHtml(htmlString))));
//        Parser parser = new Parser();
//        try {
//            parser.setProperty(Parser.schemaProperty, new HTMLSchema());
//        } catch (SAXNotRecognizedException e) {
//            e.printStackTrace();
//        } catch (SAXNotSupportedException e) {
//            e.printStackTrace();
//        }
//        parser.setContentHandler(this);
//        try {
//            parser.parse(new InputSource(new StringReader(htmlString)));
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (SAXException e) {
//            e.printStackTrace();
//        }
//        Spanned spanned = Html.fromHtml(htmlString);
//        Object[] ps = spanned.getSpans(0,spanned.length(), ParagraphStyle.class);

    }

    @Override
    public void setDocumentLocator(Locator locator) {

    }

    @Override
    public void startDocument() throws SAXException {

    }

    @Override
    public void endDocument() throws SAXException {

    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {

    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        System.out.println("startElement:"+localName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        System.out.println("endElement:"+localName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        System.out.println("ch:"+new String(ch));
        System.out.println("start:"+start);
        System.out.println("length:"+length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {

    }

    @Override
    public void skippedEntity(String name) throws SAXException {

    }
}
