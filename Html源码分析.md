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
                "<img src='https://github.com/fluidicon.png'/>" +
                "<blockquote>引用</blockquote>" +
                "<div>块</div>" +
                "<u>下划线</u><br/>" +
                "<sup>上标</sup>正常字体<sub>下标</sub><br/>" +
                "<u><b><font color='@holo_blue_light'><sup><sup>组</sup>合</sup><big>样式</big><sub>字<sub>体</sub></sub></font></b></u>";
        tv.setText(Html.fromHtml(htmlString));
    }
```
运行效果:

![Html](https://github.com/DennyCai/AndroidSdkSourceAnalysis/blob/master/img/show.jpg?raw=true)