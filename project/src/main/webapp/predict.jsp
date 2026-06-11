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
            <h3>7项特征线性回归预测</h3>
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
            <p class="hint" id="modelInfo">模型：首次预测时自动从数据库训练 rent/sale 线性回归方程</p>
            <div class="scatter-grid" id="scatterGrid">
                <canvas class="scatter-panel" data-key="area" data-title="面积 - 房价" data-xlabel="面积(㎡)"></canvas>
                <canvas class="scatter-panel" data-key="floor" data-title="楼层 - 房价" data-xlabel="楼层"></canvas>
                <canvas class="scatter-panel" data-key="distance" data-title="距地铁 - 房价" data-xlabel="距离(km)"></canvas>
                <canvas class="scatter-panel" data-key="age" data-title="房龄 - 房价" data-xlabel="房龄(年)"></canvas>
            </div>
            <p class="hint" id="impactHint">边际系数：每增加 1 个单位，预测总价变化多少万（正=推高，负=拉低）</p>
            <table class="impact-table" id="impactTable"></table>
            <div class="suggestion-box" id="suggestion"></div>
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

            const totalLabel = data.listingType === "rent" ? "预测月租(万)" : "预测总价(万)";
            const unitLabel = data.unitLabel || "万/㎡";
            document.getElementById("modelInfo").innerText =
                `模型：${data.modelType || "多元线性回归"} | R²=${data.rSquared} | 训练样本=${data.trainSampleCount}条(${data.listingTypeLabel}) | 散点图=${data.scatterCount || (data.scatterPoints || []).length}个`;
            const fmtBeta = (v) => {
                const n = Number(v);
                if (isNaN(n)) return v;
                return (n >= 0 ? "+" : "") + n.toFixed(4);
            };
            document.getElementById("impactTable").innerHTML =
                "<tr><th>特征</th><th>边际系数 β (万/单位)</th></tr>" +
                data.impact.map(x => `<tr><td>${x.name}</td><td>${fmtBeta(x.value)}</td></tr>`).join("") +
                `<tr><td>${totalLabel}</td><td>${data.totalPrice}</td></tr>` +
                `<tr><td>单位价格(${unitLabel})</td><td>${data.unitPrice}</td></tr>`;

            document.getElementById("suggestion").innerText = data.suggestion;

            drawScatter(data.fitLines || {}, data.scatterPoints || []);
        } catch (e) {
            alert("请求失败：" + e.message);
        }
    }

    let scatterAnimToken = 0;

    function preparePanelCanvas(cvs) {
        const dpr = Math.max(1, window.devicePixelRatio || 1);
        const parentW = cvs.parentElement ? cvs.parentElement.clientWidth : 640;
        const displayW = Math.max(260, Math.floor((parentW - 10) / 2));
        const displayH = 200;

        cvs.style.width = displayW + "px";
        cvs.style.height = displayH + "px";
        cvs.width = Math.floor(displayW * dpr);
        cvs.height = Math.floor(displayH * dpr);

        const ctx = cvs.getContext("2d");
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        return { ctx, logicalW: displayW, logicalH: displayH };
    }

    function stopScatterAnimations() {
        scatterAnimToken++;
    }

    function easeOutCubic(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    function buildPanelData(points, fitLines, g, logicalW, logicalH) {
        const pad = { left: 46, right: 12, top: 34, bottom: 30 };
        const plot = {
            left: pad.left,
            right: logicalW - pad.right,
            top: pad.top,
            bottom: logicalH - pad.bottom
        };

        const subset = points.map(p => ({ x: Number(p[g.key]), y: Number(p.price) }))
            .filter(p => !isNaN(p.x) && !isNaN(p.y));
        const line = (fitLines && fitLines[g.key]) ? fitLines[g.key] : [];
        const linePts = line.map(p => ({ x: Number(p.x), y: Number(p.y) }))
            .filter(p => !isNaN(p.x) && !isNaN(p.y));

        if (subset.length === 0 && linePts.length === 0) {
            return { empty: true, title: g.title };
        }

        const allX = subset.map(p => p.x).concat(linePts.map(p => p.x));
        const allY = subset.map(p => p.y).concat(linePts.map(p => p.y));
        const minX = Math.min(...allX);
        const maxX = Math.max(...allX);
        const minY = Math.min(...allY);
        const maxY = Math.max(...allY);
        const spanX = maxX - minX || 1;
        const spanY = maxY - minY || 1;
        const padX = spanX * 0.06;
        const padY = spanY * 0.08;

        const toPx = (p) => ({
            x: plot.left + ((p.x - minX + padX) / (spanX + 2 * padX)) * (plot.right - plot.left),
            y: plot.bottom - ((p.y - minY + padY) / (spanY + 2 * padY)) * (plot.bottom - plot.top)
        });

        return {
            empty: false, title: g.title, xLabel: g.xLabel,
            plot, minX, maxX, minY, maxY,
            sampleCount: subset.length,
            points: subset.map(toPx),
            line: linePts.map(toPx)
        };
    }

    function drawPanelFrame(ctx, panel, phase, logicalW, logicalH) {
        const { title } = panel;

        ctx.clearRect(0, 0, logicalW, logicalH);
        ctx.fillStyle = "#F8F9FB";
        ctx.fillRect(0, 0, logicalW, logicalH);

        ctx.strokeStyle = "#D4DCE4";
        ctx.lineWidth = 1.5;
        ctx.strokeRect(4, 4, logicalW - 8, logicalH - 8);

        ctx.fillStyle = "#1C2433";
        ctx.font = "600 12px 'Segoe UI', 'Microsoft YaHei', sans-serif";
        ctx.fillText(title, 12, 20);

        if (panel.empty) {
            ctx.fillStyle = "#6B7280";
            ctx.font = "12px 'Segoe UI', 'Microsoft YaHei', sans-serif";
            ctx.fillText("暂无样本", 16, 48);
            return;
        }

        const { plot, points, line, xLabel, minX, maxX, minY, maxY } = panel;
        const axisP = phase >= 1 ? 1 : Math.min(1, phase / 0.35);
        const contentP = phase >= 1 ? 1 : (phase <= 0.35 ? 0 : Math.min(1, (phase - 0.35) / 0.65));
        const axisEase = easeOutCubic(axisP);
        const contentEase = easeOutCubic(contentP);

        ctx.strokeStyle = "#E2E6EC";
        ctx.lineWidth = 1;
        const gridN = 4;
        for (let i = 1; i <= gridN; i++) {
            const gx = plot.left + (plot.right - plot.left) * i / gridN;
            const gy = plot.bottom - (plot.bottom - plot.top) * i / gridN;
            ctx.beginPath();
            ctx.moveTo(gx, plot.top);
            ctx.lineTo(gx, plot.bottom);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(plot.left, gy);
            ctx.lineTo(plot.right, gy);
            ctx.stroke();
        }

        ctx.strokeStyle = "#2C3E50";
        ctx.lineWidth = 2;
        ctx.lineCap = "round";
        ctx.beginPath();
        ctx.moveTo(plot.left, plot.bottom);
        ctx.lineTo(plot.left + (plot.right - plot.left) * axisEase, plot.bottom);
        ctx.stroke();
        ctx.beginPath();
        ctx.moveTo(plot.left, plot.bottom);
        ctx.lineTo(plot.left, plot.bottom - (plot.bottom - plot.top) * axisEase);
        ctx.stroke();

        if (axisP >= 1) {
            ctx.fillStyle = "#6B7280";
            ctx.font = "11px 'Segoe UI', 'Microsoft YaHei', sans-serif";
            ctx.fillText(xLabel, (plot.left + plot.right) / 2 - 24, plot.bottom + 24);
            ctx.save();
            ctx.translate(plot.left - 38, (plot.top + plot.bottom) / 2);
            ctx.rotate(-Math.PI / 2);
            ctx.fillText("价格(万)", -20, 0);
            ctx.restore();
            ctx.fillText(minX.toFixed(1), plot.left - 4, plot.bottom + 14);
            ctx.fillText(maxX.toFixed(1), plot.right - 20, plot.bottom + 14);
            ctx.fillText(minY.toFixed(0), plot.left - 44, plot.bottom + 4);
            ctx.fillText(maxY.toFixed(0), plot.left - 44, plot.top + 4);
        }

        if (contentP > 0) {
            const alpha = 0.55 + 0.45 * contentEase;
            for (let i = 0; i < points.length; i++) {
                const pt = points[i];
                const r = 3.5 + 1.0 * contentEase;
                ctx.fillStyle = `rgba(196, 92, 74, ${alpha})`;
                ctx.strokeStyle = "#ffffff";
                ctx.lineWidth = 1.2;
                ctx.beginPath();
                ctx.arc(pt.x, pt.y, r, 0, Math.PI * 2);
                ctx.fill();
                ctx.stroke();
            }
        }

        if (panel.sampleCount > 0 && axisP >= 1) {
            ctx.fillStyle = "#6B7280";
            ctx.font = "10px 'Segoe UI', 'Microsoft YaHei', sans-serif";
            ctx.fillText("散点" + panel.sampleCount + "个", 12, logicalH - 8);
        }

        if (line.length > 1 && contentP > 0) {
            const lineEase = contentEase;
            const segCount = line.length - 1;
            let remain = lineEase * segCount;
            ctx.strokeStyle = "#5B7C99";
            ctx.lineWidth = 3;
            ctx.lineCap = "round";
            ctx.lineJoin = "round";
            ctx.beginPath();
            ctx.moveTo(line[0].x, line[0].y);
            for (let i = 0; i < segCount; i++) {
                const use = Math.min(1, remain);
                if (use <= 0) break;
                const a = line[i];
                const b = line[i + 1];
                ctx.lineTo(a.x + (b.x - a.x) * use, a.y + (b.y - a.y) * use);
                remain -= 1;
            }
            ctx.stroke();

            if (contentEase >= 0.95) {
                ctx.fillStyle = "#5B7C99";
                ctx.font = "10px 'Segoe UI', 'Microsoft YaHei', sans-serif";
                ctx.fillRect(logicalW - 72, 10, 64, 16);
                ctx.fillStyle = "#FFFFFF";
                ctx.fillText("— 拟合线", logicalW - 68, 21);
            }
        }
    }

    async function loadScatterPoints(primaryPoints) {
        let points = Array.isArray(primaryPoints) ? primaryPoints : [];
        if (points.length >= 20) return points;
        try {
            const lt = sessionStorage.getItem("listingType") || "";
            const res = await fetch("api/scatter?listingType=" + encodeURIComponent(lt));
            const data = await res.json();
            if (data.success && Array.isArray(data.points) && data.points.length > points.length) {
                points = data.points;
            }
        } catch (e) { /* ignore */ }
        return points;
    }

    async function drawScatter(fitLines, scatterPoints) {
        const token = ++scatterAnimToken;
        const lines = (fitLines && typeof fitLines === "object") ? fitLines : {};
        const points = await loadScatterPoints(scatterPoints);

        await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));

        const canvases = Array.from(document.querySelectorAll(".scatter-panel"));
        const panels = canvases.map(cvs => {
            const prepared = preparePanelCanvas(cvs);
            const panel = buildPanelData(points, lines, {
                title: cvs.dataset.title,
                xLabel: cvs.dataset.xlabel,
                key: cvs.dataset.key
            }, prepared.logicalW, prepared.logicalH);
            return { cvs, prepared, panel };
        });

        panels.forEach(({ prepared, panel }) => {
            drawPanelFrame(prepared.ctx, panel, 1, prepared.logicalW, prepared.logicalH);
        });

        const duration = 1600;
        const startAt = performance.now();

        function step(now) {
            if (token !== scatterAnimToken) return;
            const progress = Math.min(1, (now - startAt) / duration);
            panels.forEach(({ prepared, panel }, idx) => {
                const delay = idx * 0.06;
                const denom = Math.max(0.01, 1 - delay);
                const local = Math.max(0, Math.min(1, (progress - delay) / denom));
                drawPanelFrame(prepared.ctx, panel, local, prepared.logicalW, prepared.logicalH);
            });
            if (progress < 1) {
                requestAnimationFrame(step);
            } else {
                panels.forEach(({ prepared, panel }) => {
                    drawPanelFrame(prepared.ctx, panel, 1, prepared.logicalW, prepared.logicalH);
                });
            }
        }

        requestAnimationFrame(step);
    }
</script>
</body>
</html>
