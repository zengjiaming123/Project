const DISTRICTS_9 = ["广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "江门", "肇庆"];
const DISTRICTS_8 = ["广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "江门"];

function randomNewsItem() {
    const year = 2024 + Math.floor(Math.random() * 2);
    const month = 1 + Math.floor(Math.random() * 12);
    const district = DISTRICTS_9[Math.floor(Math.random() * DISTRICTS_9.length)];
    const yoy = (Math.random() * 8 - 2).toFixed(1);
    const avg = (0.8 + Math.random() * 3.2).toFixed(2);
    const rent = (0.03 + Math.random() * 0.1).toFixed(3);
    const templates = [
        `${district} ${year}年${month}月住宅均价约 ${avg} 万/㎡，同比${yoy >= 0 ? "上涨" : "下降"} ${Math.abs(yoy)}%`,
        `${district} 热门板块 ${year}年${month}月成交活跃，租房参考价约 ${rent} 万/㎡/月`,
        `大湾区 ${district} 地铁沿线房源关注度上升，建议结合【价格走势图】查看趋势`,
        `${district} ${month}月新房与二手房价差收窄，购房者可多维度比价`,
        `系统提示：使用【房价预测】前请先在首页选择租房或购房模式`,
        `${district} 近地铁 1km 内房源溢价约 ${(5 + Math.random() * 8).toFixed(1)}%，刚需可关注非核心区`,
        `${year}年${month}月 ${district} 大户型（>120㎡）均价环比${Math.random() > 0.5 ? "回升" : "微降"}`,
        `租房市场：${district} ${month}月一居室供应增加，短租需求稳中有升`
    ];
    return templates[Math.floor(Math.random() * templates.length)];
}

function setNewsTicker(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;

    const items = [];
    for (let i = 0; i < 8; i++) items.push(randomNewsItem());
    const joined = items.map(t => "【楼市资讯】" + t).join("    ◆    ");

    el.innerHTML =
        '<div class="news-track">' +
        '<span class="news-group">' + joined + '</span>' +
        '<span class="news-group">' + joined + '</span>' +
        '</div>';

    startNewsMarquee(el);
}

function startNewsMarquee(tickerEl) {
    if (tickerEl._marqueeFrame) {
        cancelAnimationFrame(tickerEl._marqueeFrame);
        tickerEl._marqueeFrame = null;
    }

    const track = tickerEl.querySelector(".news-track");
    if (!track) return;

    let offset = 0;
    const speed = 0.9;

    function resetOffset() {
        offset = tickerEl.clientWidth || window.innerWidth;
    }

    function step() {
        const loopWidth = track.scrollWidth / 2;
        if (loopWidth > 0) {
            offset -= speed;
            if (offset <= -loopWidth) {
                resetOffset();
            }
            track.style.transform = "translate3d(" + offset + "px, 0, 0)";
        }
        tickerEl._marqueeFrame = requestAnimationFrame(step);
    }

    requestAnimationFrame(() => {
        resetOffset();
        step();
    });
}

window.addEventListener("resize", () => {
    const ticker = document.getElementById("newsTicker");
    if (ticker && ticker.querySelector(".news-track")) {
        startNewsMarquee(ticker);
    }
});

function addFavorite(item) {
    const old = JSON.parse(localStorage.getItem("favorites") || "[]");
    old.push(item);
    localStorage.setItem("favorites", JSON.stringify(old));
    alert("已加入【用户收藏】");
}

function renderDistrictOptions(selectId, use9) {
    const arr = use9 ? DISTRICTS_9 : DISTRICTS_8;
    const sel = document.getElementById(selectId);
    if (!sel) return;
    sel.innerHTML = "<option value=''>请选择</option>" + arr.map(d => `<option value="${d}">${d}</option>`).join("");
}

window.addEventListener("scroll", () => {
    const y = window.scrollY;
    document.querySelectorAll(".scroll-animate").forEach((el, idx) => {
        el.style.transform = `translateY(${Math.min(14, y * (0.02 + idx * 0.005))}px)`;
    });
});
