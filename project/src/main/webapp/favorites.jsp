<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户收藏</title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>
<div class="page">
    <div class="top-bar">
        <button class="btn secondary" onclick="location.href='index.jsp'">首页</button>
    </div>
    <div class="center-panel">
        <h2>用户收藏</h2>
        <div id="favList" class="result-scroll"></div>
    </div>
</div>
<script src="assets/js/app.js"></script>
<script>
    const list = JSON.parse(localStorage.getItem("favorites") || "[]");
    const box = document.getElementById("favList");
    if (!list.length) box.innerHTML = "<p class='hint'>暂未收藏房源。</p>";
    else box.innerHTML = list.map((x, i) => `
      <div class="listing-box">
        <div>${i + 1}. ${x.district} ${x.year}年${x.month}月 | 面积:${x.area}㎡ | 价格:${x.price}万</div>
        <button class="plus-btn" onclick="removeFav(${i})">-</button>
      </div>
    `).join("");

    function removeFav(i) {
        list.splice(i, 1);
        localStorage.setItem("favorites", JSON.stringify(list));
        location.reload();
    }
</script>
</body>
</html>
