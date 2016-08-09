Html源码分析
==
> 代码版本：Android API 23
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

`Html.escapeHtml`方法则是把Html标签去除，只返回转译后的字符串。

## 3、原理分析
