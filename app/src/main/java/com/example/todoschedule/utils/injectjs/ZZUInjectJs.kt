package com.example.mykotlinapplication.utils.injectjs

import android.util.Log
import android.webkit.WebView

fun WebView.injectHeadRepairScript() {
    val headContent = buildHeadContent()
    val script = """
        javascript:(function() {
            if (window.__scriptExecuted) return;
            window.__scriptExecuted = true;
            
            // 修复1: 确保jQuery加载并保留
            function loadjQuery() {
                return new Promise((resolve) => {
                    if (typeof jQuery === 'undefined') {
                        const jquery = document.createElement('script');
                        jquery.src = '/eams/static/scripts/jquery/jquery.js';
                        jquery.onload = function() {
                            // 修复2: 恢复
                            if (typeof $ === 'undefined') {
                                window.$ = jQuery;
                            }
                            // 修复3: 确保UI组件加载
                            const uiCore = document.createElement('script');
                            uiCore.src = '/eams/static/scripts/jquery/jquery.ui.core.js';
                            uiCore.onload = resolve;
                            document.head.appendChild(uiCore);
                        };
                        document.head.appendChild(jquery);
                    } else {
                        resolve();
                    }
                });
            }

            // 修复4: 增强版bg对象（兼容）
            function initBgObject() {
                window.bg = window.bg || {
                    ready: function(callback) {
                        jQuery(document).ready(callback);
                    },
                    Go: function(url, target) {
                        jQuery.ajax({
                            url: url,
                            success: function(data) {
                                const container = jQuery('#' + target);
                                container.html(data);
                                // 修复5: 执行动态加载的脚本
                                container.find('script').each(function() {
                                    if (this.src) {
                                        jQuery.getScript(this.src);
                                    } else {
                                        jQuery.globalEval(this.text);
                                    }
                                });
                            }
                        });
                    }
                };
            }

            // 主执行流程
            loadjQuery().then(() => {
                // 修复6: 强制设置
                if (typeof $ === 'undefined') {
                    window.$ = jQuery;
                }
                
                // 注入原始head内容
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = `${headContent.replace("`", "\\`")}`;

                // 处理CSS和脚本
                Array.from(tempDiv.children).forEach(element => {
                    if (element.tagName === 'LINK') {
                        document.head.appendChild(element.cloneNode(true));
                    } else if (element.tagName === 'SCRIPT') {
                        const newScript = document.createElement('script');
                        if (element.src) {
                            newScript.src = element.src;
                        } else {
                            newScript.text = element.textContent;
                        }
                        document.head.appendChild(newScript);
                    }
                });

                // 初始化bg对象
                initBgObject();
                
               // 异步控制核心
            setTimeout(async () => {
                try {
                    // 第一步：强制等待 welcome 完成
                    await bg.Go('/eams/home!welcome.action','main');
                    
                    // 第二步：检测 DOM 更新
                    await new Promise(resolve => {
                        const check = () => {
                            if (jQuery('#main').children().length > 0) {
                                resolve();
                            } else {
                                setTimeout(check, 50);
                            }
                        };
                        check();
                    });
                    
                    // 第三步：执行后续请求
                    bg.Go('/eams/home!submenus.action?menu.id=','menu_panel');
                } catch(e) {
                    console.error('流程错误:', e);
                }
            }, 300);
            });
        })();
    """.trimIndent()

    evaluateJavascript(script) {
        if (it != null) Log.e("ScriptInjection", "注入错误: $it")
    }
}

private fun buildHeadContent(): String {
    return """
            <title></title>
      <meta http-equiv="content-type" content="text/html;charset=utf-8" />
      <meta http-equiv="X-UA-Compatible" content="IE=edge" />
      <meta http-equiv="pragma" content="no-cache" />
      <meta http-equiv="cache-control" content="no-cache" />
      <meta http-equiv="expires" content="0" />
      <meta http-equiv="content-style-type" content="text/css" />
      <meta http-equiv="content-script-type" content="text/javascript" />
      <script type="text/javascript">
          window.${'$'}BG_LANG='zh';
      </script>
      <script type="text/javascript" src="/eams/static/scripts/jquery/jquery,jquery.ui.core.js?bg=3.4.3"></script>
      <script type="text/javascript" src="/eams/static/scripts/plugins/jquery-form,jquery-history,jquery-colorbox,jquery-chosen.js?bg=3.4.3"></script>
      <script type="text/javascript" src="/eams/static/js/plugins/jquery.subscribe,/js/struts2/jquery.struts2,jquery.ui.struts2.js?bg=3.4.3"></script>
      <script type="text/javascript" src="/eams/static/scripts/beangle/beangle,beangle-ui.js?bg=3.4.7"></script>
      <script type="text/javascript">var App = {contextPath:"/eams"};jQuery(document).ready(function () {jQuery.struts2_jquery.version="3.6.1";beangle.contextPath=App.contextPath;jQuery.scriptPath = App.contextPath+"/static/";jQuery.struts2_jquerySuffix = "";jQuery.ajaxSettings.traditional = true;jQuery.ajaxSetup ({cache: false});});</script>
      <script type="text/javascript" src="/eams/static/scripts/my97/WdatePicker-4.72.js?bg=3.4.10&compress=no"></script>
      <link id="jquery_theme_link" rel="stylesheet" href="/eams/static/themes/smoothness/jquery-ui.css?s2j=3.6.1" type="text/css"/>
      <link id="beangle_theme_link" href="/eams/static/themes/default/beangle-ui,colorbox,chosen.css" rel="stylesheet" type="text/css" />
      <link rel="stylesheet" href="/eams/static/css/foundation.css" type="text/css"/>
      <script type="text/javascript" src="/eams/static/scripts/highcharts.js"></script>
    
        <script src="/eams/static/scripts/require.config.js?v=3"></script>
        <script>
        require.baseUrl="/eams/static/scripts";
        </script>
        <script src="/eams/static/scripts/require.js"></script>
        <script type="text/javascript" src="/eams/static/scripts/require.js"></script>
        
        <!-- backbone & underscore -- fontend MVC framework -->
        <script type="text/javascript" src="/eams/static/scripts/underscore.min.js"></script>
        <script type="text/javascript" src="/eams/static/scripts/backbone.min.js"></script>
        <script type="text/javascript" src="/eams/static/scripts/underscore.string.min.js"></script>
        <script>
        require(['underscore.string'], function(_s) {
                _.str = _s;
                _.mixin(_.str.exports());
        });
        </script>
        
    <link href="/eams/static/themes/default/home.css?v2" rel="stylesheet" type="text/css" />
    <script src="/eams/static/scripts/jscroll.js" type="text/javascript"></script>
    <script src="/eams/static/scripts/jquery.cookie.js" type="text/javascript"></script>
    
    <style>
    #_menu_folder {
        height:100%;
        width:100%;
        background-color:rgba(0, 0, 0, 0.2);
        cursor:pointer;
        position:relative;
    }
    #_menu_folder:hover {
        height:100%;
        width:100%;
        background-color:rgba(222, 222, 222, 1);
    }
    .arrow-right {
            width: 0;
            height: 0;
            border-top: 6px solid transparent;
            border-bottom: 6px solid transparent;
            border-left: 6px solid rgba(0, 0, 0, 0.6);
            top:50%;
            position:absolute;
    }
    .arrow-left {
            width: 0;
            height: 0;
            border-top: 6px solid transparent;
            border-bottom: 6px solid transparent; 
            border-right:6px solid rgba(0, 0, 0, 0.6);
            top:50%;
            position:absolute;
    }
    .color_theme_selector {display:inline-block;width:10px;height:10px;margin-right:2px;}
    </style>
    """.trimIndent()
        .replace("\n", "")  // 去除换行防止脚本错误
        .replace("'", "\\'") // 转义单引号
}