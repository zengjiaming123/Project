<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>房价预测</title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>
<div class="page">
    <div class="top-bar">
        <button class="btn secondary" onclick="location.href='index.jsp'">首页</button>
        <button class="btn secondary" onclick="location.href='search.jsp'">房源搜索</button>
        <button class="btn secondary" onclick="location.href='trend.jsp'">价格走势图</button>
        <button class="btn secondary" onclick="location.reload()">重试</button>
    </div>

    <div class="layout-2col">
        <div class="left-panel">
            <h3>7项特征预测房价</h3>
            <p class="hint" id="modeHint">当前模式：未选择（请回首页选租房/购房）</p>

            <!-- 关闭浏览器自动填充，确保用户手动输入 -->
            <form id="predictForm" autocomplete="off" onsubmit="return false;">
                <div class="form-row"><label>地区</label><select id="district"></select></div>
                <div class="form-row"><label>年份</label><input id="year" type="number" autocomplete="off" placeholder="如 2025"></div>
                <div class="form-row"><label>月份</label><input id="month" type="number" min="1" max="12" autocomplete="off" placeholder="1-12"></div>
                <div class="form-row"><label>面积（㎡）</label><input id="area" type="number" step="0.1" autocomplete="off" placeholder="如 86"></div>
                <div class="form-row"><label>楼层</label><input id="floor" type="number" autocomplete="off" placeholder="如 16"></div>
                <div class="form-row"><label>距地铁（km）</label><input id="distanceToSubway" type="number" step="0.1" autocomplete="off" placeholder="如 0.7"></div>
                <div class="form-row"><label>房龄（年）</label><input id="houseAge" type="number" step="0.5" autocomplete="off" placeholder="如 8"></div>
                <button class="btn" style="width:100%" type="button" onclick="doPredict()">确定</button>
            </form>
        </div>

        <div class="right-panel">
            <canvas id="scatterCanvas" width="900" height="420"></canvas>
            <table class="impact-table" id="impactTable"></table>
            <div style="margin-top:10px;padding:10px;border:1px dashed #a8c2f4;border-radius:10px" id="suggestion"></div>
        </div>
    </div>
</div>

<script src="assets/js/app.js"></script>
<script>
    renderDistrictOptions("district", false);
    (function showMode() {
        const t = sessionStorage.getItem("listingType");
        const el = document.getElementById("modeHint");
        if (!t) { el.innerText = "当前模式：未选择（请回首页选租房/购房）"; return; }
        el.innerText = t === "rent" ? "当前模式：租房（仅使用 rent 数据）" : "当前模式：购房（仅使用 sale 数据）";
    })();

    function getVal(id) {
        const el = document.getElementById(id);
        return el ? el.value.trim() : "";
    }

    function validateForm() {
        const required = [
            { id: "district", name: "地区" },
            { id: "year", name: "年份" },
            { id: "month", name: "月份" },
            { id: "area", name: "面积" },
            { id: "floor", name: "楼层" },
            { id: "distanceToSubway", name: "距地铁" },
            { id: "houseAge", name: "房龄" }
        ];

        for (const item of required) {
            if (!getVal(item.id)) {
                alert("请填写：" + item.name);
                return false;
            }
        }

        const month = Number(getVal("month"));
        if (month < 1 || month > 12) {
            alert("月份必须在 1-12 之间");
            return false;
        }

        return true;
    }

    async function doPredict() {
        if (!validateForm()) return;
        const lt = sessionStorage.getItem("listingType");
        if (!lt) { alert("请返回首页先选择【租房】或【购房】"); return; }

        const form = new URLSearchParams();
        ["district","year","month","area","floor","distanceToSubway","houseAge"]
            .forEach(k => form.append(k, getVal(k)));
        form.append("listingType", sessionStorage.getItem("listingType") || "");

        try {
            const res = await fetch("api/predict", { method: "POST", body: form });
            if (!res.ok) throw new Error("HTTP " + res.status);

            const data = await res.json();
            if (!data.success) {
                alert(data.message || "预测失败");
                return;
            }

            drawScatter();

            const totalLabel = data.listingType === "rent" ? "预测月租(万)" : "预测总价(万)";
            const unitLabel = data.unitLabel || "万/㎡";
            document.getElementById("impactTable").innerHTML =
                "<tr><th>影响因子</th><th>影响幅度(%)</th></tr>" +
                data.impact.map(x => `<tr><td>${x.name}</td><td>${x.value}</td></tr>`).join("") +
                `<tr><td>${totalLabel}</td><td>${data.totalPrice}</td></tr>` +
                `<tr><td>单位价格(${unitLabel})</td><td>${data.unitPrice}</td></tr>`;

            document.getElementById("suggestion").innerText = data.suggestion;
        } catch (e) {
            alert("请求失败：" + e.message);
        }
    }

    function drawScatter() {
        const cvs = document.getElementById("scatterCanvas");
        const ctx = cvs.getContext("2d");
        ctx.clearRect(0, 0, cvs.width, cvs.height);

        const groups = ["面积-房价", "楼层-房价", "距地铁-房价", "房龄-房价"];
        const panelW = cvs.width / 2;
        const panelH = cvs.height / 2;

        groups.forEach((g, idx) => {
            const x0 = (idx % 2) * panelW;
            const y0 = Math.floor(idx / 2) * panelH;

            ctx.strokeStyle = "#95acde";
            ctx.strokeRect(x0 + 10, y0 + 10, panelW - 20, panelH - 20);

            ctx.fillStyle = "#2d3f63";
            ctx.fillText(g, x0 + 20, y0 + 28);

            for (let i = 0; i < 22; i++) {
                const x = x0 + 25 + Math.random() * (panelW - 50);
                const y = y0 + 40 + Math.random() * (panelH - 55);
                ctx.fillStyle = "rgba(255,70,70,0.76)";
                ctx.beginPath();
                ctx.arc(x, y, 3, 0, Math.PI * 2);
                ctx.fill();
            }
        });
    }
</script>
</body>
</html>
