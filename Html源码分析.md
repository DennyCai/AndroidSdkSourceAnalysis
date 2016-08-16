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
        //配置解析Html模式
        parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
    } catch (org.xml.sax.SAXNotRecognizedException e) {
        throw new RuntimeException(e);
    } catch (org.xml.sax.SAXNotSupportedException e) {
        throw new RuntimeException(e);
    }
    //初始化真正的解析器
    HtmlToSpannedConverter converter =
            new HtmlToSpannedConverter(source, imageGetter, tagHandler,parser);
    return converter.convert();
}
```

源代码中并没有包含Parser对象，根据`import org.ccil.cowan.tagsoup.Parser;`和`fromHtml`注释可知，HTML解析器是使用[Tagsoup](http://home.ccil.org/~cowan/XML/tagsoup/)库来解析HTML标签，为什么会选择该库，进行一番搜索得知[Tagsoup](http://home.ccil.org/~cowan/XML/tagsoup/)是兼容`SAX`的解析器，我们知道对XML常见的的解析方式还有`DOM`、Android系统中还使用`PULL`解析与`SAX`同样是基于事件驱动模型，之所有使用tagsoup是因为该库可以将HTML转化为XML，我们都知道HTML有时候并不像XML那样标签都需要闭合，例如`<br>`也是一个有效的标签，但是XML中则是不良格式。详情可见官方网站，但是好像没有开发文档，这里就不详细说明，只关注`SAX`解析过程。

### 3.3、HtmlToSpannedConverter原理

```java
class HtmlToSpannedConverter implements ContentHandler {

    private static final float[] HEADER_SIZES = {
        1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
    };

    private String mSource;
    private XMLReader mReader;
    private SpannableStringBuilder mSpannableStringBuilder;
    private Html.ImageGetter mImageGetter;
    private Html.TagHandler mTagHandler;

    public HtmlToSpannedConverter(
            String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler,
            Parser parser) {
        mSource = source;//html文本
        mSpannableStringBuilder = new SpannableStringBuilder();//用于存放标签中的字符串
        mImageGetter = imageGetter;//图片加载器
        mTagHandler = tagHandler;//自定义标签器
        mReader = parser;//解析器
    }

    public Spanned convert() {
        //设置内容处理器
        mReader.setContentHandler(this);
        try {
            //开始解析
            mReader.parse(new InputSource(new StringReader(mSource)));
        } catch (IOException e) {
            // We are reading from a string. There should not be IO problems.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }
        //省略
        ...
        ...
        return mSpannableStringBuilder;
    }
```
通过上面代码可以发现，`SpannableStringBuilder`是用来存放解析html标签中的字符串，类似`StringBuilder`，但它附带有样式的字符串。重点关注`convert`里面的`setContentHandler`方法，该方法接收的是`ContentHandler`接口，使用过`SAX`解析的读者应该不陌生，该接口定义了一系列`SAX`解析事件的方法。
```java
public interface ContentHandler
{
    //设置文档定位器
    public void setDocumentLocator (Locator locator);
    //文档开始解析事件
    public void startDocument ()
    throws SAXException;
    //文档结束解析事件
    public void endDocument()
    throws SAXException;
    //解析到命名空间前缀事件
    public void startPrefixMapping (String prefix, String uri)
    throws SAXException;
    //结束命名空间事件
    public void endPrefixMapping (String prefix)
    throws SAXException;
    //解析到标签事件
    public void startElement (String uri, String localName,
                  String qName, Attributes atts)
    throws SAXException;
    //标签结束事件
    public void endElement (String uri, String localName,
                String qName)
    throws SAXException;
    //标签中内容事件
    public void characters (char ch[], int start, int length)
    throws SAXException;
    //可忽略的空格事件
    public void ignorableWhitespace (char ch[], int start, int length)
    throws SAXException;
    //处理指令事件
    public void processingInstruction (String target, String data)
    throws SAXException;
    //忽略标签事件
    public void skippedEntity (String name)
    throws SAXException;
}
```
对应`HtmlToSpannedConverter`中的实现。
```java
public void setDocumentLocator(Locator locator) {}
public void startDocument() throws SAXException {}
public void endDocument() throws SAXException {}
public void startPrefixMapping(String prefix, String uri) throws SAXException {}
public void endPrefixMapping(String prefix) throws SAXException {}
public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
    handleStartTag(localName, attributes);
}
public void endElement(String uri, String localName, String qName) throws SAXException {
    handleEndTag(localName);
}
public void characters(char ch[], int start, int length) throws SAXException {
    //忽略
    ...
}
public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {}
public void processingInstruction(String target, String data) throws SAXException {}
public void skippedEntity(String name) throws SAXException {}
```

我们发现该类中只实现了`startElement`，`endElement`，`characters`这三个方法，所以只关心标签的类型和标签里的字符。然后调用`mReader.parse`方法，开始对HTML进行解析。解析的事件流如下：
`startElement` -> `characters` -> `endElement`
`startElemnt`里面调用的是`handleStartTag`方法，`endElement`则是调用`handleEndTag`方法。

```java
/**
 * @param tag：标签类型
 * @param attributes：属性值
 * 例如遇到<font color='#FFFFFF'>标签，tag="font",attributes={"color":"#FFFFFF"}
 */
private void handleStartTag(String tag, Attributes attributes) {
    if (tag.equalsIgnoreCase("br")) {
        // 我们不需要关心br标签是否有闭合，因为Tagsoup会帮我们处理
    } else if (tag.equalsIgnoreCase("p")) {
        handleP(mSpannableStringBuilder);
    } else if (tag.equalsIgnoreCase("div")) {
        handleP(mSpannableStringBuilder);
    } else if (tag.equalsIgnoreCase("strong")) {
        start(mSpannableStringBuilder, new Bold());
    } else if (tag.equalsIgnoreCase("b")) {
        start(mSpannableStringBuilder, new Bold());
    } else if (tag.equalsIgnoreCase("em")) {
        start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("cite")) {
        start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("dfn")) {
        start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("i")) {
        start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("big")) {
        start(mSpannableStringBuilder, new Big());
    } else if (tag.equalsIgnoreCase("small")) {
        start(mSpannableStringBuilder, new Small());
    } else if (tag.equalsIgnoreCase("font")) {
        startFont(mSpannableStringBuilder, attributes);
    } else if (tag.equalsIgnoreCase("blockquote")) {
        handleP(mSpannableStringBuilder);
        start(mSpannableStringBuilder, new Blockquote());
    } else if (tag.equalsIgnoreCase("tt")) {
        start(mSpannableStringBuilder, new Monospace());
    } else if (tag.equalsIgnoreCase("a")) {
        startA(mSpannableStringBuilder, attributes);
    } else if (tag.equalsIgnoreCase("u")) {
       start(mSpannableStringBuilder, new Underline());
    } else if (tag.equalsIgnoreCase("sup")) {
        start(mSpannableStringBuilder, new Super());
    } else if (tag.equalsIgnoreCase("sub")) {
        start(mSpannableStringBuilder, new Sub());
    } else if (tag.length() == 2 &&
               Character.toLowerCase(tag.charAt(0)) == 'h' &&
               tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
        handleP(mSpannableStringBuilder);
         start(mSpannableStringBuilder, new Header(tag.charAt(1) - '1'));
    } else if (tag.equalsIgnoreCase("img")) {
        startImg(mSpannableStringBuilder, attributes, mImageGetter);
    } else if (mTagHandler != null) {
        mTagHandler.handleTag(true, tag, mSpannableStringBuilder, mReader);
    }
}
//标签结束
private void handleEndTag(String tag) {
        if (tag.equalsIgnoreCase("br")) {
            handleBr(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("p")) {
            handleP(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("div")) {
            handleP(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("strong")) {
            end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("b")) {
            end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("em")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("cite")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("dfn")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("i")) {
            end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("big")) {
            end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
        } else if (tag.equalsIgnoreCase("small")) {
            end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
        } else if (tag.equalsIgnoreCase("font")) {
            endFont(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            handleP(mSpannableStringBuilder);
            end(mSpannableStringBuilder, Blockquote.class, new QuoteSpan());
        } else if (tag.equalsIgnoreCase("tt")) {
            end(mSpannableStringBuilder, Monospace.class,
                    new TypefaceSpan("monospace"));
        } else if (tag.equalsIgnoreCase("a")) {
            endA(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("u")) {
            end(mSpannableStringBuilder, Underline.class, new UnderlineSpan());
        } else if (tag.equalsIgnoreCase("sup")) {
            end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
        } else if (tag.equalsIgnoreCase("sub")) {
            end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            handleP(mSpannableStringBuilder);
            endHeader(mSpannableStringBuilder);
        } else if (mTagHandler != null) {
            mTagHandler.handleTag(false, tag, mSpannableStringBuilder, mReader);
        }
    }
//标签内容 ch[]:存放字符数组 start:开始位置，lenght:长度
public void characters(char ch[], int start, int length) throws SAXException {
        StringBuilder sb = new StringBuilder();
        /*
         * 忽略开头的空格或连续两个空格
         * 换行符视为空格符
         */
        for (int i = 0; i < length; i++) {
            char c = ch[i + start];

            if (c == ' ' || c == '\n') {
                char pred;
                int len = sb.length();

                if (len == 0) {
                    len = mSpannableStringBuilder.length();

                    if (len == 0) {//开头为空格符
                        pred = '\n';
                    } else {//获取上一个字符
                        pred = mSpannableStringBuilder.charAt(len - 1);
                    }
                } else {//获取上一个字符
                    pred = sb.charAt(len - 1);
                }

                if (pred != ' ' && pred != '\n') {//判断是否为连续空格
                    sb.append(' ');
                }
            } else {//不是空格或不连续为空格则添加字符
                sb.append(c);
            }
        }

        mSpannableStringBuilder.append(sb);
    }    
```

从上面方法中我们可以总结出支持的HTML标签列表
* br
* p
* div
* strong
* b
* em
* cite
* dfn
* i
* big
* small
* font
* blockquote
* tt
* monospace
* a
* u
* sup
* sub
* h1-h6
* img

### 3、4 标签是如何处理的

1、br标签

这里分析如何处理`<br>`标签，在`handleStartTag`方法中可以发现br标签直接被忽略了，在`handleEndTag`方法中才被真正处理。
```java
if (tag.equalsIgnoreCase("br")) {
    handleBr(mSpannableStringBuilder);
}
//代码很简单，直接加换行符
private static void handleBr(SpannableStringBuilder text) {
    text.append("\n");
}

```

2、p标签 