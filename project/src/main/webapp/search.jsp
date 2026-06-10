<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>房源搜索</title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>
<div class="page">
    <div class="top-bar">
        <button class="btn secondary" onclick="location.href='index.jsp'">首页</button>
        <button class="btn secondary" onclick="location.href='trend.jsp'">价格走势图</button>
        <button class="btn secondary" onclick="location.href='predict.jsp'">房价预测</button>
        <button class="btn secondary" onclick="resetForm()">重试</button>
    </div>

    <div class="layout-2col">
        <div class="left-panel">
            <h3>8项可选特征</h3>
            <div class="form-row"><label>地区</label><select id="district"></select></div>
            <div class="form-row"><label>年份</label><input id="year" type="number" placeholder="如 2025"></div>
            <div class="form-row"><label>月份</label><input id="month" type="number" min="1" max="12"></div>
            <div class="form-row"><label>预算（万）</label><input id="budget" type="number"></div>
            <div class="form-row"><label>面积（㎡）</label><input id="area" type="number"></div>
            <div class="form-row"><label>楼层</label><input id="floor" type="number"></div>
            <div class="form-row"><label>距地铁（km）</label><input id="distanceToSubway" type="number" step="0.1"></div>
            <div class="form-row"><label>房龄（年）</label><input id="houseAge" type="number" step="0.5"></div>
            <button class="btn" style="width:100%" onclick="doSearch()">确定</button>
        </div>

        <div class="right-panel">
            <h3>匹配房源信息</h3>
            <div class="result-scroll" id="results"></div>
        </div>
    </div>
</div>

<script src="assets/js/app.js"></script>
<script>
    renderDistrictOptions("district", true);
    let latestResults = [];

    function resetForm() {
        location.reload();
    }

    function collectFav(i) {
        addFavorite(latestResults[i]);
    }

    async function doSearch() {
        const box = document.getElementById("results");
        box.innerHTML = "<p class='hint'>正在查询，请稍候...</p>";

        try {
            const form = new URLSearchParams();
            ["district","year","month","budget","area","floor","distanceToSubway","houseAge"].forEach(k => {
                const el = document.getElementById(k);
                const v = el ? el.value.trim() : "";
                if (v) form.append(k, v);
            });

            form.append("listingType", sessionStorage.getItem("listingType") || "rent");

            const resp = await fetch("api/search", {
                method: "POST",
                body: form
            });

            if (!resp.ok) {
                throw new Error("HTTP " + resp.status);
            }

            const data = await resp.json();

            if (!data.success) {
                box.innerHTML = "<p class='hint'>查询失败：" + (data.message || "未知错误") + "</p>";
                return;
            }

            if (!data.data || data.data.length === 0) {
                box.innerHTML = "<p class='hint'>暂无匹配结果，请调整条件。</p>";
                return;
            }

            latestResults = data.data;
            box.innerHTML = data.data.map((x, i) => `
                <div class="listing-box">
                    <div>${x.district} | ${x.year}年${x.month}月 | ${x.area}㎡ | ${x.price}万</div>
                    <button class="plus-btn" onclick="collectFav(${i})">+</button>
                </div>
            `).join("");

        } catch (e) {
            box.innerHTML = "<p class='hint'>请求失败：" + e.message + "</p>";
            alert("请求失败：" + e.message);
        }
    }
</script>
</body>
</html>
