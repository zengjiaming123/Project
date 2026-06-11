<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>价格走势图</title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>
<div class="page">
    <div class="top-bar">
        <button class="btn secondary" onclick="location.href='index.jsp'">首页</button>
        <button class="btn secondary" onclick="location.href='search.jsp'">房源搜索</button>
        <button class="btn secondary" onclick="location.href='predict.jsp'">房价预测</button>
        <button class="btn secondary" onclick="location.reload()">重试</button>
    </div>

    <div class="layout-2col">
        <div class="left-panel">
            <h3>地区（年月）平均房价走势图</h3>
            <div class="form-row"><label>地区（必选）</label><select id="district"></select></div>
            <div class="form-row"><label>年份（可选）</label><input id="year" type="number" placeholder="不填则按年份绘图"></div>
            <button class="btn" style="width:100%" onclick="drawTrend()">查看</button>
            <p class="hint">仅选择地区：显示均价-年份；地区+年份：显示均价-月份（1~12月全部显示）。</p>
            <p class="hint" id="trendModeHint">当前模式：未选择</p>
            <p class="hint">售房单位：万/㎡；租房单位：万/㎡/月（按 price/area，且仅统计同类型数据）。</p>
        </div>

        <div class="right-panel">
            <canvas id="trendCanvas" width="900" height="470"></canvas>
        </div>
    </div>
</div>

<script src="assets/js/app.js"></script>
<script>
    renderDistrictOptions("district", true);
    (function () {
        const t = sessionStorage.getItem("listingType");
        const el = document.getElementById("trendModeHint");
        if (!t) { el.innerText = "当前模式：未选择（请回首页）"; return; }
        el.innerText = t === "rent" ? "当前模式：租房走势" : "当前模式：购房走势";
    })();

    async function drawTrend() {
        const district = document.getElementById("district").value;
        const year = document.getElementById("year").value.trim();
        const listingType = sessionStorage.getItem("listingType") || "";
        if (!district) {
            alert("地区必选");
            return;
        }
        if (!listingType) {
            alert("请返回首页先选择【租房】或【购房】");
            return;
        }

        try {
            const qs = new URLSearchParams({ district, listingType });
            if (year) qs.append("year", year);

            const res = await fetch("api/trend?" + qs.toString());
            if (!res.ok) throw new Error("HTTP " + res.status);

            const data = await res.json();
            if (!data.success) {
                alert(data.message || "查询失败");
                return;
            }

            animateLine(data.points || [], data.mode || "year");
        } catch (e) {
            alert("请求失败：" + e.message);
        }
    }

    function animateLine(points, mode) {
        const cvs = document.getElementById("trendCanvas");
        const ctx = cvs.getContext("2d");
        ctx.clearRect(0, 0, cvs.width, cvs.height);

        const margin = 60;
        const W = cvs.width - margin * 2;
        const H = cvs.height - margin * 2;

        drawAxes(ctx, margin, W, H);

        const valMap = {};
        points.forEach(p => {
            valMap[Number(p[0])] = Number(p[1]);
        });

        let xTicks = [];
        if (mode === "month") {
            for (let i = 1; i <= 12; i++) xTicks.push(i);
        } else {
            xTicks = points.map(p => Number(p[0]));
        }

        if (xTicks.length === 0) {
            ctx.fillStyle = "#6B7280";
            ctx.font = "14px Arial";
            ctx.fillText("暂无可绘制数据", margin + 20, margin + 20);
            return;
        }

        const plot = xTicks.map((x, i) => ({
            tick: x,
            x: margin + i * (W / Math.max(1, xTicks.length - 1)),
            yValue: Object.prototype.hasOwnProperty.call(valMap, x) ? valMap[x] : null
        }));

        const yVals = plot.filter(p => p.yValue !== null).map(p => p.yValue);
        if (yVals.length === 0) {
            ctx.fillStyle = "#6B7280";
            ctx.font = "14px Arial";
            ctx.fillText("该条件下暂无有效数据点", margin + 20, margin + 20);
            drawXAxisLabels(ctx, plot, mode, margin, H);
            return;
        }

        const minY = Math.min(...yVals) * 0.95;
        const maxY = Math.max(...yVals) * 1.05;
        const yRange = (maxY - minY) || 1;

        plot.forEach(p => {
            if (p.yValue !== null) {
                p.y = margin + (maxY - p.yValue) / yRange * H;
            }
        });

        drawXAxisLabels(ctx, plot, mode, margin, H);
        drawYAxisLabels(ctx, minY, maxY, margin, H);

        const valid = plot.filter(p => p.yValue !== null);

        if (valid.length === 1) {
            drawPoints(ctx, valid, 1.0);
            return;
        }

        let t = 0;
        function frame() {
            t += 0.02;

            ctx.clearRect(margin + 1, margin + 1, W - 2, H - 2);
            drawAxes(ctx, margin, W, H);
            drawXAxisLabels(ctx, plot, mode, margin, H);
            drawYAxisLabels(ctx, minY, maxY, margin, H);

            ctx.strokeStyle = "#5B7C99";
            ctx.lineWidth = 2.2;
            ctx.beginPath();

            valid.forEach((p, i) => {
                const k = Math.min(1, t * valid.length - i);
                if (k < 0) return;

                if (i === 0) {
                    ctx.moveTo(p.x, p.y);
                } else {
                    const prev = valid[i - 1];
                    const x = prev.x + (p.x - prev.x) * k;
                    const y = prev.y + (p.y - prev.y) * k;
                    ctx.lineTo(x, y);
                }
            });

            ctx.stroke();
            drawPoints(ctx, valid, t);

            if (t < 1.05) {
                requestAnimationFrame(frame);
            }
        }
        frame();
    }

    function drawAxes(ctx, margin, W, H) {
        ctx.strokeStyle = "#2C3E50";
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.moveTo(margin, margin);
        ctx.lineTo(margin, margin + H);
        ctx.lineTo(margin + W, margin + H);
        ctx.stroke();
    }

    function drawXAxisLabels(ctx, plot, mode, margin, H) {
        ctx.fillStyle = "#1C2433";
        ctx.font = "12px Arial";
        ctx.textAlign = "center";

        plot.forEach(p => {
            ctx.beginPath();
            ctx.moveTo(p.x, margin + H);
            ctx.lineTo(p.x, margin + H + 4);
            ctx.strokeStyle = "#9CA3AF";
            ctx.lineWidth = 1;
            ctx.stroke();

            const label = (mode === "month") ? (p.tick + "月") : String(p.tick);
            ctx.fillText(label, p.x, margin + H + 18);
        });

        ctx.textAlign = "start";
    }

    function drawYAxisLabels(ctx, minY, maxY, margin, H) {
        ctx.fillStyle = "#1C2433";
        ctx.font = "12px Arial";
        for (let i = 0; i < 5; i++) {
            const y = margin + i * H / 4;
            const label = (maxY - (maxY - minY) * i / 4).toFixed(2);
            ctx.fillText(label, 8, y + 4);
        }
    }

    function drawPoints(ctx, valid, t) {
        valid.forEach((p, i) => {
            if (t * valid.length >= i + 1) {
                ctx.fillStyle = "#C45C4A";
                ctx.beginPath();
                ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
                ctx.fill();
            }
        });
    }
</script>
</body>
</html>