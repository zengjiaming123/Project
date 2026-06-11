const DISTRICTS_9 = ["广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "江门", "肇庆"];
const DISTRICTS_8 = ["广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "江门"];

function renderNewsTicker(el, items) {
    const joined = items.map(t => "【楼市资讯】" + t).join("    ◆    ");
    el.innerHTML =
        '<div class="news-track">' +
        '<span class="news-group">' + joined + '</span>' +
        '<span class="news-group">' + joined + '</span>' +
        '</div>';
    startNewsMarquee(el);
}

function setNewsTicker(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;

    el.innerHTML = '<div class="news-track"><span class="news-group">【楼市资讯】正在加载数据库行情…</span></div>';

    fetch("api/news?count=8")
        .then(resp => resp.json())
        .then(data => {
            const items = (data.success && data.items && data.items.length)
                ? data.items
                : ["资讯加载失败，请刷新页面重试"];
            renderNewsTicker(el, items);
        })
        .catch(() => {
            renderNewsTicker(el, ["资讯加载失败，请确认数据库已启动后刷新页面"]);
        });
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
