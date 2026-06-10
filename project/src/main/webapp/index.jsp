<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>智慧房源平台</title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>
<div class="page">
    <div class="top-bar">
        <button class="btn secondary scroll-animate" onclick="location.href='favorites.jsp'">用户收藏</button>
    </div>

    <div class="center-panel scroll-animate">
        <h2>房产信息服务平台</h2>
        <p class="hint">先选择业务方向，再选择身份，最后进入功能页。</p>
        <div class="choice-row">
            <button class="btn secondary" id="modeRent" onclick="setMode('rent')">租房</button>
            <button class="btn secondary" id="modeBuy" onclick="setMode('buy')">购房</button>
        </div>
        <div class="role-grid">
            <div class="card" id="roleA" onclick="setRole('A')">租客 / 买方</div>
            <div class="card" id="roleB" onclick="setRole('B')">房东 / 售房方</div>
        </div>
        <div class="vertical-actions">
            <button class="btn" onclick="gotoPage('search.jsp')">房源搜索</button>
            <button class="btn" onclick="gotoPage('trend.jsp')">价格走势图</button>
            <button class="btn" onclick="gotoPage('predict.jsp')">房价预测</button>
        </div>
    </div>
</div>

<div class="news-ticker"><span id="newsLine"></span></div>
<script src="assets/js/app.js"></script>
<script>
    let selectedMode = "";
    let selectedRole = "";
    setNewsTicker("newsLine");
    function setMode(v) {
        selectedMode = v;
        document.getElementById("modeRent").classList.toggle("active", v === "rent");
        document.getElementById("modeBuy").classList.toggle("active", v === "buy");
    }
    function setRole(v) {
        selectedRole = v;
        document.getElementById("roleA").classList.toggle("active", v === "A");
        document.getElementById("roleB").classList.toggle("active", v === "B");
    }
    function gotoPage(path) {
        if (!selectedMode || !selectedRole) {
            alert("请先选择【租房/购房】和身份板块。");
            return;
        }
        sessionStorage.setItem("listingType", selectedMode === "rent" ? "rent" : "sale");
        location.href = path;
    }
</script>
</body>
</html>
