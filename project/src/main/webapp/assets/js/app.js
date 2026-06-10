const DISTRICTS_9 = ["广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "江门", "肇庆"];
const DISTRICTS_8 = ["广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "江门"];

function setNewsTicker(elId) {
    const year = 2024 + Math.floor(Math.random() * 2);
    const month = 1 + Math.floor(Math.random() * 12);
    const district = DISTRICTS_9[Math.floor(Math.random() * DISTRICTS_9.length)];
    const yoy = (Math.random() * 6 - 1).toFixed(1);
    const avg = (1.0 + Math.random() * 2.5).toFixed(2);
    document.getElementById(elId).innerText =
        `${district}-${year}年${month}月均价同比${yoy}% ，当前均价约 ${avg} 万/平。`;
}

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
