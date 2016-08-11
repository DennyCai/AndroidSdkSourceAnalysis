Html源码分析
==

> 代码版本：**Android API 23**

## 1.简介

Html能够通过Html标签来为文字设置样式，让TextView显示富文本信息，其只支持部分标签不是全部，具体支持哪些标签将分析中揭晓。

## 2.使用方法
申明一段带有Html标签的字符串，然后调用`Html.fromHtml`方法就能够根据标签设置对应的样式。

```java
	String htmlString =
        "<font color='#ff0000'>颜色</font><br/>" +
        "<a href='http://www.baidu.com'>链接</a><>br/>" +
        "<big>大字体</big><br/>"+
        "<small>小字体</small><br/>"+
        "<b>加粗</b><br/>"+
        "<i>斜体</i><br/>" +
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
    tv.setText(Html.fromHtml(htmlString));
```
运行效果:

![Html](https://github.com/DennyCai/AndroidSdkSourceAnalysis/blob/master/img/show.jpg?raw=true)

在Demo中发现使用`<img>`标签都显示小方块,解决这个问题的办法是调用`Html.fromHtml(String,Html.ImageGetter,Html.TagHandler)`的重载方法，并传入自定义的`Html.ImageGetter`对象，重写`getDrawable`方法，代码如下：

```java
	Html.ImageGetter getter = new Html.ImageGetter() {
        @Override
        public Drawable getDrawable(String source) {
            int id = getResources().getIdentifier(source,"mipmap",getPackageName());
            Drawable drawable = getResources().getDrawable(id);
            //必须设置手动设置drawable大小，否则无法显示
            drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
            return drawable;
        }
    };
    textView.setText( Html.fromHtml(htmlString,getter,null));
```
运行效果:

![Html](https://github.com/DennyCai/AndroidSdkSourceAnalysis/blob/master/img/showimg.png?raw=true)

原生支持的Html标签优先，为了方便扩张，我们可以通过自定义`Html.TagHandler`来支持自定义标签显示效果，代码如下：

```java
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
    textView.setText( Html.fromHtml("<custom>自定义标签</custom>",null,tagHandler));
```

运行效果：

![TagHandler](https://github.com/DennyCai/AndroidSdkSourceAnalysis/blob/master/img/custmtag.png?raw=true)

使用`Html.toHtml`方法能够将带有样式效果的Spanned文本对象生成对应的Html格式，但部分样式可能会丢失，下面为WebView显示效果，部分效果与上面TextView显示的效果有差异，代码如下：

```java
	webView.loadData(Html.toHtml(Html.fromHtml(htmlString)),"text/html", "utf-8");
```

运行效果：

![toHtml](https://github.com/DennyCai/AndroidSdkSourceAnalysis/blob/master/img/tohtml.png?raw=true)

返回字符串：

```text
<p dir="ltr"><font color ="#ff0000">&#39068;&#33394;</font><br>
<a href="http://www.baidu.com">&#38142;&#25509;</a><br>
&#22823;&#23383;&#20307;<br>
&#23567;&#23383;&#20307;<br>
<b>&#21152;&#31895;</b><br>
<i>&#26012;&#20307;</i></p>
<p dir="ltr"><b>&#26631;&#39064;&#19968;</b></p>
<p dir="ltr"><b>&#26631;&#39064;&#20108;</b></p>
<p dir="ltr"><b>&#26631;&#39064;&#19977;</b></p>
<p dir="ltr"><b>&#26631;&#39064;&#22235;</b></p>
<p dir="ltr"><img src="ic_launcher"></p>
<blockquote><p dir="ltr">&#24341;&#29992;<br>
</p>
</blockquote>
<p dir="ltr"><br>
&#22359;</p>
<p dir="ltr"><u>&#19979;&#21010;&#32447;</u><br>
<sup>&#19978;&#26631;</sup>&#27491;&#24120;&#23383;&#20307;<sub>&#19979;&#26631;</sub><br>
<sup><sup><b><u>&#32452;</u></b></sup></sup><sup><b><u>&#21512;</u></b></sup><b><u>&#26679;&#24335;</u></b><sub><b><u>&#23383;</u></b></sub><sub><sub><b><u>&#20307;</u></b></sub></sub></p>
```

`Html.escapeHtml`方法则是将Html标签进行转译,例如`<p dir="ltr">`变转译成`&lt;p dir="ltr"&gt;`。

## 3、原理分析

### 3.1、初探Html类

```java
/**
 * 该类将HTML处理成带样式的文本，但不支持所有的HTML标签
 */
public class Html {
    
    /**
     * 为<img>标签提供图片检索功能
     */
    public static interface ImageGetter {
        /**
         * 当HTML解析器解析到<img>标签时，source参数为标签中的src的属性值，
         * 返回值必须为Drawable;如果返回null则会使用小方块来显示，如前面所见，
         * 并需要调用Drawable.setBounds()方法来设置大小，否则无法显示图片。
         * @param source:
         */
        public Drawable getDrawable(String source);
    }

    /**
     * HTML标签解析扩展接口
     */
    public static interface TagHandler {
        /**
         * 当解析器解析到本身不支持或用户自定义的标签时，该方法会被调用
         * @param opening:标签是否打开
         * @param tag:标签名
         * @param output:截止到当前标签，解析到的文本内容
         * @param xmlReader:解析器对象
         */
        public void handleTag(boolean opening, String tag,
                                 Editable output, XMLReader xmlReader);
    }

    private Html() { }

    /**
     * 返回样式文本，所有<img>标签都会显示为一个小方块
     * 使用TagSoup库处理HTML
     * @param source:带有html标签字符串
     */
    public static Spanned fromHtml(String source) {
        return fromHtml(source, null, null);
    }

    /**
     * 可传入ImageGetter来获取图片源，TagHandler添加支持其他标签
     */
    public static Spanned fromHtml(String source, ImageGetter imageGetter,
                                   TagHandler tagHandler) {
        .....
    }

    /**
     * 将带样式文本反向解析成带Html的字符串，注意这个方法并不是还原成fromHtml接收的带Html标签文本
     */
    public static String toHtml(Spanned text) {
        StringBuilder out = new StringBuilder();
        withinHtml(out, text);
        return out.toString();
    }

    /**
     * 返回转译标签后的字符串
     */
    public static String escapeHtml(CharSequence text) {
        StringBuilder out = new StringBuilder();
        withinStyle(out, text, 0, text.length());
        return out.toString();
    }

    /**
     * 懒加载HTML解析器的Holder
     * a) zygote对其进行预加载
     * b) 直到需要的时候才加载
     */
    private static class HtmlParser {
        private static final HTMLSchema schema = new HTMLSchema();
    }

```

### 3.2、从fromHtml开始
Html类主要方法就`4`个，功能也简单，生成带样式的`fromHtml`方法最总都是调用重载3个参数的方法。
```java
public static Spanned fromHtml(String source, ImageGetter imageGetter,
                                   TagHandler tagHandler) {
    //初始化解析器
    Parser parser = new Parser();
    try {
        parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
    } catch (org.xml.sax.SAXNotRecognizedException e) {
        // 不该出现的异常
        throw new RuntimeException(e);
    } catch (org.xml.sax.SAXNotSupportedException e) {
        // 不该出现的异常
        throw new RuntimeException(e);
    }
    HtmlToSpannedConverter converter =
            new HtmlToSpannedConverter(source, imageGetter, tagHandler,parser);
    return converter.convert();
}
```

源代码中并没有包含Parser对象，根据`import org.ccil.cowan.tagsoup.Parser;`和`fromHtml`注释可知，Html解析器是使用[Tagsoup](http://home.ccil.org/~cowan/XML/tagsoup/)库来解析Html标签，为什么会选择该库，进行一番搜索得知[Tagsoup](http://home.ccil.org/~cowan/XML/tagsoup/)是兼容`SAX`的解析器，我们知道对XML常见的的解析方式还有`DOM`、Android系统中还使用`PULL`解析与`SAX`同样是基于事件驱动模型，之所有使用tagsoup是因为该库可以良好的解析Html，我们都知道Html有时候并不像XML那样标签都需要闭合，例如`<br>`也是一个有效的标签，但是XML中则是不良格式。

